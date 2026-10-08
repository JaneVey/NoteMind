# NoteMind · 基于 RAG 的智能知识管理平台

> 本科毕业设计 · 软件工程
>
> **笔记负责知识创造与沉淀，知识库负责知识理解与检索，AI 负责辅助整理、分析与生成。**

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring_AI-2.0.1-6DB33F?logo=spring&logoColor=white)
![Vue](https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-5.7-3178C6?logo=typescript&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16_+_pgvector-4169E1?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![MinIO](https://img.shields.io/badge/MinIO-S3_兼容-C72E49?logo=minio&logoColor=white)

---

## 这是什么

NoteMind 是一个 Web 端的个人知识管理平台，把三件事放进同一套系统里：

| 层 | 职责 |
|---|---|
| **笔记** | 多仓库 Markdown 知识创作，Obsidian 式的 `[[双向链接]]` |
| **知识库** | 文档上传 → 解析 → 分块 → 向量化 → 语义检索 |
| **AI** | 通用对话、笔记内的写作辅助、以及**基于知识库的 RAG 问答（带引用溯源）** |

它的价值不在于"又一个笔记软件"或"又一个 AI 聊天框"，而在于把**私有创作**与**可信检索**打通：
你在笔记里写下的内容可以沉淀为知识库语料，AI 回答时既能引用"我刚写的"，也能引用"我读过的"，
并标注**结论出自哪份文档的哪一段**。

---

## 核心特性

### 笔记系统
- 多仓库 / 多级文件夹 / Markdown 编辑
- `[[Wikilink]]` 双向链接，自动生成关联关系
- 侧边栏 AI 辅助：**展示修改建议 → 用户确认 → 才写入笔记**（保留用户对原文的控制权）

### 知识库与 RAG
- 多格式文档上传（Markdown / TXT / PDF / Word）
- 文档处理状态机（`pending / processing / done / failed`），支持失败重解析
- **结构感知分块**：按标题与段落切分，而非固定字数硬切
- 基于 pgvector 的语义检索
- **引用溯源**：回答标注来源文档与章节，可点击查看原文

### AI 能力
- 通用对话 + 可选绑定知识库（绑定后切换为 RAG 模式）
- **思考链路展示**：解析并单独渲染模型的思维链
- SSE 流式输出（逐字吐字）
- 用户画像注入 System Prompt
- 可自定义的 Prompt 快捷指令，集成到编辑器右键菜单

### 多供应商 AI 接入
- **对话模型与向量模型可分别配置**（这是硬需求——见下方"设计取舍"）
- 支持任意 OpenAI 兼容服务：DeepSeek / 硅基流动 / 阿里云百炼 / OpenAI / OpenRouter
- 用户自备 API Key，密钥加密存储

---

## 技术栈

### 后端

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 LTS | |
| Spring Boot | 4.1.1 | |
| Spring AI | 2.0.1 | 对话、Embedding、结构化输出 |
| Spring Security + JWT | 7.1.1 / JJWT 0.12.6 | 无状态鉴权 |
| MyBatis-Plus | 3.5.17 | `spring-boot4-starter` |
| PostgreSQL | 16 + pgvector | 业务数据 + 向量存储（**同一个库**） |
| Redis | 7 | AI 配置缓存、限流、登录失败计数 |
| MinIO | 8.5.17 | 对象存储 |
| Apache Tika | 3.3.2 | PDF / Word / TXT 解析 |
| Jackson | 3.1.5 | ⚠️ Boot 4 起包名为 `tools.jackson` |

### 前端

| 组件 | 版本 |
|------|------|
| Vue 3 + TypeScript + Vite | 3.5 / 5.7 / 6.x |
| shadcn-vue（Radix Vue + Tailwind CSS） | 1.9 / 3.4 |
| Pinia / Vue Router | 3.x / 4.5 |
| Vditor（Markdown 编辑器） | 3.10 |

---

## 系统架构

### 一个 ChatService，三种调用范围

这是本项目最核心的设计。AI 助手、笔记侧边栏、知识库问答**不是三套 AI 功能**，
而是同一个服务接受不同的检索范围：

```
                    ┌──────────────────────────────────┐
                    │  ChatService（唯一）               │
                    │  · 上下文组装                      │
                    │  · reasoning_content 解析（思考链）│
                    │  · SSE 流式推送                    │
                    └────────────────┬─────────────────┘
                                     │
                    ┌────────────────▼─────────────────┐
                    │  RagService.retrieve(scope)       │
                    │  scope: NONE | KB | NOTEBOOK | NOTE│
                    └────────────────┬─────────────────┘
                                     │
     ┌───────────────┬───────────────┼───────────────┬───────────────┐
     ▼               ▼               ▼               ▼
  主页通用对话   主页绑定知识库   笔记侧边栏 AI    知识库问答
  (NONE)         (KB)           (NOTEBOOK)      (KB)
```

**收益**：检索服务只有一个，流式输出与思考链解析只有一份实现。
这也正是"笔记与知识库融合架构"的落地形态。

### RAG 链路

```
上传 → MinIO → Tika 解析 → 扫描件检测 → 文本清洗
     → 结构感知分块（512 token，重叠 50-100，保留 source_info）
     → Embedding → 写入 pgvector
────────────────────── 以上入库 / 以下问答 ──────────────────────
提问 → 问题向量化 → 相似度检索（按 scope 过滤 + Top-K）
     → 上下文组装 + 引用标注 → LLM 生成（SSE 流式）
     → 返回答案 + 引用来源
```

### 为什么用 pgvector 而不是独立向量库

`RagService.retrieve(scope)` 需要**在同一条 SQL 里完成业务过滤与向量排序**：

```sql
SELECT id, chunk_content, source_info, 1 - (embedding <=> ?::vector) AS score
FROM knowledge_chunk
WHERE knowledge_base_id = ?          -- 业务过滤（scope）
ORDER BY embedding <=> ?::vector     -- 向量排序
LIMIT ?;
```

若拆成"PostgreSQL 存业务 + Milvus 存向量"，该查询会退化为
"先取 Top-K → 回查元数据 → 再按知识库过滤"，**结果可能大幅缩水**，
被迫改为超量取回再重排，引入正确性风险。pgvector 让过滤与排序在同一次索引扫描内完成。

---

## 快速开始

### 环境要求

- JDK 21、Maven 3.9+
- Node.js 18+、npm
- Docker（用于 PostgreSQL / Redis / MinIO）

### 1. 配置环境变量

```bash
cp .env.example .env
```

`.env` 会被 **Docker Compose** 与 **Spring Boot** 同时读取，一处配置两处生效。
必填项：

| 变量 | 说明 |
|------|------|
| `JWT_SECRET` | 随机长字符串（≥32 字节），`openssl rand -base64 48` |
| `AI_API_KEY` | AI 服务密钥（开发阶段推荐硅基流动，一个 Key 覆盖 chat + embedding + rerank） |
| `AI_CHAT_MODEL` | 模型 ID，须与控制台模型列表完全一致 |

### 2. 启动依赖服务

```bash
docker compose up -d postgres redis minio
```

| 服务 | 地址 |
|------|------|
| PostgreSQL | `127.0.0.1:5432`（库名 `notemind`，首次启动自动执行 `init.sql`） |
| Redis | `127.0.0.1:6380` |
| MinIO | `127.0.0.1:9000`（控制台 9001，仅绑本机） |

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

启动日志中应出现两行连接确认（**看不到即说明连接未建立**）：

```
MinIO bucket 'notemind' 已存在（endpoint=http://localhost:9000）
Redis 连接成功：PING=PONG version=7.4.9
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`。

---

## 项目结构

```
NoteMind/
├── backend/                    Spring Boot 后端（按功能模块分包，模块内再分层）
│   └── src/main/java/com/notemind/
│       ├── common/             统一返回结构、错误码枚举、全局异常处理
│       ├── framework/          基础设施
│       │   ├── security/       Security 配置、JWT 过滤器、当前用户工具
│       │   ├── config/         CORS、MyBatis-Plus、WebMvc
│       │   ├── storage/        MinIO 对象存储
│       │   └── cache/          Redis
│       ├── auth/               登录注册、JWT 签发
│       ├── user/               用户中心
│       └── note/ knowledge/ ai/   业务模块（开发中，各自含 controller/service/entity/mapper）
├── frontend/                   Vue 3 前端
│   └── src/
│       ├── views/              页面（大页面拆为「容器 + 子组件」）
│       ├── components/ui/      shadcn-vue 风格基础组件
│       ├── composables/        组合式函数（useDropdown 等）
│       ├── api/                接口层封装
│       ├── stores/             Pinia 状态管理
│       ├── types/              类型定义（按领域拆分）
│       └── utils/              sse.ts（SSE 流式）、format.ts（展示层格式化）
├── AGENTS.md                   给编码助手的工作约定（新会话自动加载）
├── 文档/                        设计文档、开发规范、问题台账、开发日志
├── init.sql                    数据库建表脚本（含种子数据）
└── docker-compose.yml          依赖服务编排
```

---

## 文档导航

本项目在编码前先完成了较完整的设计文档，可作为工程过程的参考：

| 文档 | 内容 |
|------|------|
| [范围决策书](文档/02-需求与方案/范围决策书.md) | **AI 能力边界**：做什么、不做什么、为什么、什么条件下才翻案 |
| [技术栈选型](文档/02-需求与方案/技术栈选型.md) | 版本矩阵、升级踩坑记录、被否决的方案及理由 |
| [方案定位](文档/02-需求与方案/方案定位.md) | 项目定位、模块划分、创新点 |
| [系统架构设计](文档/03-系统设计/01-系统架构设计.md) | 架构图、模块详细设计、包结构 |
| [数据库设计](文档/04-数据库设计/01-数据库设计文档.md) | 表结构、索引策略、DDL |
| [AI 供应商接入设计](文档/05-开发计划/03-AI供应商接入设计.md) | 多供应商抽象、SSE、Prompt 快捷指令 |
| [MVP 开发计划](文档/05-开发计划/02-MVP开发计划.md) | 分阶段计划与执行顺序 |
| [开发规范](文档/07-开发规范/README.md) | **结构与命名、后端、前端、数据库、测试** 五份规范 |
| [问题台账](文档/08-问题记录/问题台账.md) | 开发中遇到的全部真实问题：现象 / 根因 / 解决 / 可讲的点 |
| [开发日志](文档/开发日志/) | 按日期记录的过程、决策与问题排查 |

---

## 开发进度

> 本项目为在读期间完成的毕业设计，**当前处于开发中**。以下为真实状态，不含夸大。

| 模块 | 状态 |
|------|------|
| 工程脚手架 + 技术栈升级（Spring Boot 4.1.1 / Spring AI 2.0.1 / Java 21） | ✅ 完成 |
| 用户系统（注册 / 登录 / JWT / 个人资料 / 修改密码） | ✅ 完成 |
| 统一响应结构、**分段错误码**、全局异常处理、CORS、401/403 | ✅ 完成 |
| MinIO / Redis 连接与启动自检 | ✅ 完成 |
| AI 最小纵切：思考链 + SSE 流式输出（端到端验证通过） | ✅ 完成 |
| [开发规范](文档/07-开发规范/README.md)制定（结构命名 / 后端 / 前端 / 数据库 / 测试） | ✅ 完成 |
| 后端按功能模块重构（包结构、类名与表名对齐、Service 分层） | ✅ 完成 |
| 公共表重建（`sys_user` / `user_profile` / `sys_operation_log`） | ✅ 完成 |
| 前端目录调整与类型定义拆分 | ✅ 完成 |
| 前端 10 个页面原型与组件库 | ✅ 完成 |
| 笔记模块（笔记本 / 文件夹 / 笔记 CRUD） | 🚧 开发中 |
| AI 助手（会话与消息持久化、历史记录） | ⬜ 待开发 |
| 知识库 + RAG 检索 + 引用溯源 | ⬜ 待开发 |
| 知识图谱 / 全文搜索 | ⬜ 计划中 |

**已知限制**：
- 业务表（笔记 / 知识库 / AI 共 11 张）尚未定稿 —— 字段取决于功能细节，
  将在对应模块开发时一并设计（现在提前拍字段，开发时必然要改）
- 前端业务页面目前使用模拟数据，尚未接入后端接口（用户系统除外）

---

## 设计取舍

开发过程中主动放弃了一些"看起来更强"的方案，理由记录在 [范围决策书](文档/02-需求与方案/范围决策书.md)：

| 方案 | 是否采用 | 理由 |
|------|---------|------|
| AI Agent（工具调用循环 / 多 Agent 编排） | ❌ | 本项目需求是"检索增强 + 建议确认"，不需要自主工具调用；多 Agent 编排超出本科毕设范围 |
| 若依（RuoYi-Vue-Plus）作为基座 | ❌ | 它是多租户后台管理脚手架，与本项目"产品型应用"重合度极低，净收益为负 |
| 独立向量数据库（Milvus / Qdrant） | ❌ | 过滤与排序无法在一条 SQL 内完成，引入正确性风险 |
| 第三方 RAG 平台（Dify / RAGFlow）作为主链路 | ❌ | 会架空本项目的核心设计；但保留为论文的对比实验基线 |
| 桌面端应用 | ❌ | 存储栈（PostgreSQL + pgvector / Redis / MinIO）无法本地化，且对论文无增益 |
| WebSocket | ❌ | 只需服务端单向推送，SSE 更轻量，且能复用标准 HTTP 鉴权 |
| OCR（扫描件识别） | ❌ | 语料以电子版为主；改为"扫描件检测 + 明确标记失败"，比硬做 OCR 更划算 |

---

## 说明

- 本仓库为公开仓库，**不含任何密钥**：所有敏感配置通过 `.env` 注入（该文件已被 `.gitignore` 忽略）
- 个人身份材料与演示视频不纳入版本库
- 项目文档与代码同步维护，开发过程中的决策与问题排查记录在 [开发日志](文档/开发日志/) 中
