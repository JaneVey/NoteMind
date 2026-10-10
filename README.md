# NoteMind

> 基于 RAG 的智能知识管理平台 —— 把「个人笔记」与「可信检索」打通。

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring_AI-2.0-6DB33F?logo=spring&logoColor=white)
![Vue](https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_+_pgvector-4169E1?logo=postgresql&logoColor=white)

---

## 简介

NoteMind 是一个 Web 端的个人知识管理平台。三个模块构成一条完整链路：

| 模块 | 职责 |
|------|------|
| **笔记** | Markdown 创作、多级文件夹、`[[双向链接]]` |
| **知识库** | 文档上传 → 解析 → 分块 → 向量化 → 语义检索 |
| **AI 助手** | 通用对话、笔记内写作辅助、**基于知识库的 RAG 问答（带引用溯源）** |

核心思路是把**私有创作**与**可信检索**连起来：你在笔记里写下的内容可以沉淀为知识库语料，
AI 回答时既能引用"我刚写的"，也能引用"我读过的"，并标注**结论出自哪份文档的哪一段**。

---

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Java 21 · Spring Boot 4.1 · Spring AI 2.0 · Spring Security + JWT · MyBatis-Plus |
| 存储 | PostgreSQL 16 + pgvector（业务数据与向量**同库**）· Redis 7 · MinIO |
| 前端 | Vue 3 · TypeScript · Vite · Tailwind CSS · shadcn-vue · Pinia |
| 文档解析 | Apache Tika |

---

## 架构要点

### 一个 ChatService，三种检索范围

AI 助手、笔记侧边栏、知识库问答**不是三套 AI 功能**，而是同一个服务接受不同的检索范围：

```
              ChatService（唯一）
   上下文组装 · 思考链解析 · SSE 流式推送
                   │
        RagService.retrieve(scope)
                   │
    scope = NONE │ KB │ NOTEBOOK │ NOTE
                   ▼
  通用对话   绑定知识库   笔记侧边栏   知识库问答
```

检索服务只有一个，流式输出与思考链解析也只有一份实现 —— 这是"笔记与知识库融合"的落地形态。

### 检索链路

```
上传 → MinIO → Tika 解析 → 文本清洗 → 结构感知分块（保留来源信息）→ Embedding → pgvector
────────────────────────────────────────────────────────────────────────────────────
提问 → 问题向量化 → 相似度检索（按 scope 过滤 + Top-K）→ 上下文组装 → LLM 生成（SSE）
```

### 为什么向量与业务同库

检索需要在**同一条 SQL** 内完成业务过滤与向量排序：

```sql
SELECT chunk_content, source_info, 1 - (embedding <=> ?::vector) AS score
FROM knowledge_chunk
WHERE knowledge_base_id = ?        -- 业务过滤（scope）
ORDER BY embedding <=> ?::vector   -- 向量排序
LIMIT ?;
```

若拆成"PostgreSQL 存业务 + 独立向量库"，该查询会退化为"先取 Top-K → 回查元数据 → 再过滤"，
结果可能大幅缩水。pgvector 让过滤与排序在同一次索引扫描内完成。

---

## 快速开始

**环境要求**：JDK 21 · Maven 3.9+ · Node.js 18+ · Docker

```bash
# 1. 配置环境变量
#    .env 同时被 Docker Compose 与 Spring Boot 读取，一处配置两处生效
cp .env.example .env          # 按文件内注释填写 JWT_SECRET 与 AI_API_KEY

# 2. 启动依赖服务
#    只起三个依赖 —— 不要用 `docker compose up -d`，那会把生产用的 app 服务
#    一起启动，与下面的本地后端抢 8080 端口
docker compose up -d postgres redis minio

# 3. 启动后端
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 4. 启动前端
cd frontend && npm install && npm run dev
```

访问 <http://localhost:5173>。开发环境默认账号见 `init.sql` 的种子数据。

> 后端启动日志中会出现 MinIO 与 Redis 的连接确认，**看不到即说明连接未建立**。

---

## 项目结构

```
NoteMind/
├── backend/      Spring Boot 后端（按功能模块分包，模块内再分层）
│   └── src/main/java/com/notemind/
│       ├── common/       统一响应结构、错误码枚举、全局异常处理
│       ├── framework/    基础设施：security / config / storage / cache
│       └── auth user note knowledge ai    业务模块
├── frontend/     Vue 3 前端
│   └── src/  views / components / composables / api / stores / types / utils
├── 文档/         设计文档、开发规范、开发日志
├── init.sql      数据库初始化脚本
└── docker-compose.yml
```

---

## 文档

设计与工程过程记录在 [`文档/`](文档/) 中：

| 文档 | 内容 |
|------|------|
| [范围决策书](文档/02-需求与方案/范围决策书.md) | 能力边界：做什么、不做什么、什么条件下才翻案 |
| [技术栈选型](文档/02-需求与方案/技术栈选型.md) | 版本矩阵与被否决的方案及理由 |
| [系统架构设计](文档/03-系统设计/01-系统架构设计.md) | 模块划分与详细设计 |
| [数据库设计](文档/04-数据库设计/01-数据库设计文档.md) | 表结构与索引策略 |
| [开发规范](文档/07-开发规范/README.md) | 结构命名 / 后端 / 前端 / 数据库 / 测试 |
| [开发日志](文档/开发日志/) | 按日期记录的过程与决策 |

### 几个明确的取舍

| 不做 | 原因 |
|------|------|
| AI Agent（工具调用循环） | 需求是"检索增强 + 建议确认"，不需要自主工具调用 |
| 独立向量数据库 | 业务过滤与向量排序无法在一条 SQL 内完成，引入正确性风险 |
| 第三方 RAG 平台作为主链路 | 会架空本项目的核心设计（保留为对比实验基线） |

其余取舍见[范围决策书](文档/02-需求与方案/范围决策书.md)。

---

## 进度

| 模块 | 状态 |
|------|------|
| 基础设施（统一响应与分段错误码、对象存储、缓存、连接自检） | ✅ |
| 用户系统（注册 / 登录 / 双 Token / 游客模式 / 第三方登录） | ✅ |
| AI 对话链路（思考链 + SSE 流式输出） | ✅ |
| 笔记模块 | 🚧 |
| 知识库与 RAG 检索 | 🚧 |
| 知识图谱 | 计划中 |

---

## 说明

- 本仓库为公开仓库，**不含任何密钥**：所有敏感配置通过 `.env` 注入，该文件已被 `.gitignore` 忽略
- 个人身份材料不纳入版本库
