package com.iot.thingmodel.auth;

import com.iot.thingmodel.common.Constants;

public record CurrentUser(String userId, String tenantId, String authority, String email) {

    public boolean isSystemAdmin() {
        return Constants.Auth.ROLE_SYS_ADMIN.equalsIgnoreCase(authority);
    }
}
