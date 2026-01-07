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
package org.thingsboard.server.service.subscription;

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Objects;
import java.util.function.BiConsumer;

@Data
public abstract class TbSubscription<T> {

    /** Cache the hash code */
    private transient int hash; // Default to 0. The hash code calculated for this object likely never be zero

    private final String serviceId;
    private final String sessionId;
    private final int subscriptionId;
    private final TenantId tenantId;
    private final EntityId entityId;
    private final TbSubscriptionType type;
    private final BiConsumer<TbSubscription<T>, T> updateProcessor;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TbSubscription that = (TbSubscription) o;
        return subscriptionId == that.subscriptionId &&
                sessionId.equals(that.sessionId) &&
                tenantId.equals(that.tenantId) &&
                entityId.equals(that.entityId) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            hash = Objects.hash(sessionId, subscriptionId, tenantId, entityId, type);
        }
        return hash;
    }

}
