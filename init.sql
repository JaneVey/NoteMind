-- ============================================================
-- NoteMind 数据库初始化脚本
-- 目标镜像: pgvector/pgvector:pg16
-- 数据库名: notemind
--
-- 使用方式（二选一）:
--   1) Docker: 在 docker-compose 中设置 POSTGRES_DB=notemind，
--      并将本文件挂载到 /docker-entrypoint-initdb.d/init.sql
--   2) 手动: createdb notemind && psql -d notemind -f init.sql
-- ============================================================

-- 如果是手动从零初始化，取消注释下一行:
-- CREATE DATABASE notemind;

-- 确保已连接到 notemind 数据库后再执行以下内容
-- \c notemind

-- ============================================================
-- 0. pgvector 扩展
-- ============================================================
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================
-- 1. note_user — 用户表
-- ============================================================
CREATE TABLE note_user (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(100) NOT NULL,
    email       VARCHAR(255),
    password    VARCHAR(500) NOT NULL,
    nickname    VARCHAR(100),
    avatar      VARCHAR(500),
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_note_user_username ON note_user(username);
CREATE INDEX idx_note_user_email ON note_user(email);

-- ============================================================
-- 2. notebook — 笔记本
-- ============================================================
CREATE TABLE notebook (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES note_user(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    icon        VARCHAR(50) DEFAULT '📒',
    sort_order  INTEGER DEFAULT 0,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_notebook_user_id ON notebook(user_id);
CREATE INDEX idx_notebook_user_sort ON notebook(user_id, sort_order);

-- ============================================================
-- 3. folder — 文件夹（支持多级树形结构）
-- ============================================================
CREATE TABLE folder (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES note_user(id) ON DELETE CASCADE,
    notebook_id BIGINT NOT NULL REFERENCES notebook(id) ON DELETE CASCADE,
    parent_id   BIGINT REFERENCES folder(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    sort_order  INTEGER DEFAULT 0,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    is_deleted  BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_folder_user_id ON folder(user_id);
CREATE INDEX idx_folder_notebook_id ON folder(notebook_id);
CREATE INDEX idx_folder_parent_id ON folder(parent_id);
CREATE INDEX idx_folder_user_notebook ON folder(user_id, notebook_id);
CREATE INDEX idx_folder_notebook_sort ON folder(notebook_id, sort_order);

-- ============================================================
-- 4. note — 笔记
-- ============================================================
CREATE TABLE note (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES note_user(id) ON DELETE CASCADE,
    notebook_id   BIGINT NOT NULL REFERENCES notebook(id) ON DELETE CASCADE,
    folder_id     BIGINT REFERENCES folder(id) ON DELETE SET NULL,
    title         VARCHAR(500) DEFAULT '未命名笔记',
    content_md    TEXT,
    content_html  TEXT,
    word_count    INTEGER DEFAULT 0,
    is_pinned     BOOLEAN DEFAULT FALSE,
    created_at    TIMESTAMP DEFAULT NOW(),
    updated_at    TIMESTAMP DEFAULT NOW(),
    is_deleted    BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_note_user_id ON note(user_id);
CREATE INDEX idx_note_notebook_id ON note(notebook_id);
CREATE INDEX idx_note_folder_id ON note(folder_id);
CREATE INDEX idx_note_user_notebook ON note(user_id, notebook_id);
CREATE INDEX idx_note_created_at ON note(created_at DESC);
CREATE INDEX idx_note_updated_at ON note(updated_at DESC);
CREATE INDEX idx_note_is_deleted ON note(is_deleted);

-- ============================================================
-- 5. note_link — 笔记双向链接
-- ============================================================
CREATE TABLE note_link (
    id                BIGSERIAL PRIMARY KEY,
    source_note_id    BIGINT NOT NULL REFERENCES note(id) ON DELETE CASCADE,
    target_note_id    BIGINT NOT NULL REFERENCES note(id) ON DELETE CASCADE,
    link_type         VARCHAR(20) DEFAULT 'wikilink',
    created_at        TIMESTAMP DEFAULT NOW(),
    UNIQUE(source_note_id, target_note_id)
);

CREATE INDEX idx_note_link_source ON note_link(source_note_id);
CREATE INDEX idx_note_link_target ON note_link(target_note_id);

-- ============================================================
-- 6. knowledge_base — 知识库
-- ============================================================
CREATE TABLE knowledge_base (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES note_user(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    icon        VARCHAR(50) DEFAULT '📚',
    sort_order  INTEGER DEFAULT 0,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_knowledge_base_user_id ON knowledge_base(user_id);
CREATE INDEX idx_knowledge_base_user_sort ON knowledge_base(user_id, sort_order);

-- ============================================================
-- 7. document_source — 导入的文档源
-- ============================================================
CREATE TABLE document_source (
    id                BIGSERIAL PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL REFERENCES knowledge_base(id) ON DELETE CASCADE,
    note_id           BIGINT REFERENCES note(id) ON DELETE SET NULL,
    user_id           BIGINT NOT NULL REFERENCES note_user(id) ON DELETE CASCADE,
    file_name         VARCHAR(500) NOT NULL,
    file_type         VARCHAR(20) NOT NULL,
    file_size         BIGINT NOT NULL,
    storage_path      VARCHAR(1000),
    raw_content       TEXT,
    chunk_status      VARCHAR(20) DEFAULT 'pending',
    error_message     TEXT,
    imported_at       TIMESTAMP DEFAULT NOW(),
    created_at        TIMESTAMP DEFAULT NOW(),
    updated_at        TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_document_source_kb_id ON document_source(knowledge_base_id);
CREATE INDEX idx_document_source_user_id ON document_source(user_id);
CREATE INDEX idx_document_source_status ON document_source(chunk_status);

-- ============================================================
-- 8. knowledge_chunk — 知识块（含向量嵌入）
-- ============================================================
CREATE TABLE knowledge_chunk (
    id                BIGSERIAL PRIMARY KEY,
    document_id       BIGINT REFERENCES document_source(id) ON DELETE CASCADE,
    knowledge_base_id BIGINT NOT NULL REFERENCES knowledge_base(id) ON DELETE CASCADE,
    note_id           BIGINT REFERENCES note(id) ON DELETE CASCADE,
    chunk_index       INTEGER NOT NULL,
    chunk_content     TEXT NOT NULL,
    chunk_token_count INTEGER DEFAULT 0,
    embedding         VECTOR(1024),
    source_info       JSONB,
    created_at        TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_knowledge_chunk_doc_id ON knowledge_chunk(document_id);
CREATE INDEX idx_knowledge_chunk_kb_id ON knowledge_chunk(knowledge_base_id);
CREATE INDEX idx_knowledge_chunk_note_id ON knowledge_chunk(note_id);
-- 向量索引：开发阶段先不建。
-- 原因：ivfflat 需要先有数据再建索引（要对数据聚类），而几千行的规模下
-- lists=100 意味着每簇不足 100 行，召回率反而下降，不如顺序扫描精确。
-- 数据量增长后（例如 > 1 万块）再启用 HNSW（无需训练、小数据量表现好）：
-- CREATE INDEX idx_knowledge_chunk_embedding ON knowledge_chunk
--     USING hnsw (embedding vector_cosine_ops);

-- ============================================================
-- 9. ai_config — AI 供应商配置
-- ============================================================
CREATE TABLE ai_config (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES note_user(id) ON DELETE CASCADE,
    provider_name   VARCHAR(50) NOT NULL,
    display_name    VARCHAR(100),
    api_key         TEXT NOT NULL,
    api_base_url    VARCHAR(500),
    chat_model      VARCHAR(100) NOT NULL,
    embedding_model VARCHAR(100),
    is_active       BOOLEAN DEFAULT FALSE,
    sort_order      INTEGER DEFAULT 0,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, provider_name)
);

CREATE INDEX idx_ai_config_user_id ON ai_config(user_id);

-- ============================================================
-- 10. conversation — 对话会话
-- ============================================================
CREATE TABLE conversation (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES note_user(id) ON DELETE CASCADE,
    title             VARCHAR(500),
    knowledge_base_id BIGINT REFERENCES knowledge_base(id) ON DELETE SET NULL,
    provider_name     VARCHAR(50),
    model             VARCHAR(100),
    mode              VARCHAR(20) DEFAULT 'chat',
    is_favorite       BOOLEAN DEFAULT FALSE,
    message_count     INTEGER DEFAULT 0,
    token_usage       INTEGER DEFAULT 0,
    created_at        TIMESTAMP DEFAULT NOW(),
    updated_at        TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_conversation_user_id ON conversation(user_id);
CREATE INDEX idx_conversation_kb_id ON conversation(knowledge_base_id);
CREATE INDEX idx_conversation_updated_at ON conversation(updated_at DESC);

-- ============================================================
-- 11. message — 对话消息
-- ============================================================
CREATE TABLE message (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    content         TEXT NOT NULL,
    tokens          INTEGER DEFAULT 0,
    metadata        JSONB,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_message_conversation_id ON message(conversation_id);
CREATE INDEX idx_message_conv_created ON message(conversation_id, created_at);

-- ============================================================
-- 12. prompt_shortcut — 快捷提示词
-- ============================================================
CREATE TABLE prompt_shortcut (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES note_user(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    prompt      TEXT NOT NULL,
    description VARCHAR(500),
    is_preset   BOOLEAN DEFAULT FALSE,
    sort_order  INTEGER DEFAULT 0,
    is_enabled  BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_prompt_shortcut_user_id ON prompt_shortcut(user_id);
CREATE INDEX idx_prompt_shortcut_user_sort ON prompt_shortcut(user_id, sort_order);

-- ============================================================
-- 13. user_profile — 用户 AI 交互画像
-- ============================================================
CREATE TABLE user_profile (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES note_user(id) ON DELETE CASCADE,
    identity          VARCHAR(500),
    tech_stack        VARCHAR(1000),
    answer_preference VARCHAR(1000),
    custom_prompt     TEXT,
    is_enabled        BOOLEAN DEFAULT TRUE,
    created_at        TIMESTAMP DEFAULT NOW(),
    updated_at        TIMESTAMP DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_user_profile_user_id ON user_profile(user_id);

-- ============================================================
-- 14. system_config — 系统配置 KV
-- ============================================================
CREATE TABLE system_config (
    id          BIGSERIAL PRIMARY KEY,
    config_key  VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

-- ============================================================
-- 种子数据
-- ============================================================

-- 14.1 默认管理员用户
INSERT INTO note_user (username, email, password, nickname)
VALUES ('admin', 'admin@notemind.local', '$2a$10$z6ynwtTELIcYig7wasHvzudk3sZ5e/Y0/0CqJfyfn9kDW1RhKFbci', '管理员');

-- 14.2 默认笔记本（关联用户 1）
INSERT INTO notebook (user_id, name, description, icon)
VALUES (1, '默认笔记本', '系统自动创建的默认笔记本', '📒');

-- 14.3 预设快捷提示词（关联用户 1）
INSERT INTO prompt_shortcut (user_id, name, prompt, description, is_preset, sort_order) VALUES
(1, '解释',
 '请用简单易懂的方式解释以下内容，适合初学者理解：\n\n{selection}',
 '用通俗语言解释复杂概念', TRUE, 1),

(1, '总结',
 '请对以下内容进行简洁的总结，提取关键信息：\n\n{selection}',
 '提取要点，生成摘要', TRUE, 2),

(1, '润色',
 '请对以下文本进行润色，使其更加流畅、专业，保持原意不变：\n\n{selection}',
 '优化语言表达', TRUE, 3),

(1, '扩写',
 '请对以下内容进行扩写，丰富细节和例证，保持原有风格：\n\n{selection}',
 '丰富内容细节', TRUE, 4),

(1, '翻译英文',
 '请将以下内容翻译成英文，保持专业性和准确性：\n\n{selection}',
 '中译英', TRUE, 5),

(1, '翻译中文',
 '请将以下内容翻译成中文，保持通顺自然：\n\n{selection}',
 '英译中', TRUE, 6),

(1, '继续写',
 '请根据以下内容继续续写，保持风格和逻辑一致：\n\n{selection}',
 '延续写作', TRUE, 7),

(1, '论文降重',
 '请对以下论文段落进行降重处理，保持原意、学术风格和逻辑结构的同时改变表达方式：\n\n{selection}',
 '降低论文查重率', TRUE, 8);

-- 14.4 AI 供应商预设（关联用户 1）
-- 注意: api_key 设为空字符串，部署前请填入真实密钥
INSERT INTO ai_config (user_id, provider_name, display_name, api_key, api_base_url, chat_model, embedding_model, is_active, sort_order)
VALUES
(1, 'deepseek', 'DeepSeek', '', 'https://api.deepseek.com', 'deepseek-chat', NULL, TRUE, 1),
(1, 'openai', 'OpenAI', '', 'https://api.openai.com/v1', 'gpt-4o', 'text-embedding-3-small', FALSE, 2),
(1, 'gemini', 'Gemini', '', 'https://generativelanguage.googleapis.com/v1beta', 'gemini-2.0-flash', NULL, FALSE, 3);

-- 14.5 默认用户画像（关联用户 1）
INSERT INTO user_profile (user_id, identity, tech_stack, answer_preference, custom_prompt)
VALUES (1, '学生', 'Java, Spring Boot, Vue3', '详细且结构化的回答，结合实际代码示例', '你是一位经验丰富的技术导师，请用清晰、工程化的语言回答问题。优先从实际应用场景出发，再深入原理。');
