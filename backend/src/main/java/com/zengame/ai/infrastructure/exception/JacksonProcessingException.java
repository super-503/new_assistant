package com.zengame.ai.infrastructure.exception;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author john
 * @since 2022/10/20
 */
public class JacksonProcessingException extends RuntimeException {

    public JacksonProcessingException(String messageForDeveloper, JsonProcessingException cause) {
        super(messageForDeveloper, cause);
    }

}
