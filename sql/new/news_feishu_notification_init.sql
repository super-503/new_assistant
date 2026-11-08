CREATE TABLE IF NOT EXISTS news_feishu_notification_setting
(
    id         TINYINT      NOT NULL PRIMARY KEY COMMENT '全局配置固定为 1',
    app_id     VARCHAR(128) NOT NULL COMMENT '飞书自建应用 App ID',
    app_secret VARCHAR(512) NOT NULL COMMENT '飞书自建应用 App Secret',
    open_id    VARCHAR(128) NOT NULL COMMENT '日报接收人在应用内的 Open ID',
    enabled    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用日报推送'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '飞书应用机器人通知配置';
