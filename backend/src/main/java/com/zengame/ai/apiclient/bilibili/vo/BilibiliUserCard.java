package com.zengame.ai.apiclient.bilibili.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 用户卡片：UP 主的昵称、头像、签名和粉丝数。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BilibiliUserCard {

    /**
     * 用户 ID
     */
    private String mid;

    /**
     * 昵称
     */
    private String name;

    /**
     * 头像地址
     */
    private String face;

    /**
     * 签名
     */
    private String sign;

    /**
     * 粉丝数
     */
    private Long fans;
}
