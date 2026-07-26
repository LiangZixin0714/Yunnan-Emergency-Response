"""数据库操作模块，负责建表、插入和查询（使用 double precision[] 替代 pgvector）。"""

import json
from typing import Any, Optional

import psycopg2

from utils.logger import get_logger
from scripts.config import (
    PG_HOST,
    PG_PORT,
    PG_USER,
    PG_PASSWORD,
    PG_DB,
    PG_TABLE_NAME,
    PG_VECTOR_DIM,
    DB_RETRY_COUNT,
    DB_RETRY_INTERVAL,
)

logger = get_logger("db")

_CREATE_TABLE_SQL = """
CREATE TABLE IF NOT EXISTS {table} (
    id SERIAL PRIMARY KEY,
    chunk_id VARCHAR(255) UNIQUE NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    chapter VARCHAR(50) NOT NULL,
    section VARCHAR(50),
    page INTEGER NOT NULL,
    content TEXT NOT NULL,
    length INTEGER NOT NULL,
    "order" INTEGER NOT NULL,
    source VARCHAR(255) NOT NULL,
    publish_org VARCHAR(255) NOT NULL,
    publish_date VARCHAR(20),
    version VARCHAR(50) NOT NULL,
    embedding double precision[],
    model_name VARCHAR(100) NOT NULL DEFAULT 'BAAI/bge-small-zh-v1.5',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
"""

_CREATE_INDEX_SQL = """
CREATE INDEX IF NOT EXISTS idx_{table}_document_name ON {table}(document_name);
CREATE INDEX IF NOT EXISTS idx_{table}_version ON {table}(version);
CREATE INDEX IF NOT EXISTS idx_{table}_chunk_id ON {table}(chunk_id);
"""


def get_connection():
    """获取 PostgreSQL 连接，支持重试。

    Returns:
        psycopg2 连接对象。

    Raises:
        ConnectionError: 重试耗尽后仍无法连接。
    """
    import time

    last_err = None
    for attempt in range(1, DB_RETRY_COUNT + 1):
        try:
            conn = psycopg2.connect(
                host=PG_HOST,
                port=PG_PORT,
                user=PG_USER,
                password=PG_PASSWORD,
                dbname=PG_DB,
            )
            logger.info(f"PostgreSQL连接成功: {PG_HOST}:{PG_PORT}/{PG_DB}")
            return conn
        except Exception as e:
            last_err = e
            logger.warning(f"PostgreSQL连接失败(第{attempt}次): {e}")
            if attempt < DB_RETRY_COUNT:
                time.sleep(DB_RETRY_INTERVAL)

    raise ConnectionError(f"PostgreSQL连接失败，已重试{DB_RETRY_COUNT}次: {last_err}")


def ensure_table(conn) -> None:
    """确保目标表和索引存在。

    Args:
        conn: psycopg2 连接对象。
    """
    cur = conn.cursor()
    cur.execute(_CREATE_TABLE_SQL.format(table=PG_TABLE_NAME, dim=PG_VECTOR_DIM))
    for sql in _CREATE_INDEX_SQL.strip().split(";"):
        sql = sql.strip()
        if sql:
            cur.execute(sql.format(table=PG_TABLE_NAME))
    conn.commit()
    cur.close()
    logger.info(f"表 {PG_TABLE_NAME} 已就绪")


def insert_chunk(conn, chunk: dict[str, Any], embedding: list[float]) -> bool:
    """插入单条 Chunk 记录（含 Embedding 向量）。

    Args:
        conn: psycopg2 连接对象。
        chunk: 包含 Metadata 的 Chunk 字典。
        embedding: Embedding 向量列表。

    Returns:
        插入成功返回 True，失败返回 False。
    """
    try:
        cur = conn.cursor()
        cur.execute(
            f"""
            INSERT INTO {PG_TABLE_NAME}
                (chunk_id, document_name, document_type, chapter, section,
                 page, content, length, "order", source, publish_org,
                 publish_date, version, embedding, model_name)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (chunk_id) DO NOTHING
            """,
            (
                chunk["chunk_id"],
                chunk["document_name"],
                chunk.get("document_type", "综合应急预案"),
                chunk.get("chapter", "未知"),
                chunk.get("section", ""),
                chunk["page"],
                chunk["content"],
                chunk["length"],
                chunk["order"],
                chunk.get("source", "未知"),
                chunk.get("publish_org", "未知"),
                chunk.get("publish_date"),
                chunk.get("version", ""),
                embedding,
                "BAAI/bge-small-zh-v1.5",
            ),
        )
        conn.commit()
        cur.close()
        return True
    except Exception as e:
        conn.rollback()
        logger.error(f"SQL执行异常: chunk_id={chunk.get('chunk_id')}, 错误原因={e}")
        return False


def version_exists(conn, version: str) -> bool:
    """检查指定版本是否已存在数据。

    Args:
        conn: psycopg2 连接对象。
        version: 版本号字符串。

    Returns:
        存在返回 True，否则返回 False。
    """
    cur = conn.cursor()
    cur.execute(f"SELECT 1 FROM {PG_TABLE_NAME} WHERE version = %s LIMIT 1", (version,))
    result = cur.fetchone()
    cur.close()
    return result is not None


def chunk_exists(conn, chunk_id: str) -> bool:
    """检查指定 chunk_id 是否已存在。

    Args:
        conn: psycopg2 连接对象。
        chunk_id: Chunk 唯一标识。

    Returns:
        存在返回 True，否则返回 False。
    """
    cur = conn.cursor()
    cur.execute(f"SELECT 1 FROM {PG_TABLE_NAME} WHERE chunk_id = %s LIMIT 1", (chunk_id,))
    result = cur.fetchone()
    cur.close()
    return result is not None