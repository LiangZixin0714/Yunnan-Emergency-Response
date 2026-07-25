"""向量化路由模块，提供PDF向量化和向量删除接口。"""

import logging
from typing import Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from services.vectorize_service import run_vectorize_pipeline
from utils.db import get_connection

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


@router.delete("/vectors", response_model=DeleteVectorsResponse)
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