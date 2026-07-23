"""情报分析Agent，从灾情描述中提取结构化信息。"""

import os
import json
import logging
import sys
from typing import Dict, Any

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


def _build_prompt(description: str) -> str:
    """构建情报分析的Prompt。
    
    Args:
        description: 灾情描述文本
        
    Returns:
        完整的Prompt字符串
    """
    system_prompt = """你是一个专业的自然灾害情报分析专家。请从用户提供的灾情描述中提取结构化信息。

输出格式要求：
必须输出纯JSON格式，不要包含任何其他文本。JSON结构如下：
{
    "type": "地震" | "滑坡" | "洪涝" | "干旱" | "森林火灾" | "泥石流" | "其他",
    "level": "高" | "中" | "低",
    "location": "地名",
    "affected_population": 数字或null,
    "confidence": 0.0~1.0
}

字段说明：
- type: 灾害类型，从给定选项中选择最匹配的
- level: 灾害等级，根据描述判断严重程度
- location: 灾害发生地点，尽可能具体
- affected_population: 受影响人数，如果描述中没有明确提及则为null
- confidence: 你对提取结果的置信度，0.0表示不确定，1.0表示非常确定

请仔细分析描述文本，准确提取信息。"""
    
    user_prompt = f"灾情描述：\n{description}"
    
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


def extract_incident_info(description: str) -> Dict[str, Any]:
    """从灾情描述中提取关键情报信息。
    
    Args:
        description: 灾情描述文本
        
    Returns:
        包含 type、level、location、affected_population、confidence 的字典
    """
    logger.info(f"📋 开始情报分析，描述长度: {len(description)}")
    
    try:
        # 构建Prompt
        prompt = _build_prompt(description)
        
        # 调用vLLM
        response = _client.completions.create(
            model="Qwen2.5-7B-Instruct",
            prompt=prompt,
            max_tokens=512,
            temperature=0.3,
            top_p=0.9,
            stop=["<|im_end|>"]
        )
        
        # 解析响应
        result_text = response.choices[0].text.strip()
        logger.debug(f"📝 vLLM响应: {result_text[:200]}")
        
        # 提取JSON部分
        start_idx = result_text.find("{")
        end_idx = result_text.rfind("}") + 1
        
        if start_idx != -1 and end_idx != 0:
            json_str = result_text[start_idx:end_idx]
            try:
                info = json.loads(json_str)
                logger.info(f"✅ 情报分析完成: type={info.get('type')}, level={info.get('level')}, location={info.get('location')}")
                return info
            except json.JSONDecodeError as e:
                logger.error(f"❌ JSON解析失败: {e}")
        
        # 如果JSON解析失败，返回默认值
        logger.warning("⚠️ 无法解析JSON响应，使用默认值")
        return {
            "type": "其他",
            "level": "中",
            "location": "未知",
            "affected_population": None,
            "confidence": 0.5
        }
        
    except Exception as e:
        logger.error(f"❌ 情报分析失败: {e}")
        return {
            "type": "其他",
            "level": "中",
            "location": "未知",
            "affected_population": None,
            "confidence": 0.0
        }


if __name__ == "__main__":
    # 测试情报分析功能
    test_description = "云南省昆明市五华区发生4.5级地震，震源深度10公里，部分房屋受损，约500人受影响。"
    result = extract_incident_info(test_description)
    print("情报分析结果:")
    print(json.dumps(result, ensure_ascii=False, indent=2))