package com.zengame.ai.apiclient.bilibili.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * nav 接口的登录信息：当前登录账号和 WBI 签名用的 key 图片地址。未登录时只有 WBI 部分有值。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BilibiliNavData {

    /**
     * 是否已登录
     */
    @JsonProperty("isLogin")
    private boolean login;

    /**
     * 登录账号的用户 ID
     */
    private Long mid;

    /**
     * 登录账号的昵称
     */
    private String uname;

    /**
     * WBI 签名 key 所在的图片地址
     */
    @JsonProperty("wbi_img")
    private WbiImg wbiImg;

    /**
     * WBI 签名 key 所在的图片地址，文件名（不含扩展名）即 key。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WbiImg {

        /**
         * img_key 所在的图片地址
         */
        @JsonProperty("img_url")
        private String imgUrl;

        /**
         * sub_key 所在的图片地址
         */
        @JsonProperty("sub_url")
        private String subUrl;
    }
}
