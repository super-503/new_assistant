ALTER TABLE news_item
    ADD COLUMN digest_sent_time DATETIME NULL COMMENT '个人日报发送成功时间' AFTER review_time,
    ADD KEY idx_digest_pending (status, digest_sent_time, review_time, id);
