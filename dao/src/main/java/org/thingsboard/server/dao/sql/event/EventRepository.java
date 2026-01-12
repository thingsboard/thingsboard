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
