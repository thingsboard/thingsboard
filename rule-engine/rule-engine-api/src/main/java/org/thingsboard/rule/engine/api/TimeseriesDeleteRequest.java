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
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;

import java.util.List;

@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeseriesDeleteRequest {

    private final TenantId tenantId;
    private final EntityId entityId;
    private final List<String> keys;
    private final List<DeleteTsKvQuery> deleteHistoryQueries;
    private final FutureCallback<List<String>> callback;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private TenantId tenantId;
        private EntityId entityId;
        private List<String> keys;
        private List<DeleteTsKvQuery> deleteHistoryQueries;
        private FutureCallback<List<String>> callback;

        Builder() {}

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder entityId(EntityId entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder keys(List<String> keys) {
            this.keys = keys;
            return this;
        }

        public Builder deleteHistoryQueries(List<DeleteTsKvQuery> deleteHistoryQueries) {
            this.deleteHistoryQueries = deleteHistoryQueries;
            return this;
        }

        public Builder callback(FutureCallback<List<String>> callback) {
            this.callback = callback;
            return this;
        }

        public TimeseriesDeleteRequest build() {
            return new TimeseriesDeleteRequest(tenantId, entityId, keys, deleteHistoryQueries, callback);
        }

    }

}
