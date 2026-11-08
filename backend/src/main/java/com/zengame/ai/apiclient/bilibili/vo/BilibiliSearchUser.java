package com.zengame.ai.apiclient.bilibili.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 用户搜索的一条结果。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BilibiliSearchUser {

    /**
     * 用户 ID
     */
    private String mid;

    /**
     * 昵称
     */
    private String uname;

    /**
     * 签名
     */
    private String usign;

    /**
     * 头像地址，协议相对地址（//i0.hdslb.com/...）
     */
    private String upic;

    /**
     * 粉丝数
     */
    private Long fans;
}
