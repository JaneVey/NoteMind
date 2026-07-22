# MVP 开发计划

> 项目：基于 RAG 的 NoteMind 智能知识管理平台
> 周期：预计 12-14 周（约 3 个月）
> 并行：实习 + 课程，每周约 10-15 小时有效开发时间
> 起始：2026 年 7 月
> 最后更新：2026-07-20（重构后更新）

---

## 总体路线图

```
Phase 1 (Week 1-2)   项目脚手架 + 数据库奠基 + 用户认证
Phase 2 (Week 3-4)   用户系统 + 登录注册
Phase 3 (Week 5-7)   笔记核心模块（多仓库 + 三栏布局 + AI 侧边栏）
Phase 4 (Week 8-10)  AI 助手 + 多供应商对话
Phase 5 (Week 11-13) 知识库 + RAG 引擎 + 知识图谱 + 设置
Phase 6 (Week 14)    集成 + 打磨 + 部署 + 文档
```

---

## Phase 1：项目脚手架（Week 1-2）

### Week 1 — 后端骨架搭建

**目标**：能跑起来的 Spring Boot 项目 + 数据库连通

- [ ] 初始化 Spring Boot 3.x 项目（Maven）
- [ ] 配置 PostgreSQL 数据源 + MyBatis-Plus
- [ ] 配置 pgvector 扩展（Docker 启动）
- [ ] 配置 Redis
- [ ] 配置 MinIO 客户端
- [ ] 引入 Spring AI 依赖
- [ ] 引入 Spring Security + JWT 依赖
- [ ] 搭建统一返回结构 `Result<T>`
- [ ] 全局异常处理器 `GlobalExceptionHandler`
- [ ] 跨域配置 CorsConfig
- [ ] 测试：`http://localhost:8080/api/health` 返回正常

### Week 2 — 前端脚手架 + 数据库表创建

**目标**：Vue3 前端能跑 + 核心表建好

- [ ] 初始化 Vue3 + TypeScript + Vite + Element Plus 项目
- [ ] 配置路由（4 个一级模块的路由布局）
- [ ] 封装 Axios 请求工具
- [ ] 创建主布局（左侧导航 + 右侧内容区）
- [ ] 执行数据库 DDL 建表（全部核心表，含 `note_user` + `user_id`）
- [ ] 编写各实体类 + Mapper 层基础 CRUD
- [ ] 测试：前端调通后端健康检查接口

**产出物**：
```
前端：布局框架，能跳转各空页面（AI助手 / 笔记 / 知识库 / 设置）
后端：所有核心表建好，CRUD 生效，MinIO/Redis 连通
```

---

## Phase 2：用户系统（Week 3-4）

### Week 3 — 后端用户认证

**目标**：能注册、登录，JWT 鉴权生效

**后端**：
- [ ] 搭建 Spring Security 配置框架（SecurityConfig）
- [ ] 实现 JwtUtils（Token 签发、验证、解析）
- [ ] 实现 JwtAuthenticationFilter（Token 过滤器）
- [ ] 创建 `note_user` 表 + User 实体 + Mapper
- [ ] AuthController：注册 API（BCrypt 加密密码）
- [ ] AuthController：登录 API（验证密码 + 签发 JWT Token）
- [ ] 所有业务表增加 `user_id` 字段
- [ ] 测试：注册 → 登录 → 携带 Token 访问接口 → 获取 userId

**前端**：
- [ ] 登录页（Login.vue）
- [ ] 注册页（Register.vue）
- [ ] 封装 request.js 拦截器（自动携带 Token + 401 跳转登录）
- [ ] 路由守卫（未登录时不能访问业务页面）
- [ ] authStore（Token 持久化 + 用户信息）
- [ ] 测试：登录 → 跳转主页

### Week 4 — 用户信息 + 模块适配

**目标**：用户信息管理和所有业务模块适配多用户

**后端**：
- [ ] UserController：个人信息查询、修改、头像上传
- [ ] 修改密码 API
- [ ] 所有已有 Service 增加 userId 筛选（如笔记按用户隔离）
- [ ] 各 Controller 从 SecurityContext 获取 userId 传入 Service

**前端**：
- [ ] UserCenter 页面（个人信息编辑、头像上传）
- [ ] 页面布局适配用户信息（顶栏显示用户信息、退出登录）

**里程碑**：✅ 用户系统完成 —— 注册登录鉴权 + 个人信息 + 数据隔离

---

## Phase 3：笔记核心模块（Week 5-7）

### Week 5 — 仓库管理 + 目录树 + 笔记列表

**目标**：能创建仓库，管理文件夹，看到笔记列表

**后端**：
- [ ] Notebook CRUD API（仓库管理）
- [ ] Folder CRUD API（文件夹管理，支持多级嵌套）
- [ ] Note CRUD API（含分页、按文件夹筛选、按仓库筛选）

**前端**：
- [ ] 仓库切换组件（顶部下拉或侧边栏切换）
- [ ] FolderTree 组件（展开/折叠、右键菜单）
- [ ] NoteList 组件（点击文件夹 → 显示笔记列表）
- [ ] 笔记仓库状态管理（Pinia noteStore）

### Week 6 — 三栏布局 + Markdown 编辑器集成

**目标**：三栏布局能跑通，真正能写笔记了

**后端**：
- [ ] 笔记内容保存/自动保存 API

**前端**：
- [ ] 三栏布局组件搭建（目录树 | 编辑器 | AI 侧边栏）
- [ ] 集成 Vditor 编辑器到 Vue3
- [ ] 编辑器与笔记保存联动（Ctrl+S 自动保存）
- [ ] 笔记新建 / 打开 / 保存 / 删除完整链路
- [ ] 图片插入功能（上传到 MinIO，返回 URL 插入 Markdown）

### Week 7 — 笔记功能完善 + AI 侧边栏基础

**目标**：笔记模块功能完善，AI 侧边栏可用

**后端**：
- [ ] 笔记移动 API（拖拽到其他文件夹）
- [ ] 笔记搜索 API（按标题/内容关键词）
- [ ] 笔记内容变化时的字数统计

**前端**：
- [ ] 标题修改（与文件名联动）
- [ ] 笔记删除到回收站 / 永久删除
- [ ] 笔记移动（拖拽或右键菜单）
- [ ] AI 侧边栏基础框架（右侧面板，可折叠/展开）
- [ ] 笔记内容传给 AI 侧边栏做上下文

**里程碑**：✅ 笔记模块完成 —— 具备多仓库笔记能力 + AI 侧边栏基础设施

---

## Phase 4：AI 助手（Week 8-10）

### Week 8 — 通用 AI 对话 + DeepSeek 对接

**目标**：能在系统里和 AI 对话（纯对话模式）

**后端**：
- [ ] 集成 Spring AI ChatClient
- [ ] 对接 DeepSeek（兼容 OpenAI API 格式）
- [ ] SSE 流式输出（Spring AI 原生支持）
- [ ] 对话管理：创建对话、发送消息、历史记录
- [ ] 对话收藏 / 重命名 / 删除 API

**前端**：
- [ ] AI 助手页面布局（左侧历史列表 + 右侧聊天区）
- [ ] ChatPanel 组件（消息展示 + SSE 逐字渲染）
- [ ] ChatHistory 组件（历史对话列表 + 收藏标记）
- [ ] 新建对话 / 切换对话
- [ ] MessageBubble 组件（Markdown 渲染消息）
- [ ] 右键菜单：重命名 / 删除 / 收藏对话

### Week 9 — 多供应商支持 + 配置页面

**目标**：可以自由切换 AI 供应商（DeepSeek / OpenAI / Gemini）

**后端**：
- [ ] Spring AI 多供应商配置（DeepSeek 兼容 OpenAI → 复用）
- [ ] Gemini Provider 对接
- [ ] AiConfig CRUD（API Key 加密存储）
- [ ] 供应商动态切换机制

**前端**：
- [ ] Settings 页面基础布局（多 tab）
- [ ] AI 模型管理 tab：API Key / Base URL / 模型选择
- [ ] AI 参数 tab：Temperature / 最大 Token / 上下文长度
- [ ] 对话界面增加供应商切换下拉框
- [ ] 用户画像配置界面（身份、技术方向、回答偏好）

### Week 10 — 知识库绑定 + AI 记忆 + Prompt 快捷指令

**目标**：对话可绑定知识库，AI 可感知用户画像，Prompt 快捷指令可用

**后端**：
- [ ] 对话绑定知识库（Conversation 增加 knowledge_base_id 字段）
- [ ] 用户画像 Prompt 注入（在 System Prompt 中加入用户画像信息）
- [ ] PromptShortcut CRUD
- [ ] Prompt 快捷指令执行 API（选中文字 + 指令 → AI 处理）

**前端**：
- [ ] 对话设置：绑定知识库选择器
- [ ] 用户画像编辑界面
- [ ] Prompt 快捷指令管理页面（列表 + 新增 + 编辑 + 删除）
- [ ] 编辑器右键菜单集成快捷指令（选中文字 → 右键 → 快捷指令）

**里程碑**：✅ AI 助手模块完成 —— 多供应商对话 + 知识库绑定 + 用户画像 + 快捷指令

---

## Phase 5：知识库 + RAG 引擎（Week 11-13）

### Week 11 — 知识库管理 + 文档上传

**目标**：能创建知识库，上传 Markdown 文档，系统自动分块存储

**后端**：
- [ ] KnowledgeBase CRUD
- [ ] 文件上传接口（MinIO 存储）
- [ ] 文档解析（Markdown / 纯文本）
- [ ] 文本分块策略（TextSplitter）：
  - 按段落/标题分块
  - 配置块大小（如 512 tokens）
  - 块重叠（overlap）避免上下文截断
- [ ] 笔记 → 知识库同步（笔记一键加入知识库）
- [ ] DocumentSource CRUD + 处理状态管理

**前端**：
- [ ] 知识库管理页（列表 + 新建 + 编辑 + 删除）
- [ ] 文档上传组件（拖拽上传区域 + 支持多格式）
- [ ] 文档列表 + 处理状态展示（待处理 / 解析中 / 完成 / 失败）
- [ ] 笔记到知识库的"加入"按钮

### Week 12 — Embedding + 向量存储 + RAG 问答

**目标**：文档内容被向量化，能基于知识库 AI 问答

**后端**：
- [ ] EmbeddingService（Spring AI EmbeddingModel 封装）
- [ ] Embedding 写入 pgvector
- [ ] 文档导入全流程打通：
  ```
  上传 → MinIO → 解析 → 分块 → Embedding → 存入 pgvector
  ```
- [ ] RagService 核心流程：
  ```
  用户问题 → Embedding → pgvector 检索 → 上下文组装 → 调用 LLM → SSE 返回
  ```
- [ ] 上下文组装策略（Top-K 块 + Token 截断）
- [ ] RAG Prompt 模板（System Prompt + Context + 问题）

**前端**：
- [ ] 文档导入全流程联调（上传 → 状态流转 → 完成）
- [ ] 知识库内嵌 RAG 问答界面

### Week 13 — 引用溯源 + 文档详情 + 扩展格式支持

**目标**：RAG 回答带引用，文档详情可用，支持 PDF/Word 上传

**后端**：
- [ ] CitationBuilder：引用标注（回答中标注来源文档 + 章节）
- [ ] 文档详情 API（内容预览、元数据、处理日志）
- [ ] 重新解析 API
- [ ] Apache Tika 集成：PDF / Word / PPT 解析
- [ ] OCR 基础支持（Tesseract 集成，图片文字提取）

**前端**：
- [ ] 回答中引用来源展示（点击可查看原文）
- [ ] 文档详情页（文件名称、类型、大小、上传时间、解析状态）
- [ ] 解析结果预览 + 重新解析按钮
- [ ] PDF/Word 上传支持

**里程碑**：✅ 知识库模块完成 —— 多格式文档 + RAG 问答 + 引用溯源

---

## Phase 6：知识图谱 + 设置完善（Week 14-15）

### Week 14 — 知识图谱（双向链接）

**目标**：笔记间的 `[[Wikilink]]` 双向链接生效，图谱可展示

**后端**：
- [ ] Wikilink 解析器（解析 Markdown 中的 `[[note]]` 语法）
- [ ] NoteLink 维护（新建/更新笔记时自动解析链接关系）
- [ ] GraphService：获取某个笔记的关联节点和边
- [ ] 仓库级图谱 API

**前端**：
- [ ] GraphView 组件（v is-network / D3.js 可视化展示）
- [ ] 节点点击跳转到对应笔记
- [ ] 图谱页面入口（每个仓库一个图谱）

### Week 15 — 设置完善 + 搜索 + 集成打磨

**后端**：
- [ ] 全文搜索（PostgreSQL tsvector / ILIKE）
- [ ] 语义搜索（问题 Embedding → 向量检索笔记）
- [ ] 笔记加入知识库的批量操作
- [ ] 系统配置（主题、基础设置等）

**前端**：
- [ ] SearchBar 组件（全局搜索框，Ctrl+K 快捷键）
- [ ] 搜索结果展示（高亮关键词）
- [ ] Settings 页面完善（基础设置 tab）
- [ ] 主题切换（亮色/暗色）
- [ ] 全链路测试：笔记 → AI 对话 → 导入知识库 → RAG 问答
- [ ] UI 细节打磨（空状态、加载态、错误处理）

**里程碑**：✅ 知识图谱 + 设置完成 —— 核心功能全部具备

---

## Phase 7：集成 + 部署 + 文档（Week 16）

### Week 16 — 部署与文档

**目标**：一键部署运行，文档完善

- [ ] Docker Compose 编排（PostgreSQL + Redis + MinIO + App）
- [ ] 前端打包到后端静态资源目录
- [ ] 写 Dockerfile（多阶段构建）
- [ ] 部署到服务器 / 本地一键启动脚本
- [ ] 测试验证完整流程
- [ ] 填坑（前面遗留的问题）
- [ ] 优化体验
- [ ] 准备演示数据
- [ ] 写 README / 项目文档
- [ ] 完成毕设论文初稿大纲

---

## 依赖关系图

```
Week 1-2 (脚手架 + 数据库)
    │
    ▼
Week 3-4 (用户系统) ────── 认证基础设施
    │
    ├────────────────────────────┐
    ▼                            ▼
Week 5-7 (笔记模块)      Week 8-10 (AI 助手)
    │                            │
    └──────────┬────────────────┘
               ▼
    Week 11-13 (知识库 + RAG)
               │
               ▼
    Week 14-15 (知识图谱 + 设置)
               │
               ▼
    Week 16 (集成 + 部署 + 文档)
```

**并行可能性**：
- 用户系统（Week 3-4）是**前置依赖**，所有业务模块依赖用户认证
- 笔记模块（Week 5-7）和 AI 助手（Week 8-10）无强依赖，可并行开发
- 知识图谱（Week 14）依赖笔记模块完成（需要已有笔记内容才能解析双链）

---

## 开发节奏建议

| 周内 | 做什么 |
|------|--------|
| 周一-周三 | 后端开发（新功能） |
| 周四-周五 | 前端对接 + 联调 |
| 周末 | 补进度 / 学习新知识点 |

## 学习前置（建议提前了解）

- **Spring Security + JWT**：Security 配置、JWT 过滤器链、BCrypt 加密、SecurityContextHolder
- **Spring AI**：ChatClient、EmbeddingModel、流式输出配置
- **MinIO 集成**：Spring Boot MinIO 客户端配置
- **SSE 流式输出**：Spring AI 的流式处理 + JS EventSource
- **PostgreSQL + pgvector**：Docker 启动，建表带 `vector(1536)` 类型
- **PDF/Word 解析**：Apache Tika 基本使用
- **Vditor + Vue3 三栏布局**：Vditor 定制化布局

---

## MVP 范围回顾

### 包含

| 模块 | 说明 |
|------|------|
| AI 助手 | 通用对话 + 历史管理 + 收藏 + 供应商切换 + 知识库绑定 + 用户画像 |
| 笔记系统 | 多仓库 + 文件夹 + 三栏布局 + Markdown 编辑 + AI 辅助 + 文件导入（P1 扩展） |
| RAG 知识库 | 知识库管理 + 多格式上传 + 分块 Embedding + 向量检索 + 引用溯源 |
| 知识图谱 | Wikilink 解析 + 仓库级图谱可视化 |
| 设置 | AI 模型管理 + 参数配置 + Prompt 快捷指令 + 主题 |
| 用户系统 | 注册 / 登录 / Spring Security + JWT 鉴权 / 个人信息管理 |
| 搜索 | 关键词全文搜索 |
| Prompt 快捷指令 | 用户自定义指令 + 右键集成 |

### 不包含（P1 / P2 / 暂不考虑）

| 模块 | 说明 |
|------|------|
| 共享知识库 | 暂不考虑（个人毕设范围） |
| 多人协作 | 暂不考虑 |
| Spring Cloud 微服务 | 暂不考虑 |
| RabbitMQ / 消息队列 | 暂不考虑 |
| AI Agent 自动执行 | 暂不考虑 |
| 插件系统 | 暂不考虑 |
| MCP 协议 | 暂不考虑 |
| OCR（图片/PDF 扫描件） | P1 阶段加入 |
| 历史版本 | P1 阶段加入 |
| AI 自动分析关联 | 未来增强 |

---

## 各模块 API 预估

| 模块 | 接口数量 | 说明 |
|------|---------|------|
| 笔记仓库 | ~4 | 仓库 CRUD |
| 文件夹 | ~6 | 文件夹 CRUD + 移动 |
| 笔记 | ~10 | 笔记 CRUD + 搜索 + 移动 + 导入 |
| AI 对话 | ~8 | 对话 CRUD + 消息 + 收藏 + SSE |
| 知识库 | ~5 | 知识库 CRUD |
| 文档管理 | ~6 | 上传 + 删除 + 详情 + 状态 + 重新解析 |
| RAG 问答 | ~2 | 提问 + 获取引用来源 |
| 知识图谱 | ~3 | 链接解析 + 图谱数据 |
| 搜索 | ~2 | 全文搜索 + 语义搜索 |
| 用户认证 | ~4 | 注册 + 登录 + Token 刷新 + 退出 |
| 用户信息 | ~4 | 个人信息 + 修改密码 + 头像上传 |
| 设置 | ~8 | AI 配置 + Prompt 指令 + 系统配置 + 用户画像 |
| **总计** | **~62** | |

---

*本计划会根据实际开发进度动态调整。*
