// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest.MQTT_PORT;

@DaoSqlTest
@TestPropertySource(properties = {
        "service.integrations.supported=ALL",
        "transport.mqtt.enabled=true",
        "usage.stats.report.enabled=true",
        "usage.stats.report.interval=2",
        "usage.stats.report.urgent_interval=1",
})
@Slf4j
public class MqttTransportFeatureDisabledTest extends AbstractControllerTest {

    private static final int MAX_TRANSPORT_MESSAGES = 10;
    private static final double WARN_THRESHOLD = 0.5;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        log.warn("transport.mqtt.bind_port = {}", MQTT_PORT);
        registry.add("transport.mqtt.bind_port", () -> MQTT_PORT);
    }

    @Autowired
    private ApiUsageStateService apiUsageStateService;

    @Autowired
    private TbApiUsageReportClient apiUsageReportClient;

    private String deviceAccessToken;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = doGet("/api/tenantProfile/" + tenantProfileId, TenantProfile.class);
        DefaultTenantProfileConfiguration config =
                (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();
        config.setMaxTransportMessages(MAX_TRANSPORT_MESSAGES);
        config.setWarnThreshold(WARN_THRESHOLD);
        doPost("/api/tenantProfile", tenantProfile);

        loginTenantAdmin();
        Device device = new Device();
        device.setName("Transport disable test device");
        device.setType("default");
        device = doPost("/api/device", device, Device.class);
        DeviceCredentials credentials = doGet("/api/device/" + device.getId().getId() + "/credentials", DeviceCredentials.class);
        deviceAccessToken = credentials.getCredentialsId();
    }

    @After
    public void afterTest() throws Exception {
        try {
            loginSysAdmin();
            TenantProfile tenantProfile = doGet("/api/tenantProfile/" + tenantProfileId, TenantProfile.class);
            DefaultTenantProfileConfiguration config = (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();
            config.setMaxTransportMessages(0);
            doPost("/api/tenantProfile", tenantProfile);
        } catch (Exception e) {
            log.warn("Failed to reset tenant profile maxTransportMessages; subsequent tests in this class may inherit a stale limit.", e);
        }
    }

    @Test
    public void testLiveMqttSessionClosedWhenTransportDisabled() throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(deviceAccessToken);
        Assert.assertTrue("MQTT client must be connected before flipping transport state", client.isConnected());

        for (int i = 0; i < MAX_TRANSPORT_MESSAGES + 5; i++) {
            apiUsageReportClient.report(tenantId, null, ApiUsageRecordKey.TRANSPORT_MSG_COUNT);
        }

        Awaitility.await("transport state flips to DISABLED")
                .atMost(15, TimeUnit.SECONDS)
                .until(() -> apiUsageStateService.findTenantApiUsageState(tenantId).getTransportState() == ApiUsageStateValue.DISABLED);

        Awaitility.await("MQTT client receives server-side disconnect")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> !client.isConnected());

        try {
            client.disconnectForcibly();
        } catch (Exception ignored) {}
    }

}
