import json
import socket

from aiokafka import AIOKafkaProducer

from app.schema.instance import CreateInstanceMsg

conf = {'bootstrap.servers': 'localhost:9092',
        'client.id': socket.gethostname(),
        'topic':"events"
        }
async def send_response(data:CreateInstanceMsg):
    producer = AIOKafkaProducer(bootstrap_servers=conf.get("bootstrap.servers"))
    await producer.start()
    try:
        response = await  producer.send_and_wait(conf.get("topic"),json.dumps(data.model_dump()).encode("utf-8"))
    except Exception as e:
        print(e)
    finally:
        await  producer.stop()

