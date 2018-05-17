/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.service.telemetry.sub;

public enum SubscriptionErrorCode {

    NO_ERROR(0), INTERNAL_ERROR(1, "Internal Server error!"), BAD_REQUEST(2, "Bad request"), UNAUTHORIZED(3, "Unauthorized");

    private final int code;
    private final String defaultMsg;

    private SubscriptionErrorCode(int code) {
        this(code, null);
    }

    private SubscriptionErrorCode(int code, String defaultMsg) {
        this.code = code;
        this.defaultMsg = defaultMsg;
    }

    public static SubscriptionErrorCode forCode(int code) {
        for (SubscriptionErrorCode errorCode : SubscriptionErrorCode.values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        throw new IllegalArgumentException("Invalid error code: " + code);
    }

    public int getCode() {
        return code;
    }

    public String getDefaultMsg() {
        return defaultMsg;
    }
}
