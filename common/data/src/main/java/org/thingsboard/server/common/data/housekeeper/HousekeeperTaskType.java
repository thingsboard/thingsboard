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
package org.thingsboard.server.common.data.housekeeper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum HousekeeperTaskType {

    DELETE_ATTRIBUTES("attributes deletion"),
    DELETE_TELEMETRY("telemetry deletion"),
    DELETE_LATEST_TS("latest telemetry deletion"),
    DELETE_TS_HISTORY("timeseries history deletion"),
    DELETE_EVENTS("events deletion"),
    DELETE_ALARMS("alarms deletion"),
    UNASSIGN_ALARMS("alarms unassigning"),
    DELETE_TENANT_ENTITIES("tenant entities deletion"),
    DELETE_ENTITIES("entities deletion"),
    DELETE_CALCULATED_FIELDS("calculated fields deletion"),
    DELETE_JOBS("jobs deletion");

    private final String description;

}
