// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security;

public enum Authority {

    SYS_ADMIN(0),
    TENANT_ADMIN(1),
    CUSTOMER_USER(2),
    REFRESH_TOKEN(10),
    PRE_VERIFICATION_TOKEN(11);

    private int code;

    Authority(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static Authority parse(String value) {
        Authority authority = null;
        if (value != null && value.length() != 0) {
            for (Authority current : Authority.values()) {
                if (current.name().equalsIgnoreCase(value)) {
                    authority = current;
                    break;
                }
            }
        }
        return authority;
    }
}
