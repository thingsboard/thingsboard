// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
public class DeviceIdInfo implements Serializable {

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
