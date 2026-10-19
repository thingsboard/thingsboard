// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc.data;

import org.thingsboard.server.common.data.id.TenantId;

public class ClearRepositoryGitRequest extends VoidGitRequest {

    public ClearRepositoryGitRequest(TenantId tenantId) {
        super(tenantId);
    }

    public boolean requiresSettings() {
        return false;
    }

}
