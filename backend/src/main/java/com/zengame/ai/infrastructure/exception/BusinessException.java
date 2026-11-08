package com.zengame.ai.infrastructure.exception;

/**
 * @author jurzis.zhang
 * @date 2024/10/14 9:24
 **/
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
