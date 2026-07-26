"""向量化路由模块，提供PDF向量化和向量删除接口。"""

import logging
from typing import Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

import sys
import os

_AI_SERVICE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
_PROJECT_ROOT = os.path.dirname(_AI_SERVICE_DIR)

sys.path.insert(0, _AI_SERVICE_DIR)

from services.vectorize_service import run_vectorize_pipeline


def _import_root_db():
    saved_utils = {k: v for k, v in sys.modules.items() if k == 'utils' or k.startswith('utils.')}
    for k in saved_utils:
        sys.modules.pop(k, None)
    if _AI_SERVICE_DIR in sys.path:
        sys.path.remove(_AI_SERVICE_DIR)
    sys.path.insert(0, _PROJECT_ROOT)
    import utils.db
    db_mod = sys.modules['utils.db']
    root_utils = {k: v for k, v in sys.modules.items() if k == 'utils' or k.startswith('utils.')}
    for k in root_utils:
        sys.modules.pop(k, None)
    sys.path.remove(_PROJECT_ROOT)
    sys.path.insert(0, _AI_SERVICE_DIR)
    for k, v in saved_utils.items():
        sys.modules[k] = v
    return db_mod

_db_mod = _import_root_db()
get_connection = _db_mod.get_connection

logger = logging.getLogger("ai-service")

router = APIRouter(prefix="/api/v1/knowledge", tags=["knowledge"])


class VectorizeRequest(BaseModel):
    objectKey: str
    bucket: str
    fileId: str
    fileName: str
    chunkSize: int = 800
    chunkOverlap: int = 100


class VectorizeResponse(BaseModel):
    status: str
    fileId: str
    chunkCount: int = 0
    failReason: str = ""


class DeleteVectorsRequest(BaseModel):
    sourceFile: str


class DeleteVectorsResponse(BaseModel):
    deletedCount: int


@router.post("/vectorize", response_model=VectorizeResponse)
async def vectorize_file(request: VectorizeRequest):
    """对指定PDF文件执行向量化处理。"""
    logger.info(f"收到向量化请求: fileId={request.fileId}, fileName={request.fileName}")
    result = run_vectorize_pipeline(
        object_key=request.objectKey,
        bucket=request.bucket,
        file_id=request.fileId,
        file_name=request.fileName,
        chunk_size=request.chunkSize,
        chunk_overlap=request.chunkOverlap,
    )
    return VectorizeResponse(**result)


@router.post("/vectors/delete", response_model=DeleteVectorsResponse)
async def delete_vectors(request: DeleteVectorsRequest):
    """删除指定文档的向量数据。"""
    logger.info(f"收到向量删除请求: sourceFile={request.sourceFile}")
    try:
        conn = get_connection()
        cur = conn.cursor()
        cur.execute('DELETE FROM knowledge_chunks WHERE document_name = %s', (request.sourceFile,))
        deleted_count = cur.rowcount
        conn.commit()
        cur.close()
        conn.close()
        logger.info(f"向量删除完成: deletedCount={deleted_count}")
        return DeleteVectorsResponse(deletedCount=deleted_count)
    except Exception as e:
        logger.error(f"向量删除失败: {e}")
        raise HTTPException(status_code=500, detail=f"向量删除失败: {e}")