/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.server.ota.firmware;

public enum FirmwareDeliveryMethod {
    PULL(0, "Pull only"),
    PUSH(1, "Push only"),
    BOTH(2, "Push or Push");

    public int code;
    public String type;

    FirmwareDeliveryMethod(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public static FirmwareDeliveryMethod fromStateFwByType(String type) {
        for (FirmwareDeliveryMethod to : FirmwareDeliveryMethod.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW delivery type  : %s", type));
    }

    public static FirmwareDeliveryMethod fromStateFwByCode(int code) {
        for (FirmwareDeliveryMethod to : FirmwareDeliveryMethod.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW delivery code : %s", code));
    }
}
