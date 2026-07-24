"""实验3：单Agent vs多Agent质量对比"""

import sys
import os
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from openai import OpenAI
from agents.info_extractor import extract_incident_info
from agents.plan_generator import generate_plan
from agents.plan_reviewer import review_plan
from rag.retriever import retrieve_plans

TEST_DESC = "云南省昆明市五华区发生4.5级地震，震源深度10公里，部分房屋受损，约500人受影响。"

VLLM_BASE_URL = "http://127.0.0.1:8000/v1"
VLLM_API_KEY = "EMPTY"

client = OpenAI(base_url=VLLM_BASE_URL, api_key=VLLM_API_KEY)

print("实验3: 单Agent vs多Agent质量对比")
print("="*60)

# 单Agent测试
print("\n--- 单Agent（直接调用vLLM） ---")
start = time.time()

single_prompt = f"""<|im_start|>system
你是一个专业的自然灾害应急处置专家。请根据以下灾情描述，生成一份完整的应急处置方案。

输出格式要求：
- 包含事件概况、风险评估、处置目标、资源部署、应急措施、保障措施等章节
- 使用Markdown格式输出
- 语言简洁明了，可操作性强

<|im_end|>
<|im_start|>user
灾情描述：{TEST_DESC}
<|im_end|>
<|im_start|>assistant
"""

response = client.completions.create(
    model="Qwen/Qwen2.5-7B-Instruct",
    prompt=single_prompt,
    max_tokens=1500,
    temperature=0.7,
    top_p=0.9,
    stop=["<|im_end|>"]
)

single_plan = response.choices[0].text.strip()
single_time = time.time() - start

print(f"单Agent生成耗时: {single_time:.2f}s")
print(f"方案长度: {len(single_plan)}字符")

# 审查单Agent方案
single_review = review_plan(single_plan)
print(f"单Agent方案审查评分: {single_review.get('score')}")

# 多Agent测试
print("\n--- 多Agent（完整工作流） ---")
start = time.time()

info = extract_incident_info(TEST_DESC)
plans = retrieve_plans(f"{info.get('type')} {info.get('location')} 应急处置", limit=3)
multi_plan = generate_plan(info, TEST_DESC, plans)
multi_review = review_plan(multi_plan)

multi_time = time.time() - start

print(f"多Agent执行耗时: {multi_time:.2f}s")
print(f"方案长度: {len(multi_plan)}字符")
print(f"审查评分: {multi_review.get('score')}")
print(f"参考预案数: {len(plans)}")

# 保存方案到文件
with open("experiment3_single_plan.txt", "w", encoding="utf-8") as f:
    f.write("="*60 + "\n")
    f.write("单Agent方案\n")
    f.write("="*60 + "\n")
    f.write(f"生成耗时: {single_time:.2f}秒\n")
    f.write(f"方案长度: {len(single_plan)}字符\n")
    f.write(f"审查评分: {single_review.get('score')}\n")
    f.write("="*60 + "\n\n")
    f.write(single_plan)

with open("experiment3_multi_plan.txt", "w", encoding="utf-8") as f:
    f.write("="*60 + "\n")
    f.write("多Agent方案\n")
    f.write("="*60 + "\n")
    f.write(f"执行耗时: {multi_time:.2f}秒\n")
    f.write(f"方案长度: {len(multi_plan)}字符\n")
    f.write(f"审查评分: {multi_review.get('score')}\n")
    f.write(f"参考预案数: {len(plans)}\n")
    f.write("="*60 + "\n\n")
    f.write(multi_plan)

print("\n📁 方案已保存到 experiment3_single_plan.txt 和 experiment3_multi_plan.txt")

# 保存结果
result = {
    "single_agent_time_s": single_time,
    "multi_agent_time_s": multi_time,
    "single_agent_length": len(single_plan),
    "multi_agent_length": len(multi_plan),
    "single_agent_score": single_review.get("score"),
    "multi_agent_score": multi_review.get("score"),
    "plans_count": len(plans)
}

import json
with open("experiment3_result.json", "w", encoding="utf-8") as f:
    json.dump(result, f, ensure_ascii=False, indent=2)

print("\n结果已保存到 experiment3_result.json")
