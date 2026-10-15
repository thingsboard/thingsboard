// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc.data;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;

public class ListVersionsGitRequest extends PendingGitRequest<PageData<EntityVersion>> {

    public ListVersionsGitRequest(TenantId tenantId) {
        super(tenantId);
    }

}
