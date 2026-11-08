-- 资讯 AI 模型配置。新库和已有库均可执行。
CREATE TABLE IF NOT EXISTS news_ai_model_setting
(
    id         TINYINT       NOT NULL PRIMARY KEY COMMENT '全局配置固定为 1',
    model_url  VARCHAR(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '完整的 Chat Completions 请求地址',
    model      VARCHAR(255)  NOT NULL COMMENT '模型名称',
    api_key    VARCHAR(2048) NULL COMMENT '可选的 Bearer 密钥'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '资讯 AI 模型配置';
