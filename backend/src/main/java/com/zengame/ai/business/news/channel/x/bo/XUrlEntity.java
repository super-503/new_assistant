package com.zengame.ai.business.news.channel.x.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推文正文里的一个 t.co 短链及其原始链接。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XUrlEntity {

    /**
     * 正文里出现的 t.co 短链
     */
    private String url;

    /**
     * 原始链接
     */
    private String expandedUrl;
}
