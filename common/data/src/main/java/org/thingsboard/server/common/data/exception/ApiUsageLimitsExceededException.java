package org.thingsboard.server.common.data.exception;

public class ApiUsageLimitsExceededException extends RuntimeException {
    public ApiUsageLimitsExceededException(String message) {
        super(message);
    }

    public ApiUsageLimitsExceededException() {
    }
}
