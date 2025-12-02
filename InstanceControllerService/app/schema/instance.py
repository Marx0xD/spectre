from datetime import datetime
from enum import Enum
from typing import Optional, Dict, Any

from pydantic import BaseModel,Field,HttpUrl


class Instance(BaseModel):
    instanceCreationStatus:str | None
    adminUserName:str | None
    adminPassword: str | None
    instanceDbAddress:str | None
    instanceAddress: str | None
    status: str | None

class InstanceDetail(BaseModel):
    container_instance:str
    module_path:str
    host:str

class InstanceInformation(BaseModel):
    db_name: str = ""
    instanceName: str = ""
    instanceDbAddress: int = ""
    configurationFileLocation: str = ""
    instanceAddress: int = ""
    custom_addons_path: str = ""
    status: bool  = False
class CreateInstanceMsg(BaseModel):
    instanceName:str
    moduleName:str
    modulePath:str
    host:str | None = None

class InstanceCreationResponse(BaseModel):
    """
    Pydantic model for the response after a Docker container instance creation.
    """
    status: str = Field(..., description="Overall status of the instance creation (True for Active, False otherwise).")
    adminUserName: str = Field(..., description="Administrator username for the created Odoo instance.")
    adminPassword: str = Field(..., description="Administrator password for the created Odoo instance.")
    instanceDbAddress: HttpUrl = Field(..., description="URL for accessing the database of the Odoo instance (e.g., http://localhost:5432).")
    instanceAddress: HttpUrl = Field(..., description="URL for accessing the Odoo instance (e.g., http://localhost:8069).")
    instanceName: str = Field(..., description="The name of the created Docker instance.")
    configurationFileLocation: str = Field(..., description="Path to the Docker Compose configuration file.")
    custom_addons_path: str = Field(..., description="The path to the custom addons directory for the instance.")


class InitializeHostMsg(BaseModel):
    host: str
    username: str
    password: str
    token: str
    callbackUrl: str
    idempotencyKey: str
    JobId: int
class JobStatus(str, Enum):
    PENDING = "PENDING"
    RUNNING = "RUNNING"
    SUCCEEDED = "SUCCEEDED"
    FAILED = "FAILED"


class Job(BaseModel):
    id: int
    idempotencyKey: str
    status: JobStatus
    message: Optional[str] = None
    createdAt: datetime
    updatedAt: datetime
    version: int
    callbackUrl: Optional[str] = None
    correlationKey: Optional[str] = None

class ProblemDetail(BaseModel):
    title: str
    detail: str
    status: int
    code: Optional[str] = None


class CallbackResult(BaseModel):
    success: bool
    job: Optional[Job] = None
    error: Optional[ProblemDetail] = None
    http_status: int
    raw_body: Dict[str, Any]

class JobUpdateIn(BaseModel):
    status: JobStatus             # RUNNING | QUEUED | SUCCEEDED | FAILED | ...
    progress: Optional[int] = 0   # 0..100 (optional)
    code: Optional[str] = None    # short phase/error code e.g., "PKG_INSTALL"
    message: Optional[str] = None # human-readable text
    data: Optional[Dict[str, Any]] = None  # arbitrary metadata

