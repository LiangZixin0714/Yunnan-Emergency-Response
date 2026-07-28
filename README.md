# 云南自然灾害应急协同决策平台

多智能体驱动的应急响应系统，基于 **Vue 3 + Spring Boot + FastAPI + LangGraph** 构建，集成 RAG 检索增强生成、多 Agent 协同工作流和智能方案生成能力。

## 🌟 项目亮点

- **多智能体协同工作流**：基于 LangGraph 编排 5 个 Agent（情报分析 → RAG 检索 → 资源调度 → 方案生成 → 方案审查），支持自动重试
- **RAG 检索增强生成**：将云南省 7 部应急预案向量化入库，结合 pgvector 实现语义检索
- **智能应急方案生成**：输入灾情描述，自动输出结构化的应急处置方案（含事件概况、风险评估、资源调度、应急措施等 7 大章节）
- **地理编码与地图态势**：灾情上报时自动调用高德 API 获取经纬度，Leaflet 地图展示灾情点位
- **全链路 Docker 化部署**：MySQL / PostgreSQL / Redis / MinIO / 前后端 / AI 服务 一键编排

---

## 🏗️ 技术栈总览

### 前端
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4+ | 前端框架 |
| TypeScript | 5.4+ | 类型安全 |
| Vite | 5.2+ | 构建工具 |
| Element Plus | 2.6+ | UI 组件库 |
| Pinia | 2.1+ | 状态管理 |
| ECharts | 5.5+ | 图表可视化 |
| Leaflet | latest | 地图展示 |

### 后端
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 编程语言 |
| Spring Boot | 3.2+ | 后端框架 |
| Spring Security | 6.2+ | 安全框架 / JWT |
| Spring Data JPA | 3.2+ | ORM 框架 |
| Redis | 7.0+ | 缓存 / 分布式锁 |
| MinIO | latest | 对象存储 |

### AI 服务
| 技术 | 版本 | 用途 |
|------|------|------|
| Python | 3.10 | 编程语言 |
| FastAPI | 0.140+ | API 框架 |
| LangGraph | 1.2+ | 多 Agent 工作流编排 |
| vLLM | 0.4+ | LLM 推理引擎 |
| Qwen2.5-7B-Instruct | - | 中文大语言模型 |
| BAAI/bge-small-zh-v1.5 | - | 中文 Embedding 模型（512 维） |
| pgvector | - | PostgreSQL 向量扩展 |

### 数据库
| 技术 | 版本 | 用途 |
|------|------|------|
| MySQL | 8.0+ | 主业务数据库 |
| PostgreSQL | 15+ | 向量数据库（含 pgvector） |
| Redis | 7.0+ | 缓存和分布式锁 |

---

## 📁 项目结构

```
ai-service/
├── frontend/                    # 前端项目 (Vue 3 + TypeScript)
│   ├── src/
│   │   ├── api/                 # API 接口封装
│   │   ├── stores/              # Pinia 状态管理
│   │   ├── views/               # 页面组件
│   │   │   ├── Home/            # 首页仪表盘
│   │   │   ├── Incident/        # 灾情管理（列表/详情/上报）
│   │   │   ├── Plan/            # AI 方案生成
│   │   │   ├── Resource/        # 资源调度
│   │   │   ├── Screen/          # 数据大屏
│   │   │   └── ...              # 登录/用户/审计等
│   │   ├── components/          # 通用组件（统计卡片、灾害地图等）
│   │   └── ...
│   └── package.json
│
├── backend/                     # 后端项目 (Spring Boot)
│   ├── src/main/java/com/project/
│   │   ├── controller/          # REST API 控制器
│   │   ├── service/             # 业务逻辑层
│   │   ├── repository/          # 数据访问层
│   │   ├── entity/mysql/        # MySQL 实体
│   │   ├── entity/postgres/     # PostgreSQL 实体
│   │   ├── dto/                 # 数据传输对象
│   │   ├── config/              # 配置类（多数据源、MinIO、Redis 等）
│   │   └── ...
│   └── pom.xml
│
├── ai-service/                  # AI 服务 (FastAPI + LangGraph)
│   ├── main.py                  # 应用入口
│   ├── agents/                  # Agent 模块
│   │   ├── orchestrator.py      # 多 Agent 编排器
│   │   ├── info_extractor.py    # 情报分析 Agent
│   │   ├── plan_generator.py    # 方案生成 Agent
│   │   ├── plan_reviewer.py     # 方案审查 Agent
│   │   └── resource_dispatcher.py # 资源调度 Agent
│   ├── rag/retriever.py         # RAG 向量检索
│   ├── services/                # 业务服务（向量化流水线）
│   ├── middleware/               # API Key 认证
│   └── requirements.txt
│
├── scripts/                     # 数据处理脚本
│   ├── parse_pdfs.py            # PDF 批量解析
│   ├── chunk_text.py            # 文本切分
│   ├── embed_and_insert.py      # Embedding + pgvector 入库
│   └── generate_mock_data.py    # 模拟数据生成
│
├── utils/                       # 通用工具模块
│   ├── pdf_parser.py            # PDF 解析
│   ├── chunker.py               # 文本切片
│   ├── embedding.py             # Embedding 生成
│   └── db.py                    # 数据库操作
│
├── data/                        # 数据目录
│   └── pdf/                     # 原始 PDF（7 个应急预案）
│
├── deploy/                      # Docker 部署配置
│   ├── docker-compose.yml       # Docker Compose 主配置
│   ├── start.sh / stop.sh       # 一键启停脚本
│   ├── backend/                 # 后端 Dockerfile
│   ├── frontend/                # 前端 Dockerfile
│   ├── ai-service/              # AI 服务 Dockerfile
│   └── postgresql/              # PostgreSQL+pgvector Dockerfile
│
├── docs/                        # 技术文档
│   ├── API_DOCUMENTATION.md
│   └── db-schema.md
│
└── 结训文档/                     # 结训文档合集
    ├── 需求规格说明书.md
    ├── 系统设计说明书.md
    ├── 前后端接口文档.md
    ├── 数据库设计.md
    ├── 测试报告.md
    └── 结项文档.md
```

---

## 🚀 快速开始

### 方式一：Docker Compose 一键部署（推荐）

```bash
cd deploy

# 启动所有服务
docker compose up -d

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f [service-name]
```

### 方式二：手动分模块启动

#### 1. 前端开发模式

```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

开发模式下前端通过 Vite 中间件 mock 全部 API，可独立运行。

#### 2. 后端开发模式

```bash
cd backend
# 确保 MySQL / PostgreSQL / Redis / MinIO 已启动
mvn spring-boot:run
# 访问 http://localhost:8080
```

#### 3. AI 服务开发模式

```bash
cd ai-service
pip install -r requirements.txt
# 确保 PostgreSQL / MinIO / vLLM 已启动
python main.py
# 访问 http://localhost:8002/docs
```

---

## 🧠 AI 服务架构

### 多 Agent 工作流

```
灾情描述输入
    │
    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  情报分析    │───►│  RAG 检索    │───►│  资源调度    │
│  Agent       │    │  Agent       │    │  Agent       │
└──────────────┘    └──────────────┘    └──────────────┘
                                               │
                                               ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  方案审查    │◄───│  重试判断    │◄───│  方案生成    │
│  Agent       │    │  (≤3次)      │    │  Agent       │
└──────────────┘    └──────────────┘    └──────────────┘
    │
    ▼
  输出方案 + 引用来源
```

### RAG 向量化流水线

```
PDF 文件 → PyMuPDF 解析 → 固定大小切片(800字)
    → BAAI/bge-small-zh-v1.5 生成 512 维向量
    → PostgreSQL pgvector 入库
```

### 降级容错策略

| 模块 | 降级策略 |
|------|---------|
| 情报分析 | vLLM 不可用时返回默认情报 |
| RAG 检索 | 数据库失败时返回空列表 |
| 资源调度 | MySQL 不可用时使用内置模板 |
| 方案生成 | vLLM 不可用时使用默认模板方案 |
| 方案审查 | vLLM 不可用时若章节完整则直接通过 |

---

## 🗄️ 数据库设计

### MySQL（emergency_db）

| 表名 | 说明 |
|------|------|
| users | 用户表 |
| roles | 角色表 |
| incidents | 灾情表（含 latitude/longitude） |
| plans | 方案表 |
| citations | 引用表 |
| emergency_resource | 应急资源表 |
| resource_dispatch_record | 调度记录表 |
| audit_logs | 审计日志表 |
| agent_runs | Agent 执行记录表 |

### PostgreSQL（emergency_vector）

| 表名 | 说明 |
|------|------|
| knowledge_chunks | 知识片段表（512 维向量） |
| locations | 地理位置表 |
| resources | PostgreSQL 资源表 |

### pgvector 知识片段结构

```sql
CREATE TABLE knowledge_chunks (
    id SERIAL PRIMARY KEY,
    chunk_id VARCHAR(255) UNIQUE,
    document_name VARCHAR(255),
    document_type VARCHAR(50),
    chapter VARCHAR(50),
    section VARCHAR(50),
    page INTEGER,
    content TEXT,
    length INTEGER,
    "order" INTEGER,
    source VARCHAR(255),
    publish_org VARCHAR(255),
    publish_date VARCHAR(20),
    version VARCHAR(50),
    embedding vector(512),
    model_name VARCHAR(100),
    created_at TIMESTAMP
);
```

---

## 🔌 API 接口速览

### 后端接口（Spring Boot）

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 认证 | POST | /api/auth/login | 用户登录 |
| 灾情 | POST | /api/incident/report | 灾情上报（multipart/form-data） |
| 灾情 | GET | /api/incident/list | 灾情列表（分页+筛选） |
| 灾情 | GET | /api/incident/detail | 灾情详情 |
| 方案 | POST | /api/plan/generate | AI 生成方案 |
| 方案 | GET | /api/plan/stream | 方案流式输出（SSE） |
| 资源 | GET | /api/resource/list | 资源列表 |
| 资源 | POST | /api/resource/dispatch | 资源调度（Redis 分布式锁） |
| 系统 | GET | /api/dashboard/overview | 概览统计 |
| 系统 | GET | /api/dashboard/map-data | 地图数据（含经纬度） |

### AI 服务接口（FastAPI）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /health | 健康检查 |
| POST | /api/v1/generate-plan | 同步生成方案 |
| POST | /api/v1/generate-plan/stream | 流式生成方案（SSE） |
| POST | /api/v1/knowledge/vectorize | 知识库向量化 |
| POST | /api/v1/knowledge/vectors/delete | 向量删除 |

> AI 服务 API 需携带 `X-API-Key: emergency-platform-ai-service-key-2024` 请求头

### 统一响应格式

```json
{
    "code": 0,
    "message": "success",
    "data": { }
}
```

---

## 🔐 角色权限

| 功能 | VIEWER | OPERATOR | RESOURCE_MANAGER | ADMIN |
|------|:------:|:--------:|:----------------:|:-----:|
| 灾情上报 | ✅ | ✅ | ✅ | ✅ |
| 方案生成 | ❌ | ✅ | ❌ | ✅ |
| 资源调度 | ❌ | ❌ | ✅ | ✅ |
| 用户管理 | ❌ | ❌ | ❌ | ✅ |
| 审计日志 | ❌ | ❌ | ❌ | ✅ |

**默认账号**：`admin` / `operator` / `resource` / `viewer`，密码统一为 `ZAQ12wsx581!`

---

## 📊 数据统计

| 数据类型 | 数量 |
|----------|------|
| 用户 | 4 条 |
| 灾情事件 | 58 条 |
| 应急资源 | 20 条 |
| 知识片段（向量） | 201 条（512 维） |
| 应急预案 PDF | 7 部 |

---

## 🌐 服务端口

| 服务 | 端口 | 访问地址 |
|------|------|---------|
| 前端应用 | 3000 | http://localhost:3000 |
| 后端 API | 8080 | http://localhost:8080 |
| AI 服务 | 8002 | http://localhost:8002/docs |
| MySQL | 3306 | localhost:3306 |
| PostgreSQL | 5434 | localhost:5434 |
| Redis | 6379 | localhost:6379 |
| MinIO API | 9000 | http://localhost:9000 |
| MinIO Console | 9001 | http://localhost:9001 |

---

## 📚 相关文档

- [需求规格说明书](结训文档/需求规格说明书.md)
- [系统设计说明书](结训文档/系统设计说明书.md)
- [前后端接口文档](结训文档/前后端接口文档.md)
- [数据库设计](结训文档/数据库设计.md)
- [测试报告](结训文档/测试报告.md)
- [结项文档](结训文档/结项文档.md)
- [API 文档](docs/API_DOCUMENTATION.md)

---

## 📝 License

MIT License
