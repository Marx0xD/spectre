from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # version: str="0.0.0",
    host:str="localhost"
    kafka_port:str="9092"
    create_container:str="create_instance"
    produce_response:str="create_response"
    initialize_host_topic:str="initialize-server"

settings = Settings()
