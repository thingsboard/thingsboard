// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;

import java.util.UUID;

@RequiredArgsConstructor
@Data
public class VersionControlRequestCtx {
    private final String nodeId;
    private final UUID requestId;
    private final TenantId tenantId;
    private final RepositorySettings settings;

    public VersionControlRequestCtx(ToVersionControlServiceMsg msg, RepositorySettings settings) {
        this.nodeId = msg.getNodeId();
        this.requestId = new UUID(msg.getRequestIdMSB(), msg.getRequestIdLSB());
        this.tenantId = TenantId.fromUUID(new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
        this.settings = settings;
    }

    @Override
    public String toString() {
        return "VersionControlRequestCtx{" +
                "nodeId='" + nodeId + '\'' +
                ", requestId=" + requestId +
                ", tenantId=" + tenantId +
                '}';
    }
}
