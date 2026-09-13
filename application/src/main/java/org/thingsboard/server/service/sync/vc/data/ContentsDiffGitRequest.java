// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc.data;

import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;

@Getter
public class ContentsDiffGitRequest extends PendingGitRequest<String> {

    private final String content1;
    private final String content2;

    public ContentsDiffGitRequest(TenantId tenantId, String content1, String content2) {
        super(tenantId);
        this.content1 = content1;
        this.content2 = content2;
    }

}
