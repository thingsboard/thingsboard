// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.ota.software;

public enum LwM2MSoftwareUpdateStrategy {
    BINARY(1, "ObjectId 9, Binary"),
    TEMP_URL(2, "ObjectId 9, URI");

    public int code;
    public String type;

    LwM2MSoftwareUpdateStrategy(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public static LwM2MSoftwareUpdateStrategy fromStrategySwByType(String type) {
        for (LwM2MSoftwareUpdateStrategy to : LwM2MSoftwareUpdateStrategy.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported SW Strategy type  : %s", type));
    }

    public static LwM2MSoftwareUpdateStrategy fromStrategySwByCode(int code) {
        for (LwM2MSoftwareUpdateStrategy to : LwM2MSoftwareUpdateStrategy.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported SW Strategy code : %s", code));
    }

}
