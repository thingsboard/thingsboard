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

import org.springframework.context.ApplicationListener;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.queue.discovery.event.OtherServiceShutdownEvent;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.service.ws.notification.sub.NotificationUpdate;

import java.util.List;

public interface SubscriptionManagerService extends ApplicationListener<PartitionChangeEvent> {

    void onSubEvent(String serviceId, TbEntitySubEvent event, TbCallback empty);

    void onApplicationEvent(OtherServiceShutdownEvent event);

    void onTimeSeriesUpdate(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, TbCallback callback);

    void onAttributesUpdate(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, TbCallback callback);

    void onAttributesDelete(TenantId tenantId, EntityId entityId, String scope, List<String> keys, TbCallback empty);

    void onTimeSeriesDelete(TenantId tenantId, EntityId entityId, List<String> keys, TbCallback callback);

    void onAlarmUpdate(TenantId tenantId, EntityId entityId, AlarmInfo alarm, TbCallback callback);

    void onAlarmDeleted(TenantId tenantId, EntityId entityId, AlarmInfo alarm, TbCallback callback);

    void onNotificationUpdate(TenantId tenantId, UserId recipientId, NotificationUpdate notificationUpdate, TbCallback callback);

}
