package com.zengame.ai.infrastructure.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/** 将资讯 API 的业务失败和请求校验错误转换为统一响应。 */
@Slf4j
@RestControllerAdvice
public class WebExceptionAdapter {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({BindException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "请求参数不正确");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("资讯请求失败, method={}, path={}", request.getMethod(), request.getRequestURI(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "服务处理失败");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("code", status.value(), "msg", message, "errors", List.of()));
    }
}
