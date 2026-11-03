// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx;

import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

public record CalculatedFieldEntityCtxId(TenantId tenantId, CalculatedFieldId cfId, EntityId entityId) {

    public String toKey() {
        return cfId + "_" + entityId;
    }

}
