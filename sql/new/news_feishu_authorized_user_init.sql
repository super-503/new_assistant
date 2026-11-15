CREATE TABLE IF NOT EXISTS news_feishu_authorized_user
(
    app_id     VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    open_id    VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    name       VARCHAR(255) NULL,
    avatar_url VARCHAR(2048) NULL,
    PRIMARY KEY (app_id, open_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '按飞书应用保存的授权人员';
