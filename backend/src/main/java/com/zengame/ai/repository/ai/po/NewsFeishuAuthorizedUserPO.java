package com.zengame.ai.repository.ai.po;

import lombok.Data;

/** 飞书应用下已授权的人员资料。 */
@Data
public class NewsFeishuAuthorizedUserPO {
    private String appId;
    private String openId;
    private String name;
    private String avatarUrl;
}
