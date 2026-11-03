// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc.data;

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;

import java.util.List;

@Getter
public class EntitiesContentGitRequest extends PendingGitRequest<List<EntityExportData>> {

    private final String versionId;
    private final EntityType entityType;

    public EntitiesContentGitRequest(TenantId tenantId, String versionId, EntityType entityType) {
        super(tenantId);
        this.versionId = versionId;
        this.entityType = entityType;
    }
}
