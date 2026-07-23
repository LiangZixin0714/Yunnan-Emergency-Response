"""方案生成Agent，结合情报分析结果+RAG检索结果+原始描述，生成完整处置方案。"""

import os
import logging
import sys
from typing import Dict, List, Any

from openai import OpenAI

# 添加项目根目录到sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from utils.logger import setup_logger

logger = setup_logger()

# vLLM配置
VLLM_BASE_URL = os.environ.get("VLLM_BASE_URL", "http://localhost:8000/v1")
VLLM_API_KEY = os.environ.get("VLLM_API_KEY", "EMPTY")

# 创建OpenAI客户端
_client = OpenAI(
    base_url=VLLM_BASE_URL,
    api_key=VLLM_API_KEY
)


def _build_prompt(info: Dict[str, Any], rag_results: List[Dict[str, Any]], description: str) -> str:
    """构建方案生成的Prompt。
    
    Args:
        info: 情报分析结果
        rag_results: RAG检索结果
        description: 原始灾情描述
        
    Returns:
        完整的Prompt字符串
    """
    system_prompt = """你是一个专业的自然灾害应急处置方案生成专家。请根据提供的情报分析结果、相关预案参考和原始灾情描述，生成一份完整的应急处置方案。

方案必须包含以下六个章节：
1. 事件概况：简要描述灾害发生的时间、地点、类型、等级等基本信息
2. 风险评估：分析灾害可能造成的损失、影响范围、潜在风险等
3. 处置目标：明确应急处置的总体目标和具体目标
4. 资源部署：规划所需人力、物力资源的调度和分配
5. 应急措施：详细列出各项应急处置措施和操作步骤
6. 保障措施：包括通信保障、物资保障、医疗保障、安全保障等

要求：
- 方案结构清晰，逻辑严谨
- 语言专业、简洁、易懂
- 措施具体可行，具有操作性
- 结合参考预案中的相关内容，但不要直接复制
- 根据灾情等级制定相应级别的响应措施"""
    
    # 构建参考预案文本
    rag_text = "\n".join([
        f"参考预案{idx+1}（相似度: {result['similarity']:.4f}）:\n{result['text'][:500]}" 
        for idx, result in enumerate(rag_results)
    ]) if rag_results else "暂无参考预案"
    
    user_prompt = f"""请根据以下信息生成应急处置方案：

【灾情描述】
{description}

【情报分析结果】
- 灾害类型: {info.get('type', '未知')}
- 灾害等级: {info.get('level', '未知')}
- 发生地点: {info.get('location', '未知')}
- 受影响人数: {info.get('affected_population', '未知')}
- 置信度: {info.get('confidence', 0.0)}

【参考预案】
{rag_text}

请生成一份完整的应急处置方案。"""
    
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


def generate_plan(info: Dict[str, Any], rag_results: List[Dict[str, Any]], description: str) -> str:
    """结合情报分析结果、RAG检索结果和原始描述生成应急方案。
    
    Args:
        info: 情报分析结果字典
        rag_results: RAG检索结果列表
        description: 原始灾情描述
        
    Returns:
        完整的应急方案文本
    """
    logger.info(f"📝 开始生成方案，灾害类型: {info.get('type')}, 等级: {info.get('level')}, RAG结果数: {len(rag_results)}")
    
    try:
        # 构建Prompt
        prompt = _build_prompt(info, rag_results, description)
        
        # 调用vLLM
        response = _client.completions.create(
            model="Qwen2.5-7B-Instruct",
            prompt=prompt,
            max_tokens=2048,
            temperature=0.7,
            top_p=0.9,
            stop=["<|im_end|>"]
        )
        
        # 提取响应文本
        plan_text = response.choices[0].text.strip()
        
        # 如果响应为空，生成默认方案
        if not plan_text:
            logger.warning("⚠️ vLLM响应为空，生成默认方案")
            plan_text = _generate_default_plan(info, description)
        
        logger.info(f"✅ 方案生成完成，方案长度: {len(plan_text)}")
        return plan_text
        
    except Exception as e:
        logger.error(f"❌ 方案生成失败: {e}")
        return _generate_default_plan(info, description)


def _generate_default_plan(info: Dict[str, Any], description: str) -> str:
    """生成默认方案（当vLLM调用失败时使用）。
    
    Args:
        info: 情报分析结果
        description: 原始灾情描述
        
    Returns:
        默认的应急方案文本
    """
    disaster_type = info.get('type', '未知灾害')
    location = info.get('location', '未知地点')
    level = info.get('level', '未知等级')
    
    default_plan = f"""# {disaster_type}应急处置方案

## 一、事件概况
{description}

## 二、风险评估
根据初步分析，本次{disaster_type}灾害风险等级为{level}，可能造成人员伤亡和财产损失，需立即启动应急响应。

## 三、处置目标
1. 保障人民生命安全
2. 减少财产损失
3. 尽快恢复正常生产生活秩序

## 四、资源部署
- 应急救援队伍：根据灾情需要调配
- 医疗救护资源：准备充足的急救物资和医护人员
- 物资保障：准备帐篷、食品、饮用水等救灾物资

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

---
*注：本方案为自动生成的默认方案，请根据实际情况调整。*
"""
    
    return default_plan


if __name__ == "__main__":
    # 测试方案生成功能
    test_info = {
        "type": "地震",
        "level": "高",
        "location": "云南省昆明市五华区",
        "affected_population": 500,
        "confidence": 0.9
    }
    
    test_rag_results = [
        {
            "plan_id": "test-1",
            "document_name": "云南省地震应急预案",
            "text": "地震发生后，应立即启动相应级别的应急响应，组织人员疏散和搜救工作...",
            "similarity": 0.85
        }
    ]
    
    test_description = "云南省昆明市五华区发生4.5级地震，震源深度10公里，部分房屋受损，约500人受影响。"
    
    result = generate_plan(test_info, test_rag_results, test_description)
    print("方案生成结果:")
    print(result)