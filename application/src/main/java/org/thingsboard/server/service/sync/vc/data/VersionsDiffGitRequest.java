// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc.data;

import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.EntityVersionsDiff;

import java.util.List;

@Getter
public class VersionsDiffGitRequest extends PendingGitRequest<List<EntityVersionsDiff>> {

    private final String path;
    private final String versionId1;
    private final String versionId2;

    public VersionsDiffGitRequest(TenantId tenantId, String path, String versionId1, String versionId2) {
        super(tenantId);
        this.path = path;
        this.versionId1 = versionId1;
        this.versionId2 = versionId2;
    }

}
