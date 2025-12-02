from typing import Annotated

from fastapi import APIRouter,Body

from pydantic import BaseModel

from app.services.DeploymentEngine import RemoteDeploymentEngine

router = APIRouter(
    prefix="/test"
)


class TestModel(BaseModel):
    host: str
    username:str
    password:str
@router.post("/init")
async def create_test_item(item:TestModel):
    res = RemoteDeploymentEngine(host=item.host,username=item.username,password=item.password)
    intialize = res.initialize_server()

@router.post("/dockerfiles")
async def create_docker_files(item:TestModel):
    dep_engine = RemoteDeploymentEngine(host=item.host,username=item.username,password=item.password)
    dep_data = await dep_engine.create_container_on_remote_server("test-container-1")
    print(dep_data)
    return  dep_data
@router.post("/install")
async def test_installation(item:TestModel):
    dep_engine = RemoteDeploymentEngine(host=item.host,username=item.username,password=item.password)
    dep_data = await dep_engine.create_container_on_remote_server("test-container-1")
    print(dep_data)
    return  dep_data