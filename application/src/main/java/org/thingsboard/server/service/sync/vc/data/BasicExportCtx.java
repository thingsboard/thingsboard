// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.vc.data;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

@Data
public class BasicExportCtx {

    protected final SecurityUser user;
    private final CommitGitRequest commit;
    private final List<ListenableFuture<Void>> futures;
    private final EntityExportSettings settings;

    public BasicExportCtx(SecurityUser user, CommitGitRequest commit, List<ListenableFuture<Void>> futures, EntityExportSettings settings) {
        this.user = user;
        this.commit = commit;
        this.futures = futures;
        this.settings = settings;
    }

    public TenantId getTenantId() {
        return user.getTenantId();
    }

    public void add(ListenableFuture<Void> future){
        futures.add(future);
    }
}
