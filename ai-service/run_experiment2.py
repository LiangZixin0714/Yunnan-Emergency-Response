"""实验2：串行vs并行测试"""

import sys
import os
import time
import threading

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from agents.info_extractor import extract_incident_info
from agents.plan_generator import generate_plan
from agents.plan_reviewer import review_plan
from rag.retriever import retrieve_plans

TEST_DESC = "云南省昆明市五华区发生4.5级地震，震源深度10公里，部分房屋受损，约500人受影响。"

print("实验2: 串行vs并行")
print("="*60)

# 串行测试
print("\n--- 串行执行 ---")
start = time.time()
info = extract_incident_info(TEST_DESC)
plans = retrieve_plans(f"{info.get('type')} {info.get('location')} 应急处置", limit=3)
plan = generate_plan(info, TEST_DESC, plans)
review = review_plan(plan)
serial_time = time.time() - start
print(f"串行耗时: {serial_time:.2f}s")
print(f"审查评分: {review.get('score')}")

# 并行测试
print("\n--- 并行执行 ---")
start = time.time()

info_result = {}
plans_result = []

def extract_thread():
    info_result['data'] = extract_incident_info(TEST_DESC)

def retrieve_thread():
    plans_result.extend(retrieve_plans('地震 昆明 应急处置', limit=3))

t1 = threading.Thread(target=extract_thread)
t2 = threading.Thread(target=retrieve_thread)
t1.start()
t2.start()
t1.join()
t2.join()

info = info_result.get('data', {'type': '地震', 'level': '中', 'location': '昆明'})
plans = plans_result
plan = generate_plan(info, TEST_DESC, plans)
review = review_plan(plan)

parallel_time = time.time() - start
print(f"并行耗时: {parallel_time:.2f}s")
print(f"审查评分: {review.get('score')}")

improvement = ((serial_time - parallel_time) / serial_time) * 100
print(f"提升: {improvement:.2f}%")

# 保存结果
result = {
    "serial_time_s": serial_time,
    "parallel_time_s": parallel_time,
    "improvement_pct": improvement,
    "score": review.get("score")
}

import json
with open("experiment2_result.json", "w", encoding="utf-8") as f:
    json.dump(result, f, ensure_ascii=False, indent=2)

print("\n结果已保存到 experiment2_result.json")
