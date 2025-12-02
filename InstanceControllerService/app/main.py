import logging

from fastapi import FastAPI
from contextlib import asynccontextmanager

from app.KafkaManager.intializeHostListener import start_listeners, stop_listeners
from app.api.v1.endpoints import test_container_creation



#register routes later


@asynccontextmanager
async def lifespan(app:FastAPI):
    print(">>>> FASTAPI LIFESPAN STARTUP <<<<")
    logging.getLogger("kafka-listener").info("FastAPI lifespan: startup")
    start_listeners()
    yield
    await  stop_listeners()



app = FastAPI(lifespan=lifespan)
app.include_router(test_container_creation.router)

