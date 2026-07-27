"""AI服务审计客户端：在工作流执行完成后，异步向后端提交执行记录和引用来源。"""

import logging
import os
import uuid
import time
from datetime import datetime
from typing import Dict, Any, List, Optional

import httpx

from utils.logger import setup_logger

logger = setup_logger()

BACKEND_BASE_URL = os.environ.get("BACKEND_URL", "http://localhost:8080").rstrip("/")
AGENT_LOG_ENDPOINT = "/api/agent/log"
REQUEST_TIMEOUT = 5.0


def _format_time(time_val: Optional[str]) -> Optional[str]:
    """将ISO格式时间字符串转换为后端要求的 yyyy-MM-dd'T'HH:mm:ss 格式（去掉微秒）。"""
    if not time_val:
        return None
    try:
        dt = datetime.fromisoformat(time_val)
        return dt.strftime("%Y-%m-%dT%H:%M:%S")
    except (ValueError, TypeError):
        return time_val


async def submit_agent_run(
    run_id: str,
    incident_id: str,
    agent_name: str,
    input_params: str,
    output_result: str,
    status: str,
    error_message: Optional[str] = None,
    start_time: Optional[str] = None,
    end_time: Optional[str] = None,
    citations: Optional[List[Dict[str, Any]]] = None,
) -> None:
    """异步提交Agent执行记录到后端。失败时仅记录日志，不抛出异常。"""
    try:
        url = f"{BACKEND_BASE_URL}{AGENT_LOG_ENDPOINT}"
        logger.info(f"提交审计记录: runId={run_id}, url={url}, status={status}")
        payload = {
            "runId": run_id,
            "incidentId": incident_id,
            "agentName": agent_name,
            "inputParams": input_params,
            "outputResult": output_result,
            "status": status,
            "errorMessage": error_message,
            "startTime": _format_time(start_time),
            "endTime": _format_time(end_time),
            "citations": citations or [],
        }

        async with httpx.AsyncClient(timeout=REQUEST_TIMEOUT) as client:
            response = await client.post(
                url,
                json=payload,
            )
            if response.status_code == 200:
                logger.info(f"审计记录提交成功: runId={run_id}")
            else:
                logger.warning(f"审计记录提交失败: runId={run_id}, status={response.status_code}, body={response.text}")
    except Exception as e:
        logger.error(f"审计记录提交异常: runId={run_id}, error={e}")


def build_citations(retrieved_plans: List[Dict[str, Any]], run_id: str) -> List[Dict[str, Any]]:
    """将RAG检索结果转换为CitationItem列表格式。"""
    citations = []
    for plan in retrieved_plans:
        citation = {
            "citationId": str(uuid.uuid4()),
            "sourceText": plan.get("text", ""),
            "sourceUrl": plan.get("source", ""),
            "relevanceScore": plan.get("similarity", 0.0),
        }
        citations.append(citation)
    return citations