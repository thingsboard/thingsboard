// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.session;

import com.google.common.util.concurrent.FutureCallback;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;

@Slf4j
public class EdgeAttributeSaveCallback implements FutureCallback<Void> {

    private final TenantId tenantId;
    private final EdgeId edgeId;
    private final String key;
    private final Object value;

    public EdgeAttributeSaveCallback(TenantId tenantId, EdgeId edgeId, String key, Object value) {
        this.tenantId = tenantId;
        this.edgeId = edgeId;
        this.key = key;
        this.value = value;
    }

    @Override
    public void onSuccess(@Nullable Void result) {
        log.trace("[{}][{}] Successfully updated attribute [{}] with value [{}]", tenantId, edgeId, key, value);
    }

    @Override
    public void onFailure(Throwable t) {
        log.warn("[{}][{}] Failed to update attribute [{}] with value [{}]", tenantId, edgeId, key, value, t);
    }

}
