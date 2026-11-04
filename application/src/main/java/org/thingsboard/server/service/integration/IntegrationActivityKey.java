// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import lombok.NonNull;
import lombok.Value;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;

@Value
public class IntegrationActivityKey {

    @NonNull TenantId tenantId;
    @NonNull DeviceId deviceId;

}
