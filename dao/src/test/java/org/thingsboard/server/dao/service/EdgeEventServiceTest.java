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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.thingsboard.server.dao.edge.EdgeEventDao;
import org.thingsboard.server.dao.edge.EdgeEventService;

import java.io.IOException;
import java.text.ParseException;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT;

@DaoSqlTest
public class EdgeEventServiceTest extends AbstractServiceTest {

    @Autowired
    EdgeEventService edgeEventService;

    @Autowired
    EdgeEventDao edgeEventDao;

    long timeBeforeStartTime;
    long startTime;
    long eventTime;
    long endTime;
    long timeAfterEndTime;

    @Before
    public void before() throws ParseException {
        timeBeforeStartTime = ISO_8601_EXTENDED_DATETIME_FORMAT.parse("2016-11-01T11:30:00").getTime();
        startTime = ISO_8601_EXTENDED_DATETIME_FORMAT.parse("2016-11-01T12:00:00").getTime();
        eventTime = ISO_8601_EXTENDED_DATETIME_FORMAT.parse("2016-11-01T12:30:00").getTime();
        endTime = ISO_8601_EXTENDED_DATETIME_FORMAT.parse("2016-11-01T13:00:00").getTime();
        timeAfterEndTime = ISO_8601_EXTENDED_DATETIME_FORMAT.parse("2016-11-01T13:30:30").getTime();
    }

    @Test
    public void saveEdgeEvent() throws Exception {
        EdgeId edgeId = new EdgeId(Uuids.timeBased());
        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        EdgeEvent edgeEvent = generateEdgeEvent(tenantId, edgeId, deviceId);
        edgeEventService.saveAsync(edgeEvent).get();

        PageData<EdgeEvent> edgeEvents = edgeEventService.findEdgeEvents(tenantId, edgeId, 0L, null, new TimePageLink(1));
        Assert.assertFalse(edgeEvents.getData().isEmpty());

        EdgeEvent saved = edgeEvents.getData().get(0);
        Assert.assertEquals(saved.getTenantId(), edgeEvent.getTenantId());
        Assert.assertEquals(saved.getEdgeId(), edgeEvent.getEdgeId());
        Assert.assertEquals(saved.getEntityId(), edgeEvent.getEntityId());
        Assert.assertEquals(saved.getType(), edgeEvent.getType());
        Assert.assertEquals(saved.getAction(), edgeEvent.getAction());
        Assert.assertEquals(saved.getBody(), edgeEvent.getBody());

        edgeEventDao.cleanupEvents(1);
    }

    protected EdgeEvent generateEdgeEvent(TenantId tenantId, EdgeId edgeId, EntityId entityId) throws IOException {
        if (tenantId == null) {
            tenantId = TenantId.fromUUID(Uuids.timeBased());
        }
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setEntityId(entityId.getId());
        edgeEvent.setType(EdgeEventType.DEVICE);
        edgeEvent.setAction(EdgeEventActionType.ADDED);
        edgeEvent.setBody(readFromResource("TestJsonData.json"));
        return edgeEvent;
    }

    @Test
    public void findEdgeEventsBySeqIdOrder_createdTimeOrderIgnored() throws Exception {
        EdgeId edgeId = new EdgeId(Uuids.timeBased());
        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        saveEdgeEventWithProvidedTime(timeBeforeStartTime, edgeId, deviceId, tenantId).get();
        saveEdgeEventWithProvidedTime(eventTime, edgeId, deviceId, tenantId).get();
        saveEdgeEventWithProvidedTime(eventTime + 2, edgeId, deviceId, tenantId).get();
        saveEdgeEventWithProvidedTime(eventTime + 1, edgeId, deviceId, tenantId).get();
        saveEdgeEventWithProvidedTime(timeAfterEndTime, edgeId, deviceId, tenantId).get();

        TimePageLink pageLink = new TimePageLink(2, 0, "", new SortOrder("createdTime", SortOrder.Direction.DESC), startTime, endTime);
        PageData<EdgeEvent> edgeEvents = edgeEventService.findEdgeEvents(tenantId, edgeId, 0L, null, pageLink);

        Assert.assertNotNull(edgeEvents.getData());
        Assert.assertEquals(2, edgeEvents.getData().size());
        Assert.assertEquals(Uuids.startOf(eventTime), edgeEvents.getData().get(0).getUuidId());
        Assert.assertEquals(Uuids.startOf(eventTime + 2), edgeEvents.getData().get(1).getUuidId());
        Assert.assertTrue(edgeEvents.hasNext());
        Assert.assertNotNull(pageLink.nextPageLink());

        edgeEvents = edgeEventService.findEdgeEvents(tenantId, edgeId, 0L, null, pageLink.nextPageLink());

        Assert.assertNotNull(edgeEvents.getData());
        Assert.assertEquals(1, edgeEvents.getData().size());
        Assert.assertEquals(Uuids.startOf(eventTime + 1), edgeEvents.getData().get(0).getUuidId());
        Assert.assertFalse(edgeEvents.hasNext());

        edgeEventDao.cleanupEvents(1);
    }

    private ListenableFuture<Void> saveEdgeEventWithProvidedTime(long time, EdgeId edgeId, EntityId entityId, TenantId tenantId) throws Exception {
        EdgeEvent edgeEvent = generateEdgeEvent(tenantId, edgeId, entityId);
        edgeEvent.setId(new EdgeEventId(Uuids.startOf(time)));
        return edgeEventService.saveAsync(edgeEvent);
    }

}