// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.tenant;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;

@Data
public class TenantEvictEvent {
    private final TenantId tenantId;
    private final boolean invalidateExists;
}
