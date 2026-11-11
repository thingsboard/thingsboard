// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.subscription;

import lombok.Builder;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.log.sub.LogsSubscriptionUpdate;

import java.util.function.BiConsumer;

public class TbLogsSubscription extends TbSubscription<LogsSubscriptionUpdate> {

    @Builder
    public TbLogsSubscription(String serviceId, String sessionId, int subscriptionId, TenantId tenantId, EntityId entityId,
                              BiConsumer<TbSubscription<LogsSubscriptionUpdate>, LogsSubscriptionUpdate> updateProcessor) {
        super(serviceId, sessionId, subscriptionId, tenantId, entityId, TbSubscriptionType.LOGS, updateProcessor);
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
