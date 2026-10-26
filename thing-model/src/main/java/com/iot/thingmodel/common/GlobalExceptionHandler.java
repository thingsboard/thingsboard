package com.iot.thingmodel.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex) {
        ApiError body = new ApiError(ex.getMessage(), ex.getCode(), ex.getIssues(), ex.getRetry());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        var issues = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toIssue)
                .toList();
        ApiError body = new ApiError("请求参数校验失败", Constants.ErrorCode.BAD_REQUEST, issues, null);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        var issues = ex.getConstraintViolations().stream()
                .map(violation -> new Issue(
                        "FIELD_INVALID",
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .toList();
        ApiError body = new ApiError("请求参数校验失败", Constants.ErrorCode.BAD_REQUEST, issues, null);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled request failure", ex);
        ApiError body = new ApiError(
                "服务内部错误", Constants.ErrorCode.INTERNAL_ERROR, null, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Issue toIssue(FieldError error) {
        return new Issue(
                Constants.ErrorCode.FIELD_INVALID, error.getField(), error.getDefaultMessage());
    }
}
