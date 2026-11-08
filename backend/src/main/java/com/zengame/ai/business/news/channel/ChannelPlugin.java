package com.zengame.ai.business.news.channel;

import com.zengame.ai.business.news.channel.bo.ChannelCredential;
import com.zengame.ai.business.news.channel.bo.ChannelDescriptor;
import com.zengame.ai.business.news.channel.bo.FeedCursor;
import com.zengame.ai.business.news.channel.bo.FetchResult;
import com.zengame.ai.business.news.channel.bo.SourceCandidate;
import com.zengame.ai.business.news.channel.bo.SourcePreview;

import java.util.List;

/**
 * 渠道插件：每个渠道一个 Spring Bean，只负责搜索来源、拉取内容、拼装正文，输出统一条目。
 *
 * <p>插件之间互不引用，核心流程只通过本接口和能力接口（{@link QrLoginCapable}、{@link TokenCredential}、
 * {@link CredentialRefreshable}）调用插件。凭证方式为 QR 的插件必须实现 {@link QrLoginCapable}，
 * 为 TOKEN 的必须实现 {@link TokenCredential}，由 {@link ChannelPluginRegistry} 在启动时校验。
 *
 * <p>凭证不可用（未登录、登录失效、平台返回 401 / 403 等）时，各方法统一抛 {@link ChannelLoginExpiredException}；
 * 其他失败直接抛出异常，带上可读的原因。
 */
public interface ChannelPlugin {

    /**
     * @return 渠道描述：标识、名称、域名、默认内容类型、凭证方式、是否支持搜索、内嵌播放器域名
     */
    ChannelDescriptor descriptor();

    /**
     * 按关键词搜索来源。不支持搜索的渠道（{@code descriptor().isSearchable()} 为 false）忽略关键词，
     * 返回唯一的固定来源。
     *
     * @param keyword    运营输入的关键词，如公众号名称、B站昵称或 UID、X 的 @用户名
     * @param credential 渠道凭证；无凭证渠道为 {@link ChannelCredential#none()}
     * @return 搜索结果，搜不到时为空列表
     * @throws ChannelLoginExpiredException 渠道凭证不可用
     */
    List<SourceCandidate> search(String keyword, ChannelCredential credential);

    /**
     * 预览一个来源：来源信息和最近更新。
     *
     * @param sourceKey  插件内的来源标识，取自 {@link SourceCandidate#getSourceKey()}
     * @param credential 渠道凭证；无凭证渠道为 {@link ChannelCredential#none()}
     * @return 来源卡片
     * @throws ChannelLoginExpiredException 渠道凭证不可用
     */
    SourcePreview preview(String sourceKey, ChannelCredential credential);

    /**
     * 拉取订阅源最新一页，只返回订阅源出现时间晚于 {@link FeedCursor#getSince()} 的内容。
     *
     * @param cursor     来源和拉取进度
     * @param credential 渠道凭证；无凭证渠道为 {@link ChannelCredential#none()}
     * @return 新内容的统一条目
     * @throws ChannelLoginExpiredException 渠道凭证不可用，核心流程据此把渠道凭证置为已失效并中止该渠道
     */
    FetchResult fetch(FeedCursor cursor, ChannelCredential credential);

}
