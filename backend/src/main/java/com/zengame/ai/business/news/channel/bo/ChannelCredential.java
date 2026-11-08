package com.zengame.ai.business.news.channel.bo;

import com.zengame.ai.infrastructure.util.JacksonUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 渠道凭证：包装 news_channel.credential_json 里不透明的 JSON。
 *
 * <p>核心层只存取不解析；每个插件定义自己的凭证 BO，用 {@link #from(Object)} 写入、{@link #read(Class)} 读出。
 * 无凭证的渠道传 {@link #none()}。
 */
public final class ChannelCredential {

    private static final ChannelCredential NONE = new ChannelCredential(null);

    private final String json;

    private ChannelCredential(String json) {
        this.json = json;
    }

    /**
     * 包装渠道表里存的凭证 JSON。
     *
     * @param json 凭证 JSON
     * @return 渠道凭证
     * @throws IllegalArgumentException JSON 为空
     */
    public static ChannelCredential of(String json) {
        if (StringUtils.isBlank(json)) {
            throw new IllegalArgumentException("渠道凭证 JSON 不能为空");
        }
        return new ChannelCredential(json);
    }

    /**
     * 把插件自己的凭证 BO 序列化为渠道凭证。
     *
     * @param credentialBo 插件凭证 BO
     * @return 渠道凭证
     */
    public static ChannelCredential from(Object credentialBo) {
        return of(JacksonUtil.writeValueAsString(Objects.requireNonNull(credentialBo, "插件凭证不能为空")));
    }

    /**
     * @return 无凭证，用于凭证方式为 NONE 的渠道
     */
    public static ChannelCredential none() {
        return NONE;
    }

    /**
     * @return 是否带有凭证 JSON
     */
    public boolean isPresent() {
        return json != null;
    }

    /**
     * @return 凭证 JSON，用于写回渠道表
     * @throws IllegalStateException 无凭证
     */
    public String getJson() {
        if (json == null) {
            throw new IllegalStateException("渠道没有凭证");
        }
        return json;
    }

    /**
     * 把凭证 JSON 反序列化为插件自己的凭证 BO。
     *
     * @param type 插件凭证 BO 类型
     * @param <T>  插件凭证 BO 类型
     * @return 插件凭证 BO
     * @throws IllegalStateException 无凭证
     */
    public <T> T read(Class<T> type) {
        return JacksonUtil.readValue(getJson(), type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChannelCredential)) {
            return false;
        }
        return Objects.equals(json, ((ChannelCredential) o).json);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(json);
    }

    /**
     * 不输出凭证内容，避免日志泄露登录态。
     */
    @Override
    public String toString() {
        return isPresent() ? "ChannelCredential[present]" : "ChannelCredential[none]";
    }
}
