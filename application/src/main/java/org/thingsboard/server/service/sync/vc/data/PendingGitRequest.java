/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
