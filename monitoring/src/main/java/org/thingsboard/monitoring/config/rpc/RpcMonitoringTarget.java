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
package org.thingsboard.monitoring.config.rpc;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.monitoring.config.MonitoringTarget;

import java.util.UUID;

@Data
public class RpcMonitoringTarget implements MonitoringTarget {

    private String baseUrl;
    private String deviceId;
    private String accessToken;
    // Human-readable label used in metric keys and logs; defaults to first 8 chars of deviceId
    private String label;

    @Override
    public UUID getDeviceId() {
        return UUID.fromString(deviceId);
    }

    @Override
    public boolean isCheckDomainIps() {
        return false;
    }

    public String getLabel() {
        return StringUtils.isNotBlank(label) ? label : deviceId.substring(0, 8);
    }

}
