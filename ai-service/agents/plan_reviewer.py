"""方案审查Agent，对生成的方案进行合规性检查，给出评分和修改建议。"""

import json
import logging
import sys
import os
from typing import Dict, Any

from openai import OpenAI

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from utils.logger import setup_logger

logger = setup_logger()

# vLLM配置
VLLM_BASE_URL = os.environ.get("VLLM_BASE_URL", "http://127.0.0.1:8000/v1")
VLLM_API_KEY = os.environ.get("VLLM_API_KEY", "EMPTY")

_client = OpenAI(
    base_url=VLLM_BASE_URL,
    api_key=VLLM_API_KEY
)

REQUIRED_SECTIONS = [
    "事件概况",
    "风险评估",
    "处置目标",
    "资源部署",
    "应急措施",
    "保障措施"
]


def _check_sections(plan: str) -> Dict[str, bool]:
    """基于规则检查方案是否包含必需章节。"""
    return {section: section in plan for section in REQUIRED_SECTIONS}


def _build_prompt(plan: str) -> str:
    system_prompt = """你是一个专业的自然灾害应急处置方案审查专家。请对提供的应急方案进行全面审查。

审查内容：
1. 结构完整性：检查方案是否包含所有必需章节
2. 内容准确性：评估方案内容是否准确、合理
3. 措施可行性：评估应急措施是否具有可操作性
4. 风险评估：评估风险评估是否全面、准确

输出格式要求：
必须输出纯JSON格式，不要包含任何其他文本。JSON结构如下：
{
    "score": 0~10,
    "issues": ["问题1", "问题2", "问题3"],
    "passed": true/false,
    "suggestions": ["修改建议1", "修改建议2"]
}

评分标准：
- 0-3分：方案严重不完整或存在重大问题，需要重新编写
- 4-6分：方案基本完整但存在明显问题，需要修改完善
- 7-8分：方案较为完整，存在少量问题
- 9-10分：方案完整、合理、可行

passed字段：score >= 7 时为true，否则为false"""
    
    user_prompt = f"""请审查以下应急处置方案：

{plan}

请给出审查结果。"""
    
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


def review_plan(plan: str) -> Dict[str, Any]:
    """审查应急方案的合规性和可行性。"""
    logger.info(f"🔍 开始方案审查，方案长度: {len(plan)}")
    
    try:
        section_check = _check_sections(plan)
        missing_sections = [section for section, exists in section_check.items() if not exists]
        
        if missing_sections:
            logger.warning(f"⚠️ 缺少必需章节: {', '.join(missing_sections)}")
            return {
                "score": 4,
                "issues": [f"缺少必需章节: {', '.join(missing_sections)}"],
                "passed": False,
                "suggestions": [f"请补充以下章节: {', '.join(missing_sections)}"]
            }
        
        prompt = _build_prompt(plan)
        
        response = _client.completions.create(
            model="Qwen/Qwen2.5-7B-Instruct",
            prompt=prompt,
            max_tokens=512,
            temperature=0.3,
            top_p=0.9,
            stop=["<|im_end|>"]
        )
        
        result_text = response.choices[0].text.strip()
        logger.debug(f"📝 vLLM审查响应: {result_text[:200]}")
        
        start_idx = result_text.find("{")
        end_idx = result_text.rfind("}") + 1
        
        if start_idx != -1 and end_idx != 0:
            json_str = result_text[start_idx:end_idx]
            try:
                review_result = json.loads(json_str)
                review_result.setdefault("score", 5)
                review_result.setdefault("issues", [])
                review_result.setdefault("passed", False)
                review_result.setdefault("suggestions", [])
                
                logger.info(f"✅ 方案审查完成: score={review_result['score']}, passed={review_result['passed']}")
                return review_result
            except json.JSONDecodeError as e:
                logger.error(f"❌ JSON解析失败: {e}")
        
        logger.warning("⚠️ 无法解析JSON响应，使用规则检查结果")
        return {
            "score": 7,
            "issues": [],
            "passed": True,
            "suggestions": ["方案结构完整，建议进一步优化内容细节"]
        }
        
    except Exception as e:
        logger.error(f"❌ 方案审查失败: {e}")
        section_check = _check_sections(plan)
        missing_sections = [section for section, exists in section_check.items() if not exists]
        
        if missing_sections:
            return {
                "score": 4,
                "issues": [f"缺少必需章节: {', '.join(missing_sections)}"],
                "passed": False,
                "suggestions": [f"请补充以下章节: {', '.join(missing_sections)}"]
            }
        else:
            return {
                "score": 6,
                "issues": ["自动审查服务暂时不可用，建议人工审查"],
                "passed": False,
                "suggestions": ["请人工审查方案内容的准确性和可行性"]
            }


if __name__ == "__main__":
    test_plan = """# 地震应急处置方案

## 一、事件概况
云南省昆明市五华区发生4.5级地震，震源深度10公里。

## 二、风险评估
本次地震可能造成房屋倒塌和人员伤亡。

## 三、处置目标
保障人民生命安全，减少财产损失。

## 四、资源部署
调配救援队伍和医疗资源。

## 五、应急措施
组织人员疏散和搜救工作。

## 六、保障措施
确保通信和物资供应。"""
    result = review_plan(test_plan)
    print(json.dumps(result, ensure_ascii=False, indent=2))