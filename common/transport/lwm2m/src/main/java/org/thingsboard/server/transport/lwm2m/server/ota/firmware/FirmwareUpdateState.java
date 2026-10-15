// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.ota.firmware;

import lombok.Getter;

/**
 * /** State R
 * 0: Idle (before downloading or after successful updating)
 * 1: Downloading (The data sequence is on the way)
 * 2: Downloaded
 * 3: Updating
 */
public enum FirmwareUpdateState {
    IDLE(0, "Idle"),
    DOWNLOADING(1, "Downloading"),
    DOWNLOADED(2, "Downloaded"),
    UPDATING(3, "Updating");

    @Getter
    private int code;
    @Getter
    private String type;

    FirmwareUpdateState(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public static FirmwareUpdateState fromStateFwByType(String type) {
        for (FirmwareUpdateState to : FirmwareUpdateState.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW State type  : %s", type));
    }

    public static FirmwareUpdateState fromStateFwByCode(int code) {
        for (FirmwareUpdateState to : FirmwareUpdateState.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW State code : %s", code));
    }
}
