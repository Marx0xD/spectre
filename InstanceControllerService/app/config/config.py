from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # version: str="0.0.0",
    host:str="localhost"
    kafka_port:str="9092"
    create_container:str="create_instance"
    produce_response:str="create_response"
    initialize_host_topic:str="initialize-server"
    KDF_SALT:bytes = b"spectrum-ssh-v0"
    KDF_ITERATIONS:int = 120_000
    KEY_LENGTH_BITS:int = 256
    NONCE_LEN:int = 12  # 96 bits
    TAG_LEN:int = 16  # 128 bits (GCM)


settings = Settings()
