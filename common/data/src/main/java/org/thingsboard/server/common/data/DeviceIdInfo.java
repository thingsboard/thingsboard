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
package org.thingsboard.server.common.data;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;
import java.util.UUID;

@Data
@Slf4j
public class DeviceIdInfo implements Serializable, HasTenantId {

    private static final long serialVersionUID = 2233745129677581815L;

    private final TenantId tenantId;
    private final CustomerId customerId;
    private final DeviceId deviceId;

    public DeviceIdInfo(UUID tenantId, UUID customerId, UUID deviceId) {
        this.tenantId = TenantId.fromUUID(tenantId);
        this.customerId = customerId != null ? new CustomerId(customerId) : null;
        this.deviceId = new DeviceId(deviceId);
    }
}
