/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.ota;

import java.util.Optional;

public enum OtaPackageEvents {

    TELEMETRY_EVENT,
    RULECHAIN_EVENT;

    public static Optional<OtaPackageEvents> parse(String name) {
        OtaPackageEvents partition = null;
        if (name != null) {
            for (OtaPackageEvents partitionDate : OtaPackageEvents.values()) {
                if (partitionDate.name().equalsIgnoreCase(name)) {
                    partition = partitionDate;
                    break;
                }
            }
        }
        return Optional.ofNullable(partition);
    }
}