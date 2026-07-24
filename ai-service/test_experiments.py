"""Day 4优化对比实验测试脚本"""

import sys
import os
import time
import json
import psycopg2

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from rag.retriever import retrieve_plans, generate_embedding
from agents.info_extractor import extract_incident_info
from agents.plan_generator import generate_plan
from agents.plan_reviewer import review_plan

# 测试灾情描述
TEST_DESCRIPTION = "云南省昆明市五华区发生4.5级地震，震源深度10公里，部分房屋受损，约500人受影响。"

# 数据库配置
PG_HOST = "localhost"
PG_PORT = 5432
PG_DATABASE = "emergency_vector"
PG_USER = "postgres"
PG_PASSWORD = "ZAQ12wsx581!"


def get_db_connection():
    """获取数据库连接"""
    return psycopg2.connect(
        host=PG_HOST,
        port=PG_PORT,
        database=PG_DATABASE,
        user=PG_USER,
        password=PG_PASSWORD
    )


def experiment1_rag_index():
    """实验1：RAG检索无索引vs有索引"""
    print("\n" + "=" * 60)
    print("实验1：RAG检索无索引 vs 有索引")
    print("=" * 60)
    
    conn = get_db_connection()
    cur = conn.cursor()
    
    # 1. 删除索引（如果存在）
    try:
        cur.execute("DROP INDEX IF EXISTS idx_knowledge_chunks_embedding;")
        conn.commit()
        print("✅ 已删除现有索引")
    except Exception as e:
        print(f"❌ 删除索引失败: {e}")
    
    # 2. 测试无索引检索
    print("\n--- 无索引检索 ---")
    start_time = time.time()
    for i in range(3):
        results = retrieve_plans("地震应急处置", limit=3)
    avg_no_index = (time.time() - start_time) / 3
    print(f"无索引检索耗时: {avg_no_index:.4f}秒（3次平均）")
    print(f"返回结果数: {len(results)}")
    
    # 3. 创建索引
    print("\n--- 创建IVFFlat索引 ---")
    start_time = time.time()
    try:
        cur.execute("CREATE INDEX idx_knowledge_chunks_embedding ON knowledge_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);")
        conn.commit()
        index_time = time.time() - start_time
        print(f"✅ 索引创建成功，耗时: {index_time:.2f}秒")
    except Exception as e:
        print(f"❌ 创建索引失败: {e}")
        index_time = 0
    
    cur.close()
    conn.close()
    
    # 4. 测试有索引检索
    print("\n--- 有索引检索 ---")
    start_time = time.time()
    for i in range(3):
        results = retrieve_plans("地震应急处置", limit=3)
    avg_with_index = (time.time() - start_time) / 3
    print(f"有索引检索耗时: {avg_with_index:.4f}秒（3次平均）")
    print(f"返回结果数: {len(results)}")
    
    # 5. 计算提升
    if avg_no_index > 0:
        improvement = ((avg_no_index - avg_with_index) / avg_no_index) * 100
        print(f"\n📊 检索速度提升: {improvement:.2f}%")
    else:
        improvement = 0
    
    cur.close()
    conn.close()
    
    return {
        "no_index_avg_ms": avg_no_index * 1000,
        "with_index_avg_ms": avg_with_index * 1000,
        "index_creation_ms": index_time * 1000,
        "improvement_pct": improvement,
        "result_count": len(results)
    }


def experiment2_parallel_vs_serial():
    """实验2：多Agent串行vs并行"""
    print("\n" + "=" * 60)
    print("实验2：多Agent串行 vs 并行")
    print("=" * 60)
    
    # 串行执行（当前方式）
    print("\n--- 串行执行 ---")
    start_time = time.time()
    
    info = extract_incident_info(TEST_DESCRIPTION)
    plans = retrieve_plans(f"{info.get('type')} {info.get('location')} 应急处置", limit=3)
    plan = generate_plan(info, TEST_DESCRIPTION, plans)
    review = review_plan(plan)
    
    serial_time = time.time() - start_time
    print(f"串行执行总耗时: {serial_time:.2f}秒")
    print(f"情报分析耗时: 已包含")
    print(f"RAG检索耗时: 已包含")
    print(f"方案生成耗时: 已包含")
    print(f"方案审查耗时: 已包含")
    print(f"审查评分: {review.get('score')}")
    
    # 并行执行（手动模拟并行）
    print("\n--- 并行执行（情报分析与RAG并行） ---")
    start_time = time.time()
    
    # 并行步骤：情报分析和RAG检索可以同时开始
    import threading
    
    info_result = {}
    plans_result = []
    
    def extract_info_thread():
        info_result["data"] = extract_incident_info(TEST_DESCRIPTION)
    
    def retrieve_plans_thread():
        plans_result.extend(retrieve_plans("地震 昆明 应急处置", limit=3))
    
    t1 = threading.Thread(target=extract_info_thread)
    t2 = threading.Thread(target=retrieve_plans_thread)
    
    t1.start()
    t2.start()
    t1.join()
    t2.join()
    
    info = info_result.get("data", {"type": "地震", "level": "中", "location": "昆明"})
    plans = plans_result
    
    # 方案生成和审查必须串行（依赖前序结果）
    plan = generate_plan(info, TEST_DESCRIPTION, plans)
    review = review_plan(plan)
    
    parallel_time = time.time() - start_time
    print(f"并行执行总耗时: {parallel_time:.2f}秒")
    print(f"审查评分: {review.get('score')}")
    
    # 计算提升
    if serial_time > 0:
        improvement = ((serial_time - parallel_time) / serial_time) * 100
        print(f"\n📊 执行速度提升: {improvement:.2f}%")
    else:
        improvement = 0
    
    return {
        "serial_time_s": serial_time,
        "parallel_time_s": parallel_time,
        "improvement_pct": improvement,
        "serial_score": review.get("score"),
        "parallel_score": review.get("score"),
        "plans_count": len(plans)
    }


def experiment3_single_vs_multi_agent():
    """实验3：单Agent vs多Agent质量对比"""
    print("\n" + "=" * 60)
    print("实验3：单Agent vs 多Agent质量对比")
    print("=" * 60)
    
    from openai import OpenAI
    
    VLLM_BASE_URL = "http://127.0.0.1:8000/v1"
    VLLM_API_KEY = "EMPTY"
    
    client = OpenAI(base_url=VLLM_BASE_URL, api_key=VLLM_API_KEY)
    
    # 单Agent直接生成方案
    print("\n--- 单Agent（直接调用vLLM） ---")
    start_time = time.time()
    
    single_prompt = f"""<|im_start|>system
你是一个专业的自然灾害应急处置专家。请根据以下灾情描述，生成一份完整的应急处置方案。

输出格式要求：
- 包含事件概况、风险评估、处置目标、资源部署、应急措施、保障措施等章节
- 使用Markdown格式输出
- 语言简洁明了，可操作性强

<|im_end|>
<|im_start|>user
灾情描述：{TEST_DESCRIPTION}
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
    single_time = time.time() - start_time
    
    print(f"单Agent生成耗时: {single_time:.2f}秒")
    print(f"方案长度: {len(single_plan)}字符")
    print(f"方案预览:\n{single_plan[:300]}...\n")
    
    # 多Agent工作流
    print("\n--- 多Agent（完整工作流） ---")
    start_time = time.time()
    
    info = extract_incident_info(TEST_DESCRIPTION)
    plans = retrieve_plans(f"{info.get('type')} {info.get('location')} 应急处置", limit=3)
    multi_plan = generate_plan(info, TEST_DESCRIPTION, plans)
    review = review_plan(multi_plan)
    
    multi_time = time.time() - start_time
    
    print(f"多Agent执行耗时: {multi_time:.2f}秒")
    print(f"方案长度: {len(multi_plan)}字符")
    print(f"审查评分: {review.get('score')}")
    print(f"方案预览:\n{multi_plan[:300]}...")
    
    # 保存方案到文件以便人工对比
    with open("experiment3_single_plan.txt", "w", encoding="utf-8") as f:
        f.write("单Agent方案:\n" + "="*60 + "\n")
        f.write(single_plan)
    
    with open("experiment3_multi_plan.txt", "w", encoding="utf-8") as f:
        f.write("多Agent方案:\n" + "="*60 + "\n")
        f.write(multi_plan)
    
    print("\n📁 方案已保存到 experiment3_single_plan.txt 和 experiment3_multi_plan.txt")
    
    return {
        "single_agent_time_s": single_time,
        "multi_agent_time_s": multi_time,
        "single_agent_length": len(single_plan),
        "multi_agent_length": len(multi_plan),
        "multi_agent_score": review.get("score"),
        "plans_count": len(plans)
    }


def main():
    """执行所有实验"""
    print("🚀 开始执行 Day 4 优化对比实验")
    
    results = {}
    
    # 实验1：RAG检索无索引vs有索引
    try:
        results["experiment1"] = experiment1_rag_index()
    except Exception as e:
        print(f"\n❌ 实验1执行失败: {e}")
        results["experiment1"] = {"error": str(e)}
    
    # 实验2：串行vs并行
    try:
        results["experiment2"] = experiment2_parallel_vs_serial()
    except Exception as e:
        print(f"\n❌ 实验2执行失败: {e}")
        results["experiment2"] = {"error": str(e)}
    
    # 实验3：单Agent vs多Agent
    try:
        results["experiment3"] = experiment3_single_vs_multi_agent()
    except Exception as e:
        print(f"\n❌ 实验3执行失败: {e}")
        results["experiment3"] = {"error": str(e)}
    
    # 输出汇总
    print("\n" + "=" * 60)
    print("实验结果汇总")
    print("=" * 60)
    print(json.dumps(results, ensure_ascii=False, indent=2))
    
    # 保存结果到文件
    with open("experiment_results.json", "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)
    
    print("\n📁 实验结果已保存到 experiment_results.json")
    
    return results


if __name__ == "__main__":
    main()
