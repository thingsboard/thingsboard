// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc.data;

import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;

import java.util.UUID;

public class CommitGitRequest extends PendingGitRequest<VersionCreationResult> {

    @Getter
    private final UUID txId;
    private final VersionCreateRequest request;

    public CommitGitRequest(TenantId tenantId, VersionCreateRequest request) {
        super(tenantId);
        this.txId = UUID.randomUUID();
        this.request = request;
    }

}
