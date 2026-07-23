"""RAG检索模块，从pgvector数据库检索相关预案片段。"""

import os
import logging
import sys
from typing import List, Dict

import psycopg2
from sentence_transformers import SentenceTransformer

# 添加项目根目录到sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from utils.logger import setup_logger

logger = setup_logger()

# 数据库连接配置（从环境变量读取，默认使用实际工作的连接）
PG_HOST = os.environ.get("PG_HOST", "localhost")
PG_PORT = int(os.environ.get("PG_PORT", "5432"))
PG_DATABASE = os.environ.get("PG_DATABASE", "emergency_vector")
PG_USER = os.environ.get("PG_USER", "postgres")
PG_PASSWORD = os.environ.get("PG_PASSWORD", "ZAQ12wsx581!")

# Embedding模型配置
EMBEDDING_MODEL_NAME = "BAAI/bge-small-zh-v1.5"
VECTOR_DIM = 512

# 全局变量
_embedding_model = None
_connection = None


def _get_embedding_model():
    """获取Embedding模型实例（懒加载）。"""
    global _embedding_model
    if _embedding_model is None:
        logger.info(f"🔄 正在加载Embedding模型: {EMBEDDING_MODEL_NAME}")
        os.environ["HF_ENDPOINT"] = "https://hf-mirror.com"
        os.environ["HF_HUB_DISABLE_XET"] = "1"
        _embedding_model = SentenceTransformer(EMBEDDING_MODEL_NAME)
        logger.info("✅ Embedding模型加载成功！")
    return _embedding_model


def _get_connection():
    """获取PostgreSQL连接（懒加载）。"""
    global _connection
    if _connection is None:
        try:
            logger.info(f"🔄 正在连接数据库: {PG_HOST}:{PG_PORT}/{PG_DATABASE}")
            _connection = psycopg2.connect(
                host=PG_HOST,
                port=PG_PORT,
                database=PG_DATABASE,
                user=PG_USER,
                password=PG_PASSWORD
            )
            logger.info("✅ 数据库连接成功！")
        except Exception as e:
            logger.error(f"❌ 数据库连接失败: {e}")
            raise
    return _connection


def _ensure_table():
    """确保表结构存在。"""
    conn = _get_connection()
    try:
        cur = conn.cursor()
        # 创建vector扩展（如果不存在）
        cur.execute("CREATE EXTENSION IF NOT EXISTS vector;")
        
        # 创建knowledge_chunks表（如果不存在）
        create_table_sql = """
        CREATE TABLE IF NOT EXISTS knowledge_chunks (
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
            embedding vector(512),
            model_name VARCHAR(100) NOT NULL DEFAULT 'BAAI/bge-small-zh-v1.5',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        """
        cur.execute(create_table_sql)
        
        # 创建索引（如果不存在）
        cur.execute("CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_embedding ON knowledge_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);")
        
        conn.commit()
        cur.close()
        logger.info("✅ 表结构检查完成")
    except Exception as e:
        conn.rollback()
        logger.error(f"❌ 表结构检查失败: {e}")
        raise


def generate_embedding(text: str) -> List[float]:
    """生成文本的Embedding向量。
    
    Args:
        text: 输入文本
        
    Returns:
        Embedding向量列表
    """
    model = _get_embedding_model()
    embedding = model.encode(text).tolist()
    return embedding


def retrieve_plans(query: str, limit: int = 5) -> List[Dict[str, any]]:
    """根据查询从向量数据库检索相关方案。
    
    Args:
        query: 查询文本
        limit: 返回结果数量，默认5
        
    Returns:
        检索结果列表，每个元素包含text、similarity、plan_id、document_name等信息
    """
    try:
        # 确保表结构存在
        _ensure_table()
        
        # 生成查询向量
        query_embedding = generate_embedding(query)
        embedding_str = "[" + ",".join(str(v) for v in query_embedding) + "]"
        
        # 执行相似度查询
        conn = _get_connection()
        cur = conn.cursor()
        
        # 使用余弦距离查询（<=>表示向量余弦距离）
        select_sql = """
        SELECT 
            chunk_id,
            document_name,
            document_type,
            chapter,
            content,
            1 - (embedding <=> %s::vector) as similarity
        FROM knowledge_chunks
        ORDER BY embedding <=> %s::vector
        LIMIT %s;
        """
        
        cur.execute(select_sql, (embedding_str, embedding_str, limit))
        results = cur.fetchall()
        cur.close()
        
        # 转换结果格式
        output = []
        for row in results:
            output.append({
                "plan_id": row[0],
                "document_name": row[1],
                "document_type": row[2],
                "chapter": row[3],
                "text": row[4],
                "similarity": float(row[5])
            })
        
        logger.info(f"🔍 RAG检索完成，查询: '{query[:30]}...'，返回 {len(output)} 条结果")
        return output
        
    except Exception as e:
        logger.error(f"❌ RAG检索失败: {e}")
        return []


def check_database_status() -> Dict[str, any]:
    """检查数据库状态。
    
    Returns:
        数据库状态信息
    """
    try:
        conn = _get_connection()
        cur = conn.cursor()
        
        # 检查表是否存在
        cur.execute("SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'knowledge_chunks');")
        table_exists = cur.fetchone()[0]
        
        # 检查数据量
        if table_exists:
            cur.execute("SELECT COUNT(*) FROM knowledge_chunks;")
            row_count = cur.fetchone()[0]
            
            cur.execute("SELECT vector_dims(embedding) FROM knowledge_chunks LIMIT 1;")
            dim_result = cur.fetchone()
            vector_dim = dim_result[0] if dim_result else 0
        else:
            row_count = 0
            vector_dim = 0
            
        cur.close()
        
        return {
            "connected": True,
            "database": PG_DATABASE,
            "table_exists": table_exists,
            "row_count": row_count,
            "vector_dim": vector_dim
        }
        
    except Exception as e:
        return {
            "connected": False,
            "error": str(e)
        }


if __name__ == "__main__":
    # 测试检索功能
    status = check_database_status()
    print(f"数据库状态: {status}")
    
    if status["connected"]:
        results = retrieve_plans("地震应急响应措施", limit=3)
        print(f"\n检索结果:")
        for i, result in enumerate(results, 1):
            print(f"{i}. 相似度: {result['similarity']:.4f}, 文档: {result['document_name']}")
            print(f"   内容: {result['text'][:100]}...\n")