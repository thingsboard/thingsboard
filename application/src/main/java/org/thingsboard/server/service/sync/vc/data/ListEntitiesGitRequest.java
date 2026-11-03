// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc.data;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;

import java.util.List;

public class ListEntitiesGitRequest extends PendingGitRequest<List<VersionedEntityInfo>> {

    public ListEntitiesGitRequest(TenantId tenantId) {
        super(tenantId);
    }

}
