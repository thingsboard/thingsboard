/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.transport.limits;

import lombok.Getter;

public enum TransportRateLimitType {

    TENANT_ADDED_TO_DISABLED_LIST("general.tenant.disabled", true, false),
    TENANT_MAX_MSGS("transport.tenant.msg", true, true),
    TENANT_TELEMETRY_MSGS("transport.tenant.telemetry", true, true),
    TENANT_MAX_DATA_POINTS("transport.tenant.dataPoints", true, false),
    DEVICE_MAX_MSGS("transport.device.msg", false, true),
    DEVICE_TELEMETRY_MSGS("transport.device.telemetry", false, true),
    DEVICE_MAX_DATA_POINTS("transport.device.dataPoints", false, false);

    @Getter
    private final String configurationKey;
    @Getter
    private final boolean tenantLevel;
    @Getter
    private final boolean deviceLevel;
    @Getter
    private final boolean messageLevel;
    @Getter
    private final boolean dataPointLevel;

    TransportRateLimitType(String configurationKey, boolean tenantLevel, boolean messageLevel) {
        this.configurationKey = configurationKey;
        this.tenantLevel = tenantLevel;
        this.deviceLevel = !tenantLevel;
        this.messageLevel = messageLevel;
        this.dataPointLevel = !messageLevel;
    }
}
