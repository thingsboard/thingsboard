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
package org.thingsboard.server.transport.lwm2m.server.ota.software;

import lombok.Getter;

/**
 * SW Update Result
 * Contains the result of downloading or installing/uninstalling the software
 * 0: Initial value.
 * - Prior to download any new package in the Device, Update Result MUST be reset to this initial value.
 * - One side effect of executing the Uninstall resource is to reset Update Result to this initial value "0".
 * 1: Downloading.
 * - The package downloading process is on-going.
 * 2: Software successfully installed.
 * 3: Successfully Downloaded and package integrity verified
 * (( 4-49, for expansion, of other scenarios))
 * ** Failed
 * 50: Not enough storage for the new software package.
 * 51: Out of memory during downloading process.
 * 52: Connection lost during downloading process.
 * 53: Package integrity check failure.
 * 54: Unsupported package type.
 * 56: Invalid URI
 * 57: Device defined update error
 * 58: Software installation failure
 * 59: Uninstallation Failure during forUpdate(arg=0)
 * 60-200 : (for expansion, selection to be in blocks depending on new introduction of features)
 * This Resource MAY be reported by sending Observe operation.
 */
public enum SoftwareUpdateResult {
    INITIAL(0, "Initial value", false),
    DOWNLOADING(1, "Downloading", false),
    SUCCESSFULLY_INSTALLED(2, "Software successfully installed", false),
    SUCCESSFULLY_DOWNLOADED_VERIFIED(3, "Successfully Downloaded and package integrity verified", false),
    NOT_ENOUGH_STORAGE(50, "Not enough storage for the new software package", true),
    OUT_OFF_MEMORY(51, "Out of memory during downloading process", true),
    CONNECTION_LOST(52, "Connection lost during downloading process", false),
    PACKAGE_CHECK_FAILURE(53, "Package integrity check failure.", false),
    UNSUPPORTED_PACKAGE_TYPE(54, "Unsupported package type", false),
    INVALID_URI(56, "Invalid URI", true),
    UPDATE_ERROR(57, "Device defined update error", true),
    INSTALL_FAILURE(58, "Software installation failure", true),
    UN_INSTALL_FAILURE(59, "Uninstallation Failure during forUpdate(arg=0)", true);

    @Getter
    private int code;
    @Getter
    private String type;
    @Getter
    private boolean isAgain;

    SoftwareUpdateResult(int code, String type, boolean isAgain) {
        this.code = code;
        this.type = type;
        this.isAgain = isAgain;
    }

    public static SoftwareUpdateResult fromUpdateResultSwByType(String type) {
        for (SoftwareUpdateResult to : SoftwareUpdateResult.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported SW Update Result type  : %s", type));
    }

    public static SoftwareUpdateResult fromUpdateResultSwByCode(int code) {
        for (SoftwareUpdateResult to : SoftwareUpdateResult.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported SW Update Result code  : %s", code));
    }
}
