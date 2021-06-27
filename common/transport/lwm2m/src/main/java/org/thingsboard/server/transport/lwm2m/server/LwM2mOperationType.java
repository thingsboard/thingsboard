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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.Getter;

/**
 * Define the behavior of a write request.
 */
public enum LwM2mOperationType {

    READ(0, "Read", true),
    DISCOVER(1, "Discover", true),
    DISCOVER_ALL(2, "DiscoverAll", false),
    OBSERVE_READ_ALL(3, "ObserveReadAll", false),

    OBSERVE(4, "Observe", true),
    OBSERVE_CANCEL(5, "ObserveCancel", true),
    OBSERVE_CANCEL_ALL(6, "ObserveCancelAll", false),
    EXECUTE(7, "Execute", true),
    /**
     * Replaces the Object Instance or the Resource(s) with the new value provided in the “Write” operation. (see
     * section 5.3.3 of the LW M2M spec).
     * if all resources are to be replaced
     */
    WRITE_REPLACE(8, "WriteReplace", true),

    /**
     * Adds or updates Resources provided in the new value and leaves other existing Resources unchanged. (see section
     * 5.3.3 of the LW M2M spec).
     * if this is a partial update request
     */
    WRITE_UPDATE(9, "WriteUpdate", true),
    WRITE_ATTRIBUTES(10, "WriteAttributes", true),
    DELETE(11, "Delete", true),

    // only for RPC
    FW_UPDATE(12, "FirmwareUpdate", false);

//        FW_READ_INFO(12, "FirmwareReadInfo"),
//        SW_READ_INFO(15, "SoftwareReadInfo"),
//        SW_UPDATE(16, "SoftwareUpdate"),
//        SW_UNINSTALL(18, "SoftwareUninstall");

    @Getter
    private final int code;
    @Getter
    private final String type;
    @Getter
    private final boolean hasObjectId;

    LwM2mOperationType(int code, String type, boolean hasObjectId) {
        this.code = code;
        this.type = type;
        this.hasObjectId = hasObjectId;
    }

    public static LwM2mOperationType fromType(String type) {
        for (LwM2mOperationType to : LwM2mOperationType.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        return null;
    }
}
