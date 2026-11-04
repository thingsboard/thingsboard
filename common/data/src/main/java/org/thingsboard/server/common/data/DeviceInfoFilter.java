// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@Builder
public class DeviceInfoFilter {

    private TenantId tenantId;
    private boolean includeCustomers;
    private CustomerId customerId;
    private DeviceProfileId deviceProfileId;
    private Boolean active;

}
