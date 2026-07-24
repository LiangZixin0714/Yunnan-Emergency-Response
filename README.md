# 云南自然灾害应急协同决策平台

多智能体驱动的应急响应系统，包含前端、数据处理流水线两个模块。后端（Spring Boot）和 AI 服务（FastAPI）由其他成员负责。

## 项目结构

```
Yunnan-Emergency-Response/
├── frontend/                # 前端项目（Vue3 + TypeScript）
├── scripts/                 # 数据处理脚本
│   ├── config.py            # 统一配置（数据库、模型、Chunk参数）
│   ├── parse_pdfs.py        # PDF批量解析
│   ├── chunk_text.py        # 文本切分
│   ├── embed_and_insert.py  # Embedding生成 + pgvector入库
│   ├── generate_mock_data.py# 模拟业务数据生成
│   ├── mock_users.py
│   ├── mock_incidents.py
│   ├── mock_resources.py
│   └── output_writer.py
├── utils/                   # 工具模块
│   ├── logger.py            # 统一日志
│   ├── pdf_parser.py        # PDF解析（PyMuPDF）
│   ├── text_cleaner.py      # 文本清洗
│   ├── chunker.py           # Chunk切分（LangChain）
│   ├── metadata.py          # Metadata生成
│   ├── embedding.py         # Embedding生成（bge-small-zh-v1.5）
│   └── db.py                # pgvector数据库操作
├── data/
│   ├── pdf/                 # 原始PDF文件（7个应急预案）
│   ├── txt/                 # 解析后的文本
│   ├── chunks/              # 切分后的Chunk JSON
│   ├── logs/                # 运行日志
│   └── embeddings/          # Embedding中间数据
├── models/                  # 本地模型权重（已gitignore）
└── backend/                 # 后端项目（待对接）
```

---

## 一、前端模块

### 技术栈

Vue 3 + TypeScript + Vite + Element Plus + Pinia + ECharts + Axios

### 启动方式

```bash
cd frontend
npm install
npm run dev
```

开发模式下，前端通过 Vite 中间件（`src/mock/index.ts`）模拟全部后端 API，无需后端即可独立运行。

### 前端→后端 API 对接规范

#### 1. 通用约定

| 项目 | 说明 |
|------|------|
| baseURL | `/api`（Vite 代理，生产环境需 Nginx 反向代理） |
| 认证方式 | `Authorization: Bearer <token>` |
| 响应格式 | `{ code: number, message: string, data: T }` |
| 成功码 | `code === 0` 时前端自动解包为 `data` |
| 401处理 | 自动跳转登录页，清除Token |

> **重要**：前端 Axios 响应拦截器在 `code===0` 时直接返回 `res.data`（内层数据），所以前端 Store 和页面中直接访问字段，不需要再 `.data`。

#### 2. Token 存储

- JWT Token 存储在 Pinia（内存），**不使用 localStorage**
- 刷新页面后需重新登录
- Token 通过 `utils/token.ts` 中间层读写，避免循环依赖

#### 3. 完整 API 列表

##### 登录认证

| 方法 | 路径 | 请求体 | 响应 data |
|------|------|--------|-----------|
| POST | `/login` | `{ username, password }` | `{ token, userInfo }` |

**userInfo 结构：**
```typescript
{
  userId: string
  username: string
  realName: string
  role: 'admin' | 'commander' | 'reporter'
}
```

##### 灾情管理

| 方法 | 路径 | 参数 | 响应 data |
|------|------|------|-----------|
| GET | `/incident/list` | `?page&size&disasterType?&incidentLevel?&status?&keyword?` | `{ total, list }` |
| GET | `/incident/detail` | `?incidentId` | `Incident` |
| POST | `/incident/report` | `multipart/form-data` | `{ incidentId, imageUrls }` |

**Incident 结构：**
```typescript
{
  incidentId: string
  incidentName: string
  disasterType: 'earthquake' | 'mudslide' | 'flood' | 'drought' | 'landslide' | 'fire' | 'other'
  incidentLevel: 'I' | 'II' | 'III' | 'IV'
  occurTime: string           // ISO 8601
  location: string
  description: string
  status: 'pending' | 'processing' | 'completed'
  attachments: { fileName, fileUrl, fileSize }[]
  imageUrls: string[]
  reporterId: string
  reportTime: string
}
```

**灾情上报（POST）请求格式：** `multipart/form-data`
- 普通字段：`incidentName`, `disasterType`, `incidentLevel`, `occurTime`, `location`, `description`
- 图片字段：`images`（多文件，最多5张，支持 jpg/png/gif/webp，单张≤10MB）

##### 资源调度

| 方法 | 路径 | 参数 | 响应 data |
|------|------|------|-----------|
| GET | `/resource/list` | `?resourceType?` | `Resource[]` |
| POST | `/resource/dispatch` | `{ resourceId, incidentId, quantity }` | `null` |

**Resource 结构：**
```typescript
{
  resourceId: string
  resourceName: string
  resourceType: 'team' | 'medical' | 'vehicle' | 'shelter'
  quantity: number
  availableQuantity: number
  status: 'available' | 'dispatched'
  region: string
}
```

##### AI方案生成

| 方法 | 路径 | 参数 | 响应 data |
|------|------|------|-----------|
| GET | `/plan/list` | `?incidentId` | `Plan[]` |
| POST | `/plan/generate` | `{ incidentId }` | `{ planId }` |
| GET | `/plan/detail` | `?planId` | `Plan` |

**Plan 结构：**
```typescript
{
  planId: string
  incidentId: string
  planTitle: string
  planContent: string
  generateTime: string
  status: 'draft' | 'approved' | 'rejected'
}
```

> `POST /plan/generate` 超时设置为 60 秒，后端需在此时间内返回结果。

##### 数据大屏

| 方法 | 路径 | 参数 | 响应 data |
|------|------|------|-----------|
| GET | `/dashboard/overview` | - | `{ todayCount, activeCount, pendingCount }` |
| GET | `/dashboard/trend` | - | `{ dates: string[], counts: number[] }` |
| GET | `/dashboard/distribution` | - | `{ types: string[], counts: number[] }` |
| GET | `/dashboard/screen` | - | `{ statistics, incidents, resources }` |

#### 4. 枚举值对照表

| 枚举 | 值 | 中文标签 |
|------|------|---------|
| DisasterType | `earthquake` | 地震 |
| | `mudslide` | 泥石流 |
| | `flood` | 洪涝 |
| | `drought` | 干旱 |
| | `landslide` | 山体滑坡 |
| | `fire` | 森林火灾 |
| | `other` | 其他 |
| IncidentLevel | `I` | Ⅰ级（特别重大） |
| | `II` | Ⅱ级（重大） |
| | `III` | Ⅲ级（较大） |
| | `IV` | Ⅳ级（一般） |
| IncidentStatus | `pending` | 待处理 |
| | `processing` | 处理中 |
| | `completed` | 已完成 |
| ResourceType | `team` | 救援队伍 |
| | `medical` | 医疗物资 |
| | `vehicle` | 运输车辆 |
| | `shelter` | 临时安置点 |
| ResourceStatus | `available` | 可用 |
| | `dispatched` | 已调度 |
| PlanStatus | `draft` | 草稿 |
| | `approved` | 已审批 |
| | `rejected` | 已驳回 |
| UserRole | `admin` | 系统管理员 |
| | `commander` | 应急指挥人员 |
| | `reporter` | 灾情上报人员 |

#### 5. 前端页面路由

| 路径 | 页面 | 需认证 |
|------|------|--------|
| `/login` | 登录 | 否 |
| `/home` | 首页仪表盘 | 是 |
| `/incident/list` | 灾情事件列表 | 是 |
| `/incident/report` | 灾情上报 | 是 |
| `/incident/:id` | 事件详情 | 是 |
| `/resource` | 资源调度 | 是 |
| `/plan` | AI方案生成 | 是 |
| `/screen` | 大屏展示 | 是 |

---

## 二、数据处理模块

### 技术栈

Python 3.11 + PyMuPDF + LangChain + Transformers (bge-small-zh-v1.5) + psycopg2 + pgvector

### 执行流程

```bash
# 1. PDF解析 → data/txt/
python scripts/parse_pdfs.py

# 2. Chunk切分 → data/chunks/
python scripts/chunk_text.py

# 3. Embedding + pgvector入库
$env:PG_PASSWORD="postgres"
python scripts/embed_and_insert.py
```

### 数据处理结果

| 步骤 | 输入 | 输出 | 数量 |
|------|------|------|------|
| PDF解析 | `data/pdf/` (7个PDF) | `data/txt/` (7个txt) | 125,819字符 |
| Chunk切分 | `data/txt/` | `data/chunks/` (7个JSON) | 201个Chunk |
| Embedding入库 | `data/chunks/` | pgvector `knowledge_chunks` 表 | 201条记录，512维向量 |

### pgvector 表结构

后端和AI服务需查询此表进行RAG检索：

```sql
CREATE TABLE knowledge_chunks (
    id            SERIAL PRIMARY KEY,
    chunk_id      VARCHAR(255) UNIQUE NOT NULL,  -- 格式: {文档名}_{序号}
    document_name VARCHAR(255) NOT NULL,          -- 文档名称
    document_type VARCHAR(50) NOT NULL,           -- 文档类型枚举
    chapter       VARCHAR(50) NOT NULL,           -- 一级章节
    section       VARCHAR(50),                    -- 二级章节（可空）
    page          INTEGER NOT NULL,               -- 页码
    content       TEXT NOT NULL,                  -- Chunk文本内容
    length        INTEGER NOT NULL,               -- content字符长度
    "order"       INTEGER NOT NULL,               -- 文档内排序（从1开始）
    source        VARCHAR(255) NOT NULL,          -- 来源机构
    publish_org   VARCHAR(255) NOT NULL,          -- 发布单位
    publish_date  VARCHAR(20),                    -- 发布日期（可空）
    version       VARCHAR(50) NOT NULL,           -- 知识库版本号
    embedding     vector(512),                    -- 512维Embedding向量
    model_name    VARCHAR(100) NOT NULL DEFAULT 'BAAI/bge-small-zh-v1.5',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**document_type 枚举值：** `地震应急预案` / `山洪应急预案` / `泥石流应急预案` / `森林火灾应急预案` / `气象灾害应急预案` / `综合应急预案`

**version 格式：** `YYYY-MM-DD_vN`（当前版本：`2026-07-22_v1`）

### RAG 向量检索示例

```sql
-- 语义相似度检索（替换为实际查询向量）
SELECT chunk_id, document_name, chapter, section, page, content,
       1 - (embedding <=> '[0.1,0.2,...]::vector') AS similarity
FROM knowledge_chunks
ORDER BY embedding <=> '[0.1,0.2,...]::vector'
LIMIT 5;

-- 按文档类型过滤
SELECT * FROM knowledge_chunks WHERE document_type = '地震应急预案';

-- 按版本过滤
SELECT * FROM knowledge_chunks WHERE version = '2026-07-22_v1';
```

### 数据库连接信息

| 配置项 | 环境变量 | 默认值 |
|--------|---------|--------|
| PostgreSQL Host | `PG_HOST` | `localhost` |
| PostgreSQL Port | `PG_PORT` | `5432` |
| PostgreSQL User | `PG_USER` | `postgres` |
| PostgreSQL Password | `PG_PASSWORD` | （空） |
| PostgreSQL DB | `PG_DB` | `emergency_vector` |
| MySQL Host | `MYSQL_HOST` | `localhost` |
| MySQL Port | `MYSQL_PORT` | `3306` |
| MySQL User | `MYSQL_USER` | `root` |
| MySQL Password | `MYSQL_PASSWORD` | （空） |
| MySQL DB | `MYSQL_DB` | `emergency_db` |

### 模拟数据

已生成模拟业务数据（MySQL），位于 `scripts/` 目录下：
- `mock_users.csv` / `.sql` — 20条用户数据
- `mock_incidents.csv` / `.sql` — 50条灾情数据
- `mock_resources.csv` / `.sql` — 100条资源数据

默认密码：`Test@1234`

---

## 三、对接要点

### 后端（Spring Boot）需实现

1. **上述全部 API 接口**，响应格式严格遵循 `{ code: 0, message: "success", data: T }`
2. **JWT 认证**，Token 通过 `Authorization: Bearer <token>` 传递
3. **灾情上报接口**需支持 `multipart/form-data`（图片字段名为 `images`）
4. **AI方案生成接口**（`POST /plan/generate`）需调用 FastAPI AI服务，前端超时60秒
5. **CORS 配置**：开发环境允许 `http://localhost:5173`

### AI服务（FastAPI）需消费

1. **pgvector `knowledge_chunks` 表** — 执行向量相似度检索
2. **Embedding 模型**：`BAAI/bge-small-zh-v1.5`（512维），本地路径 `models/bge-small-zh-v1.5/`
3. 返回的引用信息格式：`《{document_name}》第{section}节 第{page}页 {publish_org}`

### 前端代理配置

开发环境 Vite 代理（`vite.config.ts`）：
```typescript
proxy: {
  '/api': {
    target: 'http://localhost:8080',  // Spring Boot 地址
    changeOrigin: true,
  }
}
```

生产环境需 Nginx 将 `/api` 反向代理至 Spring Boot。
