package com.zengame.ai.infrastructure.enums;

/**
 * 带稳定编码的枚举：编码用于落库、接口协议和外部交互，上线后不改写。
 */
public interface CodeEnum {

    /**
     * @return 落库和接口协议使用的枚举编码
     */
    String getCode();
}
