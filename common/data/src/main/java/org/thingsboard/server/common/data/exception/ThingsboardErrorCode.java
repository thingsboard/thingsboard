// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.exception;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ThingsboardErrorCode {

    GENERAL(2),
    AUTHENTICATION(10),
    JWT_TOKEN_EXPIRED(11),
    CREDENTIALS_EXPIRED(15),
    PERMISSION_DENIED(20),
    INVALID_ARGUMENTS(30),
    BAD_REQUEST_PARAMS(31),
    ITEM_NOT_FOUND(32),
    TOO_MANY_REQUESTS(33),
    TOO_MANY_UPDATES(34),
    VERSION_CONFLICT(35),
    SUBSCRIPTION_VIOLATION(40),
    PASSWORD_VIOLATION(45),
    DATABASE(46);

    private int errorCode;

    ThingsboardErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    @JsonValue
    public int getErrorCode() {
        return errorCode;
    }

}
