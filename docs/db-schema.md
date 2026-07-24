# 云南自然灾害应急协同决策平台 - 数据库设计文档

## 1. 数据库概览

| 数据库名 | 类型 | 用途 |
|----------|------|------|
| emergency_db | MySQL | 主业务数据库，存储灾情、预案、资源、审计日志等数据 |
| emergency_vector | PostgreSQL | 向量数据库，存储应急预案向量数据（RAG 检索用） |

---

## 2. MySQL 数据表设计

### 2.1 应急物资表 (emergency_resource)

**表说明**: 存储应急物资的基本信息和库存状态。

| 字段名 | 类型 | 约束 | 默认值 | 注释 |
|--------|------|------|--------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | - | 自增主键 |
| resource_id | VARCHAR(64) | UNIQUE, NOT NULL | - | 资源唯一标识 |
| resource_name | VARCHAR(100) | NOT NULL | - | 资源名称 |
| resource_type | VARCHAR(50) | - | '物资' | 资源类型（物资/设备/人员） |
| total_stock | INT | NOT NULL | 0 | 总库存数量 |
| available_stock | INT | NOT NULL | 0 | 可用库存数量 |
| locked_stock | INT | NOT NULL | 0 | 已锁定库存数量 |
| location | VARCHAR(200) | - | - | 存放位置 |
| unit | VARCHAR(20) | - | - | 计量单位 |
| status | VARCHAR(20) | NOT NULL | 'available' | 状态（available/unavailable） |
| description | TEXT | - | - | 资源描述 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**索引**:
| 索引名 | 字段 | 类型 |
|--------|------|------|
| uk_resource_id | resource_id | UNIQUE |
| idx_resource_type | resource_type | NORMAL |
| idx_status | status | NORMAL |

**库存关系**: `total_stock = available_stock + locked_stock`

---

### 2.2 物资调度记录表 (resource_dispatch_record)

**表说明**: 记录物资的调度流向，包括锁定、释放、分配等操作。

| 字段名 | 类型 | 约束 | 默认值 | 注释 |
|--------|------|------|--------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | - | 自增主键 |
| record_id | VARCHAR(64) | UNIQUE, NOT NULL | - | 记录唯一标识（UUID） |
| resource_id | VARCHAR(64) | NOT NULL | - | 关联资源ID |
| resource_name | VARCHAR(100) | NOT NULL | - | 资源名称（冗余） |
| incident_id | VARCHAR(64) | - | - | 关联灾情ID |
| plan_id | VARCHAR(64) | - | - | 关联方案ID |
| dispatch_type | VARCHAR(20) | NOT NULL | - | 调度类型（lock/release/allocate） |
| quantity | INT | NOT NULL | 0 | 调度数量 |
| from_location | VARCHAR(200) | - | - | 来源位置 |
| to_location | VARCHAR(200) | - | - | 目标位置 |
| operator_id | BIGINT | - | - | 操作人ID |
| operator_name | VARCHAR(100) | - | - | 操作人姓名 |
| status | VARCHAR(20) | NOT NULL | 'completed' | 状态（pending/completed/failed） |
| remark | TEXT | - | - | 备注说明 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |

**索引**:
| 索引名 | 字段 | 类型 |
|--------|------|------|
| uk_record_id | record_id | UNIQUE |
| idx_resource_id | resource_id | NORMAL |
| idx_incident_id | incident_id | NORMAL |
| idx_plan_id | plan_id | NORMAL |
| idx_dispatch_type | dispatch_type | NORMAL |
| idx_operator_id | operator_id | NORMAL |

**dispatch_type 枚举说明**:
| 值 | 说明 |
|----|------|
| lock | 锁定资源（从可用库存转移到锁定库存） |
| release | 释放资源（从锁定库存归还到可用库存） |
| allocate | 分配资源（锁定库存出库） |

---

### 2.3 审计日志表 (audit_logs)

**表说明**: 记录系统操作日志，用于安全审计和操作追溯。

| 字段名 | 类型 | 约束 | 默认值 | 注释 |
|--------|------|------|--------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | - | 自增主键 |
| user_id | BIGINT | - | - | 操作人ID |
| username | VARCHAR(100) | - | - | 操作人姓名 |
| module | VARCHAR(50) | NOT NULL | - | 操作模块（如 resource/plan/user） |
| action | VARCHAR(100) | NOT NULL | - | 操作动作（如 lock/release/review） |
| action_type | VARCHAR(50) | NOT NULL | 'UPDATE' | 操作类型（CREATE/READ/UPDATE/DELETE） |
| target_type | VARCHAR(50) | - | - | 目标类型 |
| target_id | VARCHAR(64) | - | - | 目标ID |
| request_params | TEXT | - | - | 请求参数（JSON格式） |
| detail | TEXT | - | - | 操作详情 |
| result | TEXT | - | - | 操作结果（成功/失败及错误信息） |
| ip_address | VARCHAR(50) | - | - | 客户端IP地址 |
| duration | BIGINT | - | - | 操作耗时（毫秒） |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |

**索引**:
| 索引名 | 字段 | 类型 |
|--------|------|------|
| idx_user_id | user_id | NORMAL |
| idx_module | module | NORMAL |
| idx_action | action | NORMAL |
| idx_created_at | created_at | NORMAL |

**action_type 枚举说明**:
| 值 | 说明 |
|----|------|
| CREATE | 创建操作 |
| READ | 查询操作 |
| UPDATE | 更新操作 |
| DELETE | 删除操作 |

---

### 2.4 灾情表 (incidents)

**表说明**: 存储灾情事件的基本信息。

| 字段名 | 类型 | 约束 | 默认值 | 注释 |
|--------|------|------|--------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | - | 自增主键 |
| incident_id | VARCHAR(50) | UNIQUE, NOT NULL | - | 灾情唯一标识 |
| incident_name | VARCHAR(200) | NOT NULL | - | 灾情名称 |
| disaster_type | VARCHAR(50) | NOT NULL | - | 灾害类型 |
| location | VARCHAR(200) | - | - | 发生地点 |
| description | TEXT | - | - | 灾情描述 |
| incident_level | VARCHAR(20) | - | - | 灾情级别 |
| status | VARCHAR(20) | NOT NULL | 'active' | 状态（active/resolved/closed） |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

---

### 2.5 应急预案表 (plans)

**表说明**: 存储 AI 生成的应急预案信息。

| 字段名 | 类型 | 约束 | 默认值 | 注释 |
|--------|------|------|--------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | - | 自增主键 |
| plan_id | VARCHAR(64) | UNIQUE, NOT NULL | - | 方案唯一标识 |
| incident_id | VARCHAR(50) | NOT NULL | - | 关联灾情ID |
| plan_title | VARCHAR(200) | NOT NULL | - | 方案标题 |
| plan_content | TEXT | - | - | 方案内容 |
| generate_time | DATETIME | - | CURRENT_TIMESTAMP | 生成时间 |
| status | VARCHAR(20) | NOT NULL | 'draft' | 状态（draft/approved/rejected/modified） |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**status 枚举说明**:
| 值 | 说明 |
|----|------|
| draft | 草稿状态（AI 生成后） |
| approved | 已通过审核 |
| rejected | 已驳回审核 |
| modified | 已修改 |

---

### 2.6 用户表 (users)

**表说明**: 存储系统用户信息。

| 字段名 | 类型 | 约束 | 默认值 | 注释 |
|--------|------|------|--------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | - | 自增主键 |
| username | VARCHAR(50) | UNIQUE, NOT NULL | - | 用户名 |
| password | VARCHAR(255) | NOT NULL | - | 密码（BCrypt 加密） |
| real_name | VARCHAR(100) | - | - | 真实姓名 |
| email | VARCHAR(100) | UNIQUE | - | 邮箱 |
| phone | VARCHAR(20) | - | - | 手机号 |
| role | VARCHAR(50) | NOT NULL | 'user' | 角色（admin/user/operator） |
| status | VARCHAR(20) | NOT NULL | 'active' | 状态（active/inactive/locked） |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

---

## 3. PostgreSQL 向量数据库设计

### 3.1 知识片段表 (knowledge_chunks)

**表说明**: 存储应急预案向量数据，用于 RAG 检索。

| 字段名 | 类型 | 约束 | 默认值 | 注释 |
|--------|------|------|--------|------|
| id | SERIAL | PRIMARY KEY | - | 自增主键 |
| chunk_id | VARCHAR(64) | UNIQUE, NOT NULL | - | 片段唯一标识 |
| document_id | VARCHAR(64) | NOT NULL | - | 所属文档ID |
| document_name | VARCHAR(200) | NOT NULL | - | 文档名称 |
| content | TEXT | NOT NULL | - | 片段内容 |
| embedding | vector(512) | NOT NULL | - | 向量嵌入（512维） |
| chapter | VARCHAR(100) | - | - | 章节 |
| disaster_type | VARCHAR(50) | - | - | 灾害类型 |
| location | VARCHAR(100) | - | - | 适用地区 |
| created_at | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |

**索引**:
| 索引名 | 字段 | 类型 |
|--------|------|------|
| uk_chunk_id | chunk_id | UNIQUE |
| idx_document_id | document_id | NORMAL |
| idx_disaster_type | disaster_type | NORMAL |
| idx_location | location | NORMAL |
| idx_embedding | embedding | hnsw (cosine similarity) |

---

## 4. 表关系图

```
users (用户表)
    │
    ├─── incidents (灾情表)
    │       │
    │       ├─── plans (应急预案表)
    │       │       │
    │       │       └─── resource_dispatch_record (物资调度记录表) ←── emergency_resource (应急物资表)
    │       │
    │       └─── resource_dispatch_record (物资调度记录表)
    │
    └─── audit_logs (审计日志表)
                │
                ├─── 记录 resource 模块操作
                ├─── 记录 plan 模块操作
                └─── 记录 user 模块操作
```

---

## 5. 数据流说明

### 5.1 资源调度流程

```
GET /api/resource/available
    └──→ 查询 emergency_resource (available_stock > 0)

POST /api/resource/lock
    └──→ Redis 分布式锁(resource:{resourceId})
        └──→ 更新 emergency_resource (available_stock -= quantity, locked_stock += quantity)
            └──→ 插入 resource_dispatch_record (dispatch_type='lock')

POST /api/resource/release
    └──→ Redis 分布式锁(resource:{resourceId})
        └──→ 更新 emergency_resource (available_stock += quantity, locked_stock -= quantity)
            └──→ 插入 resource_dispatch_record (dispatch_type='release')
```

### 5.2 方案审核流程

```
POST /api/plan/review
    └──→ 查询 plans (plan_id)
        └──→ 更新 plans.status (APPROVE→approved, REJECT→rejected, MODIFY→modified)
            └──→ 更新 plans.plan_content (若 action=MODIFY 且 modifyContent 不为空)
```

### 5.3 审计日志流程

```
带 @SystemAuditLog 注解的方法
    └──→ AOP 切面拦截
        └──→ 记录请求参数、IP、操作人
            └──→ 执行目标方法
                └──→ 异步保存 audit_logs (含耗时、结果)
```

---

## 6. 数据字典

### 6.1 灾害类型枚举

| 值 | 说明 |
|----|------|
| earthquake | 地震 |
| flood | 洪涝 |
| landslide | 滑坡 |
| debris_flow | 泥石流 |
| fire | 火灾 |
| drought | 干旱 |
| typhoon | 台风 |
| other | 其他 |

### 6.2 灾情级别枚举

| 值 | 说明 |
|----|------|
| I | 特别重大（一级） |
| II | 重大（二级） |
| III | 较大（三级） |
| IV | 一般（四级） |

### 6.3 资源类型枚举

| 值 | 说明 |
|----|------|
| 物资 | 应急物资（帐篷、食品、药品等） |
| 设备 | 救援设备（挖掘机、救护车等） |
| 人员 | 救援人员 |
| 车辆 | 运输车辆 |