from fastapi import APIRouter

from app.Models.ProbleModel import ProbeModel
from app.schema.DeploymentConfig import SSHReachabilityRequest
from app.schema.ResponseSchema import Response
from app.services.SystemInspector import SystemInspector

router = APIRouter(
    prefix="/proble",
    tags=["probe"],
    responses={404: {"description": "Not found"}},)

@router.post("/")
async def probe(probe_host: ProbeModel):
    system_inspector = SystemInspector(probe_host)
    probe_result = system_inspector.host_bootstrap_inspection()
    if probe_result:
        res = Response.success_response(probe_result)
        return res
    res = Response.faild_response("failed to reach server")
    return res
@router.post("/ping")
async def ssh_reachability(data:SSHReachabilityRequest):
    print(data)
    system_inspector = SystemInspector(None)
    ssh_result = system_inspector.verify_ssh_reachable(host=data.host,port=data.ssh_port)
    return ssh_result