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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.Getter;

/**
 * Define the behavior of a write request.
 */
public enum LwM2MOperationType {

    READ(0, "Read", true),
    READ_COMPOSITE(1, "ReadComposite", false, true),
    DISCOVER(2, "Discover", true),
    DISCOVER_ALL(3, "DiscoverAll", false),
    OBSERVE_READ_ALL(4, "ObserveReadAll", false),

    OBSERVE(5, "Observe", true),
    OBSERVE_COMPOSITE(6, "ObserveComposite", false, true),
    OBSERVE_CANCEL(7, "ObserveCancel", true),
    OBSERVE_COMPOSITE_CANCEL(8, "ObserveCompositeCancel", false, true),
    OBSERVE_CANCEL_ALL(9, "ObserveCancelAll", false),
    EXECUTE(10, "Execute", true),
    /**
     * Replaces the Object Instance or the Resource(s) with the new value provided in the “Write” operation. (see
     * section 5.3.3 of the LW M2M spec).
     * if all resources are to be replaced
     */
    WRITE_REPLACE(11, "WriteReplace", true),

    /**
     * Adds or updates Resources provided in the new value and leaves other existing Resources unchanged. (see section
     * 5.3.3 of the LW M2M spec).
     * if this is a partial update request
     */
    WRITE_UPDATE(12, "WriteUpdate", true),
    WRITE_COMPOSITE(14, "WriteComposite", false, true),
    WRITE_ATTRIBUTES(15, "WriteAttributes", true),
    DELETE(16, "Delete", true),
    CREATE(17, "Create", true);
    // only for RPC - future
//    FW_UPDATE(18, "FirmwareUpdate", false),
//    FW_READ_INFO(19, "FirmwareReadInfo", false),
//    SW_UPDATE(20, "SoftwareUpdate", false),
//    SW_READ_INFO(21, "SoftwareReadInfo", false),
//    SW_UNINSTALL(22, "SoftwareUninstall", false);


    @Getter
    private final int code;
    @Getter
    private final String type;
    @Getter
    private final boolean hasObjectId;

    @Getter
    private final boolean composite;

    LwM2MOperationType(int code, String type, boolean hasObjectId) {
        this(code, type, hasObjectId, false);
    }

    LwM2MOperationType(int code, String type, boolean hasObjectId, boolean composite) {
        this.code = code;
        this.type = type;
        this.hasObjectId = hasObjectId;
        this.composite = composite;
        if (hasObjectId && composite) {
            throw new IllegalArgumentException("Can't set both Composite and hasObjectId for the same operation!");
        }
    }

    public static LwM2MOperationType fromType(String type) {
        for (LwM2MOperationType to : LwM2MOperationType.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        return null;
    }
}
