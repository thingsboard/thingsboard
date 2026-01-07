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
package org.thingsboard.rule.engine.api;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.SettableFuture;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.common.util.NoOpFutureCallback;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AttributesSaveRequest implements CalculatedFieldSystemAwareRequest {

    private final TenantId tenantId;
    private final EntityId entityId;
    private final AttributeScope scope;
    private final List<AttributeKvEntry> entries;
    private final boolean notifyDevice;
    private final Strategy strategy;
    private final List<CalculatedFieldId> previousCalculatedFieldIds;
    private final UUID tbMsgId;
    private final TbMsgType tbMsgType;
    private final FutureCallback<Void> callback;

    public record Strategy(boolean saveAttributes, boolean sendWsUpdate, boolean processCalculatedFields) {

        public static final Strategy PROCESS_ALL = new Strategy(true, true, true);
        public static final Strategy WS_ONLY = new Strategy(false, true, false);
        public static final Strategy SKIP_ALL = new Strategy(false, false, false);
        public static final Strategy CF_ONLY = new Strategy(false, false, true);

    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private TenantId tenantId;
        private EntityId entityId;
        private AttributeScope scope;
        private List<AttributeKvEntry> entries;
        private boolean notifyDevice = true;
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

        public Builder entityId(EntityId entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder scope(AttributeScope scope) {
            this.scope = scope;
            return this;
        }

        @Deprecated
        public Builder scope(String scope) {
            try {
                this.scope = AttributeScope.valueOf(scope);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid attribute scope '" + scope + "'");
            }
            return this;
        }

        public Builder entries(List<AttributeKvEntry> entries) {
            this.entries = entries;
            return this;
        }

        public Builder entry(AttributeKvEntry entry) {
            return entries(List.of(entry));
        }

        public Builder entry(KvEntry kvEntry) {
            return entry(new BaseAttributeKvEntry(kvEntry, System.currentTimeMillis()));
        }

        public Builder notifyDevice(boolean notifyDevice) {
            this.notifyDevice = notifyDevice;
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

        public AttributesSaveRequest build() {
            return new AttributesSaveRequest(
                    tenantId, entityId, scope, entries, notifyDevice, requireNonNullElse(strategy, Strategy.PROCESS_ALL),
                    previousCalculatedFieldIds, tbMsgId, tbMsgType, requireNonNullElse(callback, NoOpFutureCallback.instance())
            );
        }

    }

}
