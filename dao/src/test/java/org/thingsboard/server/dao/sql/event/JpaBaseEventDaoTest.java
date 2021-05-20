/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.event.EventDao;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.thingsboard.server.common.data.DataConstants.ALARM;
import static org.thingsboard.server.common.data.DataConstants.STATS;

/**
 * Created by Valerii Sosliuk on 5/5/2017.
 */
@Slf4j
public class JpaBaseEventDaoTest extends AbstractJpaDaoTest {

    public static final long HOUR_MILLISECONDS = (long) 3.6e+6;
    @Autowired
    private EventDao eventDao;

    @Test
    public void testSaveIfNotExists() {
        UUID eventId = Uuids.timeBased();
        UUID tenantId = Uuids.timeBased();
        UUID entityId = Uuids.timeBased();
        Event event = getEvent(eventId, tenantId, entityId);
        Optional<Event> optEvent1 = eventDao.saveIfNotExists(event);
        assertTrue("Optional is expected to be non-empty", optEvent1.isPresent());
        assertEquals(event, optEvent1.get());
        Optional<Event> optEvent2 = eventDao.saveIfNotExists(event);
        assertFalse("Optional is expected to be empty", optEvent2.isPresent());
    }

    @Test
    @DatabaseSetup("classpath:dbunit/event.xml")
    public void findEvent() {
        UUID tenantId = UUID.fromString("be41c7a0-31f5-11e7-9cfd-2786e6aa2046");
        UUID entityId = UUID.fromString("be41c7a1-31f5-11e7-9cfd-2786e6aa2046");
        String eventType = STATS;
        String eventUid = "be41c7a3-31f5-11e7-9cfd-2786e6aa2046";
        Event event = eventDao.findEvent(tenantId, new DeviceId(entityId), eventType, eventUid);
        eventDao.find(AbstractServiceTest.SYSTEM_TENANT_ID).stream().forEach(System.out::println);
        assertNotNull("Event expected to be not null", event);
        assertEquals("be41c7a2-31f5-11e7-9cfd-2786e6aa2046", event.getId().getId().toString());
    }

    @Test
    public void findEventsByEntityIdAndPageLink() {
        UUID tenantId = Uuids.timeBased();
        UUID entityId1 = Uuids.timeBased();
        UUID entityId2 = Uuids.timeBased();
        long startTime = System.currentTimeMillis();
        long endTime = createEventsTwoEntities(tenantId, entityId1, entityId2, startTime, 20);

        TimePageLink pageLink1 = new TimePageLink(30);
        PageData<Event> events1 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink1);
        assertEquals(10, events1.getData().size());

        TimePageLink pageLink2 = new TimePageLink(30, 0, "", null, startTime, null);
        PageData<Event> events2 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink2);
        assertEquals(10, events2.getData().size());

        TimePageLink pageLink3 = new TimePageLink(30, 0, "", null, startTime, endTime);
        PageData<Event> events3 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink3);
        assertEquals(10, events3.getData().size());

        TimePageLink pageLink4 = new TimePageLink(5, 0, "", null, startTime, endTime);
        PageData<Event> events4 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink4);
        assertEquals(5, events4.getData().size());

        pageLink4 = pageLink4.nextPageLink();
        PageData<Event> events5 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink4);
        assertEquals(5, events5.getData().size());

        pageLink4 = pageLink4.nextPageLink();
        PageData<Event> events6 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink4);
        assertEquals(0, events6.getData().size());

    }

    @Test
    public void findEventsByEntityIdAndEventTypeAndPageLink() {
        UUID tenantId = Uuids.timeBased();
        UUID entityId1 = Uuids.timeBased();
        UUID entityId2 = Uuids.timeBased();
        long startTime = System.currentTimeMillis();
        long endTime = createEventsTwoEntitiesTwoTypes(tenantId, entityId1, entityId2, startTime, 20);

        TimePageLink pageLink1 = new TimePageLink(30);
        PageData<Event> events1 = eventDao.findEvents(tenantId, new DeviceId(entityId1), ALARM, pageLink1);
        assertEquals(5, events1.getData().size());

        TimePageLink pageLink2 = new TimePageLink(30, 0, "", null, startTime, null);
        PageData<Event> events2 = eventDao.findEvents(tenantId, new DeviceId(entityId1), ALARM, pageLink2);
        assertEquals(5, events2.getData().size());

        TimePageLink pageLink3 = new TimePageLink(30, 0, "", null, startTime, endTime);
        PageData<Event> events3 = eventDao.findEvents(tenantId, new DeviceId(entityId1), ALARM, pageLink3);
        assertEquals(5, events3.getData().size());

        TimePageLink pageLink4 = new TimePageLink(4, 0, "", null, startTime, endTime);
        PageData<Event> events4 = eventDao.findEvents(tenantId, new DeviceId(entityId1), ALARM, pageLink4);
        assertEquals(4, events4.getData().size());

        pageLink4 = pageLink4.nextPageLink();
        PageData<Event> events5 = eventDao.findEvents(tenantId, new DeviceId(entityId1), ALARM, pageLink4);
        assertEquals(2, events5.getData().size());
    }

    private long createEventsTwoEntitiesTwoTypes(UUID tenantId, UUID entityId1, UUID entityId2, long startTime, int count) {
        for (int i = 0; i < count / 2; i++) {
            String type = i % 2 == 0 ? STATS : ALARM;
            UUID eventId1 = Uuids.timeBased();
            Event event1 = getEvent(eventId1, tenantId, entityId1, type);
            eventDao.save(new TenantId(tenantId), event1);
            UUID eventId2 = Uuids.timeBased();
            Event event2 = getEvent(eventId2, tenantId, entityId2, type);
            eventDao.save(new TenantId(tenantId), event2);
        }
        return System.currentTimeMillis();
    }

    private long createEventsTwoEntities(UUID tenantId, UUID entityId1, UUID entityId2, long startTime, int count) {
        for (int i = 0; i < count / 2; i++) {
            UUID eventId1 = Uuids.timeBased();
            Event event1 = getEvent(eventId1, tenantId, entityId1);
            eventDao.save(new TenantId(tenantId), event1);
            UUID eventId2 = Uuids.timeBased();
            Event event2 = getEvent(eventId2, tenantId, entityId2);
            eventDao.save(new TenantId(tenantId), event2);
        }
        return System.currentTimeMillis();
    }

    private Event getEvent(UUID eventId, UUID tenantId, UUID entityId, String type) {
        Event event = getEvent(eventId, tenantId, entityId);
        event.setType(type);
        return event;
    }

    private Event getEvent(UUID eventId, UUID tenantId, UUID entityId) {
        Event event = new Event();
        event.setId(new EventId(eventId));
        event.setTenantId(new TenantId(tenantId));
        EntityId deviceId = new DeviceId(entityId);
        event.setEntityId(deviceId);
        event.setUid(event.getId().getId().toString());
        event.setType(STATS);
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readTree("{\"key\":\"value\"}");
            event.setBody(jsonNode);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return event;
    }
}
