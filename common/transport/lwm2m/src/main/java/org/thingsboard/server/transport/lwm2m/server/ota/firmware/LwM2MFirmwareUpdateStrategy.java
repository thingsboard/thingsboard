// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.ota.firmware;

public enum LwM2MFirmwareUpdateStrategy {
    OBJ_5_BINARY(1, "ObjectId 5, Binary"),
    OBJ_5_TEMP_URL(2, "ObjectId 5, URI"),
    OBJ_19_BINARY(3, "ObjectId 19, Binary");

    public int code;
    public String type;

    LwM2MFirmwareUpdateStrategy(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public static LwM2MFirmwareUpdateStrategy fromStrategyFwByType(String type) {
        for (LwM2MFirmwareUpdateStrategy to : LwM2MFirmwareUpdateStrategy.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW State type  : %s", type));
    }

    public static LwM2MFirmwareUpdateStrategy fromStrategyFwByCode(int code) {
        for (LwM2MFirmwareUpdateStrategy to : LwM2MFirmwareUpdateStrategy.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW Strategy code : %s", code));
    }
}
