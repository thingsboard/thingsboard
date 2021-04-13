/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.service.security.exception.AuthMethodNotSupportedException;
import org.thingsboard.server.service.security.exception.JwtExpiredTokenException;
import org.thingsboard.server.service.security.exception.UserPasswordExpiredException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j
public class ThingsboardErrorResponseHandler implements AccessDeniedHandler {

    @Autowired
    private ObjectMapper mapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException,
            ServletException {
        if (!response.isCommitted()) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            mapper.writeValue(response.getWriter(),
                    ThingsboardErrorResponse.of("You don't have permission to perform this operation!",
                            ThingsboardErrorCode.PERMISSION_DENIED, HttpStatus.FORBIDDEN));
        }
    }

    public void handle(Exception exception, HttpServletResponse response) {
        log.debug("Processing exception {}", exception.getMessage(), exception);
        if (!response.isCommitted()) {
            try {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                if (exception instanceof ThingsboardException) {
                    ThingsboardException thingsboardException = (ThingsboardException) exception;
                    if (thingsboardException.getErrorCode() == ThingsboardErrorCode.SUBSCRIPTION_VIOLATION) {
                        handleSubscriptionException((ThingsboardException) exception, response);
                    } else {
                        handleThingsboardException((ThingsboardException) exception, response);
                    }
                } else if (exception instanceof TbRateLimitsException) {
                    handleRateLimitException(response, (TbRateLimitsException) exception);
                } else if (exception instanceof AccessDeniedException) {
                    handleAccessDeniedException(response);
                } else if (exception instanceof AuthenticationException) {
                    handleAuthenticationException((AuthenticationException) exception, response);
                } else {
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    mapper.writeValue(response.getWriter(), ThingsboardErrorResponse.of(exception.getMessage(),
                            ThingsboardErrorCode.GENERAL, HttpStatus.INTERNAL_SERVER_ERROR));
                }
            } catch (IOException e) {
                log.error("Can't handle exception", e);
            }
        }
    }


    private void handleThingsboardException(ThingsboardException thingsboardException, HttpServletResponse response) throws IOException {

        ThingsboardErrorCode errorCode = thingsboardException.getErrorCode();
        HttpStatus status;

        switch (errorCode) {
            case AUTHENTICATION:
                status = HttpStatus.UNAUTHORIZED;
                break;
            case PERMISSION_DENIED:
                status = HttpStatus.FORBIDDEN;
                break;
            case INVALID_ARGUMENTS:
                status = HttpStatus.BAD_REQUEST;
                break;
            case ITEM_NOT_FOUND:
                status = HttpStatus.NOT_FOUND;
                break;
            case BAD_REQUEST_PARAMS:
                status = HttpStatus.BAD_REQUEST;
                break;
            case GENERAL:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                break;
        }

        response.setStatus(status.value());
        mapper.writeValue(response.getWriter(), ThingsboardErrorResponse.of(thingsboardException.getMessage(), errorCode, status));
    }

    private void handleRateLimitException(HttpServletResponse response, TbRateLimitsException exception) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        String message = "Too many requests for current " + exception.getEntityType().name().toLowerCase() + "!";
        mapper.writeValue(response.getWriter(),
                ThingsboardErrorResponse.of(message,
                        ThingsboardErrorCode.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS));
    }

    private void handleSubscriptionException(ThingsboardException subscriptionException, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        mapper.writeValue(response.getWriter(),
                (new ObjectMapper()).readValue(((HttpClientErrorException) subscriptionException.getCause()).getResponseBodyAsByteArray(), Object.class));
    }

    private void handleAccessDeniedException(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        mapper.writeValue(response.getWriter(),
                ThingsboardErrorResponse.of("You don't have permission to perform this operation!",
                        ThingsboardErrorCode.PERMISSION_DENIED, HttpStatus.FORBIDDEN));

    }

    private void handleAuthenticationException(AuthenticationException authenticationException, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        if (authenticationException instanceof BadCredentialsException) {
            mapper.writeValue(response.getWriter(), ThingsboardErrorResponse.of("Invalid username or password", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        } else if (authenticationException instanceof DisabledException) {
            mapper.writeValue(response.getWriter(), ThingsboardErrorResponse.of("User account is not active", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        } else if (authenticationException instanceof LockedException) {
            mapper.writeValue(response.getWriter(), ThingsboardErrorResponse.of("User account is locked due to security policy", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        } else if (authenticationException instanceof JwtExpiredTokenException) {
            mapper.writeValue(response.getWriter(), ThingsboardErrorResponse.of("Token has expired", ThingsboardErrorCode.JWT_TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED));
        } else if (authenticationException instanceof AuthMethodNotSupportedException) {
            mapper.writeValue(response.getWriter(), ThingsboardErrorResponse.of(authenticationException.getMessage(), ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        } else if (authenticationException instanceof UserPasswordExpiredException) {
            UserPasswordExpiredException expiredException = (UserPasswordExpiredException)authenticationException;
            String resetToken = expiredException.getResetToken();
            mapper.writeValue(response.getWriter(), ThingsboardCredentialsExpiredResponse.of(expiredException.getMessage(), resetToken));
        } else {
            mapper.writeValue(response.getWriter(), ThingsboardErrorResponse.of("Authentication failed", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        }
    }

}
