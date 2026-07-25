"""云南自然灾害应急协同决策平台 - FastAPI主入口。"""

import os
import json
from typing import Dict, Any, AsyncGenerator

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, StreamingResponse
import uvicorn

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from utils.logger import setup_logger
from agents.orchestrator import run_workflow

logger = setup_logger()

app = FastAPI(title="云南自然灾害应急协同决策平台", version="1.0.0")

# CORS配置
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 全局变量
_workflow_ready = False


@app.on_event("startup")
async def startup():
    """启动时执行初始化操作。"""
    global _workflow_ready
    
    logger.info("🚀 正在启动AI服务...")
    
    try:
        from rag.retriever import check_database_status
        
        db_status = check_database_status()
        logger.info(f"📊 向量数据库状态: 已连接={db_status.get('connected')}, 表存在={db_status.get('table_exists')}, 数据量={db_status.get('row_count')}")
        
        _workflow_ready = True
        logger.info("✅ AI服务启动完成！")
        
    except Exception as e:
        logger.error(f"❌ 服务启动失败: {e}")
        _workflow_ready = False


@app.get("/health")
async def health() -> Dict[str, Any]:
    """健康检查接口。"""
    return {
        "status": "ok" if _workflow_ready else "degraded",
        "service": "ai-service",
        "workflow_ready": _workflow_ready
    }


@app.post("/api/v1/generate-plan")
async def generate_plan(request: Request) -> JSONResponse:
    """生成应急方案接口。
    
    请求体：{"description": "灾情描述文本"}
    响应：{"plan": "生成的方案文本"}
    """
    try:
        # 解析请求体
        data = await request.json()
        description = data.get("description", "")
        
        if not description or not description.strip():
            raise HTTPException(status_code=400, detail="description字段不能为空")
        
        logger.info(f"📥 收到方案生成请求，描述长度: {len(description)}")
        
        # 执行工作流
        result = run_workflow(description)
        
        # 提取方案文本
        plan = result.get("plan", "")
        
        if not plan:
            raise HTTPException(status_code=500, detail="方案生成失败")
        
        logger.info(f"📤 方案生成完成，返回方案长度: {len(plan)}")
        
        return JSONResponse(content={"plan": plan})
        
    except HTTPException as e:
        logger.error(f"❌ 请求参数错误: {e.detail}")
        return JSONResponse(
            content={"detail": e.detail},
            status_code=e.status_code
        )
    except Exception as e:
        logger.error(f"❌ 方案生成失败: {e}")
        return JSONResponse(
            content={"detail": f"方案生成失败: {str(e)}"},
            status_code=500
        )


@app.post("/api/v1/generate-plan/stream")
async def generate_plan_stream(request: Request) -> StreamingResponse:
    """生成应急方案接口（SSE流式输出）。
    
    请求体：{"description": "灾情描述文本"}
    响应：流式输出方案内容
    """
    try:
        # 解析请求体
        data = await request.json()
        description = data.get("description", "")
        
        if not description or not description.strip():
            raise HTTPException(status_code=400, detail="description字段不能为空")
        
        logger.info(f"📥 收到流式方案生成请求，描述长度: {len(description)}")
        
        async def stream_generator() -> AsyncGenerator[str, None]:
            """流式输出生成器。"""
            try:
                # 执行工作流
                result = run_workflow(description)
                plan = result.get("plan", "")
                
                if not plan:
                    yield f"data: {json.dumps({'error': '方案生成失败'})}\n\n"
                    return
                
                # 流式输出方案内容（按段落分割）
                paragraphs = plan.split('\n\n')
                
                for i, paragraph in enumerate(paragraphs):
                    if paragraph.strip():
                        chunk = {
                            "chunk": paragraph.strip(),
                            "index": i,
                            "total": len(paragraphs),
                            "done": i == len(paragraphs) - 1
                        }
                        yield f"data: {json.dumps(chunk, ensure_ascii=False)}\n\n"
                
            except Exception as e:
                logger.error(f"❌ 流式输出失败: {e}")
                yield f"data: {json.dumps({'error': str(e)})}\n\n"
        
        return StreamingResponse(
            stream_generator(),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache",
                "Connection": "keep-alive",
                "Content-Type": "text/event-stream"
            }
        )
        
    except HTTPException as e:
        logger.error(f"❌ 请求参数错误: {e.detail}")
        return JSONResponse(
            content={"detail": e.detail},
            status_code=e.status_code
        )
    except Exception as e:
        logger.error(f"❌ 流式方案生成失败: {e}")
        return JSONResponse(
            content={"detail": f"方案生成失败: {str(e)}"},
            status_code=500
        )


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """全局异常处理器。"""
    logger.error(f"❌ 未处理的异常: {exc}")
    return JSONResponse(
        content={"detail": "服务器内部错误"},
        status_code=500
    )


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8002))
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=True)