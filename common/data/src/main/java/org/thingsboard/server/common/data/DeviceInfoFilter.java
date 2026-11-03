// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@Builder
public class DeviceInfoFilter {

    private TenantId tenantId;
    private CustomerId customerId;
    private EdgeId edgeId;
    private String type;
    private DeviceProfileId deviceProfileId;
    private Boolean active;

}
