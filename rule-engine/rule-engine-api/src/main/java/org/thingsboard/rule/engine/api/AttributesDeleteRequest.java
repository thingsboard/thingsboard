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
import lombok.ToString;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AttributesDeleteRequest {

    private final TenantId tenantId;
    private final EntityId entityId;
    private final AttributeScope scope;
    private final List<String> keys;
    private final boolean notifyDevice;
    private final FutureCallback<Void> callback;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private TenantId tenantId;
        private EntityId entityId;
        private AttributeScope scope;
        private List<String> keys;
        private boolean notifyDevice;
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

        public Builder keys(List<String> keys) {
            this.keys = keys;
            return this;
        }

        public Builder notifyDevice(boolean notifyDevice) {
            this.notifyDevice = notifyDevice;
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

        public AttributesDeleteRequest build() {
            return new AttributesDeleteRequest(tenantId, entityId, scope, keys, notifyDevice, callback);
        }

    }

}
