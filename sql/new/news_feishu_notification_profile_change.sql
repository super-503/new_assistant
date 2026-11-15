ALTER TABLE news_feishu_notification_setting
    ADD COLUMN recipient_name VARCHAR(255) NULL COMMENT '飞书推送接收人的显示名称',
    ADD COLUMN recipient_avatar_url VARCHAR(2048) NULL COMMENT '飞书推送接收人的头像地址';
