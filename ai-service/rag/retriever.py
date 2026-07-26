# """RAG检索模块，从pgvector数据库检索相关预案片段。"""

# import os
# import logging
# import sys
# from typing import List, Dict

# import psycopg2
# import torch
# from transformers import AutoModel, AutoTokenizer

# # 添加项目根目录到sys.path
# sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# from utils.logger import setup_logger

# logger = setup_logger()

# # 数据库连接配置（从环境变量读取，默认使用实际工作的连接）
# PG_HOST = os.environ.get("PG_HOST", "localhost")
# PG_PORT = int(os.environ.get("PG_PORT", "5432"))
# PG_DATABASE = os.environ.get("PG_DATABASE", "emergency_vector")
# PG_USER = os.environ.get("PG_USER", "postgres")
# PG_PASSWORD = os.environ.get("PG_PASSWORD", "ZAQ12wsx581!")

# # Embedding模型配置
# EMBEDDING_MODEL_NAME = "BAAI/bge-small-zh-v1.5"
# VECTOR_DIM = 512

# # 全局变量
# _embedding_model = None
# _embedding_tokenizer = None
# _connection = None


# def _get_embedding_model():
#     """获取Embedding模型和tokenizer实例（懒加载，使用transformers库）。"""
#     global _embedding_model, _embedding_tokenizer
#     if _embedding_model is None:
#         logger.info(f"🔄 正在加载Embedding模型: {EMBEDDING_MODEL_NAME}")
#         _embedding_tokenizer = AutoTokenizer.from_pretrained(
#             EMBEDDING_MODEL_NAME,
#             local_files_only=True
#         )
#         _embedding_model = AutoModel.from_pretrained(
#             EMBEDDING_MODEL_NAME,
#             local_files_only=True
#         )
#         _embedding_model = _embedding_model.to("cpu")
#         _embedding_model.eval()
#         logger.info("✅ Embedding模型加载成功！")
#     return _embedding_model, _embedding_tokenizer


# def _get_connection():
#     """获取PostgreSQL连接（懒加载）。"""
#     global _connection
#     if _connection is None:
#         try:
#             logger.info(f"🔄 正在连接数据库: {PG_HOST}:{PG_PORT}/{PG_DATABASE}")
#             _connection = psycopg2.connect(
#                 host=PG_HOST,
#                 port=PG_PORT,
#                 database=PG_DATABASE,
#                 user=PG_USER,
#                 password=PG_PASSWORD
#             )
#             logger.info("✅ 数据库连接成功！")
#         except Exception as e:
#             logger.error(f"❌ 数据库连接失败: {e}")
#             raise
#     return _connection


# # def _ensure_table():
# #     """确保表结构存在。"""
# #     conn = _get_connection()
# #     try:
# #         cur = conn.cursor()
# #         # 创建vector扩展（如果不存在）
# #         cur.execute("CREATE EXTENSION IF NOT EXISTS vector;")
        
# #         # 创建knowledge_chunks表（如果不存在）
# #         create_table_sql = """
# #         CREATE TABLE IF NOT EXISTS knowledge_chunks (
# #             id SERIAL PRIMARY KEY,
# #             chunk_id VARCHAR(255) UNIQUE NOT NULL,
# #             document_name VARCHAR(255) NOT NULL,
# #             document_type VARCHAR(50) NOT NULL,
# #             chapter VARCHAR(50) NOT NULL,
# #             section VARCHAR(50),
# #             page INTEGER NOT NULL,
# #             content TEXT NOT NULL,
# #             length INTEGER NOT NULL,
# #             "order" INTEGER NOT NULL,
# #             source VARCHAR(255) NOT NULL,
# #             publish_org VARCHAR(255) NOT NULL,
# #             publish_date VARCHAR(20),
# #             version VARCHAR(50) NOT NULL,
# #             embedding vector(512),
# #             model_name VARCHAR(100) NOT NULL DEFAULT 'BAAI/bge-small-zh-v1.5',
# #             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
# #         );
# #         """
# #         cur.execute(create_table_sql)
        
# #         # 创建索引（如果不存在）
# #         cur.execute("CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_embedding ON knowledge_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);")
        
# #         conn.commit()
# #         cur.close()
# #         logger.info("✅ 表结构检查完成")
# #     except Exception as e:
# #         conn.rollback()
# #         logger.error(f"❌ 表结构检查失败: {e}")
# #         raise


# # def generate_embedding(text: str) -> List[float]:
# #     """生成文本的Embedding向量（使用transformers库的深度学习模型）。
    
# #     Args:
# #         text: 输入文本
        
# #     Returns:
# #         Embedding向量列表
# #     """
# #     model, tokenizer = _get_embedding_model()
    
# #     inputs = tokenizer(
# #         text,
# #         padding=True,
# #         truncation=True,
# #         max_length=512,
# #         return_tensors="pt"
# #     )
    
# #     with torch.no_grad():
# #         outputs = model(**inputs)
# #         last_hidden_state = outputs.last_hidden_state
    
# #     attention_mask = inputs["attention_mask"]
# #     mask = attention_mask.unsqueeze(-1).expand(last_hidden_state.size()).float()
# #     sum_embeddings = torch.sum(last_hidden_state * mask, 1)
# #     sum_mask = torch.clamp(mask.sum(1), min=1e-9)
# #     embeddings = sum_embeddings / sum_mask
    
# #     return embeddings.squeeze().tolist()
# def _ensure_table():
#     """确保表结构存在。"""
#     conn = _get_connection()
#     try:
#         cur = conn.cursor()
#         # 创建vector扩展（如果不存在）
#         cur.execute("CREATE EXTENSION IF NOT EXISTS vector;")
        
#         # 创建knowledge_chunks表（如果不存在）
#         create_table_sql = """
#         CREATE TABLE IF NOT EXISTS knowledge_chunks (
#             id SERIAL PRIMARY KEY,
#             chunk_id VARCHAR(255) UNIQUE NOT NULL,
#             document_name VARCHAR(255) NOT NULL,
#             document_type VARCHAR(50) NOT NULL,
#             chapter VARCHAR(50) NOT NULL,
#             section VARCHAR(50),
#             page INTEGER NOT NULL,
#             content TEXT NOT NULL,
#             length INTEGER NOT NULL,
#             "order" INTEGER NOT NULL,
#             source VARCHAR(255) NOT NULL,
#             publish_org VARCHAR(255) NOT NULL,
#             publish_date VARCHAR(20),
#             version VARCHAR(50) NOT NULL,
#             embedding vector(512),
#             model_name VARCHAR(100) NOT NULL DEFAULT 'BAAI/bge-small-zh-v1.5',
#             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
#         );
#         """
#         cur.execute(create_table_sql)
        
#         # 【修改点】：注释掉或删掉这行会自动引发 ivfflat 冲突的索引创建逻辑
#         # cur.execute("CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_embedding ON knowledge_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);")
        
#         conn.commit()
#         cur.close()
#         logger.info("✅ 表结构检查完成")
#     except Exception as e:
#         conn.rollback()
#         logger.error(f"❌ 表结构检查失败: {e}")
#         raise


# def retrieve_plans(query: str, limit: int = 5) -> List[Dict[str, any]]:
#     """根据查询从向量数据库检索相关方案。
    
#     Args:
#         query: 查询文本
#         limit: 返回结果数量，默认5
        
#     Returns:
#         检索结果列表，每个元素包含text、similarity、plan_id、document_name等信息
#         如果数据库连接失败或其他异常，返回空列表，不影响主流程
#     """
#     try:
#         # 确保表结构存在
#         # _ensure_table()
        
#         # 生成查询向量
#         query_embedding = generate_embedding(query)
#         embedding_str = "[" + ",".join(str(v) for v in query_embedding) + "]"
        
#         # 执行相似度查询
#         conn = _get_connection()
#         cur = conn.cursor()
        
#         # 使用余弦距离查询（<=>表示向量余弦距离）
#         # 1 - cosine_distance = similarity_score
#         select_sql = """
#         SELECT 
#             chunk_id,
#             document_name,
#             document_type,
#             chapter,
#             section,
#             page,
#             content,
#             source,
#             publish_org,
#             version,
#             1 - (embedding <=> %s::vector) as similarity
#         FROM knowledge_chunks
#         ORDER BY embedding <=> %s::vector
#         LIMIT %s;
#         """
        
#         cur.execute(select_sql, (embedding_str, embedding_str, limit))
#         results = cur.fetchall()
#         cur.close()
        
#         # 转换结果格式
#         output = []
#         for row in results:
#             output.append({
#                 "plan_id": row[0],
#                 "document_name": row[1],
#                 "document_type": row[2],
#                 "chapter": row[3],
#                 "section": row[4],
#                 "page": row[5],
#                 "text": row[6],
#                 "source": row[7],
#                 "publish_org": row[8],
#                 "version": row[9],
#                 "similarity": float(row[10])
#             })
        
#         logger.info(f"🔍 RAG检索完成，查询: '{query[:30]}...'，返回 {len(output)} 条结果")
#         return output
        
#     except Exception as e:
#         logger.error(f"❌ RAG检索失败: {e}")
#         return []


# def check_database_status() -> Dict[str, any]:
#     """检查数据库状态。
    
#     Returns:
#         数据库状态信息
#     """
#     try:
#         conn = _get_connection()
#         cur = conn.cursor()
        
#         # 检查表是否存在
#         cur.execute("SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'knowledge_chunks');")
#         table_exists = cur.fetchone()[0]
        
#         # 检查数据量
#         if table_exists:
#             cur.execute("SELECT COUNT(*) FROM knowledge_chunks;")
#             row_count = cur.fetchone()[0]
            
#             cur.execute("SELECT vector_dims(embedding) FROM knowledge_chunks LIMIT 1;")
#             dim_result = cur.fetchone()
#             vector_dim = dim_result[0] if dim_result else 0
#         else:
#             row_count = 0
#             vector_dim = 0
            
#         cur.close()
        
#         return {
#             "connected": True,
#             "database": PG_DATABASE,
#             "table_exists": table_exists,
#             "row_count": row_count,
#             "vector_dim": vector_dim
#         }
        
#     except Exception as e:
#         return {
#             "connected": False,
#             "error": str(e)
#         }


# if __name__ == "__main__":
#     # 测试检索功能
#     status = check_database_status()
#     print(f"数据库状态: {status}")
    
#     if status["connected"]:
#         results = retrieve_plans("地震应急响应措施", limit=3)
#         print(f"\n检索结果:")
#         for i, result in enumerate(results, 1):
#             print(f"{i}. 相似度: {result['similarity']:.4f}, 文档: {result['document_name']}")
#             print(f"   内容: {result['text'][:100]}...\n")
"""RAG检索模块，从pgvector数据库检索相关预案片段。"""

import os
import logging
import sys
from typing import List, Dict

import psycopg2
import torch
from transformers import AutoModel, AutoTokenizer

# 添加项目根目录到sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from utils.logger import setup_logger

logger = setup_logger()

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
_embedding_tokenizer = None
_connection = None


def _get_embedding_model():
    """获取Embedding模型和tokenizer实例（懒加载，使用transformers库）。"""
    global _embedding_model, _embedding_tokenizer
    if _embedding_model is None:
        logger.info(f"🔄 正在加载Embedding模型: {EMBEDDING_MODEL_NAME}")
        _embedding_tokenizer = AutoTokenizer.from_pretrained(
            EMBEDDING_MODEL_NAME,
            local_files_only=True
        )
        _embedding_model = AutoModel.from_pretrained(
            EMBEDDING_MODEL_NAME,
            local_files_only=True
        )
        _embedding_model = _embedding_model.to("cpu")
        _embedding_model.eval()
        logger.info("✅ Embedding模型加载成功！")
    return _embedding_model, _embedding_tokenizer


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
    """确保 knowledge_chunks 表存在。"""
    conn = _get_connection()
    try:
        cur = conn.cursor()
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
            embedding double precision[],
            model_name VARCHAR(100) NOT NULL DEFAULT 'BAAI/bge-small-zh-v1.5',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        """
        cur.execute(create_table_sql)
        conn.commit()
        cur.close()
        logger.info("✅ knowledge_chunks 表结构检查完成")
    except Exception as e:
        conn.rollback()
        logger.error(f"❌ 表结构检查失败: {e}")
        raise


def generate_embedding(text: str) -> List[float]:
    """生成文本的Embedding向量（使用transformers库的深度学习模型）。
    
    Args:
        text: 输入文本
        
    Returns:
        Embedding向量列表
    """
    model, tokenizer = _get_embedding_model()
    
    inputs = tokenizer(
        text,
        padding=True,
        truncation=True,
        max_length=512,
        return_tensors="pt"
    )
    
    with torch.no_grad():
        outputs = model(**inputs)
        last_hidden_state = outputs.last_hidden_state
    
    attention_mask = inputs["attention_mask"]
    mask = attention_mask.unsqueeze(-1).expand(last_hidden_state.size()).float()
    sum_embeddings = torch.sum(last_hidden_state * mask, 1)
    sum_mask = torch.clamp(mask.sum(1), min=1e-9)
    embeddings = sum_embeddings / sum_mask
    
    return embeddings.squeeze().tolist()


def _cosine_similarity(v1: List[float], v2: List[float]) -> float:
    """计算两个向量的余弦相似度。"""
    import math
    dot = sum(a * b for a, b in zip(v1, v2))
    norm1 = math.sqrt(sum(a * a for a in v1))
    norm2 = math.sqrt(sum(b * b for b in v2))
    if norm1 == 0 or norm2 == 0:
        return 0.0
    return dot / (norm1 * norm2)


def retrieve_plans(query: str, limit: int = 5) -> List[Dict[str, any]]:
    """根据查询从向量数据库检索相关方案（Python端计算余弦相似度）。
    
    Args:
        query: 查询文本
        limit: 返回结果数量，默认5
        
    Returns:
        检索结果列表，每个元素包含text、similarity、plan_id、document_name等信息
        如果数据库连接失败或其他异常，返回空列表，不影响主流程
    """
    try:
        _ensure_table()
        
        query_embedding = generate_embedding(query)
        
        conn = _get_connection()
        cur = conn.cursor()
        
        cur.execute("""
            SELECT chunk_id, document_name, document_type, chapter, section,
                   page, content, source, publish_org, version, embedding
            FROM knowledge_chunks
            WHERE embedding IS NOT NULL
        """)
        results = cur.fetchall()
        cur.close()
        
        if not results:
            logger.warning("⚠️ knowledge_chunks 表中无数据，返回空结果")
            return []
        
        scored_results = []
        for row in results:
            stored_embedding = row[10]
            if stored_embedding is None:
                continue
            
            if isinstance(stored_embedding, str):
                import json
                stored_embedding = json.loads(stored_embedding)
            
            if len(stored_embedding) != len(query_embedding):
                continue
            
            similarity = _cosine_similarity(query_embedding, stored_embedding)
            
            scored_results.append({
                "plan_id": row[0],
                "document_name": row[1],
                "document_type": row[2],
                "chapter": row[3],
                "section": row[4],
                "page": row[5],
                "text": row[6],
                "source": row[7],
                "publish_org": row[8],
                "version": row[9],
                "similarity": similarity
            })
        
        scored_results.sort(key=lambda x: x["similarity"], reverse=True)
        top_results = scored_results[:limit]
        
        logger.info(f"🔍 RAG检索完成，查询: '{query[:30]}...'，共检索 {len(scored_results)} 条，返回 Top {len(top_results)} 条")
        return top_results
        
    except Exception as e:
        logger.error(f"❌ RAG检索失败: {e}")
        import traceback
        logger.error(traceback.format_exc())
        return []


def check_database_status() -> Dict[str, any]:
    """检查数据库状态。
    
    Returns:
        数据库状态信息
    """
    try:
        conn = _get_connection()
        cur = conn.cursor()
        
        cur.execute("SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'knowledge_chunks');")
        table_exists = cur.fetchone()[0]
        
        if table_exists:
            cur.execute("SELECT COUNT(*) FROM knowledge_chunks;")
            row_count = cur.fetchone()[0]
            
            cur.execute("SELECT embedding FROM knowledge_chunks WHERE embedding IS NOT NULL LIMIT 1;")
            dim_result = cur.fetchone()
            vector_dim = len(dim_result[0]) if dim_result and dim_result[0] else 0
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