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
 * SW Update State R
 * 0: INITIAL Before downloading. (see 5.1.2.1)
 * 1: DOWNLOAD STARTED The downloading process has started and is on-going. (see 5.1.2.2)
 * 2: DOWNLOADED The package has been completely downloaded  (see 5.1.2.3)
 * 3: DELIVERED In that state, the package has been correctly downloaded and is ready to be installed.  (see 5.1.2.4)
 * If executing the Install Resource failed, the state remains at DELIVERED.
 * If executing the Install Resource was successful, the state changes from DELIVERED to INSTALLED.
 * After executing the UnInstall Resource, the state changes to INITIAL.
 * 4: INSTALLED
 */
public enum SoftwareUpdateState {
    INITIAL(0, "Initial"),
    DOWNLOAD_STARTED(1, "DownloadStarted"),
    DOWNLOADED(2, "Downloaded"),
    DELIVERED(3, "Delivered"),
    INSTALLED(4, "Installed");

    @Getter
    private int code;
    @Getter
    private String type;

    SoftwareUpdateState(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public static SoftwareUpdateState fromUpdateStateSwByType(String type) {
        for (SoftwareUpdateState to : SoftwareUpdateState.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported SW State type  : %s", type));
    }

    public static SoftwareUpdateState fromUpdateStateSwByCode(int code) {
        for (SoftwareUpdateState to : SoftwareUpdateState.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported SW State type  : %s", code));
    }
}

