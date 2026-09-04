// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc.data;

import com.google.common.util.concurrent.SettableFuture;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

@Getter
public class PendingGitRequest<T> {

    private final long createdTime;
    private final UUID requestId;
    private final TenantId tenantId;
    private final SettableFuture<T> future;
    @Setter
    private ScheduledFuture<?> timeoutTask;

    public PendingGitRequest(TenantId tenantId) {
        this.createdTime = System.currentTimeMillis();
        this.requestId = UUID.randomUUID();
        this.tenantId = tenantId;
        this.future = SettableFuture.create();
    }

    public boolean requiresSettings() {
        return true;
    }
}
