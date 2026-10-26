package com.iot.thingmodel.controller;

import com.iot.thingmodel.auth.AccessGuard;
import com.iot.thingmodel.auth.AuthInterceptor;
import com.iot.thingmodel.auth.CurrentUser;
import com.iot.thingmodel.common.ApiException;
import com.iot.thingmodel.common.Constants;
import com.iot.thingmodel.product.TenantScope;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public abstract class BaseController {

    protected final AccessGuard accessGuard;

    protected BaseController(AccessGuard accessGuard) {
        this.accessGuard = accessGuard;
    }

    protected CurrentUser getCurrentUser() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            Object value = attributes.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            if (value instanceof CurrentUser currentUser) {
                return currentUser;
            }
        }
        throw new ApiException(
                HttpStatus.UNAUTHORIZED,
                Constants.ErrorCode.UNAUTHORIZED,
                "You aren't authorized to perform this operation!");
    }

    protected UUID getTenantId() {
        String tenantId = getCurrentUser().tenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        return parseUuid(tenantId, Constants.ErrorCode.TENANT_ID_INVALID);
    }

    protected UUID requireTenantId() {
        UUID tenantId = getTenantId();
        if (tenantId == null) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, Constants.ErrorCode.TENANT_REQUIRED, "当前用户不属于任何厂商租户");
        }
        return tenantId;
    }

    protected TenantScope tenantScope() {
        return new TenantScope(getTenantId(), getCurrentUser().isSystemAdmin());
    }

    protected UUID parseTenantId(String tenantId) {
        return parseUuid(tenantId, Constants.ErrorCode.TENANT_ID_INVALID);
    }

    protected UUID parseUuid(String value, String code) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, code, "UUID 不能为空");
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, code, "UUID 格式不合法");
        }
    }

    protected <T> T checkNotNull(T reference, String message) {
        if (reference == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND, Constants.ErrorCode.ITEM_NOT_FOUND, message);
        }
        return reference;
    }
}
