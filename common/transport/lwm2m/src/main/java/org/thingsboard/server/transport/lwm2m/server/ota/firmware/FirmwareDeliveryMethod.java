// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
