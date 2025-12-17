/**
 * Copyright © 2016-2025 The Thingsboard Authors
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