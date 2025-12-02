from typing import Generic, TypeVar,Optional

from pydantic import BaseModel
from enum import Enum

class ResponseStatus(Enum):
    success= "success"
    failed = "failed"
    #add more as fit

T = TypeVar('T')
class Response(BaseModel,Generic[T]):
    message:str  | None =  None
    status:ResponseStatus
    data: Optional[T] = None

def success_response( data: T = None) -> Response[T]:
    return Response(status=ResponseStatus.success, data=data)

def failed_response(message: str) -> Response[None]:
    return Response(message=message, status=ResponseStatus.failed)
