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
package org.thingsboard.rule.engine.api;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.SettableFuture;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.thingsboard.common.util.NoOpFutureCallback;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeseriesSaveRequest implements CalculatedFieldSystemAwareRequest {

    private final TenantId tenantId;
    private final CustomerId customerId;
    private final EntityId entityId;
    private final List<TsKvEntry> entries;
    private final long ttl;
    private final Strategy strategy;
    private final List<CalculatedFieldId> previousCalculatedFieldIds;
    private final UUID tbMsgId;
    private final TbMsgType tbMsgType;
    private final FutureCallback<Void> callback;

    public record Strategy(boolean saveTimeseries, boolean saveLatest, boolean sendWsUpdate, boolean processCalculatedFields) {

        public static final Strategy PROCESS_ALL = new Strategy(true, true, true, true);
        public static final Strategy WS_ONLY = new Strategy(false, false, true, false);
        public static final Strategy LATEST_AND_WS = new Strategy(false, true, true, false);
        public static final Strategy SKIP_ALL = new Strategy(false, false, false, false);

    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private TenantId tenantId;
        private CustomerId customerId;
        private EntityId entityId;
        private List<TsKvEntry> entries;
        private long ttl;
        private Strategy strategy;
        private List<CalculatedFieldId> previousCalculatedFieldIds;
        private UUID tbMsgId;
        private TbMsgType tbMsgType;
        private FutureCallback<Void> callback;

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

        public Builder strategy(Strategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder previousCalculatedFieldIds(List<CalculatedFieldId> previousCalculatedFieldIds) {
            this.previousCalculatedFieldIds = previousCalculatedFieldIds;
            return this;
        }

        public Builder tbMsgId(UUID tbMsgId) {
            this.tbMsgId = tbMsgId;
            return this;
        }

        public Builder tbMsgType(TbMsgType tbMsgType) {
            this.tbMsgType = tbMsgType;
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
            return new TimeseriesSaveRequest(
                    tenantId, customerId, entityId, entries, ttl, requireNonNullElse(strategy, Strategy.PROCESS_ALL),
                    previousCalculatedFieldIds, tbMsgId, tbMsgType, requireNonNullElse(callback, NoOpFutureCallback.instance())
            );
        }

    }

}
