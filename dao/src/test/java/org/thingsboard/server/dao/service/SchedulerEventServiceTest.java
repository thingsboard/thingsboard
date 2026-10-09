// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;

import java.util.ArrayList;
import java.util.List;

@DaoSqlTest
public class SchedulerEventServiceTest extends AbstractServiceTest {

    @Autowired
    EdgeService edgeService;
    @Autowired
    SchedulerEventService schedulerEventService;

    private final IdComparator<SchedulerEvent> idComparator = new IdComparator<>();

    @Test
    public void testFindEdgeSchedulerEventsByTenantIdAndName() {
        Edge edge = constructEdge(tenantId, "My edge", "default");
        Edge savedEdge = edgeService.saveEdge(edge);

        String name1 = "Edge Scheduler Event name 1";
        List<SchedulerEvent> schedulerEventsName1 = new ArrayList<>();
        for (int i = 0; i < 123; i++) {
            SchedulerEvent schedulerEvent = createSchedulerEvent();
            schedulerEvent.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = name1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            schedulerEvent.setName(name);
            schedulerEventsName1.add(schedulerEventService.saveSchedulerEvent(schedulerEvent));
        }
        schedulerEventsName1.forEach(schedulerEvent ->
                schedulerEventService.assignSchedulerEventToEdge(tenantId, schedulerEvent.getId(), savedEdge.getId()));

        String name2 = "Edge Scheduler Event name 2";
        List<SchedulerEvent> schedulerEventsName2 = new ArrayList<>();
        for (int i = 0; i < 193; i++) {
            SchedulerEvent schedulerEvent = createSchedulerEvent();
            schedulerEvent.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = name2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            schedulerEvent.setName(name);
            schedulerEventsName2.add(schedulerEventService.saveSchedulerEvent(schedulerEvent));
        }
        schedulerEventsName2.forEach(schedulerEvent ->
                schedulerEventService.assignSchedulerEventToEdge(tenantId, schedulerEvent.getId(), savedEdge.getId()));

        List<SchedulerEvent> loadedSchedulerEventsName1 = new ArrayList<>();
        PageLink pageLink = new PageLink(19, 0, name1);
        PageData<SchedulerEvent> pageData = null;
        do {
            pageData = schedulerEventService.findSchedulerEventsByTenantIdAndEdgeId(tenantId, savedEdge.getId(), pageLink);
            loadedSchedulerEventsName1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        schedulerEventsName1.sort(idComparator);
        loadedSchedulerEventsName1.sort(idComparator);

        Assert.assertEquals(schedulerEventsName1, loadedSchedulerEventsName1);

        List<SchedulerEvent> loadedSchedulerEventsName2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, name2);
        do {
            pageData = schedulerEventService.findSchedulerEventsByTenantIdAndEdgeId(tenantId, savedEdge.getId(), pageLink);
            loadedSchedulerEventsName2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        schedulerEventsName2.sort(idComparator);
        loadedSchedulerEventsName2.sort(idComparator);

        Assert.assertEquals(schedulerEventsName2, loadedSchedulerEventsName2);

        for (SchedulerEvent schedulerEvent : loadedSchedulerEventsName1) {
            schedulerEventService.deleteSchedulerEvent(tenantId, schedulerEvent.getId());
        }

        pageLink = new PageLink(4, 0, name1);
        pageData = schedulerEventService.findSchedulerEventsByTenantIdAndEdgeId(tenantId, savedEdge.getId(), pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (SchedulerEvent schedulerEvent : loadedSchedulerEventsName2) {
            schedulerEventService.deleteSchedulerEvent(tenantId, schedulerEvent.getId());
        }

        pageLink = new PageLink(4, 0, name2);
        pageData = schedulerEventService.findSchedulerEventsByTenantIdAndEdgeId(tenantId, savedEdge.getId(), pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    private SchedulerEvent createSchedulerEvent() {
        SchedulerEvent schedulerEvent = new SchedulerEvent();
        schedulerEvent.setName("Scheduler Event");
        schedulerEvent.setType("Custom Type");
        ObjectNode schedule = JacksonUtil.newObjectNode();
        schedule.put("startTime", System.currentTimeMillis());
        schedule.put("timezone", "UTC");
        schedulerEvent.setSchedule(schedule);
        schedulerEvent.setConfiguration(JacksonUtil.newObjectNode());
        return schedulerEvent;
    }

}
