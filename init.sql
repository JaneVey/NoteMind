-- ============================================================
-- NoteMind 数据库初始化脚本
--
-- 目标镜像：pgvector/pgvector:pg16
-- 数据库名：notemind
-- 最后更新：2026-10-08（按《04-数据库设计规范》重构）
--
-- 使用方式（二选一）：
--   1) Docker：docker-compose 将本文件挂载到 /docker-entrypoint-initdb.d/init.sql，
--      容器首次初始化数据卷时自动执行
--   2) 手动：createdb notemind && psql -d notemind -f init.sql
--
-- 本次重构的范围（重要）：
--   ✅ 公共表定稿：sys_user / user_profile / sys_operation_log
--   ⏳ 业务表待设计：笔记、知识库、AI 会话相关表
--       业务表的字段取决于功能细节，将在对应模块开发时连同设计一起定稿。
--       现在提前拍字段，开发时必然要改 —— 改 schema 比写代码贵得多。
--
-- 本脚本遵循的规范要点：
--   1. 时间字段一律 TIMESTAMPTZ（跨时区安全；与设计文档保持一致）
--   2. 不使用 ON DELETE CASCADE —— 删除的级联语义由应用层显式决定
--   3. 每张表在注释块中声明【删除策略】
--   4. 索引命名 idx_ / uk_ / pk_
--   5. 布尔字段 is_xxx；Java 侧属性名不带 is 前缀（用 @TableField 映射）
-- ============================================================

-- ============================================================
-- 0. 扩展
-- ============================================================
CREATE EXTENSION IF NOT EXISTS vector;   -- 向量检索（pgvector）

-- citext：大小写不敏感文本。
-- 用于 username 与 email —— 两者在语义上都不区分大小写
-- （Admin 与 admin 是同一个账号，Foo@x.com 与 foo@x.com 是同一个邮箱）。
-- 放在数据库层面保证，比在应用层手动 lower() 可靠：
-- 应用层只要有一处忘了规范化，就会产生重复账号。
CREATE EXTENSION IF NOT EXISTS citext;

-- ============================================================
-- 1. sys_user  用户（登录账号）
--    删除策略：硬删（注销即物理删除）
--    说明：本项目没有"注销后保留审计线索"的需求，因此不引入逻辑删除。
--          逻辑删除会让 username 的唯一索引变复杂（需改为部分索引），
--          在需求明确之前不引入这层复杂度。
--
--    ★ 2026-10-08 认证重设计后的变更：
--      · username / email 改为 CITEXT（大小写不敏感唯一）
--      · email 由可空改为 NOT NULL + 唯一 —— 它是账号找回的唯一途径
--      · password 由 NOT NULL 改为可空 —— OAuth 用户没有密码
--      · 新增 email_verified / status / last_login_at
-- ============================================================
CREATE TABLE sys_user (
    id             BIGSERIAL    PRIMARY KEY,
    username       CITEXT       NOT NULL,
    password       VARCHAR(500),
    email          CITEXT       NOT NULL,
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    nickname       VARCHAR(100),
    avatar         VARCHAR(500),
    status         VARCHAR(20)  NOT NULL DEFAULT 'active',
    last_login_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  sys_user                IS '用户（登录账号）';
COMMENT ON COLUMN sys_user.password       IS 'BCrypt 哈希。为 NULL 表示该账号只能通过第三方登录（如 GitHub）';
COMMENT ON COLUMN sys_user.email          IS '必填，用于账号找回；大小写不敏感唯一';
COMMENT ON COLUMN sys_user.email_verified IS '邮箱是否已验证。未验证的账号仍可正常使用，界面引导验证';
COMMENT ON COLUMN sys_user.status         IS '账号状态：active / disabled。用 VARCHAR 而非 PG ENUM，避免改值要写 DDL';
COMMENT ON COLUMN sys_user.last_login_at  IS '最近登录时间，登录审计用';
COMMENT ON COLUMN sys_user.avatar         IS '头像 URL（MinIO 或外部链接）';

-- 唯一索引：应用层的注册查重在并发下存在竞态，
-- 最终一致性由数据库唯一索引兜底
CREATE UNIQUE INDEX uk_sys_user_username ON sys_user (username);
CREATE UNIQUE INDEX uk_sys_user_email    ON sys_user (email);

-- ============================================================
-- 2. sys_user_oauth  第三方账号绑定
--    删除策略：硬删（解绑即删除记录）
--    说明：一个用户可以绑定多个第三方平台；一个第三方账号只能绑定一个用户
-- ============================================================
CREATE TABLE sys_user_oauth (
    id                BIGSERIAL    PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES sys_user (id),
    provider          VARCHAR(32)  NOT NULL,
    provider_user_id  VARCHAR(128) NOT NULL,
    provider_username VARCHAR(128),
    avatar_url        VARCHAR(500),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  sys_user_oauth                   IS '第三方账号绑定（GitHub 等）';
COMMENT ON COLUMN sys_user_oauth.provider          IS '平台标识：github（预留 google / wechat）';
COMMENT ON COLUMN sys_user_oauth.provider_user_id  IS '第三方平台的用户唯一 ID —— 绑定关系以它为准，不用邮箱或用户名';
COMMENT ON COLUMN sys_user_oauth.provider_username IS '第三方平台的用户名/昵称，仅用于展示';
COMMENT ON COLUMN sys_user_oauth.avatar_url        IS '第三方平台的头像，可在用户未上传头像时作为默认值';

CREATE UNIQUE INDEX uk_sys_user_oauth_provider ON sys_user_oauth (provider, provider_user_id);
CREATE INDEX        idx_sys_user_oauth_user_id ON sys_user_oauth (user_id);

-- ============================================================
-- 3. user_profile  用户画像
--    删除策略：硬删（随用户删除）
--    说明：一个用户一条记录，内容注入 System Prompt
-- ============================================================
CREATE TABLE user_profile (
    id                BIGSERIAL     PRIMARY KEY,
    user_id           BIGINT        NOT NULL REFERENCES sys_user (id),
    identity          VARCHAR(100),
    tech_stack        VARCHAR(500),
    answer_preference VARCHAR(1000),
    custom_prompt     TEXT,
    is_enabled        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  user_profile            IS '用户画像（注入 System Prompt）';
COMMENT ON COLUMN user_profile.identity   IS '身份，如"学生"、"后端开发"';
COMMENT ON COLUMN user_profile.is_enabled IS '是否注入对话；对应 Java 属性 enabled';

CREATE UNIQUE INDEX uk_user_profile_user_id ON user_profile (user_id);


-- ============================================================
-- 4. sys_operation_log  操作日志
--    删除策略：只追加，不删除（保留期由运维策略决定，本期不做清理）
--    说明：故意不设 updated_at —— 本表只追加、不修改
-- ============================================================
CREATE TABLE sys_operation_log (
    id            BIGSERIAL     PRIMARY KEY,
    user_id       BIGINT,
    module        VARCHAR(50)   NOT NULL,
    operation     VARCHAR(100)  NOT NULL,
    http_method   VARCHAR(10),
    request_uri   VARCHAR(500),
    request_param TEXT,
    success       BOOLEAN       NOT NULL DEFAULT TRUE,
    error_code    INTEGER,
    error_message VARCHAR(1000),
    client_ip     VARCHAR(64),
    user_agent    VARCHAR(500),
    duration_ms   INTEGER,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  sys_operation_log               IS '操作日志（审计与问题排查）';
COMMENT ON COLUMN sys_operation_log.module        IS '模块：auth / user / note / knowledge / ai';
COMMENT ON COLUMN sys_operation_log.operation     IS '操作名：REGISTER / LOGIN / CREATE_NOTE ...';
COMMENT ON COLUMN sys_operation_log.request_param IS '请求参数快照，必须脱敏（禁止记录密码、API Key）';
COMMENT ON COLUMN sys_operation_log.error_code    IS '失败时的业务错误码（见 ErrorCode 枚举）';

CREATE INDEX idx_sys_operation_log_user_id    ON sys_operation_log (user_id);
CREATE INDEX idx_sys_operation_log_module     ON sys_operation_log (module);
CREATE INDEX idx_sys_operation_log_created_at ON sys_operation_log (created_at DESC);

-- ============================================================
-- 5. 业务表（待设计 —— 此处仅登记表名与用途，防止命名再次漂移）
-- ============================================================
--   笔记模块   note_notebook    笔记本       软删
--              note_folder      文件夹       软删
--              note             笔记         软删
--              note_link        双向链接     硬删（派生数据）
--
--   知识库     knowledge_base       知识库    软删
--              knowledge_document   文档      软删
--              knowledge_chunk      知识块    硬删（派生数据，量大）
--
--   AI         ai_config            供应商配置   硬删
--              ai_conversation      会话         硬删
--              ai_message           消息         硬删
--              prompt_shortcut      快捷指令     硬删
--
-- 设计这些表时必须遵守《04-数据库设计规范》，特别注意：
--   · 所有业务表带 user_id 并建索引（多用户隔离的唯一依据，且每个查询都必须带上它）
--   · 不使用 ON DELETE CASCADE
--   · 每张表注释中声明删除策略
--   · knowledge_chunk.embedding 类型为 VECTOR(1024)
--   · knowledge_chunk.source_info 与 ai_message.metadata 的 JSONB 结构
--     必须按规范 5.2 节约定的字段名书写

-- ============================================================
-- 6. Redis 中使用的键（不建表，此处登记以便查阅）
-- ============================================================
-- Refresh Token 与限流计数都放 Redis，不建表 —— TTL 天然契合"到期即失效"。
-- 键的完整定义见《03-系统设计/用户系统设计.md》第七节：
--
--   refresh:{sha256(token)}       → 会话信息            TTL 7 天 / 30 天（记住我）
--   refresh:user:{userId}         → SET(该用户全部 tokenHash)   用于列出设备与强制下线
--   refresh:used:{sha256(token)}  → 重用检测标记        TTL = 原有效期
--   oauth:state:{state}           → OAuth CSRF 防护     TTL 10 分钟
--   auth:fail:ip:{ip}             → 登录失败计数        TTL 15 分钟
--   auth:fail:id:{identifier}     → 登录失败计数        TTL 15 分钟
--
-- ★ 安全要点：refresh token 在 Redis 里存的是 sha256 哈希，不是原文 ——
--   与密码同理，Redis 被读走也不能直接使用。

-- ============================================================
-- 7. 种子数据
-- ============================================================
-- 默认管理员。密码明文：admin123456
-- ⚠️ 仅开发环境使用，部署到公网前必须修改
-- 哈希由 bcryptjs 生成，前缀由 $2b$ 规范化为 $2a$
-- （对 ≤72 字节的密码，bcrypt 2a 与 2b 的验证结果完全一致）
INSERT INTO sys_user (id, username, password, email, email_verified, nickname)
VALUES (1, 'admin',
        '$2a$10$IieD705/P1uMjDESYZQA0.3xS/N5gUPpJ2nSImezICb86SOkSJBMC',
        'admin@notemind.local', TRUE, '管理员')
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_profile (user_id, identity, tech_stack, answer_preference, custom_prompt)
VALUES (1, '学生', 'Java, Spring Boot, Vue3',
        '详细且结构化的回答，结合实际代码示例',
        '你是一位经验丰富的技术导师，请用清晰、工程化的语言回答问题。优先从实际应用场景出发，再深入原理。')
ON CONFLICT (user_id) DO NOTHING;

-- 上面手工插入了显式 id，需要把自增序列推到最大值之后，否则后续插入会主键冲突
SELECT setval('sys_user_id_seq', GREATEST((SELECT MAX(id) FROM sys_user), 1));
