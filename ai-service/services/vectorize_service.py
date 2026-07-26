"""向量化流水线服务，封装PDF解析→切片→Embedding→pgvector入库的完整流程。"""

import os
import sys
import time
import logging
import importlib
from typing import Optional

_AI_SERVICE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
_PROJECT_ROOT = os.path.dirname(_AI_SERVICE_DIR)

sys.path.insert(0, _AI_SERVICE_DIR)

from utils.minio_client import download_file

_ai_service_utils = sys.modules.get('utils')


def _import_root_utils():
    saved_utils = {k: v for k, v in sys.modules.items() if k == 'utils' or k.startswith('utils.')}
    for k in saved_utils:
        sys.modules.pop(k, None)

    if _AI_SERVICE_DIR in sys.path:
        sys.path.remove(_AI_SERVICE_DIR)
    sys.path.insert(0, _PROJECT_ROOT)

    import utils.pdf_parser
    import utils.embedding
    import utils.db

    pdf_parser = sys.modules['utils.pdf_parser']
    embedding_mod = sys.modules['utils.embedding']
    db_mod = sys.modules['utils.db']

    root_utils = {k: v for k, v in sys.modules.items() if k == 'utils' or k.startswith('utils.')}
    for k in root_utils:
        sys.modules.pop(k, None)

    sys.path.remove(_PROJECT_ROOT)
    sys.path.insert(0, _AI_SERVICE_DIR)

    for k, v in saved_utils.items():
        sys.modules[k] = v

    return pdf_parser, embedding_mod, db_mod


_pdf_parser, _embedding_mod, _db_mod = _import_root_utils()

extract_text_from_pdf = _pdf_parser.extract_text_from_pdf
generate_embedding = _embedding_mod.generate_embedding
get_connection = _db_mod.get_connection
ensure_table = _db_mod.ensure_table
insert_chunk = _db_mod.insert_chunk

logger = logging.getLogger("ai-service")


def _chunk_text(text: str, chunk_size: int = 800, chunk_overlap: int = 100) -> list[str]:
    """将文本按固定大小切分为知识块。

    Args:
        text: 待切分的文本。
        chunk_size: 每个知识块的最大字符数。
        chunk_overlap: 相邻知识块的重叠字符数。

    Returns:
        知识块列表。
    """
    if not text or not text.strip():
        return []

    chunks = []
    start = 0
    while start < len(text):
        end = start + chunk_size
        chunk = text[start:end]
        if chunk.strip():
            chunks.append(chunk.strip())
        start += chunk_size - chunk_overlap

    return chunks


def run_vectorize_pipeline(
    object_key: str,
    bucket: str,
    file_id: str,
    file_name: str,
    chunk_size: int = 800,
    chunk_overlap: int = 100,
) -> dict:
    """执行完整的向量化流水线。

    Args:
        object_key: MinIO对象键。
        bucket: MinIO bucket名称。
        file_id: 文件唯一标识。
        file_name: 原始文件名。
        chunk_size: 切片大小。
        chunk_overlap: 切片重叠。

    Returns:
        结果字典: {status, fileId, chunkCount, failReason}
    """
    temp_path = None
    conn = None

    try:
        # 阶段1 - 下载PDF
        t0 = time.time()
        try:
            temp_path = download_file(bucket, object_key)
        except Exception as e:
            logger.error(f"[阶段1-下载PDF] 失败: {e}")
            return {"status": "failed", "fileId": file_id, "chunkCount": 0, "failReason": f"源文件不存在或下载失败: {e}"}
        logger.info(f"[阶段1-下载PDF] 完成, 耗时{time.time()-t0:.1f}s")

        # 阶段2 - 解析PDF
        t0 = time.time()
        text = extract_text_from_pdf(temp_path)
        if text is None:
            logger.error("[阶段2-解析PDF] 失败: 文件无法提取有效文本")
            return {"status": "failed", "fileId": file_id, "chunkCount": 0, "failReason": "PDF解析失败：文件无法提取有效文本"}
        logger.info(f"[阶段2-解析PDF] 完成, 文本长度={len(text)}, 耗时{time.time()-t0:.1f}s")

        # 阶段3 - 文本切片
        t0 = time.time()
        chunks = _chunk_text(text, chunk_size, chunk_overlap)
        if not chunks:
            return {"status": "failed", "fileId": file_id, "chunkCount": 0, "failReason": "PDF解析失败：切片后无有效内容"}
        if len(chunks) > 500:
            logger.warning(f"[阶段3-文本切片] 知识块数量({len(chunks)})超过500，截断")
            chunks = chunks[:500]
        logger.info(f"[阶段3-文本切片] 完成, 知识块数={len(chunks)}, 耗时{time.time()-t0:.1f}s")

        # 阶段4+5 - Embedding生成 + pgvector入库
        t0 = time.time()
        try:
            conn = get_connection()
        except ConnectionError as e:
            return {"status": "failed", "fileId": file_id, "chunkCount": 0, "failReason": f"向量入库失败：数据库连接异常: {e}"}

        ensure_table(conn)

        success_count = 0
        fail_count = 0
        for i, chunk_content in enumerate(chunks):
            chunk_id = f"{file_id}_{i}"
            chunk_dict = {
                "chunk_id": chunk_id,
                "document_name": file_name,
                "document_type": "综合应急预案",
                "chapter": "未知",
                "section": "",
                "page": 1,
                "content": chunk_content,
                "length": len(chunk_content),
                "order": i,
                "source": "未知",
                "publish_org": "未知",
                "publish_date": None,
                "version": file_id,
            }

            embedding = generate_embedding(chunk_content)
            if embedding is None:
                logger.error(f"[阶段4-Embedding] 生成失败: chunk_id={chunk_id}")
                fail_count += 1
                continue

            if insert_chunk(conn, chunk_dict, embedding):
                success_count += 1
            else:
                fail_count += 1

        logger.info(f"[阶段4+5-Embedding+入库] 完成, 成功={success_count}, 失败={fail_count}, 耗时{time.time()-t0:.1f}s")

        if success_count == 0:
            return {"status": "failed", "fileId": file_id, "chunkCount": 0, "failReason": "Embedding生成失败或向量入库失败"}

        return {"status": "completed", "fileId": file_id, "chunkCount": success_count, "failReason": ""}

    except Exception as e:
        logger.error(f"向量化流水线异常: {e}", exc_info=True)
        return {"status": "failed", "fileId": file_id, "chunkCount": 0, "failReason": f"向量化处理异常: {e}"}

    finally:
        if temp_path and os.path.exists(temp_path):
            try:
                os.remove(temp_path)
                logger.info(f"临时文件已清理: {temp_path}")
            except Exception:
                pass
        if conn:
            try:
                conn.close()
            except Exception:
                pass