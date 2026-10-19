// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
