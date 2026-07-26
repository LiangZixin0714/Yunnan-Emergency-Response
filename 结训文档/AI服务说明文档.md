# AI 服务说明文档

## 1. 概述

AI 服务是云南自然灾害应急协同决策平台的核心智能引擎，基于 **FastAPI** 框架构建，集成了 **LangGraph 多 Agent 编排**、**RAG 检索增强生成**、**向量化知识库** 和 **方案智能审查** 等核心能力。服务接收灾情描述，通过多 Agent 协作自动完成情报分析、预案检索、资源调度、方案生成和合规审查，最终输出一份结构化的应急处置方案。

### 技术栈

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| Web 框架 | FastAPI | latest | 异步 REST API 服务 |
| ASGI 服务器 | Uvicorn | latest | 高性能 HTTP 服务器 |
| Agent 编排 | LangGraph | latest | 多 Agent 状态机工作流 |
| LLM 推理 | vLLM / OpenAI Client | - | 对接本地部署的 Qwen2.5 模型 |
| 向量数据库 | PostgreSQL + pgvector | 15+ | 文档向量存储与检索 |
| Embedding 模型 | BAAI/bge-small-zh-v1.5 | 512 维 | 中文文本向量化 |
| 对象存储 | MinIO | latest | PDF 文件持久化存储 |
| 关系数据库 | MySQL 8.0+ | 8.0+ | 资源数据查询 |
| PDF 解析 | PyMuPDF / pypdf | latest | PDF 文本提取 |

### 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        AI 服务 (Port: 8002)                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐    ┌──────────────────────────────────────┐  │
│  │  FastAPI App  │    │         LangGraph 工作流编排         │  │
│  │              │    │                                      │  │
│  │  /health     │    │  ┌─────────┐  ┌─────────┐           │  │
│  │  /api/v1/    │    │  │ 情报分析 │→│ RAG 检索 │           │  │
│  │  generate-   │    │  └─────────┘  └─────────┘           │  │
│  │  plan        │    │       ↓            ↓                 │  │
│  │              │    │  ┌─────────┐  ┌─────────┐           │  │
│  │  /api/v1/    │    │  │ 资源调度 │→│ 方案生成 │           │  │
│  │  knowledge/  │    │  └─────────┘  └─────────┘           │  │
│  │  vectorize   │    │       ↓            ↓                 │  │
│  │              │    │  ┌─────────┐  ┌─────────┐           │  │
│  └──────┬───────┘    │  │ 方案审查 │←│ 重试判断 │           │  │
│         │            │  └─────────┘  └─────────┘           │  │
│         │            └──────────────────────────────────────┘  │
│         │                                                       │
│  ┌──────▼───────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  向量数据库   │  │  对象存储   │  │     MySQL 数据库    │  │
│  │  PostgreSQL  │  │   MinIO     │  │  (资源数据查询)    │  │
│  │  + pgvector  │  │  (PDF文件)  │  └─────────────────────┘  │
│  └──────────────┘  └─────────────┘                           │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 目录结构

```
ai-service/
├── main.py                      # 应用入口，FastAPI 路由定义
├── requirements.txt             # Python 依赖清单
├── agents/                      # Agent 模块
│   ├── orchestrator.py          # 多 Agent 编排器 (LangGraph)
│   ├── info_extractor.py        # 情报分析 Agent
│   ├── plan_generator.py        # 方案生成 Agent
│   ├── plan_reviewer.py         # 方案审查 Agent
│   ├── resource_dispatcher.py   # 资源调度 Agent
│   └── audit_client.py          # 审计日志客户端
├── rag/                         # RAG 检索模块
│   └── retriever.py             # 向量检索与 Embedding 生成
├── services/                    # 业务服务
│   └── vectorize_service.py     # 向量化流水线
├── routers/                     # API 路由
│   └── vectorize_router.py      # 向量化相关路由
├── middleware/                  # 中间件
│   └── api_key_auth.py          # API Key 认证中间件
└── utils/                       # 工具模块
    └── minio_client.py          # MinIO 文件操作客户端
```

---

## 3. Agent 工作流详解

### 3.1 工作流总览

AI 服务的核心是基于 **LangGraph StateGraph** 构建的多 Agent 工作流。该工作流由 5 个节点组成，以有向图的形式串联执行，并支持方案审查不通过时的自动重试（最多 3 次）。

### 3.2 工作流状态定义

```python
class AgentState(TypedDict):
    description: str              # 原始灾情描述
    info: Dict[str, Any]          # 情报分析结果
    retrieved_plans: List[Dict]   # RAG 检索到的预案片段
    resources: List[Dict]         # 可用资源列表
    plan: str                     # 生成的处置方案
    review: Dict[str, Any]        # 方案审查结果
    messages: Annotated[List, add] # 执行日志消息列表
    retry_count: int              # 方案重试计数
```

### 3.3 工作流节点说明

#### 节点 1：情报分析 (extract_info)

**模块**: [info_extractor.py](file:///Users/fangyuhan/Desktop/python/Yunnan-Emergency-Response/ai-service/agents/info_extractor.py)

**功能**: 从灾情描述文本中提取结构化的关键情报信息。

**输入**: 原始灾情描述（如"云南省昆明市五华区发生4.5级地震"）

**输出**: 结构化情报字典

```json
{
    "type": "地震",
    "level": "高",
    "location": "云南省昆明市五华区",
    "affected_population": 500,
    "confidence": 0.9
}
```

**实现细节**:
- 使用 vLLM 部署的 **Qwen2.5-7B-Instruct** 模型进行推理
- 通过精心设计的 System Prompt 指导模型输出 JSON 格式的结构化信息
- 支持灾害类型识别：地震、滑坡、洪涝、干旱、森林火灾、泥石流、其他
- 置信度范围 0.0-1.0，表示模型对提取结果的可信程度
- 失败时降级返回默认值，不中断工作流

**Prompt 策略**:
- 采用 Qwen 原生 ChatTemplate 格式（`<|im_start|>` / `<|im_end|>`）
- 温度参数设为 0.3，确保输出稳定性
- 通过 `stop=["<|im_end|>"]` 控制生成终止

---

#### 节点 2：RAG 检索 (retrieve_plans)

**模块**: [retriever.py](file:///Users/fangyuhan/Desktop/python/Yunnan-Emergency-Response/ai-service/rag/retriever.py)

**功能**: 基于情报分析结果构建查询向量，从向量数据库中检索最相关的应急预案片段。

**输入**: 情报分析结果（灾害类型 + 位置）

**输出**: 最相关的 3 条预案片段

**实现流程**:
1. 构建查询文本：`"{灾害类型} {位置} 应急处置"`
2. 使用 **BAAI/bge-small-zh-v1.5** 模型生成 512 维查询向量
3. 从 PostgreSQL `knowledge_chunks` 表中加载所有已入库的向量数据
4. 在 Python 端计算余弦相似度（Cosine Similarity）进行排序
5. 返回 Top-N 最相关的预案片段

**向量检索算法**:
```
similarity = dot(v_query, v_stored) / (norm(v_query) * norm(v_stored))
```

**Embedding 模型**:
- 模型：BAAI/bge-small-zh-v1.5（中文通用 Embedding 模型）
- 维度：512
- 运行设备：CPU（自动回退）
- 加载方式：懒加载（首次调用时加载，`local_files_only=True`）

---

#### 节点 3：资源调度 (dispatch_resources)

**模块**: [resource_dispatcher.py](file:///Users/fangyuhan/Desktop/python/Yunnan-Emergency-Response/ai-service/agents/resource_dispatcher.py)

**功能**: 根据灾害类型和位置，从 MySQL 数据库查询可用的应急资源。

**输入**: 情报分析结果（类型、等级、位置）

**输出**: 推荐资源列表

**实现流程**:
1. 根据灾害类型查找资源类型映射表（DISASTER_RESOURCE_MAP）
2. 查询 MySQL `emergency_resource` 表中对应类型的可用资源
3. 按可用库存降序排列，返回 Top 15 条
4. 数据库查询失败时，使用内置资源模板生成推荐资源

**灾害-资源映射**:

| 灾害类型 | 推荐资源类型 |
|---------|-------------|
| 地震、滑坡、洪涝、干旱、泥石流、台风 | 设备、人员、物资、医疗、生活 |
| 森林火灾、火灾 | 设备、人员、物资、医疗 |

**降级策略**:
- 数据库不可用时，使用 `_generate_recommended_resources()` 生成模拟资源
- 方案等级为"高"时，资源数量 ×1.5；等级为"低"时 ×0.7

---

#### 节点 4：方案生成 (generate_plan)

**模块**: [plan_generator.py](file:///Users/fangyuhan/Desktop/python/Yunnan-Emergency-Response/ai-service/agents/plan_generator.py)

**功能**: 结合情报分析结果、RAG 检索到的预案、可用资源信息，生成一份完整的应急处置方案。

**输入**: 情报信息 + 原始描述 + RAG 预案片段 + 资源列表

**输出**: 结构化应急处置方案文本

**方案结构**（7 个章节）:
1. **事件概况** — 灾害基本信息
2. **风险评估** — 可能的损失和影响
3. **处置目标** — 总体和具体目标
4. **资源调度建议** — 基于可用资源制定调度方案
5. **应急措施** — 详细处置步骤
6. **保障措施** — 通信、物资、医疗、安全保障
7. **参考资料** — 引用的预案文档来源

**实现细节**:
- 最多引用 3 条 RAG 检索结果（每条限 250 字）
- 最多引用 5 条可用资源
- 生成时温度 0.7，保证方案的创造性和多样性
- 最大输出 1500 tokens
- 支持流式生成（`stream=True`）用于 SSE 响应

**降级策略**:
- vLLM 调用失败时，使用 `_generate_default_plan()` 生成模板化方案
- 默认方案包含资源调度建议和参考资料章节

---

#### 节点 5：方案审查 (review_plan)

**模块**: [plan_reviewer.py](file:///Users/fangyuhan/Desktop/python/Yunnan-Emergency-Response/ai-service/agents/plan_reviewer.py)

**功能**: 对生成的方案进行合规性审查，判断是否通过。

**输入**: 生成的处置方案文本

**输出**: 审查结果

```json
{
    "score": 8,
    "issues": [],
    "passed": true,
    "suggestions": ["方案结构完整，建议人工复核内容细节"]
}
```

**审查流程**:
1. **规则检查**: 首先使用 `_check_sections()` 检查方案是否包含所有必需章节
2. **LLM 审查**: 使用 vLLM 进行深度内容审查
3. **降级处理**: vLLM 不可用时，若章节完整则直接标记为通过

**评分标准**:

| 分数段 | 含义 |
|-------|------|
| 0-3 分 | 严重不完整，需重写 |
| 4-6 分 | 基本完整，需修改 |
| 7-8 分 | 较为完整，少量问题 |
| 9-10 分 | 完整合理可行 |

**通过条件**: `score >= 7` 且 `passed = true`

**必需章节**: 事件概况、风险评估、处置目标、资源调度建议、应急措施、保障措施

---

### 3.4 工作流流转与重试

```
         ┌──────────────────────────────────┐
         │          工作流入口               │
         └─────────────┬────────────────────┘
                       ↓
         ┌──────────────────────────────────┐
         │  情报分析 (extract_info)         │
         └─────────────┬────────────────────┘
                       ↓
         ┌──────────────────────────────────┐
         │  RAG 检索 (retrieve_plans)      │
         └─────────────┬────────────────────┘
                       ↓
         ┌──────────────────────────────────┐
         │  资源调度 (dispatch_resources)   │
         └─────────────┬────────────────────┘
                       ↓
         ┌──────────────────────────────────┐
         │  方案生成 (generate_plan)        │◄────────┐
         └─────────────┬────────────────────┘         │
                       ↓                              │
         ┌──────────────────────────────────┐         │
         │  方案审查 (review_plan)          │         │
         └─────────────┬────────────────────┘         │
                       ↓                              │
              ┌─────────────────┐                     │
              │  _should_retry? │                     │
              └────────┬────────┘                     │
                       │                              │
          ┌────────────┴────────────┐                 │
          │ passed=true             │ passed=false    │
          │ retry_count >= 3        │ score >= 6     │
          │         或              │ retry_count < 3 │
          └────────────┬────────────┘                 │
                       ↓                              │
              ┌──────────────┐                        │
              │   结束 (END)  │   retry_count++  → 生成方案
              └──────────────┘                              ↑
                                                            └────────┘
```

**重试机制**:
- 最多重试 3 次（MAX_RETRY_COUNT = 3）
- 仅当审查不通过但评分 ≥ 6 分时触发重试
- 重试时重新调用方案生成节点，利用上下文信息可能产生不同结果

---

## 4. RAG 检索增强生成

### 4.1 整体架构

RAG（Retrieval-Augmented Generation）流程分为两个阶段：

1. **索引阶段（离线/异步）**: PDF → 文本解析 → 切片 → Embedding → pgvector 入库
2. **检索阶段（在线/实时）**: 查询 → Embedding → 向量检索 → 返回相关片段 → 注入 LLM Prompt

### 4.2 向量化流水线

**模块**: [vectorize_service.py](file:///Users/fangyuhan/Desktop/python/Yunnan-Emergency-Response/ai-service/services/vectorize_service.py)

#### 流水线阶段

**阶段 1 — PDF 下载**:
- 从 MinIO 对象存储下载指定的 PDF 文件
- 使用 `minio` 客户端库，通过容器名 `minio:9000` 访问
- 下载到本地临时路径

**阶段 2 — PDF 解析**:
- 使用 PyMuPDF 提取 PDF 文本内容
- 支持中文字符编码
- 异常处理：解析失败返回 None

**阶段 3 — 文本切片**:
- 固定大小切片策略（chunk_size=800 字符，overlap=100 字符）
- 相邻切片有 100 字符重叠，保证上下文连续性
- 单个文档最多 500 个切片（防止过大）

**阶段 4 — Embedding 生成**:
- 使用 BAAI/bge-small-zh-v1.5 模型生成 512 维向量
- 采用 Mean Pooling 方式聚合 token 嵌入
- CPU 推理模式，`torch.no_grad()` 优化

**阶段 5 — 向量入库**:
- 写入 PostgreSQL `knowledge_chunks` 表
- 存储格式：`double precision[]` 数组
- 记录每个切片的文档名、章节、页码等元信息

#### 向量化状态机

```
  pending ──► processing ──► completed
                    │
                    └──► failed (可重试)
```

### 4.3 向量数据库设计

**表结构 (knowledge_chunks)**:

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | SERIAL | 主键 |
| chunk_id | VARCHAR(255) | 切片唯一标识 (fileId_index) |
| document_name | VARCHAR(255) | 文档名称 |
| document_type | VARCHAR(50) | 文档类型 |
| chapter | VARCHAR(50) | 章节名 |
| section | VARCHAR(50) | 小节 |
| page | INTEGER | 页码 |
| content | TEXT | 切片文本内容 |
| length | INTEGER | 内容长度 |
| order | INTEGER | 原始顺序 |
| source | VARCHAR(255) | 来源 |
| publish_org | VARCHAR(255) | 发布机构 |
| publish_date | VARCHAR(20) | 发布日期 |
| version | VARCHAR(50) | 版本 |
| embedding | double[] | 512 维 Embedding 向量 |
| model_name | VARCHAR(100) | 模型名称 |
| created_at | TIMESTAMP | 创建时间 |

### 4.4 Embedding 模型

| 属性 | 值 |
|-----|-----|
| 模型名称 | BAAI/bge-small-zh-v1.5 |
| 输出维度 | 512 |
| 适用语言 | 中文为主 |
| 参数量 | ~100M |
| 推理设备 | CPU |
| 加载方式 | 懒加载 + 本地缓存 |

---

## 5. API 接口文档

### 5.1 健康检查

```
GET /health
```

**响应**:
```json
{
    "status": "ok",
    "service": "ai-service",
    "workflow_ready": true
}
```

### 5.2 方案生成

#### 同步生成
```
POST /api/v1/generate-plan
Content-Type: application/json

{
    "description": "云南省昆明市五华区发生4.5级地震",
    "incidentId": "inc-001"
}
```

**响应**:
```json
{
    "plan": "# 地震应急处置方案\n\n## 一、事件概况\n..."
}
```

#### 流式生成 (SSE)
```
POST /api/v1/generate-plan/stream
Content-Type: application/json

{
    "description": "云南省昆明市五华区发生4.5级地震",
    "incidentId": "inc-001"
}
```

**响应格式** (Server-Sent Events):
```
data: {"chunk": "# 地震应急"}

data: {"chunk": "处置方案\n\n## 一、"}

...

data: {"done": true}
```

### 5.3 知识库向量化

```
POST /api/v1/knowledge/vectorize
Content-Type: application/json
X-API-Key: emergency-platform-ai-service-key-2024

{
    "objectKey": "knowledge/20260726/file.pdf",
    "bucket": "emergency-knowledge",
    "fileId": "uuid-string",
    "fileName": "地震应急预案.pdf",
    "chunkSize": 800,
    "chunkOverlap": 100
}
```

**响应**:
```json
{
    "status": "completed",
    "fileId": "uuid-string",
    "chunkCount": 13,
    "failReason": ""
}
```

### 5.4 向量删除

```
POST /api/v1/knowledge/vectors/delete
Content-Type: application/json
X-API-Key: emergency-platform-ai-service-key-2024

{
    "sourceFile": "地震应急预案.pdf"
}
```

**响应**:
```json
{
    "deletedCount": 13
}
```

---

## 6. 审计日志

### 6.1 审计机制

每个工作流执行完成后，系统会自动向后端提交审计日志，记录 Agent 的执行过程和结果。

**审计内容**:
- `runId`: 执行唯一标识（UUID）
- `incidentId`: 关联灾情 ID
- `agentName`: Agent 名称（如 orchestrator）
- `inputParams`: 输入参数
- `outputResult`: 输出结果（方案内容摘要）
- `status`: 执行状态（success / failed）
- `startTime` / `endTime`: 执行时间
- `citations`: RAG 引用的预案来源列表
- `errorMessage`: 错误信息（失败时）

### 6.2 同步/异步兼容

审计提交采用 `_safe_submit_audit()` 函数，能自动检测当前运行环境：

- **异步上下文**（FastAPI async handler）：通过 `loop.create_task()` 异步提交
- **同步上下文**（单元测试、脚本）：通过 `asyncio.run()` 提交

---

## 7. 配置说明

### 7.1 环境变量

| 变量名 | 默认值 | 说明 |
|-------|--------|------|
| PORT | 8002 | 服务端口 |
| PG_HOST | localhost | PostgreSQL 主机 |
| PG_PORT | 5432 | PostgreSQL 端口 |
| PG_DATABASE | emergency_vector | 向量数据库名 |
| PG_USER | postgres | 数据库用户名 |
| PG_PASSWORD | ZAQ12wsx581! | 数据库密码 |
| MINIO_ENDPOINT | localhost:9000 | MinIO 服务地址 |
| MINIO_ACCESS_KEY | admin | MinIO Access Key |
| MINIO_SECRET_KEY | ZAQ12wsx581! | MinIO Secret Key |
| MINIO_SECURE | false | 是否启用 HTTPS |
| MYSQL_HOST | localhost | MySQL 主机（资源调度） |
| MYSQL_PORT | 3306 | MySQL 端口 |
| MYSQL_DATABASE | emergency_db | MySQL 数据库名 |
| BACKEND_URL | http://localhost:8080 | 后端服务地址 |
| AI_SERVICE_API_KEY | - | API Key 认证密钥 |
| VLLM_BASE_URL | http://127.0.0.1:8000/v1 | vLLM 推理服务地址 |
| VLLM_API_KEY | EMPTY | vLLM API Key |

### 7.2 Docker Compose 配置

```yaml
ai-service:
  container_name: emergency-ai-service
  ports:
    - "8002:8002"
  environment:
    PORT: "8002"
    PG_HOST: postgres
    PG_PORT: "5432"
    PG_DATABASE: emergency_vector
    PG_USER: postgres
    PG_PASSWORD: ZAQ12wsx581!
    MINIO_ENDPOINT: minio:9000
    MINIO_ACCESS_KEY: admin
    MINIO_SECRET_KEY: ZAQ12wsx581!
    MINIO_SECURE: "false"
    BACKEND_URL: http://backend:8080
    AI_SERVICE_API_KEY: emergency-platform-ai-service-key-2024
  volumes:
    - ../ai-service:/app
    - ai-model-cache:/root/.cache/huggingface
  depends_on:
    postgres:
      condition: service_healthy
    minio:
      condition: service_healthy
    backend:
      condition: service_started
```

### 7.3 API Key 认证

除 `/health` 端点外，所有 API 调用必须在 HTTP Header 中携带 `X-API-Key` 字段。

**密钥值**: `emergency-platform-ai-service-key-2024`

---

## 8. 数据流转全链路

### 8.1 灾情上报 → 方案生成

```
用户上报灾情
    │
    ▼
前端 (Vue) ──► 后端 (Spring Boot)
                    │
                    │ POST /api/ai/generate-plan
                    │
                    ▼
                 AI 服务 (FastAPI)
                    │
                    │ 1. 情报分析 Agent
                    │    └─ vLLM → 结构化情报
                    │
                    │ 2. RAG 检索 Agent
                    │    └─ Embedding → pgvector 相似度检索
                    │
                    │ 3. 资源调度 Agent
                    │    └─ MySQL → 可用资源列表
                    │
                    │ 4. 方案生成 Agent
                    │    └─ vLLM → 完整处置方案
                    │
                    │ 5. 方案审查 Agent
                    │    └─ vLLM + 规则 → 评分/通过
                    │
                    │ 6. 审计日志提交
                    │    └─ httpx → 后端 AgentRun 表
                    │
                    ▼
                 返回方案 + 引用来源
```

### 8.2 知识库管理 → RAG 入库

```
用户上传 PDF
    │
    ▼
前端 (Vue) ──► 后端 (Spring Boot)
                    │
                    │ 1. 保存文件到 MinIO
                    │
                    │ 2. 记录元信息到 MySQL
                    │
                    │ 3. 异步调用 AI 服务向量化
                    │ POST /api/v1/knowledge/vectorize
                    │
                    ▼
                 AI 服务 向量化流水线
                    │
                    │ ① 下载 PDF (MinIO)
                    │ ② 解析文本 (PyMuPDF)
                    │ ③ 切片 (800字/片, 100字重叠)
                    │ ④ Embedding (bge-small-zh-v1.5)
                    │ ⑤ 写入 pgvector (PostgreSQL)
                    │
                    ▼
                 更新向量化状态 (completed/failed)
```

---

## 9. 容错与降级策略

| 模块 | 降级策略 |
|-----|---------|
| 情报分析 | vLLM 不可用时返回默认情报（类型=其他，等级=中） |
| RAG 检索 | 数据库连接失败时返回空列表，不中断工作流 |
| 资源调度 | MySQL 不可用时使用内置资源模板生成推荐 |
| 方案生成 | vLLM 不可用时使用模板化默认方案 |
| 方案审查 | vLLM 不可用时若章节完整则直接通过 |
| 审计日志 | 提交失败仅记录日志，不影响主流程 |
| 向量化 | 单条 Embedding 失败跳过，整体失败返回失败状态 |

---

## 10. 性能指标

| 指标 | 参考值 |
|-----|--------|
| 情报分析耗时 | 1-3 秒 |
| RAG 检索耗时 | 2-5 秒（含 Embedding 生成） |
| 资源调度耗时 | < 1 秒（数据库查询） |
| 方案生成耗时 | 5-15 秒（取决于方案长度） |
| 方案审查耗时 | 1-3 秒 |
| 端到端工作流耗时 | 15-30 秒 |
| PDF 向量化耗时 | 30-60 秒（13 个切片） |
| Embedding 生成耗时 | ~0.5 秒/条（CPU） |

---

## 11. 扩展指南

### 添加新的 Agent

1. 在 `agents/` 目录下创建新的 Agent 模块
2. 实现 Agent 函数，接收 `AgentState` 作为输入，返回状态字典
3. 在 `orchestrator.py` 的 `build_workflow()` 中注册节点
4. 连接到适当的前驱和后继节点

### 添加新的工具/数据源

1. 如需对接外部 API，在 `utils/` 下创建新的客户端模块
2. 在 Agent 中引入并调用
3. 更新 `docker-compose.yml` 添加新的环境变量

### 更换 LLM 模型

1. 修改 `info_extractor.py`、`plan_generator.py`、`plan_reviewer.py` 中的 `VLLM_BASE_URL`
2. 更新 `_client.completions.create()` 中的 `model` 参数
3. 如有需要，调整 Prompt 格式以匹配新模型的 ChatTemplate

### 增大向量化规模

- 修改 `_chunk_text()` 中的 `chunk_size` 和 `chunk_overlap` 参数
- 调整 `run_vectorize_pipeline()` 中的最大切片数限制（当前 500）
- 注意 Embedding 模型的最大 token 限制（512）
