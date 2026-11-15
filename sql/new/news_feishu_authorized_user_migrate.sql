-- 导入已有飞书资料的接收人，保留授权名册中的最新资料。
INSERT IGNORE INTO news_feishu_authorized_user (app_id, open_id, name, avatar_url)
SELECT r.app_id, r.open_id, r.name, r.avatar_url
FROM news_feishu_channel_config c,
     JSON_TABLE(COALESCE(c.config_json, JSON_OBJECT()), '$.recipients[*]' COLUMNS (
         app_id VARCHAR(128) PATH '$.appId',
         open_id VARCHAR(128) PATH '$.openId',
         name VARCHAR(255) PATH '$.name',
         avatar_url VARCHAR(2048) PATH '$.avatarUrl'
     )) AS r
WHERE r.app_id IS NOT NULL AND r.open_id IS NOT NULL
  AND (NULLIF(r.name, '') IS NOT NULL OR NULLIF(r.avatar_url, '') IS NOT NULL);

INSERT IGNORE INTO news_feishu_authorized_user (app_id, open_id, name, avatar_url)
SELECT app_id, open_id, recipient_name, recipient_avatar_url
FROM news_feishu_notification_setting
WHERE NULLIF(recipient_name, '') IS NOT NULL OR NULLIF(recipient_avatar_url, '') IS NOT NULL;
