# listeners.py
import asyncio, json, logging, contextlib
from concurrent.futures import ThreadPoolExecutor
from typing import Optional
from aiokafka import AIOKafkaConsumer, TopicPartition
from pydantic import ValidationError, BaseModel

from app.schema.DeploymentConfig import EnviromentSetupStatus
from app.schema.instance import CreateInstanceMsg, JobUpdateIn, JobStatus
from app.services.CallBackService import CallBackHandler
from app.services.create_container import ContainerManagementService
from app.config.config import Settings
from app.services.DeploymentEngine import RemoteDeploymentEngine
from app.schema.instance import InitializeHostMsg
logger = logging.getLogger("kafka-listener")

# ------------- Config -------------
S = Settings()
KAFKA_BOOTSTRAP = "127.0.0.1:9092"   # force IPv4
CREATE_TOPIC    = "create_instance"
CREATE_GROUP    = "container_manager_group"

# Make this EXACTLY what your producer uses ("initialize-server")
INIT_TOPIC      = S.initialize_host_topic  # expect "initialize-server"
INIT_GROUP      = "host_initializer_group_v5"  # bump during debugging to read from beginning
DEBUG_SEEK_BEGIN = False  # set True to force reading all historical messages

executor = ThreadPoolExecutor(max_workers=5)
callBackHandler = CallBackHandler()


async def _start_consumer(c: AIOKafkaConsumer, name: str, topic: str):
    logger.info("[%s] connecting to %s (topic=%r, group=%r)", name, KAFKA_BOOTSTRAP, topic, c._group_id)
    if not topic:
        raise RuntimeError(f"[{name}] EMPTY topic. Check Settings.initialize_host_topic")
    try:
        await asyncio.wait_for(c.start(), timeout=10)
    except Exception as e:
        logger.exception("[%s] FAILED to start: %s", name, e)
        raise
    logger.info("[%s] started OK", name)



async def consume_create_loop():
    consumer = AIOKafkaConsumer(
        CREATE_TOPIC,
        bootstrap_servers=KAFKA_BOOTSTRAP,
        group_id=CREATE_GROUP,
        auto_offset_reset="earliest",
        enable_auto_commit=True,
        value_deserializer=lambda v: v,  # bytes; we'll pass raw to Pydantic JSON
    )
    await _start_consumer(consumer, "create_instance", CREATE_TOPIC)
    try:

        async for msg in consumer:
            raw = msg.value  # bytes
            try:
                data = CreateInstanceMsg.model_validate_json(raw)
            except ValidationError as ve:
                logger.error("[create_instance] invalid payload: %s | raw=%r", ve, raw)
                continue

            loop = asyncio.get_running_loop()
            loop.run_in_executor(executor, _handle_create_instance, data)
    except asyncio.CancelledError:
        logger.info("[create_instance] loop cancelled, shutting down"); raise
    finally:
        await consumer.stop()
        logger.info("[create_instance] consumer stopped")

def _handle_create_instance(msg: CreateInstanceMsg):
    try:
        container_service = ContainerManagementService(
            instance_name=msg.instanceName,
            module_name=msg.moduleName,
            module_path=msg.modulePath,
        )
        result = container_service.create_container()
        logger.info("[create_instance] container created: %s", result)
    except Exception:
        logger.exception("[create_instance] error in create_container")

# ------------- INIT HOST CONSUMER -------------
async def consume_init_loop():
    logger.info("[init_host] INIT_TOPIC resolved to %r", INIT_TOPIC)
    consumer = AIOKafkaConsumer(
        INIT_TOPIC,
        bootstrap_servers=KAFKA_BOOTSTRAP,
        group_id=INIT_GROUP,
        auto_offset_reset="earliest",
        enable_auto_commit=True,
        value_deserializer=lambda v: v.decode("utf-8") if v is not None else None,  # human-readable logs
    )
    await _start_consumer(consumer, "init_host", INIT_TOPIC)
    try:
        async for msg in consumer:
            logger.info("[init_host] got: key=%r off=%s part=%s", msg.key, msg.offset, msg.partition)
            logger.info("[init_host] val: %r", msg.value)
            try:
                data = json.loads(msg.value)
                print(data)
                init_msg = InitializeHostMsg(**data)



            except Exception as e:
                logger.error("[init_host] parse failed: %s | raw=%r", e, msg.value)
                continue

            # Hand off to blocking work if needed
            loop = asyncio.get_running_loop()
            loop.run_in_executor(executor, _handle_initialize_host, init_msg,loop)
    except asyncio.CancelledError:
        logger.info("[init_host] loop cancelled, shutting down"); raise
    finally:
        await consumer.stop()
        logger.info("[init_host] consumer stopped")

def _handle_initialize_host(msg: InitializeHostMsg,loop):
    try:
        deployment_engine = RemoteDeploymentEngine(
            host=msg.host, username=msg.username, password=msg.password,idempotency_key=msg.idempotencyKey
        )
        result = deployment_engine.initialize_server()
        if result.status == EnviromentSetupStatus.success:
            jobUpdateStatus = JobUpdateIn(
                status=JobStatus.SUCCEEDED,
                progress=100,
                code="INSTANCE_CREATED",
                message="Instance created successfully",
                data=None
            )


        else:
            jobUpdateStatus = JobUpdateIn(
                status=JobStatus.FAILED,
                progress=100,
                code="INSTANCE_CREATED",
                message="Instance creation Faild",
                data=None
            )
            logger.info("Making request")
        loop.call_soon_threadsafe(
            asyncio.create_task,
            callBackHandler.make_request(
                payload=jobUpdateStatus.model_dump(),
                url=msg.callbackUrl,
                idempotency_key=msg.idempotencyKey,
                token=msg.token
            )
        )

        logger.info("[init_host] host initialized: %s", result)

    except Exception:
        logger.info("[init_host] host initialization faild")


_create_task: Optional[asyncio.Task] = None
_init_task: Optional[asyncio.Task] = None

async def _guard(coro, name: str):
    logger.info("Listener %s: scheduling", name)
    try:
        await coro
        logger.warning("Listener %s exited cleanly (unexpected)", name)
    except asyncio.CancelledError:
        logger.info("Listener %s: cancelled", name); raise
    except Exception:
        logger.exception("Listener %s crashed")

def start_listeners():
    """Call inside FastAPI lifespan; schedules background tasks."""
    global _create_task, _init_task
    loop = asyncio.get_running_loop()
    _create_task = loop.create_task(_guard(consume_create_loop(), "create"))
    _init_task   = loop.create_task(_guard(consume_init_loop(),   "init"))
    logger.info("Both Kafka listeners scheduled")

async def stop_listeners():
    global _create_task, _init_task
    for t in (_create_task, _init_task):
        if t and not t.done():
            t.cancel()
            with contextlib.suppress(asyncio.CancelledError):
                await t
    _create_task = _init_task = None
    logger.info("Both Kafka listeners stopped")