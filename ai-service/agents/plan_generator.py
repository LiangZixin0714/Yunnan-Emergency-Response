"""方案生成Agent，结合情报分析结果和原始描述，生成完整处置方案。"""

import logging
import sys
import os
from typing import Dict, Any, List

from openai import OpenAI

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from utils.logger import setup_logger

logger = setup_logger()

# vLLM配置
VLLM_BASE_URL = os.environ.get("VLLM_BASE_URL", "http://127.0.0.1:8000/v1")
VLLM_API_KEY = os.environ.get("VLLM_API_KEY", "EMPTY")

_client = OpenAI(
    base_url=VLLM_BASE_URL,
    api_key=VLLM_API_KEY,
    timeout=120.0
)


def _build_prompt(info: Dict[str, Any], description: str, retrieved_plans: List[Dict[str, Any]] = None, resources: List[Dict[str, Any]] = None) -> str:
    system_prompt = """你是一个专业的自然灾害应急处置方案生成专家。请根据提供的情报分析结果、原始灾情描述、相关应急预案文档以及可用资源信息，生成一份完整的应急处置方案。

方案必须包含以下七个章节：
1. 事件概况：简要描述灾害发生的时间、地点、类型、等级等基本信息
2. 风险评估：分析灾害可能造成的损失、影响范围、潜在风险等
3. 处置目标：明确应急处置的总体目标和具体目标
4. 资源调度建议：根据可用资源情况，提出具体的资源调度方案和建议
5. 应急措施：详细列出各项应急处置措施和操作步骤
6. 保障措施：包括通信保障、物资保障、医疗保障、安全保障等
7. 参考资料：列出引用的应急预案文档和资源来源

特别要求：
- 方案结构清晰，逻辑严谨
- 语言专业、简洁、易懂
- 措施具体可行，具有操作性
- 根据灾情等级制定相应级别的响应措施
- 如果提供了相关应急预案文档，请参考文档内容制定方案，并在参考资料章节标注引用来源
- 如果提供了可用资源信息，请在"资源调度建议"章节中结合资源情况制定具体的调度方案
- 引用格式：在参考资料章节列出引用的预案名称和来源"""
    
    # 构建参考预案文本（限制长度以避免超出模型上下文）
    plans_text = ""
    if retrieved_plans and len(retrieved_plans) > 0:
        plans_text = "\n【参考应急预案】\n"
        max_plans = min(len(retrieved_plans), 3)  # 最多使用3条预案
        for i, plan in enumerate(retrieved_plans[:max_plans], 1):
            content = plan.get('text', '')
            # 限制单条预案内容长度为250字
            if len(content) > 250:
                content = content[:250] + "..."
            plans_text += f"---\n"
            plans_text += f"预案{i}：{plan.get('document_name', '')}\n"
            plans_text += f"来源：{plan.get('source', '')}\n"
            plans_text += f"章节：{plan.get('chapter', '')}\n"
            plans_text += f"相似度：{plan.get('similarity', 0):.4f}\n"
            plans_text += f"内容：{content}\n"
    
    # 构建可用资源文本
    resources_text = ""
    if resources and len(resources) > 0:
        resources_text = "\n【可用资源】\n"
        max_resources = min(len(resources), 5)  # 最多使用5条资源
        for i, resource in enumerate(resources[:max_resources], 1):
            resources_text += f"资源{i}：\n"
            resources_text += f"- 名称：{resource.get('name', '')}\n"
            resources_text += f"- 类型：{resource.get('type', '')}\n"
            resources_text += f"- 数量：{resource.get('quantity', 0)}\n"
            resources_text += f"- 位置：{resource.get('location', '未知')}\n"
            resources_text += f"- 优先级：{resource.get('priority', '中')}\n"
    
    user_prompt = f"""请根据以下信息生成应急处置方案：

【灾情描述】
{description}

【情报分析结果】
- 灾害类型: {info.get('type', '未知')}
- 灾害等级: {info.get('level', '未知')}
- 发生地点: {info.get('location', '未知')}
- 受影响人数: {info.get('affected_population', '未知')}
- 置信度: {info.get('confidence', 0.0)}

{plans_text}

{resources_text}

请生成一份完整的应急处置方案，包含资源调度建议章节，并在方案末尾标注引用的预案来源。"""
    
    # Qwen2.5原生格式
    prompt = f"""<|im_start|>system
{system_prompt}
<|im_end|>
<|im_start|>user
{user_prompt}
<|im_end|>
<|im_start|>assistant
"""
    return prompt


def generate_plan(info: Dict[str, Any], description: str, retrieved_plans: List[Dict[str, Any]] = None, resources: List[Dict[str, Any]] = None) -> str:
    """结合情报分析结果、原始描述、RAG检索结果和资源调度结果生成应急方案。"""
    logger.info(f"📝 开始生成方案，灾害类型: {info.get('type')}, 等级: {info.get('level')}, RAG预案数: {len(retrieved_plans) if retrieved_plans else 0}, 可用资源数: {len(resources) if resources else 0}")
    
    try:
        prompt = _build_prompt(info, description, retrieved_plans, resources)
        
        response = _client.completions.create(
            model="Qwen/Qwen2.5-7B-Instruct",
            prompt=prompt,
            max_tokens=1500,
            temperature=0.7,
            top_p=0.9,
            stop=["<|im_end|>"]
        )
        
        plan_text = response.choices[0].text.strip()
        
        if not plan_text:
            logger.warning("⚠️ vLLM响应为空，生成默认方案")
            plan_text = _generate_default_plan(info, description, retrieved_plans)
        
        logger.info(f"✅ 方案生成完成，方案长度: {len(plan_text)}")
        return plan_text
        
    except Exception as e:
        logger.error(f"❌ 方案生成失败: {e}")
        return _generate_default_plan(info, description, retrieved_plans, resources)


def generate_plan_stream(info: Dict[str, Any], description: str, retrieved_plans: List[Dict[str, Any]] = None, resources: List[Dict[str, Any]] = None):
    """流式生成应急方案，返回生成器。"""
    logger.info(f"📝 开始流式生成方案，灾害类型: {info.get('type')}, 等级: {info.get('level')}")
    
    try:
        prompt = _build_prompt(info, description, retrieved_plans, resources)
        
        stream = _client.completions.create(
            model="Qwen/Qwen2.5-7B-Instruct",
            prompt=prompt,
            max_tokens=1500,
            temperature=0.7,
            top_p=0.9,
            stop=["<|im_end|>"],
            stream=True
        )
        
        for chunk in stream:
            text = chunk.choices[0].text
            if text:
                yield text
        
        logger.info(f"✅ 流式方案生成完成")
        
    except Exception as e:
        logger.error(f"❌ 流式方案生成失败: {e}")
        yield _generate_default_plan(info, description, retrieved_plans, resources)


def _generate_default_plan(info: Dict[str, Any], description: str, retrieved_plans: List[Dict[str, Any]] = None, resources: List[Dict[str, Any]] = None) -> str:
    """生成默认方案（当vLLM调用失败时使用）。"""
    disaster_type = info.get('type', '未知灾害')
    location = info.get('location', '未知地点')
    level = info.get('level', '未知等级')
    
    # 构建资源调度建议部分
    resources_section = "## 四、资源调度建议\n"
    if resources and len(resources) > 0:
        resources_section += "根据当前可用资源情况，建议调度以下资源：\n\n"
        for i, resource in enumerate(resources, 1):
            resources_section += f"{i}. **{resource.get('name', '')}**（{resource.get('type', '')}）\n"
            resources_section += f"   - 数量：{resource.get('quantity', 0)} 单位\n"
            resources_section += f"   - 位置：{resource.get('location', '未知')}\n"
            resources_section += f"   - 优先级：{resource.get('priority', '中')}\n\n"
    else:
        resources_section += "- 应急救援队伍：根据灾情需要调配\n"
        resources_section += "- 医疗救护资源：准备充足的急救物资和医护人员\n"
        resources_section += "- 物资保障：准备帐篷、食品、饮用水等救灾物资\n\n"
    
    # 构建参考资料部分
    references = ""
    if retrieved_plans and len(retrieved_plans) > 0:
        references = "\n\n## 八、参考资料\n"
        for i, plan in enumerate(retrieved_plans, 1):
            references += f"- [{i}] {plan.get('document_name', '')}（来源：{plan.get('source', '')}，相似度：{plan.get('similarity', 0):.4f}）\n"
    
    return f"""# {disaster_type}应急处置方案

## 一、事件概况
{description}

## 二、风险评估
根据初步分析，本次{disaster_type}灾害风险等级为{level}，可能造成人员伤亡和财产损失，需立即启动应急响应。

## 三、处置目标
1. 保障人民生命安全
2. 减少财产损失
3. 尽快恢复正常生产生活秩序

{resources_section}

## 五、应急措施
1. 立即启动应急预案
2. 组织人员疏散和转移
3. 开展搜救和伤员救治工作
4. 做好受灾群众安置
5. 及时发布灾情信息

## 六、保障措施
- 通信保障：确保通信畅通
- 物资保障：确保救灾物资及时到位
- 医疗保障：确保伤员得到及时救治
- 安全保障：确保救援人员安全

## 七、资源来源
本方案中的资源调度建议基于当前系统可用资源信息生成。

{references}

---
*注：本方案为自动生成的默认方案，请根据实际情况调整。*"""


if __name__ == "__main__":
    test_info = {
        "type": "地震",
        "level": "高",
        "location": "云南省昆明市五华区",
        "affected_population": 500,
        "confidence": 0.9
    }
    test_description = "云南省昆明市五华区发生4.5级地震，震源深度10公里，部分房屋受损，约500人受影响。"
    result = generate_plan(test_info, test_description)
    print(result)