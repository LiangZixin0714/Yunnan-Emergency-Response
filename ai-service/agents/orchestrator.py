"""LangGraph多Agent编排模块，串联情报分析、RAG检索、方案生成、方案审查四个Agent。"""

import logging
import sys
import os
from typing import Dict, List, Any, TypedDict

from langgraph.graph import StateGraph, END

# 添加项目根目录到sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from utils.logger import setup_logger
from rag.retriever import retrieve_plans
from agents.info_extractor import extract_incident_info
from agents.plan_generator import generate_plan
from agents.plan_reviewer import review_plan
from agents.resource_dispatcher import dispatch_resources

logger = setup_logger()


class AgentState(TypedDict):
    """LangGraph状态定义。"""
    description: str
    info: Dict[str, Any]
    rag_results: List[Dict[str, Any]]
    plan: str
    review: Dict[str, Any]
    resources: List[Dict[str, Any]]
    messages: List[str]


def _extract_info(state: AgentState) -> AgentState:
    """情报分析节点：从灾情描述中提取结构化信息。"""
    logger.info("🔹 LangGraph: 执行情报分析")
    
    try:
        info = extract_incident_info(state["description"])
        state["info"] = info
        state["messages"].append(f"情报分析完成: 类型={info.get('type')}, 等级={info.get('level')}, 位置={info.get('location')}")
    except Exception as e:
        logger.error(f"🔹 LangGraph: 情报分析失败: {e}")
        state["info"] = {
            "type": "其他",
            "level": "中",
            "location": "未知",
            "affected_population": None,
            "confidence": 0.0
        }
        state["messages"].append(f"情报分析失败: {e}")
    
    return state


def _retrieve_plans(state: AgentState) -> AgentState:
    """RAG检索节点：从向量数据库检索相关预案。"""
    logger.info("🔹 LangGraph: 执行RAG检索")
    
    try:
        # 使用灾情描述和提取的信息作为查询
        query = f"{state['description']} {state['info'].get('type', '')} {state['info'].get('location', '')}"
        rag_results = retrieve_plans(query, limit=5)
        state["rag_results"] = rag_results
        state["messages"].append(f"RAG检索完成，找到 {len(rag_results)} 条相关预案")
    except Exception as e:
        logger.error(f"🔹 LangGraph: RAG检索失败: {e}")
        state["rag_results"] = []
        state["messages"].append(f"RAG检索失败: {e}")
    
    return state


def _dispatch_resources(state: AgentState) -> AgentState:
    """资源调度节点：根据灾情信息调度资源。"""
    logger.info("🔹 LangGraph: 执行资源调度")
    
    try:
        resources = dispatch_resources(state["info"])
        state["resources"] = resources
        state["messages"].append(f"资源调度完成，推荐 {len(resources)} 种资源")
    except Exception as e:
        logger.error(f"🔹 LangGraph: 资源调度失败: {e}")
        state["resources"] = []
        state["messages"].append(f"资源调度失败: {e}")
    
    return state


def _generate_plan(state: AgentState) -> AgentState:
    """方案生成节点：生成完整应急处置方案。"""
    logger.info("🔹 LangGraph: 执行方案生成")
    
    try:
        plan = generate_plan(state["info"], state["rag_results"], state["description"])
        state["plan"] = plan
        state["messages"].append(f"方案生成完成，长度 {len(plan)} 字符")
    except Exception as e:
        logger.error(f"🔹 LangGraph: 方案生成失败: {e}")
        state["plan"] = f"方案生成失败: {e}"
        state["messages"].append(f"方案生成失败: {e}")
    
    return state


def _review_plan(state: AgentState) -> AgentState:
    """方案审查节点：审查方案合规性。"""
    logger.info("🔹 LangGraph: 执行方案审查")
    
    try:
        review = review_plan(state["plan"])
        state["review"] = review
        state["messages"].append(f"方案审查完成: 评分={review.get('score')}, 通过={review.get('passed')}")
    except Exception as e:
        logger.error(f"🔹 LangGraph: 方案审查失败: {e}")
        state["review"] = {
            "score": 5,
            "issues": [f"审查失败: {e}"],
            "passed": False
        }
        state["messages"].append(f"方案审查失败: {e}")
    
    return state


def _should_retry(state: AgentState) -> str:
    """决策节点：判断是否需要重试方案生成。
    
    如果方案审查未通过，且评分大于4分，则重试；否则直接结束。
    
    Returns:
        "generate_plan" 或 END
    """
    review = state.get("review", {})
    passed = review.get("passed", True)
    
    if not passed and review.get("score", 0) > 4:
        logger.info("🔹 LangGraph: 方案未通过，重试生成")
        state["messages"].append("方案未通过审查，重新生成...")
        return "generate_plan"
    
    logger.info("🔹 LangGraph: 方案审查通过或无需重试，流程结束")
    return END


def build_workflow() -> StateGraph:
    """构建LangGraph多Agent工作流。
    
    流程：情报分析 → RAG检索 → 资源调度 → 方案生成 → 方案审查 → 判断是否重试
    
    Returns:
        编译后的LangGraph workflow对象
    """
    logger.info("🔧 正在构建LangGraph工作流...")
    
    # 创建状态图
    workflow = StateGraph(AgentState)
    
    # 添加节点
    workflow.add_node("extract_info", _extract_info)
    workflow.add_node("retrieve_plans", _retrieve_plans)
    workflow.add_node("dispatch_resources", _dispatch_resources)
    workflow.add_node("generate_plan", _generate_plan)
    workflow.add_node("review_plan", _review_plan)
    
    # 设置起点
    workflow.set_entry_point("extract_info")
    
    # 添加边（线性流程）
    workflow.add_edge("extract_info", "retrieve_plans")
    workflow.add_edge("retrieve_plans", "dispatch_resources")
    workflow.add_edge("dispatch_resources", "generate_plan")
    workflow.add_edge("generate_plan", "review_plan")
    
    # 添加条件边（决策节点）
    workflow.add_conditional_edges(
        "review_plan",
        _should_retry,
        {
            "generate_plan": "generate_plan",
            END: END
        }
    )
    
    # 编译工作流
    app = workflow.compile()
    logger.info("✅ LangGraph工作流构建完成")
    
    return app


def run_workflow(description: str) -> Dict[str, Any]:
    """运行完整工作流。
    
    Args:
        description: 灾情描述文本
        
    Returns:
        包含所有结果的字典
    """
    logger.info(f"🚀 开始执行工作流，描述长度: {len(description)}")
    
    try:
        # 获取编译后的工作流
        app = build_workflow()
        
        # 初始化状态
        initial_state = {
            "description": description,
            "info": {},
            "rag_results": [],
            "plan": "",
            "review": {},
            "resources": [],
            "messages": ["工作流开始"]
        }
        
        # 执行工作流
        result = app.invoke(initial_state)
        
        logger.info("✅ 工作流执行完成")
        return result
        
    except Exception as e:
        logger.error(f"❌ 工作流执行失败: {e}")
        return {
            "description": description,
            "info": {},
            "rag_results": [],
            "plan": f"工作流执行失败: {e}",
            "review": {},
            "resources": [],
            "messages": [f"工作流执行失败: {e}"]
        }


if __name__ == "__main__":
    # 测试工作流
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
    
    print("\n🚚 推荐资源:")
    for resource in result["resources"]:
        print(f"  - {resource['name']}: {resource['quantity']} 单位")
    
    print("\n🔍 审查结果:")
    print(f"  评分: {result['review'].get('score')}")
    print(f"  通过: {result['review'].get('passed')}")
    
    print("\n📄 生成的方案:")
    print(result["plan"][:500] + "..." if len(result["plan"]) > 500 else result["plan"])