"""Emergency Platform AI Service - FastAPI Main Entry."""

import os
import json
from typing import Dict, Any, AsyncGenerator

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, StreamingResponse
import uvicorn

import sys
import os

_AI_DIR = os.path.dirname(os.path.abspath(__file__))
_ROOT_DIR = os.path.dirname(_AI_DIR)

sys.path.insert(0, _ROOT_DIR)
sys.path.insert(0, _AI_DIR)

from utils.logger import setup_logger
from agents.orchestrator import run_workflow, run_workflow_stream
from middleware.api_key_auth import ApiKeyAuthMiddleware
from routers.vectorize_router import router as vectorize_router

logger = setup_logger()

app = FastAPI(title="Emergency Platform AI Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.add_middleware(ApiKeyAuthMiddleware)
app.include_router(vectorize_router)

_workflow_ready = False


@app.on_event("startup")
async def startup():
    global _workflow_ready
    logger.info("Starting AI service...")
    logger.info(f"环境变量: MINIO_ENDPOINT={os.environ.get('MINIO_ENDPOINT', '未设置')}, PG_HOST={os.environ.get('PG_HOST', '未设置')}, BACKEND_URL={os.environ.get('BACKEND_URL', '未设置')}")
    try:
        from rag.retriever import check_database_status
        db_status = check_database_status()
        logger.info(f"DB status: connected={db_status.get('connected')}, table_exists={db_status.get('table_exists')}, row_count={db_status.get('row_count')}")
        _workflow_ready = True
        logger.info("AI service started!")
    except Exception as e:
        logger.error(f"Startup failed: {e}")
        _workflow_ready = False


@app.get("/health")
async def health() -> Dict[str, Any]:
    return {"status": "ok" if _workflow_ready else "degraded", "service": "ai-service", "workflow_ready": _workflow_ready}


@app.post("/api/v1/generate-plan")
async def generate_plan(request: Request) -> JSONResponse:
    try:
        data = await request.json()
        description = data.get("description", "")
        incident_id = data.get("incidentId", "")
        if not description or not description.strip():
            raise HTTPException(status_code=400, detail="description required")
        logger.info(f"Plan request, desc len: {len(description)}")
        result = run_workflow(description, incident_id=incident_id)
        plan = result.get("plan", "")
        if not plan:
            raise HTTPException(status_code=500, detail="Plan generation failed")
        logger.info(f"Plan generated, len: {len(plan)}")
        return JSONResponse(content={"plan": plan})
    except HTTPException as e:
        return JSONResponse(content={"detail": e.detail}, status_code=e.status_code)
    except Exception as e:
        logger.error(f"Plan generation failed: {e}")
        return JSONResponse(content={"detail": f"Plan generation failed: {str(e)}"}, status_code=500)


@app.post("/api/v1/generate-plan/stream")
async def generate_plan_stream(request: Request) -> StreamingResponse:
    try:
        data = await request.json()
        description = data.get("description", "")
        incident_id = data.get("incidentId", "")
        if not description or not description.strip():
            raise HTTPException(status_code=400, detail="description required")
        logger.info(f"Stream plan request, desc len: {len(description)}")

        async def stream_generator() -> AsyncGenerator[str, None]:
            try:
                for chunk in run_workflow_stream(description, incident_id=incident_id):
                    if chunk:
                        yield f"data: {json.dumps({'chunk': chunk}, ensure_ascii=False)}\n\n"
                yield 'data: {"done": true}\n\n'
            except Exception as e:
                logger.error(f"Stream failed: {e}")
                yield f"data: {json.dumps({'error': str(e)})}\n\n"

        return StreamingResponse(
            stream_generator(),
            media_type="text/event-stream",
            headers={"Cache-Control": "no-cache", "Connection": "keep-alive", "Content-Type": "text/event-stream"}
        )
    except HTTPException as e:
        return JSONResponse(content={"detail": e.detail}, status_code=e.status_code)
    except Exception as e:
        logger.error(f"Stream plan failed: {e}")
        return JSONResponse(content={"detail": f"Plan generation failed: {str(e)}"}, status_code=500)


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    logger.error(f"Unhandled exception: {exc}")
    return JSONResponse(content={"detail": "Internal server error"}, status_code=500)


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8002))
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=True)
