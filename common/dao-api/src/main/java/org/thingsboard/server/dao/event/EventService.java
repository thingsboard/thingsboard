// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.event;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventFilter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.List;

public interface EventService {

    ListenableFuture<Void> saveAsync(Event event);

    PageData<EventInfo> findEvents(TenantId tenantId, EntityId entityId, EventType eventType, TimePageLink pageLink);

    List<EventInfo> findLatestEvents(TenantId tenantId, EntityId entityId, EventType eventType, int limit);

    EventInfo findLatestDebugRuleNodeInEvent(TenantId tenantId, EntityId entityId);

    PageData<EventInfo> findEventsByFilter(TenantId tenantId, EntityId entityId, EventFilter eventFilter, TimePageLink pageLink);

    void removeEvents(TenantId tenantId, EntityId entityId);

    void removeEvents(TenantId tenantId, EntityId entityId, Long startTime, Long endTime, EventType... types);

    void removeEvents(TenantId tenantId, EntityId entityId, EventFilter eventFilter, Long startTime, Long endTime);

    void cleanupEvents(long regularEventExpTs, long debugEventExpTs, boolean cleanupDb);

}
