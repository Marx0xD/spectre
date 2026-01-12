from pydantic import BaseModel


class ProbeModel(BaseModel):
    hostname: str
    ipAddress:str
    sshUsername: str
    sshPassword: str