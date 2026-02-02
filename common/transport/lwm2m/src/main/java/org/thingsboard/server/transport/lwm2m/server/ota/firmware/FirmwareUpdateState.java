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
