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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.event.StatisticsEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.event.EventDao;
import org.thingsboard.server.dao.model.sql.RuleNodeDebugEventEntity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
public class JpaBaseEventDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private EventDao eventDao;

    @Autowired
    private RuleNodeDebugEventRepository ruleNodeDebugEventRepository;

    UUID tenantId = Uuids.timeBased();


    @Test
    public void findEvent() throws InterruptedException, ExecutionException, TimeoutException {
        UUID entityId = Uuids.timeBased();

        Event event1 = getStatsEvent(Uuids.timeBased(), tenantId, entityId);
        eventDao.saveAsync(event1).get(1, TimeUnit.MINUTES);
        Thread.sleep(2);
        Event event2 = getStatsEvent(Uuids.timeBased(), tenantId, entityId);
        eventDao.saveAsync(event2).get(1, TimeUnit.MINUTES);

        List<? extends Event> foundEvents = eventDao.findLatestEvents(tenantId, entityId, EventType.STATS, 1);
        assertNotNull("Events expected to be not null", foundEvents);
        assertEquals(1, foundEvents.size());
        assertEquals(event2, foundEvents.get(0));
    }

    @Test
    public void findEventsByEntityIdAndPageLink() throws Exception {
        UUID entityId1 = Uuids.timeBased();
        UUID entityId2 = Uuids.timeBased();
        long startTime = System.currentTimeMillis();

        Event event1 = getStatsEvent(Uuids.timeBased(), tenantId, entityId1);
        eventDao.saveAsync(event1).get(1, TimeUnit.MINUTES);
        Thread.sleep(2);
        Event event2 = getStatsEvent(Uuids.timeBased(), tenantId, entityId2);
        eventDao.saveAsync(event2).get(1, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();

        PageData<? extends Event> events1 = eventDao.findEvents(tenantId, entityId1, EventType.STATS, new TimePageLink(30));
        assertEquals(1, events1.getData().size());

        PageData<? extends Event> events2 = eventDao.findEvents(tenantId, entityId2, EventType.STATS, new TimePageLink(30));
        assertEquals(1, events2.getData().size());

        PageData<? extends Event> events3 = eventDao.findEvents(tenantId, Uuids.timeBased(), EventType.STATS, new TimePageLink(30));
        assertEquals(0, events3.getData().size());


        TimePageLink pageLink2 = new TimePageLink(30, 0, "", null, startTime, null);
        PageData<? extends Event> events12 = eventDao.findEvents(tenantId, entityId1, EventType.STATS, pageLink2);
        assertEquals(1, events12.getData().size());
        assertEquals(event1, events12.getData().get(0));

        TimePageLink pageLink3 = new TimePageLink(30, 0, "", null, startTime, endTime);
        PageData<? extends Event> events13 = eventDao.findEvents(tenantId, entityId1, EventType.STATS, pageLink3);
        assertEquals(1, events13.getData().size());
        assertEquals(event1, events13.getData().get(0));

        TimePageLink pageLink4 = new TimePageLink(5, 0, "", null, startTime, endTime);
        PageData<? extends Event> events14 = eventDao.findEvents(tenantId, entityId1, EventType.STATS, pageLink4);
        assertEquals(1, events14.getData().size());
        assertEquals(event1, events14.getData().get(0));

        pageLink4 = pageLink4.nextPageLink();
        PageData<? extends Event> events6 = eventDao.findEvents(tenantId, entityId1, EventType.STATS, pageLink4);
        assertEquals(0, events6.getData().size());

    }

    @Test
    public void findLatestDebugRuleNodeInEvent() throws Exception {

        UUID entityId = Uuids.timeBased();

        RuleNodeDebugEventEntity event = getDebugInEventEntity(Uuids.timeBased(), tenantId, entityId);
        eventDao.saveAsync(event.toData()).get(1, TimeUnit.MINUTES);
        Thread.sleep(2);
        RuleNodeDebugEventEntity event2 = getDebugInEventEntity(Uuids.timeBased(), tenantId, entityId);
        eventDao.saveAsync(event2.toData()).get(1, TimeUnit.MINUTES);

        RuleNodeDebugEventEntity foundEvent = ruleNodeDebugEventRepository.findLatestDebugRuleNodeInEvent(tenantId, entityId).get();
        assertNotNull("Events expected to be not null", foundEvent);
        assertEquals(event2.getEventType(), foundEvent.getEventType());
        assertEquals(event2.getId(), foundEvent.getId());
    }

    private Event getStatsEvent(UUID eventId, UUID tenantId, UUID entityId) {
        StatisticsEvent.StatisticsEventBuilder event = StatisticsEvent.builder();
        event.id(eventId);
        event.ts(System.currentTimeMillis());
        event.tenantId(new TenantId(tenantId));
        event.entityId(entityId);
        event.serviceId("server A");
        event.messagesProcessed(1);
        event.errorsOccurred(0);
        return event.build();
    }

    private RuleNodeDebugEventEntity getDebugInEventEntity(UUID eventId, UUID tenantId, UUID entityId) {
        RuleNodeDebugEventEntity event = new RuleNodeDebugEventEntity();
        event.setId(eventId);
        event.setEventType("IN");
        event.setEntityId(entityId);
        event.setTenantId(tenantId);
        return event;
    }
}
