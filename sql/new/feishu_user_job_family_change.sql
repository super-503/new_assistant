-- feishu_user 增加岗位序列三字段，数据来源 HR 开放接口 /openapi/api/1/business/employee/jobFamily，
-- 通讯录定时同步时按 wx_user_id 批量拉取回填。

ALTER TABLE user_center.feishu_user
    ADD COLUMN `job_family` VARCHAR(100) NULL COMMENT '职位族，如 技术族' AFTER `position`,
    ADD COLUMN `job_category` VARCHAR(100) NULL COMMENT '职位类，如 开发类' AFTER `job_family`,
    ADD COLUMN `job_subcategory` VARCHAR(100) NULL COMMENT '职位子类，如 全栈开发' AFTER `job_category`;

-- 回滚：
-- ALTER TABLE user_center.feishu_user
--     DROP COLUMN `job_family`,
--     DROP COLUMN `job_category`,
--     DROP COLUMN `job_subcategory`;
