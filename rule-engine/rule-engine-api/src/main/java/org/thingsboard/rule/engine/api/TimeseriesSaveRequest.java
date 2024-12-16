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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.List;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeseriesSaveRequest {
    private final TenantId tenantId;
    private final CustomerId customerId;
    private final EntityId entityId;
    private final List<TsKvEntry> entries;
    private final long ttl;
    private final boolean saveLatest;
    private FutureCallback<Void> callback;

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
        private boolean saveLatest;

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
            this.entries = List.of(entry);
            return this;
        }

        public Builder ttl(long ttl) {
            this.ttl = ttl;
            return this;
        }

        public Builder saveLatest(boolean saveLatest) {
            this.saveLatest = saveLatest;
            return this;
        }

        public Builder callback(FutureCallback<Void> callback) {
            this.callback = callback;
            return this;
        }

        public TimeseriesSaveRequest build() {
            return new TimeseriesSaveRequest(tenantId, customerId, entityId, entries, ttl, saveLatest, callback);
        }

    }

}
