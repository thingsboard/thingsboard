/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import lombok.Builder;
import lombok.Getter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.service.ws.notification.cmd.UnreadNotificationsUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscriptionUpdate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@Getter
public class NotificationsSubscription extends TbSubscription<NotificationsSubscriptionUpdate> {

    private final Map<UUID, Notification> unreadNotifications = new LinkedHashMap<>();
    private final int limit;
    private final AtomicInteger totalUnreadCount = new AtomicInteger();

    @Builder
    public NotificationsSubscription(String serviceId, String sessionId, int subscriptionId, TenantId tenantId, EntityId entityId,
                                     BiConsumer<NotificationsSubscription, NotificationsSubscriptionUpdate> updateProcessor,
                                     int limit) {
        super(serviceId, sessionId, subscriptionId, tenantId, entityId, TbSubscriptionType.NOTIFICATIONS, updateProcessor);
        this.limit = limit;
    }

    public UnreadNotificationsUpdate createFullUpdate() {
        return UnreadNotificationsUpdate.builder()
                .cmdId(getSubscriptionId())
                .notifications(unreadNotifications.values())
                .totalUnreadCount(totalUnreadCount.get())
                .build();
    }

    public UnreadNotificationsUpdate createPartialUpdate(Notification notification) {
        return UnreadNotificationsUpdate.builder()
                .cmdId(getSubscriptionId())
                .update(notification)
                .totalUnreadCount(totalUnreadCount.get())
                .build();
    }

}
