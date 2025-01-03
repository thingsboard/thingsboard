/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.api;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.SettableFuture;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeseriesSaveRequest {

    private final TenantId tenantId;
    private final CustomerId customerId;
    private final EntityId entityId;
    private final List<TsKvEntry> entries;
    private final long ttl;
    private final boolean saveLatest;
    private final boolean onlyLatest;
    private final List<CalculatedFieldId> calculatedFieldIds;
    private final FutureCallback<Void> callback;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private TenantId tenantId;
        private CustomerId customerId;
        private EntityId entityId;
        private List<TsKvEntry> entries;
        private long ttl;
        private FutureCallback<Void> callback;
        private boolean saveLatest = true;
        private boolean onlyLatest;
        private List<CalculatedFieldId> calculatedFieldIds;

        Builder() {}

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder customerId(CustomerId customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder entityId(EntityId entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder entries(List<TsKvEntry> entries) {
            this.entries = entries;
            return this;
        }

        public Builder entry(TsKvEntry entry) {
            return entries(List.of(entry));
        }

        public Builder entry(KvEntry kvEntry) {
            return entry(new BasicTsKvEntry(System.currentTimeMillis(), kvEntry));
        }

        public Builder ttl(long ttl) {
            this.ttl = ttl;
            return this;
        }

        public Builder saveLatest(boolean saveLatest) {
            this.saveLatest = saveLatest;
            return this;
        }

        public Builder onlyLatest(boolean onlyLatest) {
            this.onlyLatest = onlyLatest;
            this.saveLatest = true;
            return this;
        }

        public Builder calculatedFieldIds(List<CalculatedFieldId> calculatedFieldIds) {
            this.calculatedFieldIds = calculatedFieldIds;
            return this;
        }

        public Builder callback(FutureCallback<Void> callback) {
            this.callback = callback;
            return this;
        }

        public Builder future(SettableFuture<Void> future) {
            return callback(new FutureCallback<>() {
                @Override
                public void onSuccess(Void result) {
                    future.set(result);
                }

                @Override
                public void onFailure(Throwable t) {
                    future.setException(t);
                }
            });
        }

        public TimeseriesSaveRequest build() {
            return new TimeseriesSaveRequest(tenantId, customerId, entityId, entries, ttl, saveLatest, onlyLatest, calculatedFieldIds, callback);
        }

    }

}
