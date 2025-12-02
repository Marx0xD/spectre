import aiokafka
from aiokafka import AIOKafkaProducer
from app.config.config import settings
from app.schema.instance import InstanceCreationResponse

producer = aiokafka.AIOKafkaProducer(bootstrap_servers=f"{settings.host}:{settings.kafka_port}")

async def create_response(data:InstanceCreationResponse):
    data_json = data.model_dump()
    await producer.start()
    try:
        await producer.send_and_wait(settings.produce_response,data_json,partition=1)
    except Exception as e:
        print(e)
    finally:
       await producer.stop()
    pass