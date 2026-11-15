package com.zengame.ai.business.news.setting.domain.bo;

import com.zengame.ai.business.news.setting.domain.enums.NewsFeishuCardColor;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** 飞书日报卡片的展示设置。 */
@Data
public class NewsFeishuCardSettings {
    @NotBlank
    @Size(max = 80)
    private String title = "资讯日报";

    @NotNull
    private NewsFeishuCardColor color = NewsFeishuCardColor.BLUE;

    private boolean showAuthor = true;

    @Valid
    private NewsFeishuCardTemplate template;
}
