// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.ReportJobResult;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.AffectedUserFilter;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportInfo;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.AlarmFilterConfig;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.DataSourceType;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.TableSortOrder;
import org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType;
import org.thingsboard.server.common.data.report.configuration.chart.ReportTimeSeriesChartSettings;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.DataReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ImageComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.RichTextComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesChartComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.style.Heading;
import org.thingsboard.server.common.data.report.configuration.timewindow.AggregationConfiguration;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.Interval;
import org.thingsboard.server.common.data.report.configuration.timewindow.QuickTimeInterval;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.notification.DefaultNotifications;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.report.service.TbReportService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
@TestPropertySource(properties = {
        "reports.generation_timeout_ms=4000"
})
public class ReportControllerTest extends AbstractControllerTest {

    @Autowired
    private DefaultNotifications defaultNotifications;
    @MockitoSpyBean
    private TbReportService tbReportService;
    @MockitoSpyBean
    private SubscriptionService subscriptionService;
    @Autowired
    private TimeseriesService timeseriesService;
    @Autowired
    private RelationService relationService;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
    }

    @After
    public void afterTest() throws Exception {
    }

    @Test
    public void testCSVReportWithEntityTable() throws Exception {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDeviceTypeEntityAlias(devicesAliasId);

        EntityTableComponent tableComponent = buildEntityTableComponent(devicesAliasId);
        ReportTemplateConfig configuration = createReportConfigTemplate(List.of(tableComponent), entityAlias, TbReportFormat.CSV);

        List<String> columnHeaders = getColumnHeaders(tableComponent);
        List<List<String>> generatedValues = generateLatestTestData(columnHeaders, tableComponent.getTableHeading().getText(), configuration.getTimeDataPattern());
        List<String> expectedRows = generatedValues.stream().map(row -> String.join(",", row)).toList();

        generateAndCheckCSVReport(configuration, expectedRows);
    }

    @Test
    public void testCSVReportWithOriginatorAndAggrFields() throws Exception {
        Device testDevice = new Device();
        testDevice.setName("Originator device");
        testDevice.setType("default");
        testDevice.setLabel("testLabel" + (int) (Math.random() * 1000));
        testDevice = doPost("/api/device", testDevice, Device.class);

        for (int i = 10; i < 25; i++) {
            String telemetryPayload = "{\"temperature\":" + i + "}";
            doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getId() + "/timeseries/" + DataConstants.SHARED_SCOPE, telemetryPayload, String.class, status().isOk());
            Thread.sleep(100);
        }

        String devicesAliasId = StringUtils.randomAlphabetic(10);
        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(AliasEntityId.fromEntityId(testDevice.getId()));
        EntityAlias entityAlias = new EntityAlias(devicesAliasId, "device", filter);

        EntityTableComponent tableComponent = new EntityTableComponent();
        DataKey tempField = new DataKey("temperature", "timeseries", "TEMPERATURE");
        tempField.setAggregationType(Aggregation.AVG);
        tempField.setTimewindow(buildCurrentDateTimeWindow());
        tempField.setDecimals(0);
        tableComponent.setDataSources(List.of(DataSource.builder()
                .type(DataSourceType.ENTITY)
                .entityAliasId(devicesAliasId)
                .dataKeys(List.of(
                        new DataKey("createdTime", "entityField", "CREATED TIME"),
                        new DataKey("name", "entityField", "NAME"),
                        tempField
                ))
                .build()));

        ReportTemplateConfig configuration = createReportConfigTemplate(List.of(tableComponent), entityAlias, TbReportFormat.CSV);

        ReportRequest request = new ReportRequest();
        request.setReportTemplateConfig(configuration);
        request.setOriginator(testDevice.getId());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(configuration.getTimeDataPattern()).withZone(ZoneId.systemDefault());

        TimeIntervalCalculator.TimeRange timeRange = TimeIntervalCalculator.getTimeRange(tempField.getTimewindow(), tempField.getTimewindow().getTimezone());
        Device finalDevice = testDevice;

        await().atMost(60, TimeUnit.SECONDS).until(() -> {
            String csvReport = doPost("/api/v2/report/test", request, String.class);
            List<String> actualReportRows = Arrays.stream(csvReport.split("\\r?\\n")).map(String::trim).toList();
            log.warn("Report rows: {}", actualReportRows);
            ObjectNode timeseries = doGetAsync("/api/plugins/telemetry/DEVICE/" + finalDevice.getId() + "/values/timeseries?keys=temperature&startTs={startTs}&endTs={endTs}", ObjectNode.class, timeRange.startTs, timeRange.endTs);
            log.warn("Telemetry: {}", timeseries);
            return actualReportRows
                    .containsAll(List.of("CREATED TIME,NAME,TEMPERATURE", formatter.format(Instant.ofEpochMilli(finalDevice.getCreatedTime())) + "," + finalDevice.getName() + ",17"));
        });
    }

    @Test
    public void testEntityCountDatasourceWithOriginator() throws Exception {
        Asset asset = new Asset();
        asset.setName("Originator asset");
        asset.setType("default");
        asset.setLabel("testLabel" + (int) (Math.random() * 1000));
        asset = doPost("/api/asset", asset, Asset.class);

        Device device = new Device();
        device.setName("Test device");
        device.setType("default");
        device.setLabel("testLabel" + (int) (Math.random() * 1000));
        device = doPost("/api/device", device, Device.class);

        relationService.saveRelation(tenantId, new EntityRelation(asset.getId(), device.getId(), "Contains", RelationTypeGroup.COMMON));

        String devicesAliasId = StringUtils.randomAlphabetic(10);
        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setRootStateEntity(true);
        filter.setDirection(EntitySearchDirection.FROM);
        filter.setMaxLevel(1);
        EntityAlias entityAlias = new EntityAlias(devicesAliasId, "device", filter);

        RichTextComponent richTextComponent = new RichTextComponent();
        DataKey countField = new DataKey("count", "count", "count");
        richTextComponent.setDataSources(List.of(DataSource.builder()
                .type(DataSourceType.ENTITY_COUNT)
                .entityAliasId(devicesAliasId)
                .dataKeys(List.of(countField))
                .build()));
        richTextComponent.setValue("${count}");

        ReportTemplateConfig configuration = createReportConfigTemplate(List.of(richTextComponent), entityAlias, TbReportFormat.PDF);

        ReportRequest request = new ReportRequest();
        request.setReportTemplateConfig(configuration);
        request.setOriginator(asset.getId());

        generateAndCheckPDFReportText(request, List.of("1"));
    }

    @Test
    public void testCSVReportWithMultipleAggrFields() throws Exception {
        Device testDevice = new Device();
        testDevice.setName("Originator device");
        testDevice.setType("default");
        testDevice.setLabel("testLabel" + (int) (Math.random() * 1000));
        testDevice = doPost("/api/device", testDevice, Device.class);

        for (int i = 0; i < 10; i++) {
            String telemetryPayload = "{\"temperature\":" + i + "}";
            doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getId() + "/timeseries/" + DataConstants.SHARED_SCOPE, telemetryPayload, String.class, status().isOk());
            Thread.sleep(100);
        }

        String devicesAliasId = StringUtils.randomAlphabetic(10);
        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(AliasEntityId.fromEntityId(testDevice.getId()));
        EntityAlias entityAlias = new EntityAlias(devicesAliasId, "device", filter);

        EntityTableComponent tableComponent = new EntityTableComponent();
        DataKey avgTemp = new DataKey("temperature", "timeseries", "AVG TEMPERATURE");
        avgTemp.setAggregationType(Aggregation.AVG);
        avgTemp.setTimewindow(buildCurrentDateTimeWindow());
        avgTemp.setDecimals(1);

        DataKey minTemp = new DataKey("temperature", "timeseries", "MIN TEMPERATURE");
        minTemp.setAggregationType(Aggregation.MIN);
        minTemp.setTimewindow(buildCurrentDateTimeWindow());
        minTemp.setDecimals(1);

        TimeWindowConfiguration yesterdayRange = new TimeWindowConfiguration();
        History history = new History();
        history.setHistoryType(2);
        history.setQuickInterval(QuickTimeInterval.YESTERDAY);
        yesterdayRange.setHistory(history);

        DataKey minTempYesterday = new DataKey("temperature", "timeseries", "MIN TEMPERATURE YESTERDAY");
        minTempYesterday.setAggregationType(Aggregation.MIN);
        minTempYesterday.setTimewindow(yesterdayRange);
        minTempYesterday.setDecimals(1);

        tableComponent.setDataSources(List.of(DataSource.builder()
                .type(DataSourceType.ENTITY)
                .entityAliasId(devicesAliasId)
                .dataKeys(List.of(
                        new DataKey("createdTime", "entityField", "CREATED TIME"),
                        new DataKey("name", "entityField", "NAME"),
                        avgTemp,
                        minTemp,
                        minTempYesterday
                ))
                .build()));

        ReportTemplateConfig configuration = createReportConfigTemplate(List.of(tableComponent), entityAlias, TbReportFormat.CSV);

        ReportRequest request = new ReportRequest();
        request.setReportTemplateConfig(configuration);

        Device finalTestDevice = testDevice;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(configuration.getTimeDataPattern()).withZone(ZoneId.systemDefault());

        await().atMost(60, TimeUnit.SECONDS).until(() -> {
            String csvReport = doPost("/api/v2/report/test", request, String.class);
            List<String> actualReportRows = Arrays.stream(csvReport.split("\\r?\\n")).map(String::trim).toList();
            log.warn("Report rows: {}", actualReportRows);
            return actualReportRows
                    .containsAll(List.of("CREATED TIME,NAME,AVG TEMPERATURE,MIN TEMPERATURE,MIN TEMPERATURE YESTERDAY", formatter.format(Instant.ofEpochMilli(finalTestDevice.getCreatedTime())) + "," + finalTestDevice.getName() + ",4.5,0.0,"));
        });
    }

    @Test
    public void testPDFReportWithEntityTable() throws Exception {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDeviceTypeEntityAlias(devicesAliasId);

        EntityTableComponent tableComponent = buildEntityTableComponent(devicesAliasId);
        ReportTemplateConfig configuration = createReportConfigTemplate(List.of(tableComponent), entityAlias, TbReportFormat.PDF);

        List<String> columnHeaders = getColumnHeaders(tableComponent);
        List<List<String>> generatedValues = generateLatestTestData(columnHeaders, tableComponent.getTableHeading().getText(), configuration.getTimeDataPattern());
        List<String> expectedRows = generatedValues.stream().map(row -> String.join(" ", row)).toList();

        generateAndCheckPDFReportText(configuration, expectedRows);
    }

    @Test
    public void testCSVReportIncludesNonProductionNoticeWhenDevelopment() throws Exception {
        when(subscriptionService.isDevelopment(any())).thenReturn(true);

        String csvReport = doPost("/api/v2/report/test", nonProductionNoticeReportRequest(TbReportFormat.CSV), String.class);

        // the notice is prepended as the first row (CsvReportService#generateReport), so pin it there rather
        // than just anywhere in the file
        assertThat(csvReport.split("\\r?\\n")[0].trim()).isEqualTo(DataConstants.NON_PRODUCTION_NOTICE);
    }

    @Test
    public void testCSVReportOmitsNonProductionNoticeWhenNotDevelopment() throws Exception {
        when(subscriptionService.isDevelopment(any())).thenReturn(false);

        String csvReport = doPost("/api/v2/report/test", nonProductionNoticeReportRequest(TbReportFormat.CSV), String.class);

        assertThat(csvReport).doesNotContain(DataConstants.NON_PRODUCTION_NOTICE);
    }

    @Test
    public void testPDFReportIncludesNonProductionNoticeWhenDevelopment() throws Exception {
        when(subscriptionService.isDevelopment(any())).thenReturn(true);

        // requestTestPdfReport runs the returned PDF bytes through PDFBox's PDFTextStripper - an independent
        // library from the OpenPDF one addNonProductionNotice is implemented with - so this is real proof the
        // footer is extractable text on an actual rendered report PDF, not just on a synthetic OpenPDF fixture.
        String pdfText = requestTestPdfReport(nonProductionNoticeReportRequest(TbReportFormat.PDF));

        assertThat(pdfText).contains(DataConstants.NON_PRODUCTION_NOTICE);
    }

    @Test
    public void testPDFReportOmitsNonProductionNoticeWhenNotDevelopment() throws Exception {
        when(subscriptionService.isDevelopment(any())).thenReturn(false);

        String pdfText = requestTestPdfReport(nonProductionNoticeReportRequest(TbReportFormat.PDF));

        assertThat(pdfText).doesNotContain(DataConstants.NON_PRODUCTION_NOTICE);
    }

    @Test
    public void testCSVReportWithAlarmTableComponent() throws Exception {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDeviceTypeEntityAlias(devicesAliasId);

        AlarmTableComponent tableComponent = buildAlarmTableComponent(devicesAliasId, null);
        ReportTemplateConfig configuration = createReportConfigTemplate(List.of(tableComponent), entityAlias, TbReportFormat.CSV);

        List<String> columnHeaders = getColumnHeaders(tableComponent);
        List<List<String>> generatedValues = generateTestAlarmData(columnHeaders, configuration.getTimeDataPattern());
        List<String> expectedRows = generatedValues.stream().map(row -> String.join(",", row)).toList();

        generateAndCheckCSVReport(configuration, expectedRows);
    }

    @Test
    public void testCSVReportWithAlarmTableComponentForOneDevice() throws Exception {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDeviceTypeEntityAlias(devicesAliasId);

        Device device = new Device();
        device.setName("My device");
        device.setType("typeA");
        device.setLabel("sensor");
        Device createdDevice = doPost("/api/device", device, Device.class);

        AlarmTableComponent tableComponent = buildAlarmTableComponent(null, createdDevice.getId().toString());
        Heading heading = new Heading();
        heading.setText("Alarms for device: ${entityName} ${entityLabel}");
        tableComponent.setTableHeading(heading);
        tableComponent.setShowTableHeading(true);
        ReportTemplateConfig configuration = createReportConfigTemplate(List.of(tableComponent), entityAlias, TbReportFormat.CSV);

        String expectedHeading = "Alarms for device: My device sensor";
        List<String> columnHeaders = getColumnHeaders(tableComponent);
        List<List<String>> expectedLines = generateTestAlarmData(expectedHeading, createdDevice, columnHeaders, configuration.getTimeDataPattern());
        List<String> expectedRows = expectedLines.stream().map(row -> String.join(",", row)).toList();

        generateAndCheckCSVReport(configuration, expectedRows);
    }

    @Test
    public void testPDFReportWithAlarmTableComponent() throws Exception {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDeviceTypeEntityAlias(devicesAliasId);

        AlarmTableComponent tableComponent = buildAlarmTableComponent(devicesAliasId, null);
        ReportTemplateConfig configuration = createReportConfigTemplate(List.of(tableComponent), entityAlias, TbReportFormat.PDF);

        List<String> columnHeaders = getColumnHeaders(tableComponent);
        List<List<String>> generatedValues = generateTestAlarmData(columnHeaders, configuration.getTimeDataPattern());
        List<String> expectedRows = generatedValues.stream().map(row -> String.join(" ", row)).toList();

        generateAndCheckPDFReportText(configuration, expectedRows);
    }

    @Test
    public void testReportCsvWithTimeSeriesTable() throws Exception {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDeviceTypeEntityAlias(devicesAliasId);

        TimeseriesTableComponent tsComponent = buildTimeseriesTableComponent(devicesAliasId);
        ReportTemplateConfig configuration = createReportConfigTemplate(List.of(tsComponent), entityAlias, TbReportFormat.CSV);

        List<String> columnHeaders = List.of("Timestamp", "TEMPERATURE", "Non existing", "NAME", "ACTIVE");
        List<List<String>> generatedValues = generateTsData(columnHeaders);
        List<String> expectedRows = generatedValues.stream().map(row -> String.join(",", row)).toList();

        generateAndCheckCSVReport(configuration, expectedRows);
    }

    @Test
    public void testReportCsvWithTimeSeriesTableWithAggr() throws Exception {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDeviceTypeEntityAlias(devicesAliasId);

        TimeWindowConfiguration timeWindowConf = buildCurrentDateTimeWindow(Aggregation.SUM, Interval.of(86400000));
        TimeseriesTableComponent tsComponent = new TimeseriesTableComponent();
        tsComponent.setDataSources(List.of(DataSource.builder()
                .type(DataSourceType.ENTITY)
                .entityAliasId(devicesAliasId)
                .dataKeys(List.of(
                        new DataKey("energy", "timeseries", "ENERGY")
                ))
                .latestDataKeys(List.of(
                        new DataKey("name", "entityField", "NAME")
                ))
                .build()));
        tsComponent.setTimewindow(timeWindowConf);

        ReportTemplateConfig configuration = createReportConfigTemplate(List.of(tsComponent), entityAlias, TbReportFormat.CSV);

        List<String> columnHeaders = List.of("ENERGY", "NAME");
        List<List<String>> generatedValues = generateTsDataForSumAggr(columnHeaders);
        List<String> expectedRows = generatedValues.stream().map(row -> String.join(",", row)).toList();

        generateAndCheckCSVReport(configuration, expectedRows);
    }

    @Test
    public void testReportPdfWithTimeSeriesTable() throws Exception {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDeviceTypeEntityAlias(devicesAliasId);

        TimeseriesTableComponent tsComponent = buildTimeseriesTableComponent(devicesAliasId);
        ReportTemplateConfig configuration = createReportConfigTemplate(List.of(tsComponent), entityAlias, TbReportFormat.PDF);

        List<String> columnHeaders = List.of("Timestamp", "TEMPERATURE", "Non existing", "NAME", "ACTIVE");
        List<List<String>> expectedLines = generateTsData(columnHeaders);
        List<String> expectedRows = expectedLines.stream().map(row -> String.join(" ", row)).toList();

        generateAndCheckPDFReportText(configuration, expectedRows);
    }

    @Test
    public void testCreateReportJobByTenantAdmin() throws Exception {
        loginTenantAdmin();
        testCreateReportJob(Authority.TENANT_ADMIN);
    }

    public void testCreateReportJob(Authority authority) throws Exception {
        boolean tenantUser = authority == Authority.TENANT_ADMIN;
        List<String> expectedReportLines = new ArrayList<>();

        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDeviceTypeEntityAlias(devicesAliasId);

        EntityTableComponent tableComponent = new EntityTableComponent();
        tableComponent.setDataSources(List.of(DataSource.builder()
                .type(DataSourceType.ENTITY)
                .entityAliasId(devicesAliasId)
                .dataKeys(List.of(
                        new DataKey("createdTime", "entityField", "CREATED TIME"),
                        new DataKey("name", "entityField", "NAME"),
                        new DataKey("type", "entityField", "TYPE"),
                        new DataKey("temperature", "timeseries", "TEMPERATURE"),
                        new DataKey("threshold", "attribute", "THRESHOLD")
                ))
                .build()));

        CsvReportTemplateConfig configuration = new CsvReportTemplateConfig();
        configuration.setEntityAliases(List.of(entityAlias));
        configuration.setComponents(List.of(tableComponent));
        configuration.setNamePattern("test.csv");

        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setConfiguration(configuration);
        reportTemplate.setName("Devices report");
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setFormat(TbReportFormat.CSV);
        reportTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);
        ReportTemplateId reportTemplateId = reportTemplate.getId();

        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            device = doPost("/api/device", device, Device.class);
            devices.add(device);

            long temperature = (long) (Math.random() * 100);
            long threshold = (long) (Math.random() * 100);
            String telemetryPayload = "{\"temperature\":" + temperature + "}";
            String attributePayload = "{\"threshold\":" + threshold + "}";
            doPost("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/" + DataConstants.SHARED_SCOPE, telemetryPayload, String.class, status().isOk());
            doPost("/api/plugins/telemetry/" + device.getId() + "/" + DataConstants.SHARED_SCOPE, attributePayload, String.class, status().isOk());
            expectedReportLines.add(device.getCreatedTime() + "," +
                    device.getName() + "," +
                    device.getType() + "," +
                    temperature + "," +
                    threshold);
        }

        //generate report
        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setReportTemplateId(reportTemplateId);

        if (tenantUser) {
            NotificationTarget recipient = createNotificationTarget(new AffectedUserFilter());
            NotificationTemplate notificationTemplate = saveNotificationTemplate(DefaultNotifications.reportGenerated.toTemplate());
            reportRequest.setTargets(List.of(recipient.getId().getId()));
            reportRequest.setNotificationTemplateId(notificationTemplate.getId());
        }
        doPost("/api/v2/report/request", reportRequest, Job.class);

        List<Job> jobs = await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() ->
                        findJobs(List.of(JobType.REPORT), List.of(reportTemplateId.getId())),
                result -> !result.isEmpty() && result.get(0).getStatus() == JobStatus.COMPLETED);
        Job job = jobs.get(0);
        assertThat(job.getResult().getCompletedCount()).isEqualTo(1);
        assertThat(job.getResult().getTotalCount()).isEqualTo(1);
        assertThat(job.getEntityId()).isEqualTo(reportTemplateId);
        assertThat(job.getEntityName()).isEqualTo(reportTemplate.getName());

        ReportJobResult result = (ReportJobResult) job.getResult();
        Report report = result.getReport();
        assertThat(report.getName()).isEqualTo("test.csv");
        String csvReport = doGet("/api/v2/report/" + report.getId() + "/download", String.class);

        // Check headers and content
        String[] lines = csvReport.split("\r?\n");
        assertThat(lines[0]).contains("CREATED TIME,NAME,TYPE,TEMPERATURE,THRESHOLD");
        for (int i = 0; i < devices.size(); i++) {
            assertThat(lines[i + 1]).contains(expectedReportLines.get(i));
        }

        if (tenantUser) {
            await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
                Notification reportNotification = getMyNotifications(true, 10).stream()
                        .filter(notification -> notification.getType() == NotificationType.REPORT_GENERATED)
                        .findFirst().orElse(null);
                assertThat(reportNotification).isNotNull();
                assertThat(reportNotification.getSubject()).isEqualTo("Report generated");
                assertThat(reportNotification.getText()).isEqualTo("CSV report 'test.csv' is ready");
            });
        }

        //update report file name pattern without extension
        ((CsvReportTemplateConfig) reportTemplate.getConfiguration()).setNamePattern("without_extension");
        doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);

        //generate report
        doPost("/api/v2/report/request", reportRequest, Job.class);
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() ->
                        findJobs(List.of(JobType.REPORT), List.of(reportTemplateId.getId()))
                                .stream()
                                .filter(j -> (j.getStatus() == JobStatus.COMPLETED &&
                                        ((ReportJobResult) j.getResult()).getReport().getName().equals("without_extension.csv")))
                                .collect(Collectors.toList()),
                res -> res.size() == 1);
    }

    @Test
    public void testGetReportInfos() throws Exception {
        loginTenantAdmin();
        ReportTemplate csvTemplate = buildReportTemplate(TbReportFormat.CSV);
        csvTemplate = doPost("/api/reportTemplate", csvTemplate, ReportTemplate.class);

        ReportTemplate pdfTemplate = buildReportTemplate(TbReportFormat.PDF);
        pdfTemplate = doPost("/api/reportTemplate", pdfTemplate, ReportTemplate.class);

        NotificationTarget recipient = createNotificationTarget(new AffectedUserFilter());
        NotificationTemplate notificationTemplate = saveNotificationTemplate(DefaultNotifications.reportGenerated.toTemplate());
        for (int i = 0; i < 5; i++) {
            ReportRequest csvRequest = new ReportRequest();
            csvRequest.setReportTemplateId(csvTemplate.getId());
            csvRequest.setTargets(List.of(recipient.getId().getId()));
            csvRequest.setNotificationTemplateId(notificationTemplate.getId());
            doPost("/api/v2/report/request", csvRequest, Job.class);

            ReportRequest pdfRequest = new ReportRequest();
            pdfRequest.setReportTemplateId(pdfTemplate.getId());
            pdfRequest.setTargets(List.of(recipient.getId().getId()));
            pdfRequest.setNotificationTemplateId(notificationTemplate.getId());
            doPost("/api/v2/report/request", pdfRequest, Job.class);
        }

        PageData<ReportInfo> reportInfos = await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() ->
                        doGetTypedWithPageLink("/api/v2/reportInfos/all?", new TypeReference<PageData<ReportInfo>>() {
                        }, new PageLink(30)),
                result -> result.getData().size() == 10);
        for (ReportInfo info : reportInfos.getData()) {
            assertThat(info.getUserName()).isEqualTo(TENANT_ADMIN_EMAIL);
        }

        // filter by reportTemplateId
        PageData<ReportInfo> csvReportsByTemplateId = doGetTypedWithPageLink("/api/v2/reportInfos/all?reportTemplateId=" + csvTemplate.getId().getId() + "&", new TypeReference<>() {
        }, new PageLink(30));

        assertThat(csvReportsByTemplateId.getData()).hasSize(5);
        for (ReportInfo info : csvReportsByTemplateId.getData()) {
            assertThat(info.getTemplateInfo()).isEqualTo(new EntityInfo(csvTemplate.getId(), csvTemplate.getName()));
            assertThat(info.getFormat()).isEqualTo(TbReportFormat.CSV);
        }

        // filter by userId
        PageData<ReportInfo> csvReportsByUserId = doGetTypedWithPageLink("/api/v2/reportInfos/all?userId=" + customerAdminUserId.getId() + "&", new TypeReference<>() {
        }, new PageLink(30));

        assertThat(csvReportsByUserId.getData()).isEmpty();
    }

    @Test
    public void testDownloadReportByPublicLink() throws Exception {
        List<String> expectedRows = requestSimpleReport();

        PageData<ReportInfo> reportInfos = await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() ->
                        doGetTypedWithPageLink("/api/v2/reportInfos/all?", new TypeReference<PageData<ReportInfo>>() {
                        }, new PageLink(30)),
                result -> result.getData().size() == 1);
        ReportInfo generatedReport = reportInfos.getData().get(0);

        // Report should NOT have a public link until the user makes the report public
        assertThat(generatedReport.getPublicKey()).isNull();

        //make report public
        Report updatedReport = doPut("/api/v2/report/" + generatedReport.getId() + "/public/true", "", Report.class);
        assertThat(updatedReport.isPublic()).isTrue();
        assertThat(updatedReport.getPublicKey()).isNotNull();

        // Download without authentication
        resetTokens();
        String downloaded = doGet("/api/v2/report/public/" + updatedReport.getPublicKey() + "/download", String.class);
        assertThat(Arrays.stream(downloaded.split("\\r?\\n")).map(String::trim).toList()).containsAll(expectedRows);

        // Toggle public status off
        loginTenantAdmin();
        Report disabledReport = doPut("/api/v2/report/" + generatedReport.getId() + "/public/false", "", Report.class);
        assertThat(disabledReport.isPublic()).isFalse();
        assertThat(disabledReport.getPublicKey()).isNull();

        // Verify the link stops working
        resetTokens();
        doGet("/api/v2/report/public/" + updatedReport.getPublicKey() + "/download").andExpect(status().isNotFound());
    }

    @Test
    public void testDownloadReportByPublicLink_madePublicAtRequestTime() throws Exception {
        // Request the report with makePublic=true: the public key is generated during generation (TbReportService),
        // so the report is already public when it appears - no separate PUT to /public/true is needed.
        List<String> expectedRows = requestSimpleReport(true);

        ReportInfo generatedReport = await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() ->
                        doGetTypedWithPageLink("/api/v2/reportInfos/all?", new TypeReference<PageData<ReportInfo>>() {
                        }, new PageLink(30)).getData().stream().findFirst().orElse(null),
                report -> report != null && report.isPublic());

        assertThat(generatedReport.getPublicKey()).isNotNull();

        // Download without authentication using the request-time public link
        resetTokens();
        String downloaded = doGet("/api/v2/report/public/" + generatedReport.getPublicKey() + "/download", String.class);
        assertThat(Arrays.stream(downloaded.split("\\r?\\n")).map(String::trim).toList()).containsAll(expectedRows);
    }

    private List<String> requestSimpleReport() throws Exception {
        return requestSimpleReport(false);
    }

    private List<String> requestSimpleReport(boolean makePublic) throws Exception {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDeviceTypeEntityAlias(devicesAliasId);
        EntityTableComponent tableComponent = buildEntityTableComponent(devicesAliasId);
        ReportTemplateConfig configuration = createReportConfigTemplate(List.of(tableComponent), entityAlias, TbReportFormat.CSV);

        List<String> columnHeaders = getColumnHeaders(tableComponent);
        List<List<String>> generatedValues = generateLatestTestData(columnHeaders, tableComponent.getTableHeading().getText(), configuration.getTimeDataPattern());
        List<String> expectedRows = generatedValues.stream().map(row -> String.join(",", row)).toList();

        ReportTemplate template = buildReportTemplate(TbReportFormat.CSV, configuration);
        template = doPost("/api/reportTemplate", template, ReportTemplate.class);

        ReportRequest pdfRequest = new ReportRequest();
        pdfRequest.setReportTemplateId(template.getId());
        pdfRequest.setMakePublic(makePublic);
        doPost("/api/v2/report/request", pdfRequest, Job.class);
        return expectedRows;
    }

    @Test
    public void testTimeoutForTestReport() throws Exception {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDeviceTypeEntityAlias(devicesAliasId);

        String image = StringUtils.randomAlphabetic(5) + ".jpg";
        uploadImage(HttpMethod.POST, "/api/image", image, "image/jpeg", ImageControllerTest.JPEG_IMAGE);
        List<ReportComponent> components = List.of(buildImageComponent("tb-image;/api/images/tenant/" + image));

        ReportTemplateConfig configuration = createReportConfigTemplate(components, entityAlias, TbReportFormat.PDF);

        when(tbReportService.generateTestReport(any(ReportTask.class)))
                .thenReturn(new CompletableFuture<>());

        ReportRequest request = new ReportRequest();
        request.setReportTemplateConfig(configuration);
        String errorMessage = getErrorMessage(doPost("/api/v2/report/test", request).andExpect(status().isInternalServerError()));
        assertThat(errorMessage).isEqualTo("Timeout for test report generation. Generation took more than 4000 milliseconds!");
    }

    @Test
    public void testPDFReportWithChineseAndJapaneseWords() throws Exception {
        String chineseText = "你好 (Nǐ hǎo): Hello";
        String japaneseText = "こんにちは (Konnichiwa): Hello in Japanese";

        ReportTemplateConfig configuration = PdfReportTemplateConfig.builder()
                .components(List.of(buildRichTextComponent(chineseText), buildRichTextComponent(japaneseText)))
                .build();
        List<String> expectedRows = List.of(chineseText, japaneseText);

        generateAndCheckPDFReportText(configuration, expectedRows);
    }

    @Test
    public void testCreateCSVReportByCustomerUser() throws Exception {
        loginCustomerAdminUser();
        testCSVReportWithEntityTable();
    }

    @Test
    public void testCreateReportJobByCustomer() throws Exception {
        loginCustomerAdminUser();
        testCreateReportJob(Authority.CUSTOMER_USER);
    }

    @Test
    public void testRequestReportForOutOfHierarchyUserDenied() throws Exception {
        loginCustomerAdminUser();
        ReportTemplate reportTemplate = buildReportTemplate(TbReportFormat.CSV);
        reportTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);

        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setReportTemplateId(reportTemplate.getId());
        // The tenant administrator is outside the customer's ownership hierarchy
        reportRequest.setUserId(tenantAdminUserId.getId().toString());

        doPost("/api/v2/report/request", reportRequest).andExpect(status().isForbidden());
    }

    @Test
    public void testJobIsolationBetweenCustomers() throws Exception {
        // Customer A creates a report template and submits a job
        loginCustomerAdminUser();
        ReportTemplate reportTemplate = buildReportTemplate(TbReportFormat.CSV);
        reportTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);

        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setReportTemplateId(reportTemplate.getId());
        Job customerAJob = doPost("/api/v2/report/request", reportRequest, Job.class);

        // Customer A can see their own job
        List<Job> customerAJobs = findJobs(List.of(JobType.REPORT), List.of(reportTemplate.getId().getId()));
        assertThat(customerAJobs).extracting(Job::getId).contains(customerAJob.getId());

        // Customer B cannot see Customer A's job in the list
        loginDifferentCustomerAdmin();
        List<Job> customerBJobs = findJobs();
        assertThat(customerBJobs).extracting(Job::getId).doesNotContain(customerAJob.getId());

        // Customer B cannot access Customer A's job by ID
        doGet("/api/job/" + customerAJob.getId().getId()).andExpect(status().isForbidden());
    }

    @Test
    public void testPDFReportWithLineChart() throws Exception {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDeviceTypeEntityAlias(devicesAliasId);

        TimeseriesChartComponent lineChart = buildLineChartComponent(devicesAliasId);
        ReportTemplateConfig configuration = createReportConfigTemplate(List.of(lineChart), entityAlias, TbReportFormat.PDF);

        ReportRequest request = new ReportRequest();
        request.setReportTemplateConfig(configuration);
        doPost("/api/v2/report/test", request).andExpect(status().isOk());
    }

    private TimeseriesTableComponent buildTimeseriesTableComponent(String devicesAliasId) {
        return buildTimeseriesTableComponent(devicesAliasId, buildCurrentDateTimeWindow());
    }

    private TimeseriesTableComponent buildTimeseriesTableComponent(String devicesAliasId, TimeWindowConfiguration timewindow) {
        TimeseriesTableComponent tsComponent = new TimeseriesTableComponent();
        tsComponent.setShowTimestamp(true);
        tsComponent.setTimestampLabel("Timestamp");
        tsComponent.setTimestampPattern("milliseconds");
        DataKey nonExistingDataKey = new DataKey("nonExisting", "timeseries", "Non existing");
        nonExistingDataKey.setUsePostProcessing(true);
        nonExistingDataKey.setPostFuncBody("return value == null ? \"N/A\" : value;");
        tsComponent.setDataSources(List.of(DataSource.builder()
                .type(DataSourceType.ENTITY)
                .entityAliasId(devicesAliasId)
                .dataKeys(List.of(
                        new DataKey("temperature", "timeseries", "TEMPERATURE"),
                        nonExistingDataKey
                ))
                .latestDataKeys(List.of(
                        new DataKey("name", "entityField", "NAME"),
                        new DataKey("active", "attribute", "ACTIVE")
                ))
                .build()));
        tsComponent.setTimewindow(timewindow);
        return tsComponent;
    }

    private List<String> getColumnHeaders(DataReportComponent component) {
        return component.getDataSources().get(0).getDataKeys().stream().map(DataKey::getLabel).collect(Collectors.toList());
    }

    private static TimeWindowConfiguration buildCurrentDateTimeWindow() {
        return buildCurrentDateTimeWindow(Aggregation.NONE, null);
    }

    private static TimeWindowConfiguration buildCurrentDateTimeWindow(Aggregation aggregationType, Interval interval) {
        TimeWindowConfiguration timewindow = new TimeWindowConfiguration();
        History history = new History();
        history.setHistoryType(2);
        history.setQuickInterval(QuickTimeInterval.CURRENT_DAY);
        history.setInterval(interval);
        timewindow.setHistory(history);
        timewindow.setTimezone(TimeZone.getDefault().getID());
        AggregationConfiguration aggregation = new AggregationConfiguration();
        aggregation.setType(aggregationType);
        aggregation.setLimit(25000);
        timewindow.setAggregation(aggregation);
        return timewindow;
    }

    private void generateAndCheckCSVReport(ReportTemplateConfig config, List<String> expectedRows) {
        ReportRequest request = new ReportRequest();
        request.setReportTemplateConfig(config);
        String csvReport = doPost("/api/v2/report/test", request, String.class);
        assertThat(Arrays.stream(csvReport.split("\\r?\\n")).map(String::trim).toList()).containsAll(expectedRows);
    }

    private void generateAndCheckPDFReportText(ReportTemplateConfig config, List<String> expectedRows) throws Exception {
        String pdfReport = generateTestPDFReportText(config);
        assertThat(Arrays.stream(pdfReport.split("\\r?\\n")).map(String::trim).toList()).containsAll(expectedRows);
    }

    private void generateAndCheckPDFReportText(ReportRequest request, List<String> expectedRows) throws Exception {
        String pdfReport = requestTestPdfReport(request);
        assertThat(Arrays.stream(pdfReport.split("\\r?\\n")).map(String::trim).toList()).containsAll(expectedRows);
    }

    private String generateTestPDFReportText(ReportTemplateConfig config) throws Exception {
        ReportRequest request = new ReportRequest();
        request.setReportTemplateConfig(config);
        return requestTestPdfReport(request);
    }

    /**
     * The fixture the four non-production notice tests share: only the format and whether the instance calls
     * itself a development one differ between them, so everything else is built once here. The tests stay
     * separate rather than becoming one parameterised run, because the CSV pair pins the notice to the first
     * row of the file while the PDF pair reads extracted page text - two different assertions, not one.
     */
    private ReportRequest nonProductionNoticeReportRequest(TbReportFormat format) {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDeviceTypeEntityAlias(devicesAliasId);
        EntityTableComponent tableComponent = buildEntityTableComponent(devicesAliasId);

        ReportRequest request = new ReportRequest();
        request.setReportTemplateConfig(createReportConfigTemplate(List.of(tableComponent), entityAlias, format));
        return request;
    }

    private String requestTestPdfReport(ReportRequest request) throws Exception {
        ResultActions result = doPost("/api/v2/report/test", request);
        byte[] pdfBytes = result.andReturn().getResponse().getContentAsByteArray();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private ReportTemplateConfig createReportConfigTemplate(List<ReportComponent> component, EntityAlias entityAlias, TbReportFormat tbReportFormat) {
        String timeDataPattern = "yyyy-MM-dd";
        return tbReportFormat == TbReportFormat.PDF ?
                PdfReportTemplateConfig.builder()
                        .entityAliases(List.of(entityAlias))
                        .components(component)
                        .timeDataPattern(timeDataPattern)
                        .build() :
                CsvReportTemplateConfig.builder()
                        .entityAliases(List.of(entityAlias))
                        .components(component)
                        .timeDataPattern(timeDataPattern)
                        .build();
    }

    private EntityTableComponent buildEntityTableComponent(String devicesAliasId) {
        EntityTableComponent tableComponent = new EntityTableComponent();
        List<DataKey> dataKeys = List.of(
                DataKey.builder().name("createdTime").type("entityField").label("CREATED TIME").usePostProcessing(false).build(),
                DataKey.builder().name("name").type("entityField").label("NAME").usePostProcessing(false).build(),
                DataKey.builder().name("type").type("entityField").label("TYPE").usePostProcessing(false).build(),
                DataKey.builder().name("temperature").type("timeseries").label("TEMPERATURE").usePostProcessing(false).units("K").decimals(2).build(),
                DataKey.builder().name("threshold").type("attribute").label("THRESHOLD").usePostProcessing(false).build()
        );
        tableComponent.setDataSources(List.of(DataSource.builder()
                .type(DataSourceType.ENTITY)
                .entityAliasId(devicesAliasId)
                .dataKeys(dataKeys)
                .build()));
        tableComponent.setShowTableHeading(true);
        String tableHeadingText = "This is my table";
        Heading tableHeading = new Heading();
        tableHeading.setText(tableHeadingText);
        tableComponent.setTableHeading(tableHeading);
        tableComponent.setTableSortOrder(new TableSortOrder("NAME", TableSortOrder.Direction.ASC));
        return tableComponent;
    }

    private TimeseriesChartComponent buildLineChartComponent(String devicesAliasId) {
        List<DataKey> dataKeys = List.of(
                DataKey.builder().name("createdTime").type("entityField").label("CREATED TIME").usePostProcessing(false).build(),
                DataKey.builder().name("name").type("entityField").label("NAME").usePostProcessing(false).build(),
                DataKey.builder().name("type").type("entityField").label("TYPE").usePostProcessing(false).build(),
                DataKey.builder().name("temperature").type("timeseries").label("TEMPERATURE").usePostProcessing(false).units("K").decimals(2).build(),
                DataKey.builder().name("threshold").type("attribute").label("THRESHOLD").usePostProcessing(false).build()
        );

        TimeseriesChartComponent timeseriesChartComponent = new TimeseriesChartComponent();
        ReportTimeSeriesChartSettings settings = new ReportTimeSeriesChartSettings();
        timeseriesChartComponent.setSubType(ReportComponentSubType.LINE_CHART);
        timeseriesChartComponent.setDataSources(List.of(DataSource.builder()
                .type(DataSourceType.ENTITY)
                .entityAliasId(devicesAliasId)
                .dataKeys(dataKeys)
                .build()));
        timeseriesChartComponent.setTimeSeriesChartSettings(settings);
        timeseriesChartComponent.setTimewindow(buildCurrentDateTimeWindow());
        return timeseriesChartComponent;
    }

    private RichTextComponent buildRichTextComponent(String richText) {
        RichTextComponent richTextComponent = new RichTextComponent();
        richTextComponent.setValue(richText);
        return richTextComponent;
    }

    private List<List<String>> generateLatestTestData(List<String> columnHeaders, String tableHeading, String timeDataPattern) throws Exception {
        List<List<String>> expectedLines = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeDataPattern).withZone(ZoneId.systemDefault());

        for (int i = 0; i < 15; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            device = doPost("/api/device", device, Device.class);

            long temperature = (long) (Math.random() * 100);
            long threshold = (long) (Math.random() * 100);
            String telemetryPayload = "{\"temperature\":" + temperature + "}";
            String attributePayload = "{\"threshold\":" + threshold + "}";
            doPost("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/" + DataConstants.SHARED_SCOPE, telemetryPayload, String.class, status().isOk());
            doPost("/api/plugins/telemetry/" + device.getId() + "/" + DataConstants.SHARED_SCOPE, attributePayload, String.class, status().isOk());

            expectedLines.add(List.of(
                    formatter.format(Instant.ofEpochMilli(device.getCreatedTime())),
                    device.getName(),
                    device.getType(),
                    BigDecimal.valueOf(temperature).setScale(2, RoundingMode.HALF_UP) + "K",
                    String.valueOf(threshold)));
        }

        expectedLines.sort((a, b) -> a.get(1).compareToIgnoreCase(b.get(1))); // sort by NAME column
        expectedLines.add(0, List.of(tableHeading)); // table heading
        expectedLines.add(1, columnHeaders); // column headers
        return expectedLines;
    }

    private List<List<String>> generateTestAlarmData(List<String> columnHeaders, String timeDataPattern) throws InterruptedException {
        List<List<String>> expectedLines = new ArrayList<>();
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setCustomerId(customerId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(doPost("/api/device", device, Device.class));
            Thread.sleep(1);
        }

        expectedLines.add(columnHeaders);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeDataPattern).withZone(ZoneId.systemDefault());
        for (int i = 0; i < devices.size(); i++) {
            Alarm alarm = new Alarm();
            Device device = devices.get(i);
            alarm.setOriginator(device.getId());
            alarm.setType("alarm" + i);
            alarm.setSeverity(AlarmSeverity.WARNING);
            Alarm createdAlarm = doPost("/api/alarm", alarm, Alarm.class);
            Thread.sleep(1);

            expectedLines.add(List.of(
                    formatter.format(Instant.ofEpochMilli(device.getCreatedTime())),
                    device.getName(),
                    alarm.getType()));
        }
        return expectedLines;
    }

    private List<List<String>> generateTestAlarmData(String tableHeading, Device device, List<String> columnHeaders, String timeDataPattern) throws InterruptedException {
        List<List<String>> expectedLines = new ArrayList<>();

        if (tableHeading != null) {
            expectedLines.add(List.of(tableHeading));
        }
        expectedLines.add(columnHeaders);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeDataPattern).withZone(ZoneId.systemDefault());
        Alarm alarm = new Alarm();
        alarm.setOriginator(device.getId());
        alarm.setType("alarm");
        alarm.setSeverity(AlarmSeverity.WARNING);
        Alarm createdAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Thread.sleep(1);

        expectedLines.add(List.of(
                formatter.format(Instant.ofEpochMilli(device.getCreatedTime())),
                device.getName(),
                alarm.getType()));

        return expectedLines;
    }

    private AlarmTableComponent buildAlarmTableComponent(String devicesAliasId, String deviceId) {
        AlarmTableComponent alarmTableComponent = new AlarmTableComponent();
        AlarmFilterConfig alarmFilterConfig = new AlarmFilterConfig();
        alarmFilterConfig.setSeverityList(List.of(AlarmSeverity.WARNING));
        List<DataKey> dataKeys = List.of(
                new DataKey("createdTime", "alarm", "ALARM CREATED TIME"),
                new DataKey("name", "entityField", "ORIGINATOR"),
                new DataKey("type", "alarm", "ALARM TYPE")
        );
        alarmTableComponent.setAlarmSource(DataSource.builder()
                .type(devicesAliasId != null ? DataSourceType.ENTITY : DataSourceType.DEVICE)
                .deviceId(deviceId)
                .entityAliasId(devicesAliasId)
                .alarmFilterConfig(alarmFilterConfig)
                .dataKeys(dataKeys)
                .build());
        TimeWindowConfiguration timewindow = buildCurrentDateTimeWindow();
        alarmTableComponent.setTimewindow(timewindow);
        return alarmTableComponent;
    }

    private ImageComponent buildImageComponent(String url) {
        ImageComponent imageComponent = new ImageComponent();
        imageComponent.setImageUrl(url);
        return imageComponent;
    }

    private List<List<String>> generateTsData(List<String> columnHeaders) throws Exception {
        List<Device> devices = new ArrayList<>();
        List<List<String>> expectedLines = new ArrayList<>();
        int telemetryCount = 3;

        expectedLines.add(columnHeaders);

        for (int i = 0; i < 18; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            device = doPost("/api/device", device, Device.class);
            devices.add(device);

            for (int j = 0; j < telemetryCount; j++) {
                long temperature = (long) (Math.random() * 100);
                long threshold = (long) (Math.random() * 100);
                String attributePayload = "{\"threshold\":" + threshold + "}";
                long ts = System.currentTimeMillis() - 300000L * j;

                doPost("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode(String.format("{\"ts\": %s, \"values\": {\"temperature\":%s}}", ts, temperature)));
                doPost("/api/plugins/telemetry/" + device.getId() + "/" + DataConstants.SHARED_SCOPE, attributePayload, String.class, status().isOk());
                expectedLines.add(List.of(
                        String.valueOf(ts),
                        String.valueOf(temperature),
                        "N/A",
                        device.getName(),
                        "false"));
            }
        }
        return expectedLines;
    }

    private List<List<String>> generateTsDataForSumAggr(List<String> columnHeaders) throws Exception {
        Device device = new Device();
        device.setName("To test aggr device");
        device.setType("default");
        device.setLabel("testLabel" + (int) (Math.random() * 1000));
        device = doPost("/api/device", device, Device.class);

        List<List<String>> expectedLines = new ArrayList<>();

        expectedLines.add(columnHeaders);

        long energySum = 0;
        for (int i = 0; i < 18; i++) {
            long energyConsumption = (long) (Math.random() * 100);
            long ts = System.currentTimeMillis() - 300L * i;
            doPost("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode(String.format("{\"ts\": %s, \"values\": {\"energy\":%s}}", ts, energyConsumption)));
            energySum += energyConsumption;
        }
        expectedLines.add(List.of(
                String.valueOf(energySum),
                device.getName()));
        return expectedLines;
    }

    private ReportTemplate buildReportTemplate(TbReportFormat format) {
        ReportTemplate template = new ReportTemplate();
        template.setName(StringUtils.randomAlphabetic(10));
        template.setType(ReportTemplateType.REPORT);
        template.setFormat(format);
        CsvReportTemplateConfig configuration = new CsvReportTemplateConfig();
        configuration.setComponents(new ArrayList<>());
        template.setConfiguration(configuration);
        return template;
    }

    private ReportTemplate buildReportTemplate(TbReportFormat format, ReportTemplateConfig reportTemplateConfig) {
        ReportTemplate template = new ReportTemplate();
        template.setName(StringUtils.randomAlphabetic(10));
        template.setType(ReportTemplateType.REPORT);
        template.setFormat(format);
        template.setConfiguration(reportTemplateConfig);
        return template;
    }

    private static EntityAlias buildDeviceTypeEntityAlias(String aliasId) {
        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");
        return new EntityAlias(aliasId, "devices", filter);
    }

}
