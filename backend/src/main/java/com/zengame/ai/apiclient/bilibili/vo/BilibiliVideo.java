package com.zengame.ai.apiclient.bilibili.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * UP 主投稿列表里的一个视频。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BilibiliVideo {

    /**
     * 视频 BV 号
     */
    private String bvid;

    /**
     * 标题
     */
    private String title;

    /**
     * 封面地址
     */
    private String pic;

    /**
     * 简介，纯文本
     */
    private String description;

    /**
     * 投稿时间，秒级时间戳
     */
    private Long created;

    /**
     * 时长，mm:ss 或 h:mm:ss
     */
    private String length;

    /**
     * 播放数；隐藏播放数的视频为「--」
     */
    private String play;

    /**
     * 评论数
     */
    private Long comment;
}
