/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

/**
 * /** Delivery Method R
 * 0: Pull only
 * 1: Push only
 * 2: Both:
 * - In this case the LwM2M Server MAY choose the preferred mechanism for conveying the firmware image to the LwM2M Client.
 */
public enum UpdateDeliveryFw {
    PULL(0, "Pull only"),
    PUSH(1, "Push only"),
    BOTH(2, "Push or Push");

    public int code;
    public String type;

    UpdateDeliveryFw(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public static UpdateDeliveryFw fromStateFwByType(String type) {
        for (UpdateDeliveryFw to : UpdateDeliveryFw.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW delivery type  : %s", type));
    }

    public static UpdateDeliveryFw fromStateFwByCode(int code) {
        for (UpdateDeliveryFw to : UpdateDeliveryFw.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW delivery code : %s", code));
    }
}
