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
package org.thingsboard.server.dao.sql.event;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.event.EventDao;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/5/2017.
 */
@Slf4j
public class JpaBaseEventDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private EventDao eventDao;
    UUID tenantId = Uuids.timeBased();

    @After
    public void deleteEvents() {
        throw new RuntimeException("fix me!");
//        List<EventInfo> events = eventDao.find(TenantId.fromUUID(tenantId));
//        for (EventInfo event : events) {
//            eventDao.removeById(TenantId.fromUUID(tenantId), event.getUuidId());
//        }
    }

//    @Test
//    public void findEvent() {
//        UUID entityId = Uuids.timeBased();
//        EventInfo savedEvent = eventDao.save(TenantId.fromUUID(tenantId), getEvent(entityId, tenantId, entityId));
//        EventInfo foundEvent = eventDao.findEvent(tenantId, new DeviceId(entityId), DataConstants.STATS, savedEvent.getUid());
//        assertNotNull("Event expected to be not null", foundEvent);
//        assertEquals(savedEvent.getId(), foundEvent.getId());
//    }
//
//    @Test
//    public void findEventsByEntityIdAndPageLink() throws Exception {
//        UUID entityId1 = Uuids.timeBased();
//        UUID entityId2 = Uuids.timeBased();
//        long startTime = System.currentTimeMillis();
//        long endTime = createEventsTwoEntities(tenantId, entityId1, entityId2, 20);
//
//        TimePageLink pageLink1 = new TimePageLink(30);
//        PageData<EventInfo> events1 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink1);
//        assertEquals(10, events1.getData().size());
//
//        TimePageLink pageLink2 = new TimePageLink(30, 0, "", null, startTime, null);
//        PageData<EventInfo> events2 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink2);
//        assertEquals(10, events2.getData().size());
//
//        TimePageLink pageLink3 = new TimePageLink(30, 0, "", null, startTime, endTime);
//        PageData<EventInfo> events3 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink3);
//        assertEquals(10, events3.getData().size());
//
//        TimePageLink pageLink4 = new TimePageLink(5, 0, "", null, startTime, endTime);
//        PageData<EventInfo> events4 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink4);
//        assertEquals(5, events4.getData().size());
//
//        pageLink4 = pageLink4.nextPageLink();
//        PageData<EventInfo> events5 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink4);
//        assertEquals(5, events5.getData().size());
//
//        pageLink4 = pageLink4.nextPageLink();
//        PageData<EventInfo> events6 = eventDao.findEvents(tenantId, new DeviceId(entityId1), pageLink4);
//        assertEquals(0, events6.getData().size());
//
//    }
//
//    @Test
//    public void findEventsByEntityIdAndEventTypeAndPageLink() throws Exception {
//        UUID entityId1 = Uuids.timeBased();
//        UUID entityId2 = Uuids.timeBased();
//        long startTime = System.currentTimeMillis();
//        long endTime = createEventsTwoEntitiesTwoTypes(tenantId, entityId1, entityId2, 20);
//
//        TimePageLink pageLink1 = new TimePageLink(30);
//        PageData<EventInfo> events1 = eventDao.findEvents(tenantId, new DeviceId(entityId1), ALARM, pageLink1);
//        assertEquals(5, events1.getData().size());
//
//        TimePageLink pageLink2 = new TimePageLink(30, 0, "", null, startTime, null);
//        PageData<EventInfo> events2 = eventDao.findEvents(tenantId, new DeviceId(entityId1), ALARM, pageLink2);
//        assertEquals(5, events2.getData().size());
//
//        TimePageLink pageLink3 = new TimePageLink(30, 0, "", null, startTime, endTime);
//        PageData<EventInfo> events3 = eventDao.findEvents(tenantId, new DeviceId(entityId1), ALARM, pageLink3);
//        assertEquals(5, events3.getData().size());
//
//        TimePageLink pageLink4 = new TimePageLink(4, 0, "", null, startTime, endTime);
//        PageData<EventInfo> events4 = eventDao.findEvents(tenantId, new DeviceId(entityId1), ALARM, pageLink4);
//        assertEquals(4, events4.getData().size());
//
//        pageLink4 = pageLink4.nextPageLink();
//        PageData<EventInfo> events5 = eventDao.findEvents(tenantId, new DeviceId(entityId1), ALARM, pageLink4);
//        assertEquals(1, events5.getData().size());
//    }
//
//    private long createEventsTwoEntitiesTwoTypes(UUID tenantId, UUID entityId1, UUID entityId2, int count) throws Exception {
//        for (int i = 0; i < count / 2; i++) {
//            String type = i % 2 == 0 ? STATS : ALARM;
//            UUID eventId1 = Uuids.timeBased();
//            EventInfo event1 = getEvent(eventId1, tenantId, entityId1, type);
//            eventDao.saveAsync(event1).get();
//            UUID eventId2 = Uuids.timeBased();
//            EventInfo event2 = getEvent(eventId2, tenantId, entityId2, type);
//            eventDao.saveAsync(event2).get();
//        }
//        return System.currentTimeMillis();
//    }
//
//    private long createEventsTwoEntities(UUID tenantId, UUID entityId1, UUID entityId2, int count) throws Exception {
//        for (int i = 0; i < count / 2; i++) {
//            UUID eventId1 = Uuids.timeBased();
//            EventInfo event1 = getEvent(eventId1, tenantId, entityId1);
//            eventDao.saveAsync(event1).get();
//            UUID eventId2 = Uuids.timeBased();
//            EventInfo event2 = getEvent(eventId2, tenantId, entityId2);
//            eventDao.saveAsync(event2).get();
//        }
//        return System.currentTimeMillis();
//    }
//
//    private EventInfo getEvent(UUID eventId, UUID tenantId, UUID entityId, String type) {
//        EventInfo event = getEvent(eventId, tenantId, entityId);
//        event.setType(type);
//        return event;
//    }
//
//    private EventInfo getEvent(UUID eventId, UUID tenantId, UUID entityId) {
//        EventInfo event = new EventInfo();
//        event.setId(new EventId(eventId));
//        event.setTenantId(TenantId.fromUUID(tenantId));
//        EntityId deviceId = new DeviceId(entityId);
//        event.setEntityId(deviceId);
//        event.setUid(event.getId().getId().toString());
//        event.setType(STATS);
//        ObjectMapper mapper = new ObjectMapper();
//        try {
//            JsonNode jsonNode = mapper.readTree("{\"key\":\"value\"}");
//            event.setBody(jsonNode);
//        } catch (IOException e) {
//            log.error(e.getMessage(), e);
//        }
//        return event;
//    }
}
