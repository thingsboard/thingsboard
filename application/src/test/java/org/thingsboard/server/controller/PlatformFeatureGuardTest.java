// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.SystemParams;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.scheduler.MonthlyRepeat;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.subscription.SubscriptionErrorCode;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.solution.SolutionData;
import org.thingsboard.server.common.data.sync.solution.SolutionExportRequest;
import org.thingsboard.server.common.data.sync.solution.SolutionExportResponse;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.subscription.PlatformFeature;
import org.thingsboard.server.dao.subscription.SubscriptionService;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Block management, keep running: a licence that withholds a platform feature refuses the management
 * (write) endpoints for it, while the read endpoints stay open so existing entities remain visible.
 * <p>
 * The one write direction deliberately let through is the one that reduces use of a withheld feature -
 * deleting an entity, pausing a scheduler event, un-publishing a report. Entities persisted before the
 * feature was withheld keep running, so refusing those would leave a tenant unable to stop them. The
 * tests below pin both halves of that split, so a guard added to a removal path fails here rather than
 * silently taking the remedy away.
 */
@DaoSqlTest
@TestPropertySource(properties = {
        "js.evaluator=local",
        "service.integrations.supported=ALL"
})
public class PlatformFeatureGuardTest extends AbstractControllerTest {

    private static final JsonNode CONVERTER_CONFIGURATION = JacksonUtil.newObjectNode()
            .put("decoder", "return {deviceName: 'Device A', deviceType: 'thermostat'};");

    @MockitoSpyBean
    private SubscriptionService subscriptionService;

    private ConverterId converterId;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
        converterId = doPost("/api/converter", createConverter(), Converter.class).getId();
    }

    @Test
    public void savingAnIntegrationIsRefusedWhenIntegrationsAreDisabled() throws Exception {
        disable(PlatformFeature.INTEGRATIONS);

        doPost("/api/integration", createIntegration())
                .andExpect(status().isForbidden());
    }

    @Test
    public void savingAConverterIsRefusedWhenIntegrationsAreDisabled() throws Exception {
        disable(PlatformFeature.INTEGRATIONS);

        doPost("/api/converter", createConverter())
                .andExpect(status().isForbidden());
    }

    @Test
    public void readingIntegrationsIsStillAllowedWhenIntegrationsAreDisabled() throws Exception {
        // Block management, keep running: existing entities stay visible and diagnosable.
        disable(PlatformFeature.INTEGRATIONS);

        doGet("/api/integrations?pageSize=10&page=0")
                .andExpect(status().isOk());
        doGet("/api/converters?pageSize=10&page=0")
                .andExpect(status().isOk());
    }

    @Test
    public void savingASchedulerEventIsRefusedWhenTheSchedulerIsDisabled() throws Exception {
        disable(PlatformFeature.SCHEDULER);

        doPost("/api/schedulerEvent", createSchedulerEvent())
                .andExpect(status().isForbidden());
    }

    @Test
    public void savingAReportSchedulerEventIsRefusedWhenReportingIsDisabled() throws Exception {
        // The Scheduler must not be a way around a withheld reporting feature: a scheduler event of a
        // report-producing type generates reports on its own schedule once persisted.
        disable(PlatformFeature.REPORTING);

        doPost("/api/schedulerEvent", createSchedulerEvent(DataConstants.GENERATE_DASHBOARD_REPORT))
                .andExpect(status().isForbidden());
    }

    @Test
    public void savingASchedulerEventThatGeneratesReportsThroughItsMsgTypeIsRefusedWhenReportingIsDisabled() throws Exception {
        // The configuration half of the same rule, and the half a plain type comparison would let through:
        // the type here is an ordinary one and only the msgType makes the event generate reports.
        disable(PlatformFeature.REPORTING);

        doPost("/api/schedulerEvent", createSchedulerEventGeneratingReportsByMsgType())
                .andExpect(status().isForbidden());
    }

    @Test
    public void savingAReportSchedulerEventCarryingAnotherMsgTypeIsRefusedWhenReportingIsDisabled() throws Exception {
        // The scheduler submits the report job on the event type alone, so a msgType pointing elsewhere does
        // not stop the event generating reports - and must not talk the guard out of refusing it either.
        SchedulerEvent maskedEvent = createSchedulerEvent(DataConstants.GENERATE_REPORT);
        maskedEvent.setConfiguration(JacksonUtil.newObjectNode().put("msgType", "postTelemetry"));
        disable(PlatformFeature.REPORTING);

        doPost("/api/schedulerEvent", maskedEvent)
                .andExpect(status().isForbidden());
    }

    @Test
    public void savingAnOrdinarySchedulerEventIsStillAllowedWhenReportingIsDisabled() throws Exception {
        // The other half of the guard above: withholding reporting must not take the Scheduler away from
        // an instance that is entitled to it.
        disable(PlatformFeature.REPORTING);

        doPost("/api/schedulerEvent", createSchedulerEvent())
                .andExpect(status().isOk());
    }

    @Test
    public void readingSchedulerEventsIsStillAllowedWhenTheSchedulerIsDisabled() throws Exception {
        disable(PlatformFeature.SCHEDULER);

        doGet("/api/schedulerEvents?pageSize=10&page=0")
                .andExpect(status().isOk());
    }

    @Test
    public void savingAReportTemplateIsRefusedWhenReportingIsDisabled() throws Exception {
        disable(PlatformFeature.REPORTING);

        doPost("/api/reportTemplate", createReportTemplate())
                .andExpect(status().isForbidden());
    }

    @Test
    public void readingReportTemplatesIsStillAllowedWhenReportingIsDisabled() throws Exception {
        disable(PlatformFeature.REPORTING);

        doGet("/api/reportTemplateInfos/all?pageSize=10&page=0")
                .andExpect(status().isOk());
    }

    @Test
    public void readingScheduledReportEventsIsStillAllowedWhenTheSchedulerIsDisabled() throws Exception {
        disable(PlatformFeature.SCHEDULER);

        doGet("/api/scheduledReports?pageSize=10&page=0")
                .andExpect(status().isOk());
    }

    @Test
    public void storingAGeneratedReportIsStillAllowedWhenReportingIsDisabled() throws Exception {
        // POST /api/v2/report is the report microservice's persistence callback, not a management endpoint:
        // a scheduled report generated by an already-persisted scheduler event is stored through it.
        ReportTemplate reportTemplate = doPost("/api/reportTemplate", createReportTemplate(), ReportTemplate.class);
        disable(PlatformFeature.REPORTING);

        storeGeneratedReport(reportTemplate)
                .andExpect(status().isOk());
    }

    @Test
    public void downloadingAnExistingReportIsStillAllowedWhenReportingIsDisabled() throws Exception {
        // Retrieving an already-generated artefact is a read.
        ReportTemplate reportTemplate = doPost("/api/reportTemplate", createReportTemplate(), ReportTemplate.class);
        Report report = readResponse(storeGeneratedReport(reportTemplate).andExpect(status().isOk()), Report.class);
        disable(PlatformFeature.REPORTING);

        doGet("/api/v2/report/" + report.getId().getId() + "/download")
                .andExpect(status().isOk());
    }

    @Test
    public void importingAConverterIsRefusedWhenIntegrationsAreDisabled() throws Exception {
        // Entity import bypasses ConverterController entirely, so the guard lives at the import choke point.
        SolutionData solution = exportSolution(converterId);

        doPost("/api/solution/import", solution)
                .andExpect(status().isOk());

        disable(PlatformFeature.INTEGRATIONS);

        doPost("/api/solution/import", solution)
                .andExpect(status().isForbidden());
    }

    @Test
    public void importingAnIntegrationIsRefusedWhenIntegrationsAreDisabled() throws Exception {
        // The converter travels with the integration, since the integration refers to it.
        SolutionData solution = exportSolution(
                doPost("/api/integration", createIntegration(), Integration.class).getId(), converterId);

        doPost("/api/solution/import", solution)
                .andExpect(status().isOk());

        disable(PlatformFeature.INTEGRATIONS);

        doPost("/api/solution/import", solution)
                .andExpect(status().isForbidden());
    }

    @Test
    public void importingASchedulerEventIsRefusedWhenTheSchedulerIsDisabled() throws Exception {
        // Each licensed entity type routes to its own feature, so a type mapped to the wrong one is only
        // caught by exercising that type: the converter case says nothing about this one.
        SolutionData solution = exportSolution(
                doPost("/api/schedulerEvent", createSchedulerEvent(), SchedulerEvent.class).getId());

        doPost("/api/solution/import", solution)
                .andExpect(status().isOk());

        disable(PlatformFeature.SCHEDULER);

        doPost("/api/solution/import", solution)
                .andExpect(status().isForbidden());
    }

    @Test
    public void importingAReportProducingSchedulerEventIsRefusedWhenReportingIsDisabled() throws Exception {
        // The import path checks the report-producing rule separately from the entity-type-to-feature map, and
        // only this shape reaches it: the Scheduler stays granted throughout, so nothing else can refuse.
        SolutionData solution = exportSolution(
                doPost("/api/schedulerEvent", createSchedulerEventGeneratingReportsByMsgType(), SchedulerEvent.class).getId());

        doPost("/api/solution/import", solution)
                .andExpect(status().isOk());

        disable(PlatformFeature.REPORTING);

        doPost("/api/solution/import", solution)
                .andExpect(status().isForbidden());
    }

    @Test
    public void importingAReportTemplateIsRefusedWhenReportingIsDisabled() throws Exception {
        SolutionData solution = exportSolution(
                doPost("/api/reportTemplate", createReportTemplate(), ReportTemplate.class).getId());

        doPost("/api/solution/import", solution)
                .andExpect(status().isOk());

        disable(PlatformFeature.REPORTING);

        doPost("/api/solution/import", solution)
                .andExpect(status().isForbidden());
    }

    @Test
    public void deletingIntegrationEntitiesIsStillAllowedWhenIntegrationsAreDisabled() throws Exception {
        // Removal is the one write direction that reduces use of a withheld feature, and is deliberately let
        // through: an integration persisted before the feature was withheld keeps running and keeps ingesting,
        // so refusing to take it down would leave a tenant with no way to stop it. Creating one is still
        // refused - see savingAnIntegrationIsRefusedWhenIntegrationsAreDisabled.
        Integration integration = doPost("/api/integration", createIntegration(), Integration.class);
        disable(PlatformFeature.INTEGRATIONS);

        // The integration goes first: a converter still referenced by one is refused deletion for a reason
        // that has nothing to do with the licence, which would say nothing about the guard.
        doDelete("/api/integration/" + integration.getId().getId())
                .andExpect(status().isOk());
        doDelete("/api/converter/" + converterId.getId())
                .andExpect(status().isOk());
    }

    @Test
    public void assigningAnIntegrationToAnEdgeIsRefusedWhenIntegrationsAreDisabled() throws Exception {
        // Assignment hands a withheld integration to an edge to run, so both directions are management writes.
        disable(PlatformFeature.INTEGRATIONS);

        doPost("/api/edge/" + UUID.randomUUID() + "/integration/" + UUID.randomUUID())
                .andExpect(status().isForbidden());
        doDelete("/api/edge/" + UUID.randomUUID() + "/integration/" + UUID.randomUUID())
                .andExpect(status().isForbidden());
    }

    @Test
    public void deletingASchedulerEventIsStillAllowedWhenTheSchedulerIsDisabled() throws Exception {
        // The removal direction again: a scheduler event persisted before the feature was withheld keeps
        // firing on its own schedule, so refusing this would leave a tenant unable to stop it.
        SchedulerEvent schedulerEvent = doPost("/api/schedulerEvent", createSchedulerEvent(), SchedulerEvent.class);
        disable(PlatformFeature.SCHEDULER);

        doDelete("/api/schedulerEvent/" + schedulerEvent.getId().getId())
                .andExpect(status().isOk());
    }

    @Test
    public void enablingASchedulerEventIsRefusedWhenTheSchedulerIsDisabled() throws Exception {
        // The refusal comes before the event is looked up, which is what makes a random id enough here.
        disable(PlatformFeature.SCHEDULER);

        doPut("/api/schedulerEvent/" + UUID.randomUUID() + "/enabled/true", JacksonUtil.newObjectNode())
                .andExpect(status().isForbidden());
    }

    @Test
    public void pausingASchedulerEventIsStillAllowedWhenTheSchedulerIsDisabled() throws Exception {
        // The other direction of the same endpoint, and the half that is deliberately let through: pausing
        // reduces use of a withheld feature, and an event persisted before it was withheld keeps firing until
        // someone can turn it off. Turning one back on is refused by the test above.
        SchedulerEvent schedulerEvent = doPost("/api/schedulerEvent", createSchedulerEvent(), SchedulerEvent.class);
        disable(PlatformFeature.SCHEDULER);

        doPut("/api/schedulerEvent/" + schedulerEvent.getId().getId() + "/enabled/false", JacksonUtil.newObjectNode())
                .andExpect(status().isOk());
    }

    @Test
    public void enablingAReportSchedulerEventIsRefusedWhenReportingIsDisabled() throws Exception {
        // A second, distinct call site of the report-producing check: the type is read off the persisted event
        // here rather than off a request body, so the save path passing says nothing about this one.
        SchedulerEvent reportEvent = doPost("/api/schedulerEvent",
                createSchedulerEvent(DataConstants.GENERATE_DASHBOARD_REPORT), SchedulerEvent.class);
        disable(PlatformFeature.REPORTING);

        doPut("/api/schedulerEvent/" + reportEvent.getId().getId() + "/enabled/true", JacksonUtil.newObjectNode())
                .andExpect(status().isForbidden());
    }

    @Test
    public void assigningASchedulerEventToAnEdgeIsRefusedWhenTheSchedulerIsDisabled() throws Exception {
        disable(PlatformFeature.SCHEDULER);

        doPost("/api/edge/" + UUID.randomUUID() + "/schedulerEvent/" + UUID.randomUUID())
                .andExpect(status().isForbidden());
        doDelete("/api/edge/" + UUID.randomUUID() + "/schedulerEvent/" + UUID.randomUUID())
                .andExpect(status().isForbidden());
    }

    @Test
    public void assigningAReportProducingSchedulerEventToAnEdgeIsRefusedWhenReportingIsDisabled() throws Exception {
        // The edge runs a scheduler of its own, so handing it a report-producing event is a reporting write
        // even though the Scheduler itself stays granted. The event has to be a real one: it is read off the
        // database here rather than taken from a request body.
        SchedulerEvent reportEvent = doPost("/api/schedulerEvent",
                createSchedulerEvent(DataConstants.GENERATE_DASHBOARD_REPORT), SchedulerEvent.class);
        disable(PlatformFeature.REPORTING);

        // The event is looked up before the edge, which is what makes a random edge id enough here.
        doPost("/api/edge/" + UUID.randomUUID() + "/schedulerEvent/" + reportEvent.getId().getId())
                .andExpect(status().isForbidden());
    }

    @Test
    public void deletingAReportIsStillAllowedWhenReportingIsDisabled() throws Exception {
        // Deliberately let through, and for one reason more than the other removals: a report made public
        // before the feature was withheld keeps its unauthenticated download link live, and deleting it is one
        // of the two ways to close that link. Generating a new report is still refused.
        ReportTemplate reportTemplate = doPost("/api/reportTemplate", createReportTemplate(), ReportTemplate.class);
        Report report = readResponse(storeGeneratedReport(reportTemplate).andExpect(status().isOk()), Report.class);
        disable(PlatformFeature.REPORTING);

        doDelete("/api/v2/report/" + report.getId().getId())
                .andExpect(status().isOk());
    }

    @Test
    public void deletingAReportTemplateIsStillAllowedWhenReportingIsDisabled() throws Exception {
        // The removal direction again: a template persisted before the feature was withheld is still referenced
        // by scheduler events, so refusing this would leave a tenant unable to clean it up.
        ReportTemplate reportTemplate = doPost("/api/reportTemplate", createReportTemplate(), ReportTemplate.class);
        disable(PlatformFeature.REPORTING);

        doDelete("/api/reportTemplate/" + reportTemplate.getId().getId())
                .andExpect(status().isOk());
    }

    @Test
    public void publishingAReportIsRefusedWhenReportingIsDisabled() throws Exception {
        // Handing out a fresh unauthenticated download link is a reporting write. Refused before the report is
        // looked up, which is what makes a random id enough here.
        disable(PlatformFeature.REPORTING);

        doPut("/api/v2/report/" + UUID.randomUUID() + "/public/true", JacksonUtil.newObjectNode())
                .andExpect(status().isForbidden());
    }

    @Test
    public void unpublishingAReportIsStillAllowedWhenReportingIsDisabled() throws Exception {
        // The other half of the same endpoint: the public link of a report published before the feature was
        // withheld stays live and unauthenticated, so refusing to close it would leave a tenant with an open
        // share link and no self-service way to shut it.
        ReportTemplate reportTemplate = doPost("/api/reportTemplate", createReportTemplate(), ReportTemplate.class);
        Report report = readResponse(storeGeneratedReport(reportTemplate).andExpect(status().isOk()), Report.class);
        doPut("/api/v2/report/" + report.getId().getId() + "/public/true", JacksonUtil.newObjectNode())
                .andExpect(status().isOk());
        disable(PlatformFeature.REPORTING);

        doPut("/api/v2/report/" + report.getId().getId() + "/public/false", JacksonUtil.newObjectNode())
                .andExpect(status().isOk());
    }

    @Test
    public void generatingAReportOnDemandIsRefusedWhenReportingIsDisabled() throws Exception {
        // The subtlest line this class draws: downloading an already-generated artefact is a read and stays
        // open, while asking the instance to produce one now is a reporting write, whichever of the four
        // endpoints is used to ask.
        disable(PlatformFeature.REPORTING);

        doPost("/api/v2/report/test", new ReportRequest())
                .andExpect(status().isForbidden());
        doPost("/api/v2/report/request", new ReportRequest())
                .andExpect(status().isForbidden());
        doPost("/api/report/" + UUID.randomUUID() + "/download", JacksonUtil.newObjectNode())
                .andExpect(status().isForbidden());
        // The dashboard report preview the report editor calls: refused before the configuration is read, which
        // is what makes an empty one enough here.
        doPost("/api/report/test", new DashboardReportConfig())
                .andExpect(status().isForbidden());
    }

    /**
     * The flags the session bootstrap reports are what the front end hides the Integrations, Scheduler and
     * Reporting menus by, and they are three near-identical lines reading three different features - the shape
     * where a wrong constant changes nothing that any other test observes. Each feature is withheld in turn and
     * every flag is checked, so a line reading its neighbour's feature fails on the flag that did not move.
     */
    @Test
    public void theSystemParametersReportEachFeatureFlagIndependently() throws Exception {
        // Every constant, not a list of them: SystemParams mirrors the enum by hand, so a fourth feature has
        // to be reported alongside the other three or nothing tells the tenant it was withheld.
        List<PlatformFeature> reportedFeatures = List.of(PlatformFeature.values());

        for (PlatformFeature withheld : reportedFeatures) {
            doReturn(false).when(subscriptionService).isFeatureEnabled(any(), eq(withheld));

            SystemParams systemParams = doGet("/api/system/params", SystemParams.class);
            for (PlatformFeature reported : reportedFeatures) {
                assertThat(flagFor(systemParams, reported))
                        .as("%s reported while %s is withheld", reported, withheld)
                        .isEqualTo(reported != withheld);
            }

            // Put it back rather than resetting the spy: under this profile the bean grants every feature, so
            // this is the answer the next iteration would otherwise get from it.
            doReturn(true).when(subscriptionService).isFeatureEnabled(any(), eq(withheld));
        }
    }

    @Test
    public void everythingIsAllowedWhenNoFeatureIsDisabled() throws Exception {
        // What this pins is that the guard refuses nothing when nothing is withheld - no licence and no
        // plan-to-feature mapping is exercised here, since the spied bean this profile supplies grants
        // everything. The rule that a plan silent about a feature grants it is pinned by
        // PlatformFeatureContractTest#aPlanThatSaysNothingAboutAFeatureGrantsIt.
        doPost("/api/integration", createIntegration()).andExpect(status().isOk());
        doPost("/api/schedulerEvent", createSchedulerEvent()).andExpect(status().isOk());
        doPost("/api/reportTemplate", createReportTemplate()).andExpect(status().isOk());
    }

    private SolutionData exportSolution(EntityId... entityIds) {
        SolutionExportRequest request = SolutionExportRequest.builder()
                .internalIds(Set.of(entityIds))
                .settings(EntityExportSettings.builder()
                        .exportRelations(false).exportAttributes(false)
                        .exportCredentials(false).exportCalculatedFields(false)
                        .exportPermissions(false).exportGroupEntities(false)
                        .embedGroupMembers(false).build())
                .build();
        return doPost("/api/solution/export", request, SolutionExportResponse.class).getSolution();
    }

    private ResultActions storeGeneratedReport(ReportTemplate reportTemplate) throws Exception {
        Report report = new Report();
        report.setTenantId(tenantId);
        report.setTemplateId(reportTemplate.getId());
        report.setFormat(TbReportFormat.PDF);
        report.setName("Generated report " + StringUtils.randomAlphanumeric(10));
        report.setUserId(currentUserId);

        MockMultipartFile file = new MockMultipartFile("file", report.getName(),
                MediaType.APPLICATION_PDF_VALUE, "generated report content".getBytes(StandardCharsets.UTF_8));
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart(HttpMethod.POST, "/api/v2/report").file(file);
        request.part(new MockPart("info", JacksonUtil.toString(report).getBytes(StandardCharsets.UTF_8)));
        setJwtToken(request);
        return mockMvc.perform(request);
    }

    private static boolean flagFor(SystemParams systemParams, PlatformFeature feature) {
        return switch (feature) {
            case INTEGRATIONS -> systemParams.isIntegrationsEnabled();
            case SCHEDULER -> systemParams.isSchedulerEnabled();
            case REPORTING -> systemParams.isReportingEnabled();
            // No default arm on purpose: an exhaustive switch expression is what turns a new PlatformFeature
            // into a compile error here, and so into a SystemParams field and a SystemInfoController line.
        };
    }

    private void disable(PlatformFeature feature) {
        doThrow(new SubscriptionException(feature.getDisplayName() + " feature is disabled!", SubscriptionErrorCode.FEATURE_DISABLED))
                .when(subscriptionService).checkFeatureAllowed(any(), eq(feature));
    }

    private Converter createConverter() {
        Converter converter = new Converter();
        converter.setName("Converter " + StringUtils.randomAlphanumeric(10));
        converter.setType(ConverterType.UPLINK);
        converter.setIntegrationType(IntegrationType.HTTP);
        converter.setConfiguration(CONVERTER_CONFIGURATION);
        return converter;
    }

    private Integration createIntegration() {
        Integration integration = new Integration();
        integration.setName("Integration " + StringUtils.randomAlphanumeric(10));
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.HTTP);
        ObjectNode configuration = JacksonUtil.newObjectNode();
        configuration.putObject("metadata").put("key1", "val1");
        integration.setConfiguration(configuration);
        integration.setDefaultConverterId(converterId);
        return integration;
    }

    private SchedulerEvent createSchedulerEvent() {
        return createSchedulerEvent("Custom Type");
    }

    /**
     * The other half of the report-producing rule: the scheduler dispatches on the configuration's
     * {@code msgType} when it has one, so an event whose type says nothing about reports still generates them.
     * The type stays the ordinary one, or the event type alone would be enough to refuse it.
     */
    private SchedulerEvent createSchedulerEventGeneratingReportsByMsgType() {
        SchedulerEvent schedulerEvent = createSchedulerEvent();
        schedulerEvent.setConfiguration(JacksonUtil.newObjectNode().put("msgType", DataConstants.GENERATE_REPORT));
        return schedulerEvent;
    }

    private SchedulerEvent createSchedulerEvent(String type) {
        SchedulerEvent schedulerEvent = new SchedulerEvent();
        schedulerEvent.setName("Scheduler Event " + StringUtils.randomAlphanumeric(10));
        schedulerEvent.setType(type);
        ObjectNode schedule = JacksonUtil.newObjectNode();
        schedule.put("startTime", System.currentTimeMillis());
        schedule.put("timezone", TimeZone.getDefault().getID());
        MonthlyRepeat schedulerRepeat = new MonthlyRepeat();
        schedulerRepeat.setEndsOn(Long.MAX_VALUE);
        schedule.set("repeat", JacksonUtil.valueToTree(schedulerRepeat));
        schedulerEvent.setSchedule(schedule);
        return schedulerEvent;
    }

    private ReportTemplate createReportTemplate() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("Report template " + StringUtils.randomAlphanumeric(10));
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setDescription("My report");
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        return reportTemplate;
    }

}
