package com.zengame.ai.repository.ai.mapper;

import com.zengame.ai.business.news.domain.enums.NewsItemStatus;
import com.zengame.ai.business.news.library.domain.bo.NewsItemGroupCountBO;
import com.zengame.ai.business.news.review.domain.bo.NewsItemQueryBO;
import com.zengame.ai.repository.ai.po.NewsItemPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资讯持久化接口。
 */
@Mapper
public interface NewsItemMapper {

    /** 未配置模型时，将本批仍在加工中的资讯直接转入待审。 */
    int markPendingWithoutProcessing(@Param("ids") List<Long> ids);

    /**
     * 采集入库一条资讯，主键回填到 {@code record.id}。
     * AI 结果写为空对象，未生成和已修改的字段标识为空数组，审核字段为空；status、stats_json 为空时取库表默认值。
     *
     * @param record 资讯
     * @return 影响行数
     */
    int insert(NewsItemPO record);

    /**
     * @param id 主键
     * @return 资讯；不存在时为 null
     */
    NewsItemPO selectById(@Param("id") Long id);

    /**
     * 按主键整体覆盖可变字段：origin_title、author、author_avatar_url、publish_time、cover_url、body_html、
     * playback_json、stats_json、duration_seconds、status、ai_values_json、ai_failed_keys_json、edited_keys_json、
     * reject_reason_id、reject_note、reviewer、review_time。订阅源、渠道、原文链接、内容类型不可修改。
     * 调用方先查出整行再修改。
     *
     * @param record 资讯
     * @return 影响行数
     */
    int updateById(NewsItemPO record);

    /**
     * @param canonicalUrl 规范化后的原文链接
     * @return 资讯；不存在时为 null
     */
    NewsItemPO selectByCanonicalUrl(@Param("canonicalUrl") String canonicalUrl);

    /**
     * 查出资讯并加行锁，在事务内使用，保证审核动作基于最新状态。
     *
     * @param id 主键
     * @return 资讯；不存在时为 null
     */
    NewsItemPO selectByIdForUpdate(@Param("id") Long id);

    /**
     * 按审核时间读取尚未推送的已通过资讯，并锁定这些资讯直到本批推送事务结束。
     *
     * @param status 审核通过状态
     * @param limit 本批最多读取的条数
     * @return 待推送的资讯，包含主键、原标题、作者和原文链接
     */
    List<NewsItemPO> selectUnsentDigestItemsForUpdate(@Param("status") NewsItemStatus status, @Param("limit") int limit);

    /**
     * 记录已推送到个人日报的资讯。
     *
     * @param ids 飞书已确认接收的资讯主键
     */
    void markDigestSent(@Param("ids") List<Long> ids);

    /**
     * 按条件查询资讯列表，审核队列和资讯库共用，按发布时间排序。
     *
     * @param query 查询条件，为空的条件表示不限
     * @return 符合条件的资讯
     */
    List<NewsItemPO> selectByQuery(@Param("query") NewsItemQueryBO query);

    /**
     * @param status 资讯状态
     * @return 该状态的资讯条数
     */
    long countByStatus(@Param("status") NewsItemStatus status);

    /**
     * 按类型、渠道、订阅源分组统计某状态的资讯条数。
     *
     * @param status 资讯状态
     * @return 各分组的条数
     */
    List<NewsItemGroupCountBO> selectGroupCountByStatus(@Param("status") NewsItemStatus status);
}
