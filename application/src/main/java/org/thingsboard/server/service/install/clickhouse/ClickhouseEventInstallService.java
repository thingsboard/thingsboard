/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.install.clickhouse;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventFilter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.event.EventService;

import java.util.List;

/**
 * @author baigod
 * @version : ClickhouseEventInstallService.java, v 0.1 2024年05月30日 23:02 baigod Exp $
 */
@Service
@Profile("install")
@ConditionalOnProperty(prefix = "event.debug", value = "type", havingValue = "clickhouse")
public class ClickhouseEventInstallService implements EventService {

    @Override
    public ListenableFuture<Void> saveAsync(Event event) {
        return Futures.immediateVoidFuture();
    }

    @Override
    public PageData<EventInfo> findEvents(TenantId tenantId, EntityId entityId, EventType eventType, TimePageLink pageLink) {
        return new PageData<>();
    }

    @Override
    public List<EventInfo> findLatestEvents(TenantId tenantId, EntityId entityId, EventType eventType, int limit) {
        return List.of();
    }

    @Override
    public EventInfo findLatestDebugRuleNodeInEvent(TenantId tenantId, EntityId entityId) {
        return new EventInfo();
    }

    @Override
    public PageData<EventInfo> findEventsByFilter(TenantId tenantId, EntityId entityId, EventFilter eventFilter, TimePageLink pageLink) {
        return new PageData<>();
    }

    @Override
    public void removeEvents(TenantId tenantId, EntityId entityId) {

    }

    @Override
    public void removeEvents(TenantId tenantId, EntityId entityId, EventFilter eventFilter, Long startTime, Long endTime) {

    }

    @Override
    public void cleanupEvents(long regularEventExpTs, long debugEventExpTs, boolean cleanupDb) {

    }
}
