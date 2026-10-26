package com.iot.thingmodel.auth;

import com.iot.thingmodel.common.ApiException;
import com.iot.thingmodel.common.Constants;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AccessGuard {

    public void checkSystemAdmin(CurrentUser user) {
        if (!user.isSystemAdmin()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, Constants.ErrorCode.FORBIDDEN, "仅平台超级管理员可执行此操作");
        }
    }

    public void checkTenantAccess(CurrentUser user, UUID tenantId) {
        if (user.isSystemAdmin()) {
            return;
        }
        if (!userTenant(user).equals(tenantId)) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND, Constants.ErrorCode.PRODUCT_NOT_FOUND, "产品不存在");
        }
    }

    public UUID resolveWriteTenant(CurrentUser user, String requestedTenant) {
        if (user.isSystemAdmin()) {
            if (requestedTenant == null || requestedTenant.isBlank()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        Constants.ErrorCode.TENANT_ID_REQUIRED,
                        "超级管理员创建产品时必须指定租户");
            }
            return parseUuid(requestedTenant, Constants.ErrorCode.TENANT_ID_INVALID);
        }
        UUID tenantId = userTenant(user);
        if (requestedTenant != null && !requestedTenant.isBlank()) {
            UUID requested = parseUuid(requestedTenant, Constants.ErrorCode.TENANT_ID_INVALID);
            if (!tenantId.equals(requested)) {
                throw new ApiException(
                        HttpStatus.FORBIDDEN, Constants.ErrorCode.FORBIDDEN, "不能操作其他厂商租户的产品");
            }
        }
        return tenantId;
    }

    private UUID userTenant(CurrentUser user) {
        if (user.tenantId() == null || user.tenantId().isBlank()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, Constants.ErrorCode.TENANT_REQUIRED, "当前用户不属于任何厂商租户");
        }
        return parseUuid(user.tenantId(), Constants.ErrorCode.TENANT_ID_INVALID);
    }

    private UUID parseUuid(String value, String code) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, code, "UUID 格式不合法");
        }
    }
}
