// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class PendingCommit {

    private final UUID txId;
    private final String nodeId;
    private final TenantId tenantId;
    private final String workingBranch;
    private String branch;
    private String versionName;

    private String authorName;
    private String authorEmail;

    private Map<String, String[]> chunkedMsgs;

    public PendingCommit(TenantId tenantId, String nodeId, UUID txId, String branch, String versionName, String authorName, String authorEmail) {
        this.tenantId = tenantId;
        this.nodeId = nodeId;
        this.txId = txId;
        this.branch = branch;
        this.versionName = versionName;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.workingBranch = txId.toString();
    }

    public Map<String, String[]> getChunkedMsgs() {
        if (chunkedMsgs == null) {
            chunkedMsgs = new ConcurrentHashMap<>();
        }
        return chunkedMsgs;
    }

}
