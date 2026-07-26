"""数据处理流水线统一配置模块。"""

import os
import re

# ── 模拟数据配置 ──
USER_COUNT: int = 20
INCIDENT_COUNT: int = 50
RESOURCE_COUNT: int = 100
OUTPUT_DIR: str = "scripts"

USER_ROLE_DISTRIBUTION: dict[str, int] = {
    "admin": 2,
    "commander": 5,
    "reporter": 13,
}

DISASTER_TYPES: list[str] = [
    "earthquake",
    "mudslide",
    "flood",
    "drought",
    "landslide",
    "fire",
    "other",
]

INCIDENT_LEVELS: list[str] = ["I", "II", "III", "IV"]

INCIDENT_STATUSES: list[str] = ["pending", "processing", "completed"]

RESOURCE_TYPES: list[str] = ["team", "medical", "vehicle", "shelter"]

YUNNAN_REGIONS: list[str] = [
    "昆明市",
    "曲靖市",
    "玉溪市",
    "保山市",
    "昭通市",
    "丽江市",
    "普洱市",
    "临沧市",
    "楚雄彝族自治州",
    "红河哈尼族彝族自治州",
    "文山壮族苗族自治州",
    "西双版纳傣族自治州",
    "大理白族自治州",
    "德宏傣族景颇族自治州",
    "怒江傈僳族自治州",
    "迪庆藏族自治州",
]

DEFAULT_PASSWORD: str = "Test@1234"

# ── MySQL 配置 ──
MYSQL_HOST: str = os.environ.get("MYSQL_HOST", "localhost")
MYSQL_PORT: int = int(os.environ.get("MYSQL_PORT", "3306"))
MYSQL_USER: str = os.environ.get("MYSQL_USER", "root")
MYSQL_PASSWORD: str = os.environ.get("MYSQL_PASSWORD", "")
MYSQL_DB: str = os.environ.get("MYSQL_DB", "emergency_db")

# ── PostgreSQL / pgvector 配置 ──
PG_HOST: str = os.environ.get("PG_HOST", "localhost")
PG_PORT: int = int(os.environ.get("PG_PORT", "5432"))
PG_USER: str = os.environ.get("PG_USER", "postgres")
PG_PASSWORD: str = os.environ.get("PG_PASSWORD", "ZAQ12wsx581!")
PG_DB: str = os.environ.get("PG_DB", "emergency_vector")

PG_TABLE_NAME: str = "knowledge_chunks"
PG_VECTOR_DIM: int = 512

# ── PDF 解析配置 ──
PDF_DIR: str = os.environ.get("PDF_DIR", "data/pdf")
TXT_DIR: str = os.environ.get("TXT_DIR", "data/txt")
CHUNKS_DIR: str = os.environ.get("CHUNKS_DIR", "data/chunks")
EMBEDDINGS_DIR: str = os.environ.get("EMBEDDINGS_DIR", "data/embeddings")
LOGS_DIR: str = os.environ.get("LOGS_DIR", "data/logs")

# ── Chunk 切分配置 ──
CHUNK_SIZE: int = 800
CHUNK_OVERLAP: int = 100

# ── Embedding 模型配置 ──
EMBEDDING_MODEL_NAME: str = "BAAI/bge-small-zh-v1.5"
EMBEDDING_DIM: int = 512

# ── 知识库版本 ──
KB_VERSION: str = os.environ.get("KB_VERSION", "2026-07-22_v1")

# ── 文档类型映射 ──
DOCUMENT_TYPE_MAP: dict[str, str] = {
    "地震": "地震应急预案",
    "防汛": "山洪应急预案",
    "洪涝泥石流": "泥石流应急预案",
    "泥石流": "泥石流应急预案",
    "气象": "气象灾害应急预案",
    "森林": "森林火灾应急预案",
    "草原火灾": "森林火灾应急预案",
    "地质灾害": "综合应急预案",
    "自然灾害救助": "综合应急预案",
}

# ── 数据库重试配置 ──
DB_RETRY_COUNT: int = 3
DB_RETRY_INTERVAL: int = 5


def validate_config() -> None:
    """校验关键配置项的合法性，不合法时抛出 ValueError。"""
    if not (1 <= MYSQL_PORT <= 65535):
        raise ValueError(f"MySQL端口配置无效: {MYSQL_PORT}")
    if not (1 <= PG_PORT <= 65535):
        raise ValueError(f"PostgreSQL端口配置无效: {PG_PORT}")
    if CHUNK_SIZE <= 0:
        raise ValueError(f"Chunk Size必须为正整数: {CHUNK_SIZE}")
    if CHUNK_OVERLAP < 0:
        raise ValueError(f"Chunk Overlap不能为负数: {CHUNK_OVERLAP}")
    if CHUNK_OVERLAP >= CHUNK_SIZE:
        raise ValueError(f"Chunk Overlap({CHUNK_OVERLAP})必须小于Chunk Size({CHUNK_SIZE})")
    if not re.match(r"^\d{4}-\d{2}-\d{2}_v\d+$", KB_VERSION):
        raise ValueError(f"版本号格式错误: {KB_VERSION}，期望格式: YYYY-MM-DD_vN")
