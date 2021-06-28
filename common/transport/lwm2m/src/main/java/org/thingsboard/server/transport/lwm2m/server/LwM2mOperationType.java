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

    READ(0, "Read", 1),
    READ_COMPOSITE(1, "ReadComposite", 2),
    DISCOVER(2, "Discover", 1),
    DISCOVER_ALL(3, "DiscoverAll", 0),
    OBSERVE_READ_ALL(4, "ObserveReadAll", 0),

    OBSERVE(5, "Observe", 1),
    OBSERVE_COMPOSITE(6, "ObserveComposite", 2),
    OBSERVE_CANCEL(7, "ObserveCancel", 1),
    OBSERVE_COMPOSITE_CANCEL(8, "ObserveCompositeCancel", 2),
    OBSERVE_CANCEL_ALL(9, "ObserveCancelAll", 0),
    EXECUTE(10, "Execute", 1),
    /**
     * Replaces the Object Instance or the Resource(s) with the new value provided in the “Write” operation. (see
     * section 5.3.3 of the LW M2M spec).
     * if all resources are to be replaced
     */
    WRITE_REPLACE(11, "WriteReplace", 1),

    /**
     * Adds or updates Resources provided in the new value and leaves other existing Resources unchanged. (see section
     * 5.3.3 of the LW M2M spec).
     * if this is a partial update request
     */
    WRITE_UPDATE(12, "WriteUpdate", 1),
    WRITE_COMPOSITE(14, "WriteComposite", 2),
    WRITE_ATTRIBUTES(15, "WriteAttributes", 1),
    DELETE(16, "Delete", 1),

    // only for RPC
    FW_UPDATE(17, "FirmwareUpdate", 0);

//        FW_READ_INFO(18, "FirmwareReadInfo"),
//        SW_READ_INFO(19, "SoftwareReadInfo"),
//        SW_UPDATE(20, "SoftwareUpdate"),
//        SW_UNINSTALL(21, "SoftwareUninstall");

    @Getter
    private final int code;
    @Getter
    private final String type;
    @Getter
    private final int hasObjectIdOrComposite;

    LwM2mOperationType(int code, String type, int hasObjectIdOrComposite) {
        this.code = code;
        this.type = type;
        this.hasObjectIdOrComposite = hasObjectIdOrComposite;
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
