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
 * FW Update Result
 * 0: Initial value. Once the updating process is initiated (Download /Update), this Resource MUST be reset to Initial value.
 * 1: Firmware updated successfully.
 * 2: Not enough flash memory for the new firmware package.
 * 3: Out of RAM during downloading process.
 * 4: Connection lost during downloading process.
 * 5: Integrity check failure for new downloaded package.
 * 6: Unsupported package type.
 * 7: Invalid URI.
 * 8: Firmware update failed.
 * 9: Unsupported protocol.
 */
public enum FirmwareUpdateResult {
    INITIAL(0, "Initial value", false),
    UPDATE_SUCCESSFULLY(1, "Firmware updated successfully", false),
    NOT_ENOUGH(2, "Not enough flash memory for the new firmware package", false),
    OUT_OFF_MEMORY(3, "Out of RAM during downloading process", false),
    CONNECTION_LOST(4, "Connection lost during downloading process", true),
    INTEGRITY_CHECK_FAILURE(5, "Integrity check failure for new downloaded package", true),
    UNSUPPORTED_TYPE(6, "Unsupported package type", false),
    INVALID_URI(7, "Invalid URI", false),
    UPDATE_FAILED(8, "Firmware update failed", false),
    UNSUPPORTED_PROTOCOL(9, "Unsupported protocol", false);

    @Getter
    private int code;
    @Getter
    private String type;
    @Getter
    private boolean again;

    FirmwareUpdateResult(int code, String type, boolean isAgain) {
        this.code = code;
        this.type = type;
        this.again = isAgain;
    }

    public static FirmwareUpdateResult fromUpdateResultFwByType(String type) {
        for (FirmwareUpdateResult to : FirmwareUpdateResult.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW Update Result type  : %s", type));
    }

    public static FirmwareUpdateResult fromUpdateResultFwByCode(int code) {
        for (FirmwareUpdateResult to : FirmwareUpdateResult.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW Update Result code  : %s", code));
    }
}
