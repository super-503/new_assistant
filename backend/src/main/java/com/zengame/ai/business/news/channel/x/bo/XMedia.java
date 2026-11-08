package com.zengame.ai.business.news.channel.x.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

/**
 * 推文里的一张图片、一段视频或一个 GIF。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XMedia {

    /**
     * 媒体类型
     */
    private XMediaType type;

    /**
     * 正文末尾指向该媒体的 t.co 短链
     */
    private String tcoUrl;

    /**
     * 图片为原图地址（name=orig），视频和 GIF 为封面地址
     */
    private String imageUrl;

    /**
     * 视频和 GIF 码率最高的 mp4 地址；图片，或视频没有 mp4 时为空
     */
    private Optional<String> videoUrl;

    /**
     * 视频时长（秒）；图片和 GIF 为空
     */
    private Optional<Integer> durationSeconds;
}
