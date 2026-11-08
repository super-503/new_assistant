package com.zengame.ai.apiclient.bilibili.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 网页端扫码登录生成的二维码。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BilibiliQrCodeData {

    /**
     * 二维码内容，即手机端扫码打开的地址
     */
    private String url;

    /**
     * 扫码登录密钥，轮询扫码结果时传回
     */
    @JsonProperty("qrcode_key")
    private String qrcodeKey;
}
