from fastapi import APIRouter

from app.Models.ProbleModel import ProbeModel
from app.schema.ResponseSchema import Response
from app.services.SystemInspector import SystemInspector

router = APIRouter(
    prefix="/proble",
    tags=["probe"],
    responses={404: {"description": "Not found"}},)

@router.post("/")
async def probe(probe_host: ProbeModel):
    system_inspector = SystemInspector(probe_host)
    probe_result = system_inspector.probe()
    if probe_result:
        res = Response.success_response(probe_result)
        return res
    res = Response.faild_response("failed to reach server")
    return res