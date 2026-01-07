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
