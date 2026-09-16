// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.limits;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;

@Data
public class EntityLimitKey {

    private final TenantId tenantId;
    private final String deviceName;

}
