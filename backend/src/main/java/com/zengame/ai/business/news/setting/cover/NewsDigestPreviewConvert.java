package com.zengame.ai.business.news.setting.cover;

import com.zengame.ai.apiclient.feishu.FeishuDigestCard;
import com.zengame.ai.business.news.setting.domain.vo.NewsDigestPreviewItemVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/** 飞书发送内容与管理端布局预览之间的转换。 */
@Mapper
public interface NewsDigestPreviewConvert {
    NewsDigestPreviewConvert INSTANCE = Mappers.getMapper(NewsDigestPreviewConvert.class);

    /** @return 按发送顺序排列的资讯预览 */
    List<NewsDigestPreviewItemVO> toVO(List<FeishuDigestCard.Entry> entries);

    /** @return 保留标题、链接和字段顺序的单条资讯 */
    NewsDigestPreviewItemVO toVO(FeishuDigestCard.Entry entry);

    /** @return 可直接展示的字段文本 */
    NewsDigestPreviewItemVO.Field toVO(FeishuDigestCard.Field field);
}
