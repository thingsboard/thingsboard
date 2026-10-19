// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.apiusage;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.DataSourceType;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.controller.TbUrlConstants;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.service.report.ReportJobProcessor.REPORT_CREATION_DISABLED;

@DaoSqlTest
@TestPropertySource(properties = {
        "usage.stats.report.enabled=true",
        "usage.stats.report.interval=2",
        "usage.stats.report.urgent_interval=1",
        "usage.stats.gauge_report_interval=1",
})
public class ApiUsageTest extends AbstractControllerTest {

    private Tenant savedTenant;
    private User tenantAdmin;

    private static final int MAX_DP_ENABLE_VALUE = 12;
    private static final int MAX_SMS_ENABLE_VALUE = 10;
    private static final double WARN_THRESHOLD_VALUE = 0.5;
    private static final int MAX_ALLOWED_REPORT = 5;

    @Autowired
    private ApiUsageStateService apiUsageStateService;
    @Autowired
    private TbApiUsageReportClient apiUsageReportClient;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        TenantProfile tenantProfile = createTenantProfile();
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        assertNotNull(savedTenantProfile);

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        tenant.setTenantProfileId(savedTenantProfile.getId());
        savedTenant = saveTenant(tenant);
        tenantId = savedTenant.getId();
        assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @Test
    public void testDbStorageApiUsage() throws Exception {
        Device device = createDevice();
        assertNotNull(device);
        String telemetryPayload = "{\"temperature\":25, \"humidity\":60}";
        String url = TbUrlConstants.TELEMETRY_URL_PREFIX + "/DEVICE/" + device.getId() + "/timeseries/ANY";

        long VALUE_WARNING = (long) (MAX_DP_ENABLE_VALUE * WARN_THRESHOLD_VALUE) / 2;

        for (int i = 0; i < VALUE_WARNING; i++) {
            doPostAsync(url, telemetryPayload, String.class, status().isOk());
        }

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() ->
                assertEquals(ApiUsageStateValue.WARNING, getUsageState().getDbStorageState()));

        long VALUE_DISABLE = (long) (MAX_DP_ENABLE_VALUE - (MAX_DP_ENABLE_VALUE * WARN_THRESHOLD_VALUE)) / 2;

        for (int i = 0; i < VALUE_DISABLE; i++) {
            doPostAsync(url, telemetryPayload, String.class, status().isOk());
        }

        await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertEquals(ApiUsageStateValue.DISABLED, getUsageState().getDbStorageState());
                });
    }

    @Test
    public void testSmsApiUsage() {
        long smsWarnThreshold = (long) (MAX_SMS_ENABLE_VALUE * WARN_THRESHOLD_VALUE);

        for (int i = 0; i < smsWarnThreshold; i++) {
            apiUsageReportClient.report(tenantId, null, ApiUsageRecordKey.SMS_EXEC_COUNT);
        }

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() ->
                assertEquals(ApiUsageStateValue.WARNING, getUsageState().getSmsExecState()));

        long smsDisableCount = MAX_SMS_ENABLE_VALUE - smsWarnThreshold;

        for (int i = 0; i < smsDisableCount; i++) {
            apiUsageReportClient.report(tenantId, null, ApiUsageRecordKey.SMS_EXEC_COUNT);
        }

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() ->
                assertEquals(ApiUsageStateValue.DISABLED, getUsageState().getSmsExecState()));
    }

    @Test
    public void testReportApiUsage() {
        ReportTemplate reportTemplate = buildTestReportTemplate();
        ReportTemplate savedReportTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);

        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setReportTemplateId(savedReportTemplate.getId());

        for (int i = 0; i < MAX_ALLOWED_REPORT; i++) {
            doPost("/api/v2/report/request", reportRequest, Job.class);
        }

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() ->
                assertEquals(ApiUsageStateValue.DISABLED, getUsageState().getReportExecState()));

        Job job = doPost("/api/v2/report/request", reportRequest, Job.class);
        Job failedJob = await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> doGet("/api/job/" + job.getId(), Job.class),
                result -> result.getStatus() == JobStatus.FAILED);
        assertThat(failedJob.getResult().getGeneralError()).isEqualTo(REPORT_CREATION_DISABLED);
    }

    private ApiUsageState getUsageState() {
        return apiUsageStateService.findTenantApiUsageState(tenantId);
    }

    private static ReportTemplate buildTestReportTemplate() {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDevicesEntityAlias(devicesAliasId);

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

        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("My report");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setDescription("My report");
        PdfReportTemplateConfig pdfReportTemplateConfig = new PdfReportTemplateConfig();
        pdfReportTemplateConfig.setEntityAliases(List.of(entityAlias));
        pdfReportTemplateConfig.setComponents(List.of(tableComponent));
        reportTemplate.setConfiguration(pdfReportTemplateConfig);
        return reportTemplate;
    }

    private static EntityAlias buildDevicesEntityAlias(String aliasId) {
        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");
        return new EntityAlias(aliasId, "devices", filter);
    }

    private TenantProfile createTenantProfile() {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("Tenant Profile");
        tenantProfile.setDescription("Tenant Profile" + " Test");

        TenantProfileData tenantProfileData = new TenantProfileData();
        DefaultTenantProfileConfiguration config = DefaultTenantProfileConfiguration.builder()
                .maxDPStorageDays(MAX_DP_ENABLE_VALUE)
                .maxSms(MAX_SMS_ENABLE_VALUE)
                .smsEnabled(true)
                .warnThreshold(WARN_THRESHOLD_VALUE)
                .maxGeneratedReports(MAX_ALLOWED_REPORT)
                .build();

        tenantProfileData.setConfiguration(config);
        tenantProfile.setProfileData(tenantProfileData);
        return tenantProfile;
    }

    private Device createDevice() throws Exception {
        String testToken = "TEST_TOKEN";

        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);

        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId(testToken);

        SaveDeviceWithCredentialsRequest saveRequest = new SaveDeviceWithCredentialsRequest(device, deviceCredentials);

        return readResponse(doPost("/api/device-with-credentials", saveRequest).andExpect(status().isOk()), Device.class);
    }

}
