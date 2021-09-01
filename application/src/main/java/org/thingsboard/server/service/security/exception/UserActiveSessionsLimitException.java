package org.thingsboard.server.service.security.exception;

import org.springframework.security.core.AuthenticationException;

public class UserActiveSessionsLimitException extends AuthenticationException {
    public UserActiveSessionsLimitException(String msg) {
        super(msg);
    }
}
