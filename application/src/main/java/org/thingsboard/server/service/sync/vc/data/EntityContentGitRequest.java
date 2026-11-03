// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc.data;

import lombok.Getter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;

@Getter
public class EntityContentGitRequest extends PendingGitRequest<EntityExportData> {

    private final String versionId;
    private final EntityId entityId;

    public EntityContentGitRequest(TenantId tenantId, String versionId, EntityId entityId) {
        super(tenantId);
        this.versionId = versionId;
        this.entityId = entityId;
    }
}
