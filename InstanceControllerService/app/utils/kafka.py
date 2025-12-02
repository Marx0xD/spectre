# app/utils/kafka.py
import asyncio
import contextlib
import logging
from concurrent.futures import ThreadPoolExecutor

from app.KafkaManager.intializeHostListener import (
    consume_create_loop,
    consume_init_loop,
)

logger = logging.getLogger("kafka-listener")

executor = ThreadPoolExecutor(max_workers=5)

_create_task: asyncio.Task | None = None
_init_task: asyncio.Task | None = None
_started = False

async def start_kafka() -> None:
    global _create_task, _init_task, _started
    if _started:
        logger.info("Kafka listeners already started")
        return
    loop = asyncio.get_running_loop()

    # wrap tasks to log crashes instead of dying silently
    async def _guard(coro, name: str):
        try:
            await coro
        except asyncio.CancelledError:
            raise
        except Exception as e:
            logger.exception("Kafka listener %s crashed: %s", name, e)

    _create_task = loop.create_task(_guard(consume_create_loop(), "create"))
    _init_task   = loop.create_task(_guard(consume_init_loop(), "init"))
    _started = True
    logger.info("Kafka listeners started")

async def stop_kafka() -> None:
    global _create_task, _init_task, _started
    for t in (_create_task, _init_task):
        if t and not t.done():
            t.cancel()
            with contextlib.suppress(asyncio.CancelledError):
                await t
    executor.shutdown(wait=False)
    _create_task = _init_task = None
    _started = False
    logger.info("Kafka listeners stopped")
