// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.dao.model.sql.EventEntity;

import java.util.List;
import java.util.UUID;

public interface EventRepository<T extends EventEntity<V>, V extends Event> {

    List<T> findLatestEvents(UUID tenantId, UUID entityId, int limit);

    Page<T> findEvents(UUID tenantId, UUID entityId, Long startTime, Long endTime, Pageable pageable);

    void removeEvents(UUID tenantId, UUID entityId, Long startTime, Long endTime);

}
