// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.data.report.ReportInfo;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;
import org.thingsboard.server.common.data.scheduler.MonthlyRepeat;
import org.thingsboard.server.common.data.scheduler.ScheduledReportInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class SchedulerEventControllerTest extends AbstractControllerTest {

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testSaveSchedulerEvent() throws Exception {
        SchedulerEvent schedulerEvent = createSchedulerEvent();
        SchedulerEvent savedSchedulerEvent = saveSchedulerEvent(schedulerEvent);
        Assert.assertNotNull(savedSchedulerEvent);
        Assert.assertNotNull(savedSchedulerEvent.getId());
        Assert.assertTrue(savedSchedulerEvent.getCreatedTime() > 0);
        Assert.assertEquals(schedulerEvent.getName(), savedSchedulerEvent.getName());
        savedSchedulerEvent.setName("New Scheduler Event");
        saveSchedulerEvent(savedSchedulerEvent);
        SchedulerEvent foundSchedulerEvent = doGet("/api/schedulerEvent/" + savedSchedulerEvent.getId().getId().toString(), SchedulerEvent.class);
        Assert.assertEquals(savedSchedulerEvent.getName(), foundSchedulerEvent.getName());
        Assert.assertTrue(savedSchedulerEvent.isEnabled());
        Assert.assertNotNull(savedSchedulerEvent.getVersion());
    }

    @Test
    public void testFindSchedulerEventById() throws Exception {
        SchedulerEvent schedulerEvent = createSchedulerEvent();
        SchedulerEvent savedSchedulerEvent = saveSchedulerEvent(schedulerEvent);
        SchedulerEvent foundSchedulerEvent = doGet("/api/schedulerEvent/" + savedSchedulerEvent.getId().getId().toString(), SchedulerEvent.class);
        Assert.assertNotNull(foundSchedulerEvent);
        Assert.assertEquals(savedSchedulerEvent, foundSchedulerEvent);
    }

    @Test
    public void testDeleteSchedulerEvent() throws Exception {
        SchedulerEvent schedulerEvent = createSchedulerEvent();
        SchedulerEvent savedSchedulerEvent = saveSchedulerEvent(schedulerEvent);

        doDelete("/api/schedulerEvent/" + savedSchedulerEvent.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/schedulerEvent/" + savedSchedulerEvent.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindSchedulerEvents() throws Exception {
        List<SchedulerEventId> tenantSchedulerEvents = new ArrayList<>();
        List<SchedulerEventId> customerSchedulerEvents = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            SchedulerEvent schedulerEvent = createSchedulerEvent();
            schedulerEvent.setName("event-" + i);
            schedulerEvent.setType("tenantType");
            tenantSchedulerEvents.add(saveSchedulerEvent(schedulerEvent).getId());
        }
        loginCustomerAdminUser();
        for (int i = 6; i <= 10; i++) {
            SchedulerEvent schedulerEvent = createSchedulerEvent();
            schedulerEvent.setName("event-" + i);
            schedulerEvent.setType("customerType");
            customerSchedulerEvents.add(saveSchedulerEvent(schedulerEvent).getId());
        }
        SchedulerEventId[] allSchedulerEvents = Stream.concat(tenantSchedulerEvents.stream(), customerSchedulerEvents.stream())
                .toArray(SchedulerEventId[]::new);

        List<SchedulerEventWithCustomerInfo> events = findSchedulerEvents(null, null);
        assertThat(events).as("all customer events").extracting(SchedulerEventInfo::getId)
                .containsExactlyInAnyOrderElementsOf(customerSchedulerEvents);
        events = findSchedulerEvents("customerType", null);
        assertThat(events).as("customer events with customerType").extracting(SchedulerEventInfo::getId)
                .containsExactlyInAnyOrderElementsOf(customerSchedulerEvents);
        assertThat(events).allSatisfy(event -> {
            assertThat(event.getCustomerTitle()).isEqualTo("Customer");
        });

        loginTenantAdmin();
        events = findSchedulerEvents(null, null);
        assertThat(events).as("all tenant events").extracting(SchedulerEventInfo::getId)
                .containsOnly(allSchedulerEvents);
        events = findSchedulerEvents("tenantType", null);
        assertThat(events).as("tenant events with tenantType").extracting(SchedulerEventInfo::getId)
                .containsExactlyInAnyOrderElementsOf(tenantSchedulerEvents);
        events = findSchedulerEvents("customerType", null);
        assertThat(events).as("tenant events with customerType").extracting(SchedulerEventInfo::getId)
                .containsExactlyInAnyOrderElementsOf(customerSchedulerEvents);

        events = findSchedulerEvents(null, "unknown");
        assertThat(events).as("events with search 'unknown'").isEmpty();
        events = findSchedulerEvents(null, "event-2");
        assertThat(events).as("events with search 'event-2'").singleElement().extracting(SchedulerEventInfo::getId)
                .isEqualTo(tenantSchedulerEvents.get(1));
        events = findSchedulerEvents(null, "Type");
        assertThat(events).as("events with search 'Type'").extracting(SchedulerEventInfo::getId)
                .containsOnly(allSchedulerEvents);
        events = findSchedulerEvents(null, "customer");
        assertThat(events).as("events with search 'customer'").extracting(SchedulerEventInfo::getId)
                .containsExactlyInAnyOrderElementsOf(customerSchedulerEvents);

        long weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
        events = findSchedulerEvents(null, weekAgo, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30 + 7), null);
        assertThat(events).as("events from week ago to next month").extracting(SchedulerEventInfo::getId)
                .containsOnly(allSchedulerEvents);
        assertThat(events).allSatisfy(event -> {
            assertThat(event.getTimestamps()).hasSize(2);
            long startTime = event.getSchedule().get("startTime").asLong();
            assertThat(event.getTimestamps().get(0)).as("first event time").isEqualTo(startTime);

            long nextEventTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault()).plusMonths(1)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            assertThat(event.getTimestamps().get(1)).as("second event time").isEqualTo(nextEventTime);
        });

        long twoWeeksAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14);
        events = findSchedulerEvents(null, twoWeeksAgo, weekAgo, null);
        assertThat(events).as("events from two weeks ago to one week ago").isEmpty();
    }

    @Test
    public void testFindEdgeSchedulerEventInfosByTenantIdAndName() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        List<SchedulerEventId> edgeSchedulerEvents = new ArrayList<>();
        for (int i = 0; i < 28; i++) {
            SchedulerEvent schedulerEvent = createSchedulerEvent();
            schedulerEvent.setName("Scheduler Event " + i);
            SchedulerEvent savedSchedulerEvent = saveSchedulerEvent(schedulerEvent);
            doPost("/api/edge/" + savedEdge.getId().getId().toString()
                    + "/schedulerEvent/" + savedSchedulerEvent.getId().getId().toString(), SchedulerEvent.class);
            edgeSchedulerEvents.add(savedSchedulerEvent.getId());
        }

        List<SchedulerEventId> loadedEdgeSchedulerEvents = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<SchedulerEventWithCustomerInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/schedulerEvents?edgeId=" + savedEdge.getId().getId() + "&",
                    new TypeReference<>() {
                    }, pageLink);
            loadedEdgeSchedulerEvents.addAll(pageData.getData().stream().map(IdBased::getId).toList());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertTrue(edgeSchedulerEvents.size() == loadedEdgeSchedulerEvents.size() &&
                edgeSchedulerEvents.containsAll(loadedEdgeSchedulerEvents));

        for (SchedulerEventId schedulerEventId : loadedEdgeSchedulerEvents) {
            doDelete("/api/edge/" + savedEdge.getId().getId().toString()
                    + "/schedulerEvent/" + schedulerEventId.getId().toString(), SchedulerEventInfo.class);
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/schedulerEvents?edgeId=" + savedEdge.getId().getId() + "&",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getTotalElements());
    }

    @Test
    public void testScheduleReportGenerationByTenantAdmin() throws Exception {
        loginTenantAdmin();
        testScheduleReportGeneration(tenantAdminUserId, false);
    }

    @Test
    public void testScheduleReportGenerationByCustomerUser() throws Exception {
        loginCustomerAdminUser();
        testScheduleReportGeneration(customerAdminUserId, false);
    }

    @Test
    public void testSchedulePublicReportGenerationByTenantAdmin() throws Exception {
        loginTenantAdmin();
        testScheduleReportGeneration(tenantAdminUserId, true);
    }

    @Test
    public void testCustomerUserCannotScheduleReportForInaccessibleUser() throws Exception {
        loginCustomerAdminUser();
        ReportTemplate template = doPost("/api/reportTemplate",
                buildTemplate("CSV report template", ReportTemplateType.REPORT, TbReportFormat.CSV), ReportTemplate.class);

        // referencing the tenant admin (above the customer in the ownership hierarchy) must be forbidden
        SchedulerEvent schedulerEvent = createReportSchedulerEvent(template.getId(), tenantAdminUserId);
        doPost("/api/schedulerEvent", schedulerEvent)
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCustomerUserCannotScheduleReportForInaccessibleTemplate() throws Exception {
        // a different customer of the same tenant owns a report template
        loginDifferentCustomerAdmin();
        ReportTemplate otherCustomerTemplate = doPost("/api/reportTemplate",
                buildTemplate("Other customer CSV report template", ReportTemplateType.REPORT, TbReportFormat.CSV), ReportTemplate.class);

        // referencing a report template the customer can't access (even with their own user id) must be forbidden
        loginCustomerAdminUser();
        SchedulerEvent schedulerEvent = createReportSchedulerEvent(otherCustomerTemplate.getId(), customerAdminUserId);
        doPost("/api/schedulerEvent", schedulerEvent)
                .andExpect(status().isForbidden());
    }

    private void testScheduleReportGeneration(UserId userId, boolean makeReportPublic) throws Exception {
        ReportTemplate template = buildTemplate("CSV report template", ReportTemplateType.REPORT, TbReportFormat.CSV);
        template = doPost("/api/reportTemplate", template, ReportTemplate.class);

        ReportTemplate template2 = buildTemplate("CSV report template 2", ReportTemplateType.REPORT, TbReportFormat.CSV);
        template2 = doPost("/api/reportTemplate", template2, ReportTemplate.class);

        SchedulerEvent schedulerEvent = createReportSchedulerEvent(template.getId(), userId, makeReportPublic);
        doPost("/api/schedulerEvent", schedulerEvent, SchedulerEvent.class);

        // wait for reports
        PageData<ReportInfo> reports = await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() ->
                        doGetTypedWithPageLink("/api/v2/reportInfos/all?", new TypeReference<PageData<ReportInfo>>() {
                        }, new PageLink(30)),
                result -> result.getData().size() == 1);
        ReportInfo reportInfo = reports.getData().get(0);
        assertThat(reportInfo.isPublic()).isEqualTo(makeReportPublic);

        // check report infos
        PageData<ScheduledReportInfo> scheduledReportInfos = doGetTypedWithPageLink("/api/scheduledReports?", new TypeReference<PageData<ScheduledReportInfo>>() {
        }, new PageLink(30));
        assertThat(scheduledReportInfos.getData()).hasSize(1);

        // filter by report template id
        PageData<ScheduledReportInfo> reportInfos = doGetTypedWithPageLink("/api/scheduledReports?reportTemplateId=" + template2.getId().getId() + "&", new TypeReference<PageData<ScheduledReportInfo>>() {
        }, new PageLink(30));
        assertThat(reportInfos.getData()).hasSize(0);
    }

    private ReportTemplate buildTemplate(String templateName, ReportTemplateType report, TbReportFormat format) {
        ReportTemplate template = new ReportTemplate();
        template.setName(templateName);
        template.setType(report);
        template.setFormat(format);
        CsvReportTemplateConfig configuration = new CsvReportTemplateConfig();
        configuration.setComponents(new ArrayList<>());
        template.setConfiguration(configuration);
        return template;
    }

    private SchedulerEvent createReportSchedulerEvent(ReportTemplateId templateId, UserId userId) {
        return createReportSchedulerEvent(templateId, userId, false);
    }

    private SchedulerEvent createReportSchedulerEvent(ReportTemplateId templateId, UserId userId, boolean makeReportPublic) {
        SchedulerEvent schedulerEvent = new SchedulerEvent();
        schedulerEvent.setName("Report Scheduler Event");
        schedulerEvent.setType("generateReport");
        ObjectNode schedule = JacksonUtil.newObjectNode();
        schedule.put("startTime", System.currentTimeMillis() + 3000);
        schedule.put("timezone", "UTC");
        MonthlyRepeat schedulerRepeat = new MonthlyRepeat();
        schedule.set("repeat", JacksonUtil.valueToTree(schedulerRepeat));
        schedulerEvent.setSchedule(schedule);
        ReportConfig reportConfig = new ReportConfig();
        reportConfig.setReportTemplateId(templateId);
        reportConfig.setTimezone("Europe/Kiev");
        reportConfig.setUserId(userId);
        reportConfig.setMakePublic(makeReportPublic);
        schedulerEvent.setConfiguration(JacksonUtil.valueToTree(reportConfig));
        return schedulerEvent;
    }

    private SchedulerEvent saveSchedulerEvent(SchedulerEvent schedulerEvent) {
        return doPost("/api/schedulerEvent", schedulerEvent, SchedulerEvent.class);
    }

    private List<SchedulerEventWithCustomerInfo> findSchedulerEvents(String type, String searchText) throws Exception {
        return doGetTypedWithPageLink("/api/schedulerEvents?type=" + Strings.nullToEmpty(type) + "&",
                new TypeReference<PageData<SchedulerEventWithCustomerInfo>>() {
                }, new PageLink(100, 0, searchText)).getData();
    }

    private List<SchedulerEventWithCustomerInfo> findSchedulerEvents(String type, long startTime, long endTime, String searchText) throws Exception {
        return doGetTyped("/api/schedulerEvents?type=" + Strings.nullToEmpty(type) + "&startTime=" + startTime + "&endTime=" + endTime + "&" +
                        "textSearch=" + Strings.nullToEmpty(searchText),
                new TypeReference<List<SchedulerEventWithCustomerInfo>>() {
                });
    }

    private SchedulerEvent createSchedulerEvent() {
        SchedulerEvent schedulerEvent = new SchedulerEvent();
        schedulerEvent.setName("Scheduler Event");
        schedulerEvent.setType("Custom Type");
        ObjectNode schedule = JacksonUtil.newObjectNode();
        schedule.put("startTime", System.currentTimeMillis());
        schedule.put("timezone", TimeZone.getDefault().getID());
        MonthlyRepeat schedulerRepeat = new MonthlyRepeat();
        schedulerRepeat.setEndsOn(Long.MAX_VALUE);
        schedule.set("repeat", JacksonUtil.valueToTree(schedulerRepeat));
        schedulerEvent.setSchedule(schedule);
        return schedulerEvent;
    }

}
