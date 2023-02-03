/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeEventId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;

public abstract class BaseEdgeEventServiceTest extends AbstractServiceTest {

    long timeBeforeStartTime;
    long startTime;
    long eventTime;
    long endTime;
    long timeAfterEndTime;

    @Before
    public void before() throws ParseException {
        timeBeforeStartTime = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T11:30:00Z").getTime();
        startTime = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T12:00:00Z").getTime();
        eventTime = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T12:30:00Z").getTime();
        endTime = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T13:00:00Z").getTime();
        timeAfterEndTime = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2016-11-01T13:30:30Z").getTime();
    }

    @Test
    public void saveEdgeEvent() throws Exception {
        EdgeId edgeId = new EdgeId(Uuids.timeBased());
        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        TenantId tenantId = new TenantId(Uuids.timeBased());
        EdgeEvent edgeEvent = generateEdgeEvent(tenantId, edgeId, deviceId, EdgeEventActionType.ADDED);
        edgeEventService.saveAsync(edgeEvent).get();

        PageData<EdgeEvent> edgeEvents = edgeEventService.findEdgeEvents(tenantId, edgeId, new TimePageLink(1), false);
        Assert.assertFalse(edgeEvents.getData().isEmpty());

        EdgeEvent saved = edgeEvents.getData().get(0);
        Assert.assertEquals(saved.getTenantId(), edgeEvent.getTenantId());
        Assert.assertEquals(saved.getEdgeId(), edgeEvent.getEdgeId());
        Assert.assertEquals(saved.getEntityId(), edgeEvent.getEntityId());
        Assert.assertEquals(saved.getType(), edgeEvent.getType());
        Assert.assertEquals(saved.getAction(), edgeEvent.getAction());
        Assert.assertEquals(saved.getBody(), edgeEvent.getBody());
    }

    protected EdgeEvent generateEdgeEvent(TenantId tenantId, EdgeId edgeId, EntityId entityId, EdgeEventActionType edgeEventAction) throws IOException {
        if (tenantId == null) {
            tenantId = TenantId.fromUUID(Uuids.timeBased());
        }
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setEntityId(entityId.getId());
        edgeEvent.setType(EdgeEventType.DEVICE);
        edgeEvent.setAction(edgeEventAction);
        edgeEvent.setBody(readFromResource("TestJsonData.json"));
        return edgeEvent;
    }

    @Test
    public void findEdgeEventsByTimeDescOrder() throws Exception {
        EdgeId edgeId = new EdgeId(Uuids.timeBased());
        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());

        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(saveEdgeEventWithProvidedTime(timeBeforeStartTime, edgeId, deviceId, tenantId));
        futures.add(saveEdgeEventWithProvidedTime(eventTime, edgeId, deviceId, tenantId));
        futures.add(saveEdgeEventWithProvidedTime(eventTime + 1, edgeId, deviceId, tenantId));
        futures.add(saveEdgeEventWithProvidedTime(eventTime + 2, edgeId, deviceId, tenantId));
        futures.add(saveEdgeEventWithProvidedTime(timeAfterEndTime, edgeId, deviceId, tenantId));

        Futures.allAsList(futures).get();

        TimePageLink pageLink = new TimePageLink(2, 0, "", new SortOrder("createdTime", SortOrder.Direction.DESC), startTime, endTime);
        PageData<EdgeEvent> edgeEvents = edgeEventService.findEdgeEvents(tenantId, edgeId, pageLink, true);

        Assert.assertNotNull(edgeEvents.getData());
        Assert.assertEquals(2, edgeEvents.getData().size());
        Assert.assertEquals(Uuids.startOf(eventTime + 2), edgeEvents.getData().get(0).getUuidId());
        Assert.assertEquals(Uuids.startOf(eventTime + 1), edgeEvents.getData().get(1).getUuidId());
        Assert.assertTrue(edgeEvents.hasNext());
        Assert.assertNotNull(pageLink.nextPageLink());

        edgeEvents = edgeEventService.findEdgeEvents(tenantId, edgeId, pageLink.nextPageLink(), true);

        Assert.assertNotNull(edgeEvents.getData());
        Assert.assertEquals(1, edgeEvents.getData().size());
        Assert.assertEquals(Uuids.startOf(eventTime), edgeEvents.getData().get(0).getUuidId());
        Assert.assertFalse(edgeEvents.hasNext());

        edgeEventService.cleanupEvents(1);
    }

    @Test
    public void findEdgeEventsWithTsUpdateAndWithout() throws Exception {
        EdgeId edgeId = new EdgeId(Uuids.timeBased());
        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
        TimePageLink pageLink = new TimePageLink(1, 0, null, new SortOrder("createdTime", SortOrder.Direction.ASC));

        EdgeEvent edgeEventWithTsUpdate = generateEdgeEvent(tenantId, edgeId, deviceId, EdgeEventActionType.TIMESERIES_UPDATED);
        edgeEventService.saveAsync(edgeEventWithTsUpdate).get();

        PageData<EdgeEvent> allEdgeEvents = edgeEventService.findEdgeEvents(tenantId, edgeId, pageLink, true);
        PageData<EdgeEvent> edgeEventsWithoutTsUpdate = edgeEventService.findEdgeEvents(tenantId, edgeId, pageLink, false);

        Assert.assertNotNull(allEdgeEvents.getData());
        Assert.assertNotNull(edgeEventsWithoutTsUpdate.getData());
        Assert.assertEquals(1, allEdgeEvents.getData().size());
        Assert.assertEquals(allEdgeEvents.getData().get(0).getUuidId(), edgeEventWithTsUpdate.getUuidId());
        Assert.assertTrue(edgeEventsWithoutTsUpdate.getData().isEmpty());
    }

    private ListenableFuture<Void> saveEdgeEventWithProvidedTime(long time, EdgeId edgeId, EntityId entityId, TenantId tenantId) throws Exception {
        EdgeEvent edgeEvent = generateEdgeEvent(tenantId, edgeId, entityId, EdgeEventActionType.ADDED);
        edgeEvent.setId(new EdgeEventId(Uuids.startOf(time)));
        return edgeEventService.saveAsync(edgeEvent);
    }
}