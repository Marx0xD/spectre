from fastapi import APIRouter
from pydantic import BaseModel,Field
router = APIRouter()



@router.post("/manager/create")
async def create_instance():
    pass
