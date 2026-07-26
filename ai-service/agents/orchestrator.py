"""LangGraph多Agent编排模块，串联情报分析、方案生成、方案审查三个Agent。"""

import logging
import sys
import os
import time
from typing import Dict, Any, TypedDict, List

from langgraph.graph import StateGraph, END

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from utils.logger import setup_logger
from agents.info_extractor import extract_incident_info
from agents.plan_generator import generate_plan, generate_plan_stream
from agents.plan_reviewer import review_plan
from agents.resource_dispatcher import dispatch_resources
from rag.retriever import retrieve_plans

logger = setup_logger()


MAX_RETRY_COUNT = 3

class AgentState(TypedDict):
    """LangGraph状态定义。"""
    description: str
    info: Dict[str, Any]
    retrieved_plans: List[Dict[str, Any]]
    resources: List[Dict[str, Any]]
    plan: str
    review: Dict[str, Any]
    messages: List[str]
    retry_count: int


def _extract_info(state: AgentState) -> AgentState:
    """情报分析节点：从灾情描述中提取结构化信息。"""
    logger.info("🔹 LangGraph: 执行情报分析")
    start_time = time.time()
    
    try:
        info = extract_incident_info(state["description"])
        elapsed = time.time() - start_time
        state["info"] = info
        state["messages"].append(f"情报分析完成: 类型={info.get('type')}, 等级={info.get('level')}, 位置={info.get('location')}, 耗时={elapsed:.2f}秒")
        logger.info(f"✅ 情报分析完成，耗时: {elapsed:.2f}秒")
    except Exception as e:
        elapsed = time.time() - start_time
        logger.error(f"🔹 LangGraph: 情报分析失败: {e}")
        state["info"] = {
            "type": "其他",
            "level": "中",
            "location": "未知",
            "affected_population": None,
            "confidence": 0.0
        }
        state["messages"].append(f"情报分析失败: {e}, 耗时={elapsed:.2f}秒")
    
    return state


def _retrieve_plans(state: AgentState) -> AgentState:
    """RAG检索节点：根据灾情信息检索相关预案。"""
    logger.info("🔹 LangGraph: 执行RAG检索")
    start_time = time.time()
    
    try:
        info = state.get("info", {})
        disaster_type = info.get("type", "")
        location = info.get("location", "")
        query = f"{disaster_type} {location} 应急处置"
        
        plans = retrieve_plans(query, limit=3)
        elapsed = time.time() - start_time
        state["retrieved_plans"] = plans
        
        if plans:
            logger.info(f"✅ RAG检索成功，返回 {len(plans)} 条预案，耗时: {elapsed:.2f}秒")
            plan_names = ", ".join([p["document_name"] for p in plans[:3]])
            state["messages"].append(f"RAG检索完成，找到 {len(plans)} 条相关预案: {plan_names}{'...' if len(plans) > 3 else ''}, 耗时={elapsed:.2f}秒")
        else:
            logger.info(f"🔹 RAG检索未找到相关预案，耗时: {elapsed:.2f}秒")
            state["messages"].append(f"RAG检索未找到相关预案，将基于通用知识生成方案，耗时={elapsed:.2f}秒")
    
    except Exception as e:
        elapsed = time.time() - start_time
        logger.error(f"❌ RAG检索失败: {e}, 耗时: {elapsed:.2f}秒")
        state["retrieved_plans"] = []
        state["messages"].append(f"RAG检索失败: {e}, 耗时={elapsed:.2f}秒")
    
    return state


def _dispatch_resources(state: AgentState) -> AgentState:
    """资源调度节点：根据灾情信息查询可用资源。"""
    logger.info("🔹 LangGraph: 执行资源调度")
    start_time = time.time()
    
    try:
        info = state.get("info", {})
        resources = dispatch_resources(info)
        elapsed = time.time() - start_time
        state["resources"] = resources
        
        if resources:
            resource_names = ", ".join([r["name"] for r in resources[:3]])
            logger.info(f"✅ 资源调度成功，返回 {len(resources)} 条资源，耗时: {elapsed:.2f}秒")
            state["messages"].append(f"资源调度完成，找到 {len(resources)} 条可用资源: {resource_names}{'...' if len(resources) > 3 else ''}, 耗时={elapsed:.2f}秒")
        else:
            logger.info(f"🔹 资源调度未找到可用资源，耗时: {elapsed:.2f}秒")
            state["messages"].append(f"资源调度未找到可用资源，将使用默认资源配置，耗时={elapsed:.2f}秒")
    
    except Exception as e:
        elapsed = time.time() - start_time
        logger.error(f"❌ 资源调度失败: {e}, 耗时: {elapsed:.2f}秒")
        state["resources"] = []
        state["messages"].append(f"资源调度失败: {e}, 耗时={elapsed:.2f}秒")
    
    return state


def _generate_plan(state: AgentState) -> AgentState:
    """方案生成节点：生成完整应急处置方案（结合RAG检索结果和资源调度结果）。"""
    logger.info("🔹 LangGraph: 执行方案生成")
    start_time = time.time()
    
    try:
        retrieved_plans = state.get("retrieved_plans", [])
        resources = state.get("resources", [])
        plan = generate_plan(state["info"], state["description"], retrieved_plans, resources)
        elapsed = time.time() - start_time
        state["plan"] = plan
        state["messages"].append(f"方案生成完成，长度 {len(plan)} 字符，耗时={elapsed:.2f}秒")
        logger.info(f"✅ 方案生成完成，方案长度: {len(plan)} 字符，耗时: {elapsed:.2f}秒")
    except Exception as e:
        elapsed = time.time() - start_time
        logger.error(f"❌ 方案生成失败: {e}, 耗时: {elapsed:.2f}秒")
        state["plan"] = f"方案生成失败: {e}"
        state["messages"].append(f"方案生成失败: {e}, 耗时={elapsed:.2f}秒")
    
    return state


def _review_plan(state: AgentState) -> AgentState:
    """方案审查节点：审查方案合规性。"""
    logger.info("🔹 LangGraph: 执行方案审查")
    start_time = time.time()
    
    try:
        review = review_plan(state["plan"])
        elapsed = time.time() - start_time
        state["review"] = review
        state["messages"].append(f"方案审查完成: 评分={review.get('score')}, 通过={review.get('passed')}, 耗时={elapsed:.2f}秒")
        logger.info(f"✅ 方案审查完成: score={review.get('score')}, 通过={review.get('passed')}, 耗时: {elapsed:.2f}秒")
    except Exception as e:
        elapsed = time.time() - start_time
        logger.error(f"❌ 方案审查失败: {e}, 耗时: {elapsed:.2f}秒")
        state["review"] = {
            "score": 5,
            "issues": [f"审查失败: {e}"],
            "passed": False
        }
        state["messages"].append(f"方案审查失败: {e}, 耗时={elapsed:.2f}秒")
    
    return state


def _should_retry(state: AgentState) -> str:
    """决策节点：判断是否需要重试方案生成。"""
    review = state.get("review", {})
    passed = review.get("passed", True)
    retry_count = state.get("retry_count", 0)
    
    if not passed and review.get("score", 0) > 4 and retry_count < MAX_RETRY_COUNT:
        state["retry_count"] = retry_count + 1
        logger.info(f"🔹 LangGraph: 方案未通过，重试生成 (第 {state['retry_count']}/{MAX_RETRY_COUNT} 次)")
        state["messages"].append(f"方案未通过审查，重新生成... (第 {state['retry_count']}/{MAX_RETRY_COUNT} 次)")
        return "generate_plan"
    
    logger.info("🔹 LangGraph: 方案审查通过或达到最大重试次数，流程结束")
    return END


def build_workflow() -> StateGraph:
    """构建LangGraph多Agent工作流。
    
    流程：情报分析 → RAG检索 → 资源调度 → 方案生成 → 方案审查 → 判断是否重试
    
    Returns:
        编译后的LangGraph workflow对象
    """
    logger.info("🔧 正在构建LangGraph工作流...")
    
    workflow = StateGraph(AgentState)
    
    workflow.add_node("extract_info", _extract_info)
    workflow.add_node("retrieve_plans", _retrieve_plans)
    workflow.add_node("dispatch_resources", _dispatch_resources)
    workflow.add_node("generate_plan", _generate_plan)
    workflow.add_node("review_plan", _review_plan)
    
    workflow.set_entry_point("extract_info")
    
    workflow.add_edge("extract_info", "retrieve_plans")
    workflow.add_edge("retrieve_plans", "dispatch_resources")
    workflow.add_edge("dispatch_resources", "generate_plan")
    workflow.add_edge("generate_plan", "review_plan")
    
    workflow.add_conditional_edges(
        "review_plan",
        _should_retry,
        {
            "generate_plan": "generate_plan",
            END: END
        }
    )
    
    app = workflow.compile()
    logger.info("✅ LangGraph工作流构建完成")
    
    return app


def run_workflow(description: str) -> Dict[str, Any]:
    """运行完整工作流。"""
    logger.info(f"🚀 开始执行工作流，描述长度: {len(description)}")
    total_start_time = time.time()
    
    try:
        app = build_workflow()
        
        initial_state = {
            "description": description,
            "info": {},
            "retrieved_plans": [],
            "resources": [],
            "plan": "",
            "review": {},
            "messages": ["工作流开始"],
            "retry_count": 0
        }
        
        result = app.invoke(initial_state)
        
        total_elapsed = time.time() - total_start_time
        result["messages"].append(f"工作流执行完成，总耗时={total_elapsed:.2f}秒")
        logger.info(f"✅ 工作流执行完成，总耗时: {total_elapsed:.2f}秒")
        return result
        
    except Exception as e:
        total_elapsed = time.time() - total_start_time
        logger.error(f"❌ 工作流执行失败: {e}, 耗时: {total_elapsed:.2f}秒")
        return {
            "description": description,
            "info": {},
            "plan": f"工作流执行失败: {e}",
            "review": {},
            "messages": [f"工作流执行失败: {e}, 耗时={total_elapsed:.2f}秒"]
        }


def run_workflow_stream(description: str):
    """运行流式工作流，返回生成器。"""
    logger.info(f"🚀 开始执行流式工作流，描述长度: {len(description)}")
    
    try:
        info = extract_incident_info(description)
        logger.info(f"✅ 情报分析完成")
        
        disaster_type = info.get("type", "")
        location = info.get("location", "")
        query = f"{disaster_type} {location} 应急处置"
        retrieved_plans = retrieve_plans(query, limit=3)
        logger.info(f"✅ RAG检索完成，返回 {len(retrieved_plans)} 条预案")
        
        resources = dispatch_resources(info)
        logger.info(f"✅ 资源调度完成，返回 {len(resources)} 条资源")
        
        for chunk in generate_plan_stream(info, description, retrieved_plans, resources):
            yield chunk
            
        logger.info(f"✅ 流式工作流执行完成")
        
    except Exception as e:
        logger.error(f"❌ 流式工作流执行失败: {e}")
        yield f"工作流执行失败: {e}"


if __name__ == "__main__":
    test_description = "云南省昆明市五华区发生4.5级地震，震源深度10公里，部分房屋受损，约500人受影响。"
    result = run_workflow(test_description)
    
    print("\n" + "=" * 60)
    print("工作流执行结果")
    print("=" * 60)
    
    print("\n📝 执行日志:")
    for msg in result["messages"]:
        print(f"  - {msg}")
    
    print("\n📋 情报分析:")
    print(f"  类型: {result['info'].get('type')}")
    print(f"  等级: {result['info'].get('level')}")
    print(f"  位置: {result['info'].get('location')}")
    
    print("\n🔍 审查结果:")
    print(f"  评分: {result['review'].get('score')}")
    print(f"  通过: {result['review'].get('passed')}")
    
    print("\n📄 生成的方案:")
    print(result["plan"][:500] + "..." if len(result["plan"]) > 500 else result["plan"])