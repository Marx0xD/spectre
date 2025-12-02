import asyncio
import json
import logging
from aiokafka import AIOKafkaConsumer

from app.schema.instance import CreateInstanceMsg
from app.services.create_container import ContainerManagementService
from pydantic import BaseModel,ValidationError
from concurrent.futures import ThreadPoolExecutor
import contextlib
logger = logging.getLogger("kakfa-listener")


KAFKA_BOOTSTRAP = "localhost:9092"
TOPIC             = "create_instance"
GROUP_ID          = "container_manager_group"

executor = ThreadPoolExecutor(max_workers=5)



async def consume_loop():
    consumer = AIOKafkaConsumer(
        TOPIC,
        bootstrap_servers=KAFKA_BOOTSTRAP,
        group_id=GROUP_ID,
        auto_offset_reset="earliest",
    )
    await consumer.start()
    logger.info("Kafka consumer started for topic %r", TOPIC)

    try:
        async for msg in consumer:
            raw = msg.value
            # offload CPU-bound work to the thread pool
            try:
                data = CreateInstanceMsg.model_validate_json(raw)
            except ValidationError as ve:
                logger.error("Invalid message payload: %s", ve)
                continue

            # run your blocking container-creation in a thread
            loop = asyncio.get_running_loop()
            loop.run_in_executor(
                executor,
                _handle_create_instance,
                data,
            )
    except asyncio.CancelledError:
        logger.info("Consume loop cancelled, shutting down")
    finally:
        await consumer.stop()
        executor.shutdown(wait=False)
        logger.info("Kafka consumer stopped")

def _handle_create_instance(msg: CreateInstanceMsg):
    try:
        container_service = ContainerManagementService(
            instance_name=msg.instanceName,
            module_name=msg.moduleName,
            module_path = msg.modulePath
        )
        result = container_service.create_container()
        #sendResponse(result)
        logger.info("creating container",result)
    except Exception:
        logger.exception("Error in create_container")

# 3) Helpers to start/stop from FastAPI
