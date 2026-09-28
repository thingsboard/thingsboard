// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.api.ThingsboardApi.DeleteSchedulerEventArgs;
import org.thingsboard.client.api.ThingsboardApi.EnableSchedulerEventArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAllSchedulerEventsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetScheduledReportEventsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetSchedulerEventByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetSchedulerEventInfoByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetSchedulerEventsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetSchedulerEventsByIdsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetSchedulerEventsByRangeArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveSchedulerEventArgs;
import org.thingsboard.client.model.PageDataScheduledReportInfo;
import org.thingsboard.client.model.PageDataSchedulerEventWithCustomerInfo;
import org.thingsboard.client.model.SchedulerEvent;
import org.thingsboard.client.model.SchedulerEventInfo;
import org.thingsboard.client.model.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class SchedulerEventApiClientTest extends AbstractApiClientTest {

    private static final String EVENT_TYPE = "testEventType";

    @Test
    public void testSchedulerEventLifecycle() throws Exception {
        long ts = System.currentTimeMillis();
        String name = TEST_PREFIX + ts;

        SchedulerEvent saved = createEvent(name);
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(name, saved.getName());
        assertEquals(EVENT_TYPE, saved.getType());
        String eventId = saved.getId().getId().toString();

        SchedulerEvent fetched = client.getSchedulerEventById(GetSchedulerEventByIdArgs.builder()
                .schedulerEventId(eventId)
                .build());
        assertNotNull(fetched);
        assertEquals(eventId, fetched.getId().getId().toString());
        assertEquals(name, fetched.getName());

        SchedulerEventWithCustomerInfo info = client.getSchedulerEventInfoById(GetSchedulerEventInfoByIdArgs.builder()
                .schedulerEventId(eventId)
                .build());
        assertNotNull(info);
        assertEquals(eventId, info.getId().getId().toString());
        assertEquals(name, info.getName());

        fetched.setName(name + "_updated");
        SchedulerEvent updated = client.saveSchedulerEvent(SaveSchedulerEventArgs.builder()
                .schedulerEvent(fetched)
                .build());
        assertEquals(name + "_updated", updated.getName());

        SchedulerEvent disabled = client.enableSchedulerEvent(EnableSchedulerEventArgs.builder()
                .schedulerEventId(eventId)
                .enabledValue(false)
                .build());
        assertNotNull(disabled);
        assertEquals(Boolean.FALSE, disabled.getEnabled());

        SchedulerEvent enabled = client.enableSchedulerEvent(EnableSchedulerEventArgs.builder()
                .schedulerEventId(eventId)
                .enabledValue(true)
                .build());
        assertNotNull(enabled);
        assertEquals(Boolean.TRUE, enabled.getEnabled());

        client.deleteSchedulerEvent(DeleteSchedulerEventArgs.builder()
                .schedulerEventId(eventId)
                .build());
        assertReturns404(() -> client.getSchedulerEventById(GetSchedulerEventByIdArgs.builder()
                .schedulerEventId(eventId)
                .build()));
    }

    @Test
    public void testGetAllSchedulerEventsV2() throws Exception {
        long ts = System.currentTimeMillis();

        SchedulerEvent e1 = createEvent(TEST_PREFIX + ts + "_1");
        SchedulerEvent e2 = createEvent(TEST_PREFIX + ts + "_2");
        String id1 = e1.getId().getId().toString();
        String id2 = e2.getId().getId().toString();

        List<SchedulerEventWithCustomerInfo> all = client.getAllSchedulerEvents(GetAllSchedulerEventsArgs.builder()

                .build());
        assertNotNull(all);
        assertTrue(all.stream().anyMatch(e -> e.getId().getId().toString().equals(id1)));
        assertTrue(all.stream().anyMatch(e -> e.getId().getId().toString().equals(id2)));

        List<SchedulerEventWithCustomerInfo> filtered = client.getAllSchedulerEvents(GetAllSchedulerEventsArgs.builder()
                .type(EVENT_TYPE)
                .build());
        assertNotNull(filtered);
        assertTrue(filtered.stream().anyMatch(e -> e.getId().getId().toString().equals(id1)));

        client.deleteSchedulerEvent(DeleteSchedulerEventArgs.builder()
                .schedulerEventId(id1)
                .build());
        client.deleteSchedulerEvent(DeleteSchedulerEventArgs.builder()
                .schedulerEventId(id2)
                .build());
    }

    @Test
    public void testGetSchedulerEvents() throws Exception {
        long ts = System.currentTimeMillis();

        List<String> createdIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            SchedulerEvent e = createEvent(TEST_PREFIX + ts + "_" + i);
            createdIds.add(e.getId().getId().toString());
        }

        PageDataSchedulerEventWithCustomerInfo page =
                client.getSchedulerEvents(GetSchedulerEventsArgs.builder()
                        .pageSize(100)
                        .page(0)
                        .type(EVENT_TYPE)
                        .build());
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 3);
        for (String id : createdIds) {
            assertTrue(page.getData().stream()
                    .anyMatch(e -> e.getId().getId().toString().equals(id)));
        }

        for (String id : createdIds) client.deleteSchedulerEvent(DeleteSchedulerEventArgs.builder()
                .schedulerEventId(id)
                .build());
    }

    @Test
    public void testGetSchedulerEventsByRange() throws Exception {
        long ts = System.currentTimeMillis();

        SchedulerEvent event = createEvent(TEST_PREFIX + ts);
        String eventId = event.getId().getId().toString();

        List<SchedulerEventWithCustomerInfo> range =
                client.getSchedulerEventsByRange(GetSchedulerEventsByRangeArgs.builder()
                        .startTime(0L)
                        .endTime(ts + 2 * 86_400_000L)
                        .type(EVENT_TYPE)
                        .build());
        assertNotNull(range);
        assertTrue(range.stream().anyMatch(e -> e.getId().getId().toString().equals(eventId)));

        client.deleteSchedulerEvent(DeleteSchedulerEventArgs.builder()
                .schedulerEventId(eventId)
                .build());
    }

    @Test
    public void testGetScheduledReportEvents() throws Exception {
        PageDataScheduledReportInfo page =
                client.getScheduledReportEvents(GetScheduledReportEventsArgs.builder()
                        .pageSize("100")
                        .page("0")
                        .build());
        assertNotNull(page);
        assertNotNull(page.getData());
    }

    @Test
    public void testGetSchedulerEventsByIdsV2() throws Exception {
        long ts = System.currentTimeMillis();

        SchedulerEvent e1 = createEvent(TEST_PREFIX + ts + "_a");
        SchedulerEvent e2 = createEvent(TEST_PREFIX + ts + "_b");
        String id1 = e1.getId().getId().toString();
        String id2 = e2.getId().getId().toString();

        List<SchedulerEventInfo> result = client.getSchedulerEventsByIds(GetSchedulerEventsByIdsArgs.builder()
                .schedulerEventIds(List.of(id1, id2))
                .build());
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e -> e.getId().getId().toString().equals(id1)));
        assertTrue(result.stream().anyMatch(e -> e.getId().getId().toString().equals(id2)));

        client.deleteSchedulerEvent(DeleteSchedulerEventArgs.builder()
                .schedulerEventId(id1)
                .build());
        client.deleteSchedulerEvent(DeleteSchedulerEventArgs.builder()
                .schedulerEventId(id2)
                .build());
    }

    @Test
    public void testGetSchedulerEventByIdNotFound() {
        assertReturns404(() -> client.getSchedulerEventById(GetSchedulerEventByIdArgs.builder()
                .schedulerEventId(UUID.randomUUID().toString())
                .build()));
    }

    private static com.fasterxml.jackson.databind.JsonNode buildSchedule() {
        ObjectNode schedule = OBJECT_MAPPER.createObjectNode();
        schedule.put("type", "ONE_TIME");
        schedule.put("startTime", System.currentTimeMillis() + 86_400_000L);
        schedule.put("timezone", "UTC");
        return schedule;
    }

    private SchedulerEvent buildEvent(String name) {
        SchedulerEvent event = new SchedulerEvent();
        event.setName(name);
        event.setType(EVENT_TYPE);
        event.setSchedule(buildSchedule());
        event.setConfiguration(OBJECT_MAPPER.createObjectNode());
        return event;
    }

    private SchedulerEvent createEvent(String name) throws ApiException {
        return client.saveSchedulerEvent(SaveSchedulerEventArgs.builder()
                .schedulerEvent(buildEvent(name))
                .build());
    }

}
