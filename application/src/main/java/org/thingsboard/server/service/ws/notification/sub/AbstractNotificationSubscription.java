// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.notification.sub;


import lombok.Getter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.subscription.TbSubscription;
import org.thingsboard.server.service.subscription.TbSubscriptionType;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@Getter
public abstract class AbstractNotificationSubscription<T> extends TbSubscription<T> {

    protected final AtomicInteger sequence = new AtomicInteger();
    protected final AtomicInteger totalUnreadCounter = new AtomicInteger();

    public AbstractNotificationSubscription(String serviceId, String sessionId, int subscriptionId, TenantId tenantId, EntityId entityId, TbSubscriptionType type, BiConsumer<TbSubscription<T>, T> updateProcessor) {
        super(serviceId, sessionId, subscriptionId, tenantId, entityId, type, updateProcessor);
    }

}
