// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.event;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventFilter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.List;
import java.util.UUID;

public interface EventDao {

    ListenableFuture<Void> saveAsync(Event event);

    PageData<? extends Event> findEvents(UUID tenantId, UUID entityId, EventType eventType, TimePageLink pageLink);

    PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, EventFilter eventFilter, TimePageLink pageLink);

    List<? extends Event> findLatestEvents(UUID tenantId, UUID entityId, EventType eventType, int limit);

    Event findLatestDebugRuleNodeInEvent(UUID tenantId, UUID entityId);

    void cleanupEvents(long regularEventExpTs, long debugEventExpTs, boolean cleanupDb);

    void removeEvents(UUID tenantId, UUID entityId, Long startTime, Long endTime);

    void removeEvents(UUID tenantId, UUID entityId, Long startTime, Long endTime, EventType... types);

    void removeEvents(UUID tenantId, UUID entityId, EventFilter eventFilter, Long startTime, Long endTime);

}
