from pydantic import BaseModel
from enum import Enum

class EnviromentSetupStatus(str, Enum):
    success="success"
    failed="failed"
    pending="pending"

class DeploymentConfig(BaseModel):
    host:str
    ssh_port:int=22
    hostname:str
    password:str
    mode:str


class DPResponseScheme(BaseModel):
    status:EnviromentSetupStatus | None = EnviromentSetupStatus.pending
    message:str | None  = "Something went wrong"

class DPInitializationResponseScheme(DPResponseScheme):
    config_dir:str | None = None


class DPContainerCreationResponseScheme(DPResponseScheme):
    app_port:str | None = None
    database_port:str | None = None
    instance_artifact_folder_path: str | None = None


