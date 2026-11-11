// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.vc.data;

import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;

@Getter
public class FileContentGitRequest extends PendingGitRequest<String> {

    private final String versionId;
    private final String path;

    public FileContentGitRequest(TenantId tenantId, String versionId, String path) {
        super(tenantId);
        this.versionId = versionId;
        this.path = path;
    }
}
