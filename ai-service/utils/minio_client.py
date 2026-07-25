"""MinIO客户端工具模块，从MinIO下载文件到本地临时目录。"""

import os
import tempfile
import logging

from minio import Minio

logger = logging.getLogger("ai-service")

MINIO_ENDPOINT = os.environ.get("MINIO_ENDPOINT", "localhost:9000")
MINIO_ACCESS_KEY = os.environ.get("MINIO_ACCESS_KEY", "admin")
MINIO_SECRET_KEY = os.environ.get("MINIO_SECRET_KEY", "ZAQ12wsx581!")
MINIO_SECURE = os.environ.get("MINIO_SECURE", "false").lower() == "true"

_client = None


def _get_client() -> Minio:
    global _client
    if _client is None:
        _client = Minio(
            MINIO_ENDPOINT,
            access_key=MINIO_ACCESS_KEY,
            secret_key=MINIO_SECRET_KEY,
            secure=MINIO_SECURE,
        )
        logger.info(f"MinIO客户端已初始化: {MINIO_ENDPOINT}")
    return _client


def download_file(bucket: str, object_key: str) -> str:
    """从MinIO下载文件到系统临时目录。

    Args:
        bucket: MinIO bucket名称。
        object_key: MinIO对象键。

    Returns:
        临时文件路径。

    Raises:
        Exception: 下载失败时抛出异常。
    """
    client = _get_client()
    suffix = os.path.splitext(object_key)[1] or ".pdf"
    fd, temp_path = tempfile.mkstemp(suffix=suffix)
    os.close(fd)

    try:
        client.fget_object(bucket, object_key, temp_path)
        logger.info(f"MinIO文件已下载: {bucket}/{object_key} -> {temp_path}")
        return temp_path
    except Exception as e:
        if os.path.exists(temp_path):
            os.remove(temp_path)
        logger.error(f"MinIO文件下载失败: {bucket}/{object_key} - {e}")
        raise