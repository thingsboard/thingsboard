/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import com.fasterxml.jackson.annotation.JsonValue;

public enum ThingsboardErrorCode {

    GENERAL(2),
    AUTHENTICATION(10),
    JWT_TOKEN_EXPIRED(11),
    PERMISSION_DENIED(20),
    INVALID_ARGUMENTS(30),
    BAD_REQUEST_PARAMS(31),
    ITEM_NOT_FOUND(32);

    private int errorCode;

    ThingsboardErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    @JsonValue
    public int getErrorCode() {
        return errorCode;
    }

}
