-- ============================================================
-- 资讯系统 - 数据库初始化脚本
--
-- 七张表：渠道、订阅源、资讯、处理记录、AI 字段、拒绝理由、运行记录。
-- 枚举字段用 VARCHAR 存枚举名，取值由 business/news/domain/enums 下的 Java 枚举控制。
-- 渠道记录与渠道插件一一对应，应用启动时比对，任一方多出即启动失败。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `news_assistant` CHARACTER SET utf8mb4;
USE `news_assistant`;

CREATE TABLE IF NOT EXISTS news_channel
(
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    code                   VARCHAR(32)  NOT NULL COMMENT '渠道标识，与插件 ChannelDescriptor.code 一致',
    name                   VARCHAR(32)  NOT NULL COMMENT '渠道名称',
    enabled                TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0停用',
    credential_json        TEXT         NULL COMMENT '渠道凭证，插件自定义的不透明 JSON；无凭证或未登录时为空',
    credential_status      VARCHAR(16)  NOT NULL COMMENT '凭证状态：NONE 无需凭证 / VALID 有效 / INVALID 已失效',
    credential_account     VARCHAR(128) NULL COMMENT '凭证对应的登录账号',
    credential_update_time DATETIME     NULL COMMENT '凭证最近一次保存时间',
    create_time            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '资讯渠道';

CREATE TABLE IF NOT EXISTS news_feed
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_code   VARCHAR(32)   NOT NULL COMMENT '所属渠道标识（news_channel.code）',
    source_key     VARCHAR(128)  CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '插件内的来源标识：公众号 fakeid、B站 mid、X 用户 rest_id、AIBase 固定值',
    source_name    VARCHAR(128)  NOT NULL COMMENT '来源在渠道上的名称',
    display_name   VARCHAR(128)  NOT NULL COMMENT '显示名称',
    avatar_url     VARCHAR(1024) NULL COMMENT '来源头像地址',
    description    VARCHAR(1000) NULL COMMENT '来源简介',
    content_type   VARCHAR(16)   NOT NULL COMMENT '内容类型：ARTICLE 文章 / VIDEO 视频',
    enabled        TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '是否启用订阅：1启用 0停用',
    pull_status    VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT '拉取状态：PENDING 待首次拉取 / OK 正常 / ERROR 拉取失败',
    pull_error     VARCHAR(1000) NULL COMMENT '最近一次拉取失败的原因',
    last_pull_time DATETIME      NULL COMMENT '最近一次拉取时间',
    cursor_time    DATETIME      NULL COMMENT '拉取进度：已入库条目中最晚的订阅源出现时间，下次只取此后的内容；为空表示未拉取过',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_channel_source (channel_code, source_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '资讯订阅源';

CREATE TABLE IF NOT EXISTS news_item
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    feed_id             BIGINT        NOT NULL COMMENT '来源订阅源（news_feed.id）',
    channel_code        VARCHAR(32)   NOT NULL COMMENT '所属渠道标识（news_channel.code）',
    canonical_url       VARCHAR(768)  CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '规范化后的原文链接，全局去重依据',
    content_type        VARCHAR(16)   NOT NULL COMMENT '内容类型：ARTICLE 文章 / VIDEO 视频，取自订阅源',
    origin_title        VARCHAR(512)  NULL COMMENT '原文标题；X 推文没有标题',
    author              VARCHAR(128)  NULL COMMENT '作者',
    author_avatar_url   VARCHAR(1024) NULL COMMENT '作者头像地址',
    publish_time        DATETIME      NOT NULL COMMENT '发布时间',
    cover_url           VARCHAR(1024) NULL COMMENT '封面地址',
    body_html           MEDIUMTEXT    NULL COMMENT '清洗后的正文 HTML；为空表示正文暂不可获取',
    playback_json       VARCHAR(2048) NULL COMMENT '站内播放方式 JSON：{kind: EMBED/FILE, url, coverUrl}；没有时为空',
    stats_json          VARCHAR(1024) NOT NULL DEFAULT '[]' COMMENT '统计项 JSON 数组：[{name, value}]',
    duration_seconds    INT           NULL COMMENT '视频时长（秒），视频才有',
    status              VARCHAR(16)   NOT NULL DEFAULT 'PROCESSING' COMMENT '状态：PROCESSING 加工中 / PENDING 待审 / APPROVED 已通过 / REJECTED 已拒绝',
    ai_values_json      TEXT          NOT NULL COMMENT 'AI 字段结果 JSON 对象：{字段标识: 值}',
    ai_failed_keys_json VARCHAR(1024) NOT NULL DEFAULT '[]' COMMENT '未生成的字段标识 JSON 数组',
    edited_keys_json    VARCHAR(1024) NOT NULL DEFAULT '[]' COMMENT '人工修改过的字段标识 JSON 数组',
    reject_reason_id    BIGINT        NULL COMMENT '拒绝理由（news_reject_reason.id）',
    reject_note         VARCHAR(500)  NULL COMMENT '拒绝补充说明',
    reviewer            VARCHAR(64)   NULL COMMENT '审核人登录名',
    review_time         DATETIME      NULL COMMENT '审核时间',
    create_time         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    update_time         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_canonical_url (canonical_url),
    KEY idx_status_publish (status, publish_time),
    KEY idx_channel_status (channel_code, status, publish_time),
    KEY idx_feed_status (feed_id, status, publish_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '资讯';

CREATE TABLE IF NOT EXISTS news_item_log
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id     BIGINT      NOT NULL COMMENT '资讯（news_item.id）',
    action      VARCHAR(32) NOT NULL COMMENT '动作：COLLECTED / PROCESSED / PROCESS_FAILED / FIELD_EDITED / APPROVED / REJECTED / RESTORED',
    operator    VARCHAR(64) NULL COMMENT '操作人登录名；系统动作为空',
    detail_json TEXT        NULL COMMENT '附加信息 JSON，如拒绝理由、修改的字段；没有时为空',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    KEY idx_item (item_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '资讯处理记录';

CREATE TABLE IF NOT EXISTS news_ai_field
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    field_key    VARCHAR(64)   CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '字段标识，格式 ^[a-z][a-z0-9_]*$，结构化结果按它存 JSON',
    name         VARCHAR(32)   NOT NULL COMMENT '字段名称',
    kind         VARCHAR(16)   NOT NULL DEFAULT 'CUSTOM' COMMENT '种类：SYSTEM 系统 / BUILTIN 内置 / CUSTOM 普通',
    source       VARCHAR(16)   NOT NULL COMMENT '来源内容：BODY 正文 / TITLE 标题 / TITLE_BODY 标题 + 正文',
    output_type  VARCHAR(16)   NOT NULL COMMENT '输出类型：TEXT 文本 / NUMBER 数字 / SINGLE 单选 / MULTI 多选',
    multiline    TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '文本是否多行：1是 0否',
    options_json VARCHAR(4000) NOT NULL DEFAULT '[]' COMMENT '单选、多选的选项 JSON 字符串数组',
    show_as_tag  TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否显示为标签：1是 0否',
    prompt       VARCHAR(2000) NOT NULL COMMENT '提示词',
    required     TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否必填：1是 0否',
    editable     TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '是否允许人工修改：1是 0否',
    enabled      TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0停用',
    article_only TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否只对文章生成：1是 0否',
    sort_no      INT           NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前，即详情页展示顺序',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_field_key (field_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '资讯 AI 字段';

CREATE TABLE IF NOT EXISTS news_reject_reason
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64) NOT NULL COMMENT '理由名称',
    enabled     TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0停用',
    sort_no     INT         NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '资讯拒绝理由';

CREATE TABLE IF NOT EXISTS news_channel_run
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_code VARCHAR(32) NOT NULL COMMENT '渠道标识（news_channel.code）',
    feed_id      BIGINT      NULL COMMENT '手动触发时拉取的订阅源（news_feed.id）；定时运行为空',
    trigger_type VARCHAR(16) NOT NULL COMMENT '触发方式：SCHEDULE 定时 / MANUAL 手动',
    status       VARCHAR(16) NOT NULL COMMENT '结果：SUCCEEDED 成功 / FAILED 失败',
    new_count    INT         NOT NULL DEFAULT 0 COMMENT '新增资讯条数',
    detail       TEXT        NULL COMMENT '运行详情，如各订阅源的结果和失败原因',
    start_time   DATETIME    NOT NULL COMMENT '开始时间',
    end_time     DATETIME    NOT NULL COMMENT '结束时间',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_channel_start (channel_code, start_time),
    KEY idx_feed_start (feed_id, start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '资讯渠道运行记录';

START TRANSACTION;

-- ------------------------------------------------------------
-- 渠道：AIBase 无需凭证，其余渠道待超管登录
-- ------------------------------------------------------------
INSERT INTO news_channel (code, name, credential_status)
VALUES ('wechat_mp', '公众号', 'INVALID'),
       ('bilibili', 'B站', 'INVALID'),
       ('x', 'X', 'INVALID'),
       ('aibase', 'AIBase', 'NONE');

-- ------------------------------------------------------------
-- AIBase 的固定订阅源
-- ------------------------------------------------------------
INSERT INTO news_feed (channel_code, source_key, source_name, display_name, content_type)
VALUES ('aibase', 'news', 'AIBase 资讯', 'AIBase 资讯', 'ARTICLE');

-- ------------------------------------------------------------
-- AI 字段：岗位分类选项可接入职位族同步，主题选项由管理员维护
-- ------------------------------------------------------------
INSERT INTO news_ai_field
(field_key, name, kind, source, output_type, multiline, options_json, show_as_tag, prompt, required, editable, enabled, article_only, sort_no)
VALUES ('title', '标题', 'SYSTEM', 'TITLE_BODY', 'TEXT', 0, '[]', 0,
        '根据资讯标题与正文生成准确、简洁的中文标题，不夸大、不添加原文没有的信息。', 1, 1, 1, 0, 1),
       ('summary', '摘要', 'CUSTOM', 'BODY', 'TEXT', 1, '[]', 0,
        '请根据资讯正文生成简洁的中文摘要，保留主要事实、适用场景与技术要点，不补充原文未提及的信息。', 1, 1, 1, 1, 2),
       ('job_family', '岗位分类', 'BUILTIN', 'TITLE_BODY', 'MULTI', 0, '[]', 1,
        '从岗位分类选项中选择会从这条资讯中获益的岗位，可多选；战略方向、行业动态、专家观点、产品上线后的成绩这类宏观内容不细分到具体岗位族，选择全部选项；没有相关岗位时返回空。', 0, 1, 1, 0, 3),
       ('topic', '主题', 'CUSTOM', 'TITLE_BODY', 'MULTI', 0, '[]', 1,
        '从主题选项中选择与资讯内容相关的主题，可多选；没有合适的主题时返回空。', 0, 1, 1, 0, 4),
       ('highlights', '划重点', 'CUSTOM', 'BODY', 'TEXT', 1, '[]', 0,
        '从中找出快讯需要关注的三个重点，每个重点描述不超过25字，按 ① ② ③ 逐条输出，每条一行；不足三个时按实际条数输出。',
        0, 1, 1, 0, 5),
       ('mentioned_product', '提及产品', 'CUSTOM', 'TITLE_BODY', 'TEXT', 0, '[]', 0,
        '提取资讯提及的具体产品名称，只总结输出一个产品名称；没有提及任何产品时留空，不要强行总结。',
        0, 1, 1, 0, 6);

-- ------------------------------------------------------------
-- 拒绝理由
-- ------------------------------------------------------------
INSERT INTO news_reject_reason (name, sort_no)
VALUES ('与技术方向无关', 1),
       ('营销推广', 2),
       ('内容质量不足', 3),
       ('内容重复', 4),
       ('其他', 5);

COMMIT;
