CREATE TABLE IF NOT EXISTS news_feishu_channel_config
(
    id          TINYINT NOT NULL PRIMARY KEY COMMENT '飞书渠道配置固定为 1',
    revision    BIGINT NOT NULL DEFAULT 0 COMMENT '保存版本',
    config_json JSON NULL COMMENT '飞书应用、推送人员与日报卡片'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '飞书渠道配置';

INSERT IGNORE INTO news_feishu_channel_config (id) VALUES (1);

CREATE TABLE IF NOT EXISTS news_feishu_digest_delivery
(
    item_id BIGINT NOT NULL,
    app_id  VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    open_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (item_id, app_id, open_id),
    CONSTRAINT fk_news_feishu_digest_item FOREIGN KEY (item_id) REFERENCES news_item (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '尚未完成全部人员投递的日报回执';
