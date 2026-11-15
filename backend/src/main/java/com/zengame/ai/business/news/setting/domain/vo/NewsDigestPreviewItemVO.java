package com.zengame.ai.business.news.setting.domain.vo;

import lombok.Value;

import java.util.List;

/** 按推送显示配置生成的一条资讯及其有值的附加字段。 */
@Value
public class NewsDigestPreviewItemVO {
    String title;
    String url;
    List<Field> fields;

    /** 推送卡片中的字段名称和已格式化内容。 */
    @Value
    public static class Field {
        String key;
        String label;
        String display;
    }
}
