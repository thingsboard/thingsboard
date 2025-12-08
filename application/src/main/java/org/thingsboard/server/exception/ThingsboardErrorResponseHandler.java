/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.msg.tools.MaxPayloadSizeExceededException;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.dao.exception.EntitiesLimitException;
import org.thingsboard.server.service.security.exception.AuthMethodNotSupportedException;
import org.thingsboard.server.service.security.exception.JwtExpiredTokenException;
import org.thingsboard.server.service.security.exception.UserPasswordExpiredException;
import org.thingsboard.server.service.security.exception.UserPasswordNotValidException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RestControllerAdvice
public class ThingsboardErrorResponseHandler extends ResponseEntityExceptionHandler implements AccessDeniedHandler, ErrorController {

    private static final Map<HttpStatus, ThingsboardErrorCode> statusToErrorCodeMap = new HashMap<>();

    static {
        statusToErrorCodeMap.put(HttpStatus.BAD_REQUEST, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        statusToErrorCodeMap.put(HttpStatus.UNAUTHORIZED, ThingsboardErrorCode.AUTHENTICATION);
        statusToErrorCodeMap.put(HttpStatus.FORBIDDEN, ThingsboardErrorCode.PERMISSION_DENIED);
        statusToErrorCodeMap.put(HttpStatus.NOT_FOUND, ThingsboardErrorCode.ITEM_NOT_FOUND);
        statusToErrorCodeMap.put(HttpStatus.METHOD_NOT_ALLOWED, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        statusToErrorCodeMap.put(HttpStatus.NOT_ACCEPTABLE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        statusToErrorCodeMap.put(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        statusToErrorCodeMap.put(HttpStatus.TOO_MANY_REQUESTS, ThingsboardErrorCode.TOO_MANY_REQUESTS);
        statusToErrorCodeMap.put(HttpStatus.INTERNAL_SERVER_ERROR, ThingsboardErrorCode.GENERAL);
        statusToErrorCodeMap.put(HttpStatus.SERVICE_UNAVAILABLE, ThingsboardErrorCode.GENERAL);
    }

    private static final Map<ThingsboardErrorCode, HttpStatus> errorCodeToStatusMap = new HashMap<>();

    static {
        errorCodeToStatusMap.put(ThingsboardErrorCode.GENERAL, HttpStatus.INTERNAL_SERVER_ERROR);
        errorCodeToStatusMap.put(ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED);
        errorCodeToStatusMap.put(ThingsboardErrorCode.JWT_TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED);
        errorCodeToStatusMap.put(ThingsboardErrorCode.CREDENTIALS_EXPIRED, HttpStatus.UNAUTHORIZED);
        errorCodeToStatusMap.put(ThingsboardErrorCode.PERMISSION_DENIED, HttpStatus.FORBIDDEN);
        errorCodeToStatusMap.put(ThingsboardErrorCode.INVALID_ARGUMENTS, HttpStatus.BAD_REQUEST);
        errorCodeToStatusMap.put(ThingsboardErrorCode.BAD_REQUEST_PARAMS, HttpStatus.BAD_REQUEST);
        errorCodeToStatusMap.put(ThingsboardErrorCode.ITEM_NOT_FOUND, HttpStatus.NOT_FOUND);
        errorCodeToStatusMap.put(ThingsboardErrorCode.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS);
        errorCodeToStatusMap.put(ThingsboardErrorCode.TOO_MANY_UPDATES, HttpStatus.TOO_MANY_REQUESTS);
        errorCodeToStatusMap.put(ThingsboardErrorCode.SUBSCRIPTION_VIOLATION, HttpStatus.FORBIDDEN);
        errorCodeToStatusMap.put(ThingsboardErrorCode.ENTITIES_LIMIT_EXCEEDED, HttpStatus.FORBIDDEN);
        errorCodeToStatusMap.put(ThingsboardErrorCode.VERSION_CONFLICT, HttpStatus.CONFLICT);
    }

    private static ThingsboardErrorCode statusToErrorCode(HttpStatus status) {
        return statusToErrorCodeMap.getOrDefault(status, ThingsboardErrorCode.GENERAL);
    }

    private static HttpStatus errorCodeToStatus(ThingsboardErrorCode errorCode) {
        return errorCodeToStatusMap.getOrDefault(errorCode, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @RequestMapping("/error")
    public ResponseEntity<Object> handleError(HttpServletRequest request) {
        HttpStatus httpStatus = Optional.ofNullable(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE))
                .map(status -> HttpStatus.resolve(Integer.parseInt(status.toString())))
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
        String errorMessage = Optional.ofNullable(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION))
                .map(e -> (ExceptionUtils.getMessage((Throwable) e)))
                .orElse(httpStatus.getReasonPhrase());
        return new ResponseEntity<>(ThingsboardErrorResponse.of(errorMessage, statusToErrorCode(httpStatus), httpStatus), httpStatus);
    }

    @Override
    @ExceptionHandler(AccessDeniedException.class)
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException,
            ServletException {
        if (!response.isCommitted()) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            JacksonUtil.writeValue(response.getWriter(),
                    ThingsboardErrorResponse.of("You don't have permission to perform this operation!",
                            ThingsboardErrorCode.PERMISSION_DENIED, HttpStatus.FORBIDDEN));
        }
    }

    @ExceptionHandler(Exception.class)
    public void handle(Exception exception, HttpServletResponse response) {
        log.debug("Processing exception {}", exception.getMessage(), exception);
        if (!response.isCommitted()) {
            try {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                if (exception instanceof ThingsboardException thingsboardException) {
                    if (thingsboardException.getErrorCode() == ThingsboardErrorCode.SUBSCRIPTION_VIOLATION) {
                        handleSubscriptionException(thingsboardException, response);
                    } else if (thingsboardException.getErrorCode() == ThingsboardErrorCode.DATABASE) {
                        handleDatabaseException(thingsboardException.getCause(), response);
                    } else if (thingsboardException.getErrorCode() == ThingsboardErrorCode.ENTITIES_LIMIT_EXCEEDED) {
                        if (thingsboardException.getCause() instanceof EntitiesLimitException entitiesLimitException) {
                            handleEntitiesLimitException(entitiesLimitException, response);
                        } else {
                            handleEntitiesLimitException(thingsboardException, response);
                        }
                    } else {
                        handleThingsboardException(thingsboardException, response);
                    }
                } else if (exception instanceof TbRateLimitsException rateLimitsException) {
                    handleRateLimitException(response, rateLimitsException);
                } else if (exception instanceof AccessDeniedException) {
                    handleAccessDeniedException(response);
                } else if (exception instanceof AuthenticationException authenticationException) {
                    handleAuthenticationException(authenticationException, response);
                } else if (exception instanceof MaxPayloadSizeExceededException maxPayloadSizeExceededException) {
                    handleMaxPayloadSizeExceededException(response, maxPayloadSizeExceededException);
                } else if (exception instanceof DataAccessException e) {
                    handleDatabaseException(e, response);
                } else {
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of(exception.getMessage(),
                            ThingsboardErrorCode.GENERAL, HttpStatus.INTERNAL_SERVER_ERROR));
                }
            } catch (IOException e) {
                log.error("Can't handle exception", e);
            }
        }
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body,
            HttpHeaders headers, HttpStatusCode statusCode,
            WebRequest request) {
        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(statusCode)) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
        }
        ThingsboardErrorCode errorCode = statusToErrorCode((HttpStatus) statusCode);
        return new ResponseEntity<>(ThingsboardErrorResponse.of(ex.getMessage(), errorCode, (HttpStatus) statusCode), headers, statusCode);
    }

    private void handleThingsboardException(ThingsboardException thingsboardException, HttpServletResponse response) throws IOException {
        ThingsboardErrorCode errorCode = thingsboardException.getErrorCode();
        HttpStatus status = errorCodeToStatus(errorCode);
        response.setStatus(status.value());
        JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of(thingsboardException.getMessage(), errorCode, status));
    }

    private void handleRateLimitException(HttpServletResponse response, TbRateLimitsException exception) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        String message = "Too many requests for current " + exception.getEntityType().name().toLowerCase() + "!";
        JacksonUtil.writeValue(response.getWriter(),
                ThingsboardErrorResponse.of(message,
                        ThingsboardErrorCode.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS));
    }

    private void handleMaxPayloadSizeExceededException(HttpServletResponse response, MaxPayloadSizeExceededException exception) throws IOException {
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        JacksonUtil.writeValue(response.getWriter(),
                ThingsboardErrorResponse.of(exception.getMessage(),
                        ThingsboardErrorCode.BAD_REQUEST_PARAMS, HttpStatus.PAYLOAD_TOO_LARGE));
    }

    private void handleSubscriptionException(ThingsboardException subscriptionException, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        JacksonUtil.writeValue(response.getWriter(),
                JacksonUtil.fromBytes(((HttpClientErrorException) subscriptionException.getCause()).getResponseBodyAsByteArray(), Object.class));
    }

    private void handleDatabaseException(Throwable databaseException, HttpServletResponse response) throws IOException {
        ThingsboardErrorResponse errorResponse;
        if (databaseException instanceof ConstraintViolationException) {
            errorResponse = ThingsboardErrorResponse.of(ExceptionUtils.getRootCause(databaseException).getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS, HttpStatus.BAD_REQUEST);
        } else {
            log.warn("Database error: {} - {}", databaseException.getClass().getSimpleName(), ExceptionUtils.getRootCauseMessage(databaseException));
            errorResponse = ThingsboardErrorResponse.of("Database error", ThingsboardErrorCode.DATABASE, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        writeResponse(errorResponse, response);
    }

    private void handleEntitiesLimitException(ThingsboardException entitiesLimitException, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        JacksonUtil.writeValue(response.getWriter(),
                JacksonUtil.fromBytes(((HttpClientErrorException) entitiesLimitException.getCause()).getResponseBodyAsByteArray(), Object.class));
    }

    private void handleEntitiesLimitException(EntitiesLimitException entitiesLimitException, HttpServletResponse response) throws IOException {
        EntityType entityType = entitiesLimitException.getEntityType();
        Long limit = entitiesLimitException.getLimit();
        HttpStatus status = HttpStatus.FORBIDDEN;
        response.setStatus(status.value());
        JacksonUtil.writeValue(response.getWriter(),
                ThingsboardErrorResponse.ofEntityLimitExceeded(entitiesLimitException.getMessage(), entityType, limit, status));
    }

    private void handleAccessDeniedException(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        JacksonUtil.writeValue(response.getWriter(),
                ThingsboardErrorResponse.of("You don't have permission to perform this operation!",
                        ThingsboardErrorCode.PERMISSION_DENIED, HttpStatus.FORBIDDEN));

    }

    private void handleAuthenticationException(AuthenticationException authenticationException, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        if (authenticationException instanceof BadCredentialsException || authenticationException instanceof UsernameNotFoundException) {
            JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of("Invalid username or password", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        } else if (authenticationException instanceof DisabledException) {
            JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of("User account is not active", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        } else if (authenticationException instanceof LockedException) {
            JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of("User account is locked due to security policy", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        } else if (authenticationException instanceof JwtExpiredTokenException) {
            JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of("Token has expired", ThingsboardErrorCode.JWT_TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED));
        } else if (authenticationException instanceof AuthMethodNotSupportedException) {
            JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of(authenticationException.getMessage(), ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        } else if (authenticationException instanceof UserPasswordExpiredException expiredException) {
            String resetToken = expiredException.getResetToken();
            JacksonUtil.writeValue(response.getWriter(), ThingsboardCredentialsExpiredResponse.of(expiredException.getMessage(), resetToken));
        } else if (authenticationException instanceof UserPasswordNotValidException expiredException) {
            JacksonUtil.writeValue(response.getWriter(), ThingsboardCredentialsViolationResponse.of(expiredException.getMessage()));
        } else if (authenticationException instanceof CredentialsExpiredException credentialsExpiredException) {
            JacksonUtil.writeValue(response.getWriter(), ThingsboardCredentialsViolationResponse.of(credentialsExpiredException.getMessage(), ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        } else {
            JacksonUtil.writeValue(response.getWriter(), ThingsboardErrorResponse.of("Authentication failed", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        }
    }

    // TODO: refactor this class to use this method instead of boilerplate JacksonUtil.writeValue(response.getWriter(), ...
    private void writeResponse(ThingsboardErrorResponse errorResponse, HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(errorResponse.getStatus());
        JacksonUtil.writeValue(response.getWriter(), errorResponse);
    }

}
