/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.notification.rule.trigger.RateLimitsTrigger;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.thingsboard.server.common.data.limit.LimitedApi.TRANSPORT_MESSAGES_PER_GATEWAY;

@DaoSqlTest
@TestPropertySource(properties = {
        "service.integrations.supported=ALL",
        "transport.mqtt.enabled=true",
})
public class MqttGatewayRateLimitsTest extends AbstractControllerTest {

    private static final String TOPIC = "v1/gateway/telemetry";
    private static final String DEVICE_A = "DeviceA";
    private static final String DEVICE_B = "DeviceB";

    private DeviceId gatewayId;
    private String gatewayAccessToken;

    @SpyBean
    private NotificationRuleProcessor notificationRuleProcessor;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        TenantProfile tenantProfile = doGet("/api/tenantProfile/" + tenantProfileId, TenantProfile.class);
        Assert.assertNotNull(tenantProfile);

        DefaultTenantProfileConfiguration profileConfiguration = (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();

        profileConfiguration.setTransportGatewayMsgRateLimit(null);
        profileConfiguration.setTransportGatewayTelemetryMsgRateLimit(null);
        profileConfiguration.setTransportGatewayTelemetryDataPointsRateLimit(null);

        doPost("/api/tenantProfile", tenantProfile);

        loginTenantAdmin();
        createGateway();

        Mockito.reset(notificationRuleProcessor);
    }

    @Test
    public void transportGatewayMsgRateLimitTest() throws Exception {
        // Device A 2 msgs success ('create device', 'to device actor'), Device B 'create device success' , 'to device actor' - limited
        transportGatewayRateLimitTest(profileConfiguration -> profileConfiguration.setTransportGatewayMsgRateLimit("3:600"));
    }

    @Test
    public void transportGatewayTelemetryMsgRateLimitTest() throws Exception {
        transportGatewayRateLimitTest(profileConfiguration -> profileConfiguration.setTransportGatewayTelemetryMsgRateLimit("1:600"));
    }

    @Test
    public void transportGatewayTelemetryDataPointsRateLimitTest() throws Exception {
        transportGatewayRateLimitTest(profileConfiguration -> profileConfiguration.setTransportGatewayTelemetryDataPointsRateLimit("1:600"));
    }

    private void transportGatewayRateLimitTest(Consumer<DefaultTenantProfileConfiguration> profileConfiguration) throws Exception {
        loginSysAdmin();

        TenantProfile tenantProfile = doGet("/api/tenantProfile/" + tenantProfileId, TenantProfile.class);
        Assert.assertNotNull(tenantProfile);

        profileConfiguration.accept((DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration());

        doPost("/api/tenantProfile", tenantProfile);

        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        client.publishAndWait(TOPIC, getGatewayPayload(DEVICE_A));

        loginTenantAdmin();

        Device deviceA = getDeviceByName(DEVICE_A);

        var deviceATrigger = createRateLimitsTrigger(deviceA);

        Mockito.verify(notificationRuleProcessor, Mockito.never()).process(eq(deviceATrigger));

        try {
            client.publishAndWait(TOPIC, getGatewayPayload(DEVICE_B));
        } catch (Exception t) {
        }

        Device deviceB = getDeviceByName(DEVICE_B);

        var deviceBTrigger = createRateLimitsTrigger(deviceB);

        Mockito.verify(notificationRuleProcessor, Mockito.times(1)).process(deviceBTrigger);

        if (client.isConnected()) {
            client.disconnect();
        }
    }

    private void createGateway() throws Exception {
        Device device = new Device();
        device.setName("gateway");
        ObjectNode additionalInfo = JacksonUtil.newObjectNode();
        additionalInfo.put("gateway", true);
        device.setAdditionalInfo(additionalInfo);
        device = doPost("/api/device", device, Device.class);
        assertNotNull(device);
        gatewayId = device.getId();
        assertNotNull(gatewayId);

        DeviceCredentials deviceCredentials = doGet("/api/device/" + gatewayId + "/credentials", DeviceCredentials.class);
        assertNotNull(deviceCredentials);
        assertEquals(gatewayId, deviceCredentials.getDeviceId());
        gatewayAccessToken = deviceCredentials.getCredentialsId();
        assertNotNull(gatewayAccessToken);
    }

    private Device getDeviceByName(String deviceName) throws Exception {
        Device device = doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);
        assertNotNull(device);
        return device;
    }

    private byte[] getGatewayPayload(String deviceName) {
        return String.format("{\"%s\": [{\"values\": {\"temperature\": 42}}]}", deviceName).getBytes();
    }

    private RateLimitsTrigger createRateLimitsTrigger(Device device) {
        return RateLimitsTrigger.builder()
                .tenantId(tenantId)
                .api(TRANSPORT_MESSAGES_PER_GATEWAY)
                .limitLevel(device.getId())
                .limitLevelEntityName(device.getName())
                .build();
    }
}
