package com.iot.thingmodel.common;

import java.util.List;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<Issue> issues;
    private final String retry;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, List.of(), null);
    }

    public ApiException(HttpStatus status, String code, String message, List<Issue> issues) {
        this(status, code, message, issues, null);
    }

    public ApiException(HttpStatus status, String code, String message, List<Issue> issues, String retry) {
        super(message);
        this.status = status;
        this.code = code;
        this.issues = issues == null ? List.of() : List.copyOf(issues);
        this.retry = retry;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public String getRetry() {
        return retry;
    }
}
