CREATE TABLE IF NOT EXISTS news_push_plan
(
    id          BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    revision    BIGINT NOT NULL DEFAULT 0,
    enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    next_run_at DATETIME NULL COMMENT 'UTC',
    config_json JSON NOT NULL,
    INDEX idx_news_push_plan_due (enabled, next_run_at, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '资讯推送计划';

CREATE TABLE IF NOT EXISTS news_push_plan_item
(
    plan_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    PRIMARY KEY (plan_id, item_id),
    FOREIGN KEY (plan_id) REFERENCES news_push_plan (id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES news_item (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '已完成全部接收人投递的计划资讯';

CREATE TABLE IF NOT EXISTS news_push_plan_delivery
(
    plan_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    app_id  VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    open_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (plan_id, item_id, app_id, open_id),
    FOREIGN KEY (plan_id) REFERENCES news_push_plan (id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES news_item (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '推送计划的接收人投递回执';
