// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.msa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

@Slf4j
@Listeners(TestListener.class)
public abstract class AbstractContainerTest {
    protected static final String HTTPS_URL = "https://localhost";

    protected static final int TIMEOUT = 30;

    protected final static String TEST_PROVISION_DEVICE_KEY = "test_provision_key";
    protected final static String TEST_PROVISION_DEVICE_SECRET = "test_provision_secret";
    protected static long timeoutMultiplier = 1;
    protected static final String TELEMETRY_KEY = "temperature";
    protected static final String ATTRIBUTE_KEY = "humidity";
    protected static final String TELEMETRY_VALUE = "42";
    protected static final int CONNECT_TRY_COUNT = 50;
    protected static final int CONNECT_TIMEOUT_MS = 500;
    protected static final String SYS_ADMIN_EMAIL = "sysadmin@thingsboard.org";
    protected static final String SYS_ADMIN_PASSWORD = "sysadmin";

    protected static final ContainerTestSuite containerTestSuite = ContainerTestSuite.getInstance();
    protected static TestRestClient testRestClient;
    protected static TestRestClient remoteHttpClient;
    protected static TestRestClient integrationExecutorHttpClient;

    @BeforeSuite
    public void beforeSuite() {
        if ("false".equals(System.getProperty("runLocal", "false"))) {
            containerTestSuite.start();
        }
        testRestClient = new TestRestClient(TestProperties.getBaseUrl());
        testRestClient.ensureSystemSetup(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD, true);
        remoteHttpClient = new TestRestClient(TestProperties.getRemoteHttpUrl());
        integrationExecutorHttpClient = new TestRestClient(TestProperties.getIntegrationExecutorHttpUrl());
        if (!"kafka".equals(System.getProperty("blackBoxTests.queue", "kafka"))) {
            timeoutMultiplier = 10;
        }
    }

    // alwaysRun, so a failure in @BeforeSuite - which skips the configuration methods that depend on it - still
    // tears down whatever the bring-up had already created.
    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        if (containerTestSuite.isActive()) {
            containerTestSuite.stop();
        }
    }

    protected WsClient subscribeToWebSocket(DeviceId deviceId, String scope, CmdsType property) throws Exception {
        String webSocketUrl = TestProperties.getWebSocketUrl();
        WsClient wsClient = new WsClient(new URI(webSocketUrl + "/api/ws/plugins/telemetry?token=" + testRestClient.getToken()), timeoutMultiplier);
        if (webSocketUrl.matches("^(wss)://.*$")) {
            SSLContextBuilder builder = SSLContexts.custom();
            builder.loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true);
            wsClient.setSocketFactory(builder.build().getSocketFactory());
        }
        wsClient.connectBlocking();

        JsonObject cmdsObject = new JsonObject();
        cmdsObject.addProperty("entityType", EntityType.DEVICE.name());
        cmdsObject.addProperty("entityId", deviceId.toString());
        cmdsObject.addProperty("scope", scope);
        cmdsObject.addProperty("cmdId", new Random().nextInt(100));

        JsonArray cmd = new JsonArray();
        cmd.add(cmdsObject);
        JsonObject wsRequest = new JsonObject();
        wsRequest.add(property.toString(), cmd);
        wsClient.send(wsRequest.toString());
        wsClient.waitForFirstReply();
        return wsClient;
    }

    protected Map<String, Long> getExpectedLatestValues(long ts) {
        return ImmutableMap.<String, Long>builder()
                .put("booleanKey", ts)
                .put("stringKey", ts)
                .put("doubleKey", ts)
                .put("longKey", ts)
                .build();
    }

    protected boolean verify(WsTelemetryResponse wsTelemetryResponse, String key, Long expectedTs, String expectedValue) {
        List<Object> list = wsTelemetryResponse.getDataValuesByKey(key);
        return expectedTs.equals(list.get(0)) && expectedValue.equals(list.get(1));
    }

    protected boolean verify(WsTelemetryResponse wsTelemetryResponse, String key, String expectedValue) {
        List<Object> list = wsTelemetryResponse.getDataValuesByKey(key);
        return expectedValue.equals(list.get(1));
    }

    protected JsonObject createGatewayConnectPayload(String deviceName) {
        JsonObject payload = new JsonObject();
        payload.addProperty("device", deviceName);
        return payload;
    }

    protected JsonObject createGatewayPayload(String deviceName, long ts) {
        JsonObject payload = new JsonObject();
        payload.add(deviceName, createGatewayTelemetryArray(ts));
        return payload;
    }

    protected JsonArray createGatewayTelemetryArray(long ts) {
        JsonArray telemetryArray = new JsonArray();
        if (ts > 0)
            telemetryArray.add(createPayload(ts));
        else
            telemetryArray.add(createPayload());
        return telemetryArray;
    }

    protected JsonObject createPayload(long ts) {
        JsonObject values = createPayload();
        JsonObject payload = new JsonObject();
        payload.addProperty("ts", ts);
        payload.add("values", values);
        return payload;
    }

    protected JsonObject createPayload() {
        JsonObject values = new JsonObject();
        values.addProperty("stringKey", "value1");
        values.addProperty("booleanKey", true);
        values.addProperty("doubleKey", 42.6);
        values.addProperty("longKey", 73L);

        return values;
    }

    protected enum CmdsType {
        TS_SUB_CMDS("tsSubCmds"),
        HISTORY_CMDS("historyCmds"),
        ATTR_SUB_CMDS("attrSubCmds");

        private final String text;

        CmdsType(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    protected JsonNode createPayloadForUplink(Device device, String parameterKey, String parameterValue) throws JsonProcessingException {
        JsonObject values = new JsonObject();
        values.addProperty("deviceName", device.getName());
        values.addProperty("deviceType", device.getType());
        values.addProperty(parameterKey, parameterValue);
        return JacksonUtil.toJsonNode(values.toString());
    }

    protected JsonNode createPayloadForUplink(Device device, String temperatureValue) throws JsonProcessingException {
        return createPayloadForUplink(device, TELEMETRY_KEY, temperatureValue);
    }

    protected JsonNode createPayloadForUplink() {
        ObjectNode values = JacksonUtil.newObjectNode();
        values.put(TELEMETRY_KEY, TELEMETRY_VALUE);
        return values;
    }

    protected DeviceProfile updateDeviceProfileWithProvisioningStrategy(DeviceProfile deviceProfile, DeviceProfileProvisionType provisionType) {
        DeviceProfileProvisionConfiguration provisionConfiguration;
        String testProvisionDeviceKey = TEST_PROVISION_DEVICE_KEY;
        deviceProfile.setProvisionType(provisionType);
        provisionConfiguration = switch (provisionType) {
            case ALLOW_CREATE_NEW_DEVICES -> new AllowCreateNewDevicesDeviceProfileProvisionConfiguration(TEST_PROVISION_DEVICE_SECRET);
            case CHECK_PRE_PROVISIONED_DEVICES -> new CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration(TEST_PROVISION_DEVICE_SECRET);
            default -> {
                testProvisionDeviceKey = null;
                yield new DisabledDeviceProfileProvisionConfiguration(null);
            }
        };
        DeviceProfileData deviceProfileData = deviceProfile.getProfileData();
        deviceProfileData.setProvisionConfiguration(provisionConfiguration);
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setProvisionDeviceKey(testProvisionDeviceKey);
        return testRestClient.postDeviceProfile(deviceProfile);
    }

    protected void updateDefaultTenantProfile(Consumer<TenantProfile> updater) {
        EntityInfo defaultTenantProfileInfo = testRestClient.getDefaultTenantProfileInfo();
        TenantProfile oldTenantProfile = testRestClient.getTenantProfileById(defaultTenantProfileInfo.getId().getId().toString());
        TenantProfile tenantProfile = JacksonUtil.clone(oldTenantProfile);
        updater.accept(tenantProfile);
        testRestClient.postTenantProfile(tenantProfile);
    }

}
