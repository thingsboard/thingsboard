/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.cache.device;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
@Builder
public class DeviceCacheKey implements Serializable {

    private final TenantId tenantId;
    private final DeviceId deviceId;
    private final String deviceName;

    public DeviceCacheKey(TenantId tenantId, DeviceId deviceId) {
        this(tenantId, deviceId, null);
    }

    public DeviceCacheKey(TenantId tenantId, String deviceName) {
        this(tenantId, null, deviceName);
    }

    @Override
    public String toString() {
        if (deviceId != null) {
            return tenantId + "_" + deviceId;
        } else {
            return tenantId + "_n_" + deviceName;
        }
    }

}
