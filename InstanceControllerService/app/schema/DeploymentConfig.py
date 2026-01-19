from pydantic import BaseModel
from enum import Enum

from typing import Optional, Literal
from pydantic import BaseModel, Field


class SSHReachabilityRequest(BaseModel):
    host: str = Field(
        ...,
        description="Hostname or IP address to check SSH reachability against."
    )

    ssh_port: int = Field(
        22,
        ge=1,
        le=65535,
        description="SSH port to check. Defaults to 22."
    )
SSHReachabilityReason = Literal[
    "ok",
    "connection_refused",
    "timeout",
    "not_ssh",
    "unknown_error",
]


class SSHReachabilityResult(BaseModel):
    reachable: bool = Field(
        ...,
        description="Final verdict: true if an SSH server is reachable on the given port."
    )

    reason: SSHReachabilityReason = Field(
        ...,
        description="High-level reason for the result. Closed set, protocol-level."
    )

    ssh_port: int = Field(
        ...,
        description="SSH port that was checked."
    )

    banner: Optional[str] = Field(
        None,
        description="SSH identification banner if received (e.g. 'SSH-2.0-OpenSSH_9.3')."
    )

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


