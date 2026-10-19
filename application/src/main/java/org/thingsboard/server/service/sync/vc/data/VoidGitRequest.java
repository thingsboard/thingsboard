// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc.data;

import org.thingsboard.server.common.data.id.TenantId;

public class VoidGitRequest extends PendingGitRequest<Void> {

    public VoidGitRequest(TenantId tenantId) {
        super(tenantId);
    }

}
