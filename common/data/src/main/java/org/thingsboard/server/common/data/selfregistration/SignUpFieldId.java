// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.selfregistration;

import lombok.Getter;

public enum SignUpFieldId {

    EMAIL(true),
    PASSWORD(true),
    REPEAT_PASSWORD(false),
    FIRST_NAME(true),
    LAST_NAME(true),
    PHONE(true),
    COUNTRY(true),
    CITY(true),
    STATE(true),
    ZIP(true),
    ADDRESS(true),
    ADDRESS2(true);

    @Getter
    private final boolean validate;

    SignUpFieldId(boolean needCheck) {
        this.validate = needCheck;
    }
}
