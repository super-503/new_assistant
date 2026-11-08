-- 资讯 AI 字段：新增「划重点」「提及产品」，岗位分类补充宏观内容的打标规则，测试 -> 线上
-- 岗位分类保留现有选项。脚本按字段标识更新配置。

START TRANSACTION;

INSERT INTO news_ai_field
    (field_key, name, kind, source, output_type, multiline, options_json, show_as_tag,
     prompt, required, editable, enabled, article_only, sort_no)
VALUES ('highlights', '划重点', 'CUSTOM', 'BODY', 'TEXT', 1, '[]', 0,
        '从中找出快讯需要关注的三个重点，每个重点描述不超过25字，按 ① ② ③ 逐条输出，每条一行；不足三个时按实际条数输出。',
        0, 1, 1, 0, 5),
       ('mentioned_product', '提及产品', 'CUSTOM', 'TITLE_BODY', 'TEXT', 0, '[]', 0,
        '提取资讯提及的具体产品名称，只总结输出一个产品名称；没有提及任何产品时留空，不要强行总结。',
        0, 1, 1, 0, 6)
ON DUPLICATE KEY UPDATE name         = VALUES(name),
                        kind         = VALUES(kind),
                        source       = VALUES(source),
                        output_type  = VALUES(output_type),
                        multiline    = VALUES(multiline),
                        options_json = VALUES(options_json),
                        show_as_tag  = VALUES(show_as_tag),
                        prompt       = VALUES(prompt),
                        required     = VALUES(required),
                        editable     = VALUES(editable),
                        enabled      = VALUES(enabled),
                        article_only = VALUES(article_only),
                        sort_no      = VALUES(sort_no);

UPDATE news_ai_field
SET prompt = '从岗位分类选项中选择会从这条资讯中获益的岗位，可多选；战略方向、行业动态、专家观点、产品上线后的成绩这类宏观内容不细分到具体岗位族，选择全部选项；没有相关岗位时返回空。'
WHERE field_key = 'job_family';

COMMIT;

SELECT id, field_key, name, kind, source, output_type, multiline, options_json, show_as_tag, sort_no, prompt
FROM news_ai_field
WHERE field_key IN ('highlights', 'mentioned_product', 'job_family')
ORDER BY sort_no;
