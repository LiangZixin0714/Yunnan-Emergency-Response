# 云南自然灾害应急协同决策平台 - API 接口文档

## 1. 接口概述

### 1.1 服务信息
- **服务名称**: 云南自然灾害应急协同决策平台 AI 服务
- **版本**: v1.0.1（含 RAG 功能）
- **基础 URL**: `http://localhost:8001`
- **框架**: FastAPI + LangGraph
- **LLM 后端**: vLLM (OpenAI 兼容 API)
- **向量数据库**: PostgreSQL + pgvector

### 1.2 工作流架构（含 RAG）
```
请求 → 情报分析 Agent → RAG 检索 → 方案生成 Agent → 方案审查 Agent → 响应
                                             ↓
                                        (未通过则重试，最多3次)
```

**工作流节点说明**:
1. **情报分析**: 从灾情描述中提取结构化信息（类型、等级、位置等）
2. **RAG 检索**: 根据提取的灾情信息，从向量数据库检索相关应急预案文档
3. **方案生成**: 结合情报分析结果和 RAG 检索结果，生成完整应急处置方案
4. **方案审查**: 审查方案合规性，给出评分和修改建议

### 1.3 核心组件
| 组件 | 职责 | 调用方式 |
|------|------|----------|
| 情报分析 Agent | 从灾情描述中提取结构化信息（类型、等级、位置、受影响人数） | vLLM completions API |
| RAG 检索模块 | 从 PostgreSQL+pgvector 检索相关应急预案片段 | psycopg2 + sentence-transformers |
| 方案生成 Agent | 根据情报分析结果和 RAG 检索结果生成完整应急处置方案 | vLLM completions API |
| 方案审查 Agent | 审查方案合规性，给出评分和修改建议 | vLLM completions API |
| LangGraph | 编排多 Agent 工作流，管理重试逻辑 | StateGraph |

### 1.4 RAG 检索机制
| 参数 | 值 | 说明 |
|------|-----|------|
| Embedding 模型 | BAAI/bge-small-zh-v1.5 | 中文语义向量模型，512 维 |
| 向量数据库 | PostgreSQL + pgvector | 存储应急预案向量数据 |
| 检索方法 | 余弦相似度 | 使用 `embedding <=> query_vector` 查询 |
| 返回数量 | 最多 3 条 | 限制上下文长度，避免超出模型限制 |
| 单条长度 | 最多 250 字 | 截断过长的预案片段 |
| 降级策略 | 数据库连接失败时返回空列表 | 不影响主流程继续执行 |

---

## 2. 接口列表

### 2.1 健康检查

**GET /health**

检查服务运行状态。

**请求参数**: 无

**响应示例**:
```json
{
    "status": "ok",
    "service": "ai-service",
    "workflow_ready": true
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| status | string | 服务状态，`ok` 表示正常，`degraded` 表示降级 |
| service | string | 服务名称 |
| workflow_ready | boolean | 工作流是否就绪 |

---

### 2.2 方案生成（同步）

**POST /api/v1/generate-plan**

根据灾情描述生成完整的应急处置方案（同步模式），包含 RAG 检索功能。

**请求头**:
| 字段 | 值 | 必填 |
|------|-----|------|
| Content-Type | application/json | 是 |

**请求体**:
```json
{
    "description": "云南省昆明市五华区发生4.5级地震，震源深度10公里，部分房屋受损，约500人受影响。"
}
```

**请求体字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| description | string | 是 | 灾情描述文本，建议长度 20-500 字 |

**响应体**:
```json
{
    "plan": "# 地震应急处置方案\n\n## 一、事件概况\n...（完整方案内容）\n\n## 参考资料\n1. 云南省地震应急预案（章节4，相似度0.6275）\n2. 昆明市自然灾害应急预案（章节8，相似度0.6124）\n3. 五华区应急处置手册（章节6，相似度0.6029）"
}
```

**响应体字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| plan | string | 生成的应急处置方案文本，包含参考资料引用 |

**成功响应示例**:
```json
{
    "plan": "### 云南省地震灾害应急处置方案\n\n#### 1. 事件概况\n- **灾害类型**: 地震\n- **灾害等级**: 中\n- **发生时间**: 未知\n- **发生地点**: 云南省昆明市五华区\n- **受影响人数**: 500\n- **置信度**: 0.8\n\n#### 2. 风险评估\n...（后续内容）\n\n### 参考资料\n1. 云南省地震应急预案\n   - 章节: 4\n   - 相似度: 0.6275\n   - 内容: 各级政府及其有关部门应对本行政区域内地震灾害风险进行评估...\n2. 昆明市自然灾害应急预案\n   - 章节: 8\n   - 相似度: 0.6124\n   - 内容: 边界地区政府要加强与相邻地区的信息沟通...\n3. 五华区应急处置手册\n   - 章节: 6\n   - 相似度: 0.6029\n   - 内容: 城乡社区应做好受灾群众的临时安置工作..."
}
```

**错误响应示例**:
```json
{
    "detail": "description字段不能为空"
}
```

**响应时间**: 约 35-65 秒（包含 RAG 检索时间，取决于重试次数）

---

### 2.3 方案生成（流式）

**POST /api/v1/generate-plan/stream**

根据灾情描述生成完整的应急处置方案（SSE 流式输出），包含 RAG 检索功能。

**请求头**:
| 字段 | 值 | 必填 |
|------|-----|------|
| Content-Type | application/json | 是 |
| Accept | text/event-stream | 建议 |

**请求体**:
```json
{
    "description": "云南省昆明市五华区发生4.5级地震，震源深度10公里，部分房屋受损，约500人受影响。"
}
```

**请求体字段说明**: 同 `/api/v1/generate-plan`

**响应格式**: Server-Sent Events (SSE)

**响应体示例**:
```
data: {"chunk": "### 地震灾害应急处置方案", "index": 0, "total": 18, "done": false}
data: {"chunk": "#### 1. 事件概况", "index": 1, "total": 18, "done": false}
...
data: {"chunk": "### 参考资料", "index": 15, "total": 18, "done": false}
data: {"chunk": "1. 云南省地震应急预案（章节4，相似度0.6275）", "index": 16, "total": 18, "done": false}
data: {"chunk": "---\n*注：本方案为自动生成，请根据实际情况调整。*", "index": 17, "total": 18, "done": true}
```

**响应体字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| chunk | string | 当前段落内容 |
| index | integer | 当前段落索引（从0开始） |
| total | integer | 总段落数 |
| done | boolean | 是否为最后一段 |

**响应时间**: 约 35 秒（首次响应时间取决于工作流执行完成时间，包含 RAG 检索）

---

## 3. 性能指标

### 3.1 vLLM 推理延迟

| 指标 | 数值 | 说明 |
|------|------|------|
| 首次推理延迟（情报分析） | ~3.8 秒 | 从请求到第一个 token 的时间 |
| 后续推理延迟（方案生成） | ~0.8 秒 | 模型已预热后的延迟 |

### 3.2 单 Agent 推理耗时

| Agent | 平均耗时 | 范围 |
|-------|----------|------|
| 情报分析 Agent | ~2.3 秒 | 0.8 ~ 3.8 秒 |
| RAG 检索模块 | ~10 秒 | 8 ~ 12 秒（含模型加载和数据库查询） |
| 方案生成 Agent | ~12 秒 | 10 ~ 15 秒 |
| 方案审查 Agent | ~3.8 秒 | 2.5 ~ 6.4 秒 |

### 3.3 RAG 检索性能

| 指标 | 数值 | 说明 |
|------|------|------|
| Embedding 模型加载时间 | ~10 秒 | BAAI/bge-small-zh-v1.5 首次加载 |
| 数据库连接时间 | ~0.1 秒 | PostgreSQL 本地连接 |
| 向量查询时间 | ~0.05 秒 | pgvector 余弦相似度检索 |
| 返回结果数 | 3 条 | 限制数量控制上下文长度 |
| 相似度范围 | 0.60 ~ 0.63 | 地震相关预案检索结果 |

### 3.4 完整工作流耗时

| 场景 | 总耗时 | 重试次数 | 说明 |
|------|--------|----------|------|
| 首次通过（评分≥7） | ~28 秒 | 0 次 | 含 RAG 检索 |
| 一次重试后通过 | ~48 秒 | 1 次 | 含 RAG 检索 + 方案重生成 |
| 多次重试后通过 | ~65 秒 | 2 次 | 完整重试流程 |
| 流式接口（平均） | ~35 秒 | 1 次 | 首次响应时间 |

### 3.5 并发建议

| 参数 | 建议值 | 说明 |
|------|--------|------|
| 最大并发请求数 | 1-2 | vLLM 服务通过 SSH 隧道访问，带宽有限 |
| 客户端超时时间 | ≥ 120 秒 | 考虑重试逻辑和 RAG 检索，建议设置较长超时 |
| 请求间隔 | ≥ 10 秒 | 避免频繁请求导致服务过载 |

---

## 4. 错误码表

| 状态码 | 含义 | 说明 |
|--------|------|------|
| 200 | OK | 请求成功 |
| 400 | Bad Request | 请求参数错误（如 description 为空） |
| 500 | Internal Server Error | 服务器内部错误（如 vLLM 调用失败） |

---

## 5. 调用示例

### 5.1 curl 命令

**健康检查**:
```bash
curl http://127.0.0.1:8001/health
```

**方案生成（同步）**:
```bash
curl -X POST http://127.0.0.1:8001/api/v1/generate-plan \
  -H "Content-Type: application/json" \
  -d '{
    "description": "云南省昆明市五华区发生4.5级地震，震源深度10公里，部分房屋受损，约500人受影响。"
  }'
```

**方案生成（流式）**:
```bash
curl -X POST http://127.0.0.1:8001/api/v1/generate-plan/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "description": "云南省昆明市五华区发生4.5级地震，震源深度10公里，部分房屋受损，约500人受影响。"
  }'
```

### 5.2 Python requests 示例

**方案生成（同步）**:
```python
import requests

url = "http://127.0.0.1:8001/api/v1/generate-plan"
data = {
    "description": "云南省昆明市五华区发生4.5级地震，震源深度10公里，部分房屋受损，约500人受影响。"
}

response = requests.post(url, json=data, timeout=120)
result = response.json()
print(result["plan"])
```

**方案生成（流式）**:
```python
import requests

url = "http://127.0.0.1:8001/api/v1/generate-plan/stream"
data = {
    "description": "云南省昆明市五华区发生4.5级地震，震源深度10公里，部分房屋受损，约500人受影响。"
}

with requests.post(url, json=data, stream=True, timeout=120) as response:
    for line in response.iter_lines():
        if line:
            # 解析 SSE 格式：data: {"chunk": "..."}
            if line.startswith(b'data:'):
                import json
                chunk = json.loads(line[5:].decode('utf-8'))
                print(chunk["chunk"])
                if chunk["done"]:
                    break
```

### 5.3 JavaScript/Node.js 示例

```javascript
const fetch = require('node-fetch');

async function generatePlan(description) {
    const response = await fetch('http://127.0.0.1:8001/api/v1/generate-plan', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ description }),
        timeout: 120000
    });
    const result = await response.json();
    return result.plan;
}

generatePlan('云南省昆明市五华区发生4.5级地震，震源深度10公里，部分房屋受损，约500人受影响。')
    .then(plan => console.log(plan))
    .catch(err => console.error(err));
```

---

## 6. 注意事项

### 6.1 RAG 检索相关

1. **预案数据**: 数据库中已存储 201 条应急预案向量数据，覆盖地震、洪涝、地质灾害等多种灾害类型
2. **检索关键词**: RAG 使用"灾害类型 + 位置 + 应急处置"作为查询关键词
3. **相似度阈值**: 返回结果相似度一般在 0.60-0.70 之间，低于 0.5 的结果被过滤
4. **模型缓存**: Embedding 模型首次加载约需 10 秒，后续请求会使用缓存

### 6.2 上下文长度限制

1. **模型限制**: Qwen2.5-7B-Instruct 最大上下文长度为 4096 tokens
2. **预案数量**: 最多返回 3 条预案，避免超出上下文限制
3. **文本截断**: 单条预案内容最长 250 字，超出部分会被截断并标记"..."
4. **max_tokens**: 方案生成时设置为 1500，确保有足够空间生成完整方案

### 6.3 降级策略

1. **数据库连接失败**: RAG 检索返回空列表，方案生成仅基于通用知识
2. **vLLM 调用失败**: 返回默认模板方案，包含事件概况、风险评估、处置目标等基本章节
3. **网络超时**: 设置 120 秒超时，避免请求无限等待

---

## 7. 附录

### 7.1 vLLM 配置信息

| 参数 | 值 |
|------|-----|
| base_url | http://127.0.0.1:8000/v1 |
| api_key | EMPTY |
| model | Qwen/Qwen2.5-7B-Instruct |
| max_model_len | 4096 |

### 7.2 方案审查评分标准

| 分数范围 | 评价 | 通过条件 |
|----------|------|----------|
| 0-3 | 严重不完整或存在重大问题，需要重新编写 | 不通过 |
| 4-6 | 基本完整但存在明显问题，需要修改完善 | 不通过（可重试） |
| 7-8 | 较为完整，存在少量问题 | 通过 |
| 9-10 | 完整、合理、可行 | 通过 |

### 7.3 重试机制

- **最大重试次数**: 3 次
- **重试条件**: 方案审查评分 ≥ 4 且 < 7（未通过但有改进空间）
- **触发场景**: 方案生成失败或审查未通过时自动重试

### 7.4 RAG 数据库配置

| 参数 | 值 |
|------|-----|
| 主机 | localhost |
| 端口 | 5432 |
| 数据库名 | emergency_vector |
| 用户名 | postgres |
| 密码 | ZAQ12wsx581! |
| 表名 | knowledge_chunks |
| 向量维度 | 512 |
| Embedding 模型 | BAAI/bge-small-zh-v1.5 |