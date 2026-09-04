// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.subscription;

import lombok.Builder;
import lombok.Getter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.ws.telemetry.sub.TelemetrySubscriptionUpdate;

import java.util.Map;
import java.util.function.BiConsumer;

public class TbAttributeSubscription extends TbSubscription<TelemetrySubscriptionUpdate> {

    @Getter private final long queryTs;
    @Getter private final boolean allKeys;
    @Getter private final Map<String, Long> keyStates;
    @Getter private final TbAttributeSubscriptionScope scope;

    @Builder
    public TbAttributeSubscription(String serviceId, String sessionId, int subscriptionId, TenantId tenantId, EntityId entityId,
                                   BiConsumer<TbSubscription<TelemetrySubscriptionUpdate>, TelemetrySubscriptionUpdate> updateProcessor,
                                   long queryTs, boolean allKeys, Map<String, Long> keyStates, TbAttributeSubscriptionScope scope) {
        super(serviceId, sessionId, subscriptionId, tenantId, entityId, TbSubscriptionType.ATTRIBUTES, updateProcessor);
        this.queryTs = queryTs;
        this.allKeys = allKeys;
        this.keyStates = keyStates;
        this.scope = scope;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
