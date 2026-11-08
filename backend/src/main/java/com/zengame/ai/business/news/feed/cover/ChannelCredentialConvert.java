package com.zengame.ai.business.news.feed.cover;

import com.zengame.ai.business.news.channel.bo.CredentialVerifyResult;
import com.zengame.ai.business.news.channel.bo.QrLoginStart;
import com.zengame.ai.business.news.domain.enums.NewsQrLoginStatus;
import com.zengame.ai.business.news.feed.domain.vo.CredentialVerifyVO;
import com.zengame.ai.business.news.feed.domain.vo.QrLoginPollVO;
import com.zengame.ai.business.news.feed.domain.vo.QrLoginStartVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 渠道凭证维护结果的转换：扫码发起、扫码轮询、验证连接。
 */
@Mapper
public interface ChannelCredentialConvert {

    ChannelCredentialConvert INSTANCE = Mappers.getMapper(ChannelCredentialConvert.class);

    /**
     * @param sessionId 扫码会话标识
     * @param start     插件发起扫码的结果
     * @return 扫码弹窗数据
     */
    @Mapping(target = "sessionId", source = "sessionId")
    @Mapping(target = "qrImage", source = "start.qrImage")
    @Mapping(target = "expireSeconds", source = "start.expireSeconds")
    QrLoginStartVO toStartVO(String sessionId, QrLoginStart start);

    /**
     * @param status  扫码状态
     * @param account CONFIRMED 时的登录账号；其余为 null
     * @return 轮询结果
     */
    @Mapping(target = "status", source = "status")
    @Mapping(target = "statusDesc", source = "status.desc")
    @Mapping(target = "account", source = "account")
    QrLoginPollVO toPollVO(NewsQrLoginStatus status, String account);

    /**
     * @param result 插件的凭证校验结果
     * @return 验证连接结果
     */
    CredentialVerifyVO toVerifyVO(CredentialVerifyResult result);
}
