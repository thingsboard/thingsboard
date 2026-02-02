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
package org.thingsboard.server.dao.service.event;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.event.RuleNodeDebugEvent;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.text.ParseException;
import java.util.List;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT;

public abstract class BaseEventServiceTest extends AbstractServiceTest {

    @Autowired
    EventService eventService;

    long timeBeforeStartTime;
    long startTime;
    long eventTime;
    long endTime;
    long timeAfterEndTime;

    @Before
    public void before() throws ParseException {
        timeBeforeStartTime = ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T11:30:00Z").getTime();
        startTime = ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T12:00:00Z").getTime();
        eventTime = ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T12:30:00Z").getTime();
        endTime = ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T13:00:00Z").getTime();
        timeAfterEndTime = ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T13:30:30Z").getTime();
    }

    @Test
    public void saveEvent() throws Exception {
        TenantId tenantId = new TenantId(Uuids.timeBased());
        DeviceId devId = new DeviceId(Uuids.timeBased());
        RuleNodeDebugEvent event = generateEvent(tenantId, devId);
        eventService.saveAsync(event).get();
        List<EventInfo> loaded = eventService.findLatestEvents(event.getTenantId(), devId, event.getType(), 1);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(1, loaded.size());
        Assert.assertEquals(event.getData(), loaded.get(0).getBody().get("data").asText());
    }

    @Test
    public void findEventsByTypeAndTimeAscOrder() throws Exception {
        CustomerId customerId = new CustomerId(Uuids.timeBased());
        TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
        saveEventWithProvidedTime(timeBeforeStartTime, customerId, tenantId);
        Event savedEvent = saveEventWithProvidedTime(eventTime, customerId, tenantId);
        Event savedEvent2 = saveEventWithProvidedTime(eventTime + 1, customerId, tenantId);
        Event savedEvent3 = saveEventWithProvidedTime(eventTime + 2, customerId, tenantId);
        saveEventWithProvidedTime(timeAfterEndTime, customerId, tenantId);

        TimePageLink timePageLink = new TimePageLink(2, 0, "", new SortOrder("ts"), startTime, endTime);

        PageData<EventInfo> events = eventService.findEvents(tenantId, customerId, EventType.DEBUG_RULE_NODE, timePageLink);

        Assert.assertNotNull(events.getData());
        Assert.assertEquals(2, events.getData().size());
        Assert.assertEquals(savedEvent.getUuidId(), events.getData().get(0).getUuidId());
        Assert.assertEquals(savedEvent2.getUuidId(), events.getData().get(1).getUuidId());
        Assert.assertTrue(events.hasNext());

        events = eventService.findEvents(tenantId, customerId, EventType.DEBUG_RULE_NODE, timePageLink.nextPageLink());

        Assert.assertNotNull(events.getData());
        Assert.assertEquals(1, events.getData().size());
        Assert.assertEquals(savedEvent3.getUuidId(), events.getData().get(0).getUuidId());
        Assert.assertFalse(events.hasNext());

        eventService.cleanupEvents(timeBeforeStartTime - 1, timeAfterEndTime + 1, true);
    }

    @Test
    public void findEventsByTypeAndTimeDescOrder() throws Exception {
        CustomerId customerId = new CustomerId(Uuids.timeBased());
        TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
        saveEventWithProvidedTime(timeBeforeStartTime, customerId, tenantId);
        Event savedEvent = saveEventWithProvidedTime(eventTime, customerId, tenantId);
        Event savedEvent2 = saveEventWithProvidedTime(eventTime + 1, customerId, tenantId);
        Event savedEvent3 = saveEventWithProvidedTime(eventTime + 2, customerId, tenantId);
        saveEventWithProvidedTime(timeAfterEndTime, customerId, tenantId);

        TimePageLink timePageLink = new TimePageLink(2, 0, "", new SortOrder("ts", SortOrder.Direction.DESC), startTime, endTime);

        PageData<EventInfo> events = eventService.findEvents(tenantId, customerId, EventType.DEBUG_RULE_NODE, timePageLink);

        Assert.assertNotNull(events.getData());
        Assert.assertEquals(2, events.getData().size());
        Assert.assertEquals(savedEvent3.getUuidId(), events.getData().get(0).getUuidId());
        Assert.assertEquals(savedEvent2.getUuidId(), events.getData().get(1).getUuidId());
        Assert.assertTrue(events.hasNext());

        events = eventService.findEvents(tenantId, customerId, EventType.DEBUG_RULE_NODE, timePageLink.nextPageLink());

        Assert.assertNotNull(events.getData());
        Assert.assertEquals(1, events.getData().size());
        Assert.assertEquals(savedEvent.getUuidId(), events.getData().get(0).getUuidId());
        Assert.assertFalse(events.hasNext());

        eventService.cleanupEvents(timeBeforeStartTime - 1, timeAfterEndTime + 1, true);
    }

    @Test
    public void findLatestDebugRuleNodeInEvent() throws Exception {
        CustomerId customerId = new CustomerId(Uuids.timeBased());
        TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());

        Event event1 = saveEventWithProvidedTimeAndEventType(eventTime, "IN", customerId, tenantId);
        Event event2 = saveEventWithProvidedTimeAndEventType(eventTime + 1, "IN", customerId, tenantId);

        EventInfo event = eventService.findLatestDebugRuleNodeInEvent(tenantId, customerId);

        Assert.assertNotNull(event);
        Assert.assertEquals(event2.getUuidId(), event.getUuidId());

        eventService.cleanupEvents(timeBeforeStartTime - 1, timeAfterEndTime + 1, true);
    }

    private Event saveEventWithProvidedTime(long time, EntityId entityId, TenantId tenantId) throws Exception {
        return saveEventWithProvidedTimeAndEventType(time, null, entityId, tenantId);
    }

    private Event saveEventWithProvidedTimeAndEventType(long time, String eventType, EntityId entityId, TenantId tenantId) throws Exception {
        RuleNodeDebugEvent event = generateEvent(tenantId, entityId, eventType);
        event.setId(new EventId(Uuids.timeBased()));
        event.setCreatedTime(time);
        eventService.saveAsync(event).get();
        return event;
    }
}
