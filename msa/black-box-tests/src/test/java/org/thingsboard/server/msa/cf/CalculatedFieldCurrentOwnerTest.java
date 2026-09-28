// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.cf;

import com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CurrentOwnerDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.ScriptCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.common.data.AttributeScope.SERVER_SCOPE;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomer;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultDeviceProfile;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultTenantAdmin;

public class CalculatedFieldCurrentOwnerTest extends AbstractContainerTest {

    public final int TIMEOUT = 60;
    public final int POLL_INTERVAL = 1;

    private TenantId tenantId;
    private UserId tenantAdminId;

    @BeforeClass
    public void beforeClass() {
        testRestClient.login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);

        tenantId = testRestClient.postTenant(EntityPrototypes.defaultTenantPrototype("Tenant")).getId();
        tenantAdminId = testRestClient.createUserAndLogin(defaultTenantAdmin(tenantId, "tenantAdmin@thingsboard.org"), "tenant");
    }

    @BeforeMethod
    public void beforeMethod() {
        testRestClient.login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);
    }

    @AfterClass
    public void afterClass() {
        testRestClient.resetToken();
        testRestClient.login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);
        testRestClient.deleteTenant(tenantId);
    }

    @Test
    public void testPerformInitialCalculationWhenCurrentOwner() {
        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);

        DeviceProfileId deviceProfileId = testRestClient.postDeviceProfile(defaultDeviceProfile("Device Profile 1")).getId();
        String deviceToken = "zm235nIVf263lvnTP2XBE";
        Device device = testRestClient.postDevice(deviceToken, createDevice("Device 1", deviceProfileId));
        CustomerId customerId = testRestClient.postCustomer(defaultCustomer(tenantId, "Customer 1")).getId();

        testRestClient.changeOwner(customerId, device.getId());
        testRestClient.postTelemetryAttribute(customerId, SERVER_SCOPE, JacksonUtil.toJsonNode("{\"attrKey\":5}"));

        CalculatedField savedCalculatedField = createSimpleCalculatedField(device.getId());

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode result = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(result).isNotNull();
                    assertThat(result.get("result")).isNotNull();
                    assertThat(result.get("result").get(0).get("value").asText()).isEqualTo("105");
                });

        testRestClient.postTelemetryAttribute(customerId, SERVER_SCOPE, JacksonUtil.toJsonNode("{\"attrKey\":15}"));

        await().alias("update telemetry -> perform calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode result = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(result).isNotNull();
                    assertThat(result.get("result")).isNotNull();
                    assertThat(result.get("result").get(0).get("value").asText()).isEqualTo("115");
                });

        testRestClient.deleteCalculatedFieldIfExists(savedCalculatedField.getId());
    }

    @Test
    public void testPerformInitialCalculationWhenOwnerChanged() {
        testRestClient.login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);

        testRestClient.postTelemetryAttribute(tenantId, SERVER_SCOPE, JacksonUtil.toJsonNode("{\"attrKey\":50}"));

        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);

        DeviceProfileId deviceProfileId = testRestClient.postDeviceProfile(defaultDeviceProfile("Device Profile 2")).getId();
        String deviceToken = "zmzUVndirwl5jzx8rtgiBE";
        Device device = testRestClient.postDevice(deviceToken, createDevice("Device 2", deviceProfileId));
        CustomerId customerId = testRestClient.postCustomer(defaultCustomer(tenantId, "Customer 2")).getId();

        testRestClient.changeOwner(customerId, device.getId());
        testRestClient.postTelemetryAttribute(customerId, SERVER_SCOPE, JacksonUtil.toJsonNode("{\"attrKey\":5}"));

        CalculatedField savedCalculatedField = createSimpleCalculatedField(device.getId());

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode result = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(result).isNotNull();
                    assertThat(result.get("result")).isNotNull();
                    assertThat(result.get("result").get(0).get("value").asText()).isEqualTo("105");
                });

        testRestClient.changeOwner(tenantId, device.getId());

        await().alias("change owner -> perform calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode result = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(result).isNotNull();
                    assertThat(result.get("result")).isNotNull();
                    assertThat(result.get("result").get(0).get("value").asText()).isEqualTo("150");
                });

        testRestClient.deleteCalculatedFieldIfExists(savedCalculatedField.getId());
    }

    @Test
    public void testEntityIdIsProfileAndCurrentOwner() {
        testRestClient.login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);

        testRestClient.postTelemetry(tenantId, JacksonUtil.toJsonNode("{\"key\":100}"));
        testRestClient.postTelemetry(tenantId, JacksonUtil.toJsonNode("{\"key\":200}"));

        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);
        CustomerId customerId = testRestClient.postCustomer(defaultCustomer(tenantId, "Customer 3")).getId();

        testRestClient.postTelemetry(customerId, JacksonUtil.toJsonNode("{\"key\":10}"));
        testRestClient.postTelemetry(customerId, JacksonUtil.toJsonNode("{\"key\":17}"));
        testRestClient.postTelemetry(customerId, JacksonUtil.toJsonNode("{\"key\":18}"));

        DeviceProfileId deviceProfileId = testRestClient.postDeviceProfile(defaultDeviceProfile("Device Profile 3")).getId();

        String deviceToken = "zmzUVRfrejhgni82vf6nj3E";
        Device device = testRestClient.postDevice(deviceToken, createDevice("Device 3", deviceProfileId));
        testRestClient.changeOwner(customerId, device.getId());

        String newDeviceToken = "mmmXRIVSgh65nqP2PQE";
        Device newDevice = testRestClient.postDevice(newDeviceToken, createDevice("Device 4", deviceProfileId));

        CalculatedField savedCalculatedField = createScriptCalculatedField(deviceProfileId);

        await().alias("create CF -> perform initial calculation for devices by profile").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode avgValue3 = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(avgValue3).isNotNull();
                    assertThat(avgValue3.get("avgValue")).isNotNull();
                    assertThat(avgValue3.get("avgValue").get(0).get("value").asText()).isEqualTo("15.0");

                    JsonNode avgValue4 = testRestClient.getLatestTelemetry(newDevice.getId());
                    assertThat(avgValue4).isNotNull();
                    assertThat(avgValue4.get("avgValue")).isNotNull();
                    assertThat(avgValue4.get("avgValue").get(0).get("value").asText()).isEqualTo("150.0");
                });

        testRestClient.changeOwner(tenantId, device.getId());

        await().alias("change owner -> perform calculation for device2").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode avgValue = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(avgValue).isNotNull();
                    assertThat(avgValue.get("avgValue")).isNotNull();
                    assertThat(avgValue.get("avgValue").get(0).get("value").asText()).isEqualTo("150.0");
                });

        testRestClient.deleteCalculatedFieldIfExists(savedCalculatedField.getId());
    }

    @Test
    public void testAddedNewEntityToProfile() {
        testRestClient.login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);

        testRestClient.postTelemetryAttribute(tenantId, SERVER_SCOPE, JacksonUtil.toJsonNode("{\"attrKey\":50}"));
        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);

        DeviceProfileId deviceProfileId = testRestClient.postDeviceProfile(defaultDeviceProfile("New Device Profile")).getId();
        String deviceToken = "zm235nIVf26n67vnTP2XBE";
        Device customerDevice = createDevice("Customer Device", deviceProfileId);
        CustomerId customerId = testRestClient.postCustomer(defaultCustomer(tenantId, "Customer 4")).getId();

        customerDevice.setOwnerId(customerId);
        Device device = testRestClient.postDevice(deviceToken, customerDevice);

        testRestClient.postTelemetryAttribute(customerId, SERVER_SCOPE, JacksonUtil.toJsonNode("{\"attrKey\":5}"));

        CalculatedField savedCalculatedField = createSimpleCalculatedField(deviceProfileId);

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode result = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(result).isNotNull();
                    assertThat(result.get("result")).isNotNull();
                    assertThat(result.get("result").get(0).get("value").asText()).isEqualTo("105");
                });

        String tenantDeviceToken = "zm235nIVf26n67vhdgtn91E";
        Device tenantDevice = testRestClient.postDevice(tenantDeviceToken, createDevice("Tenant Device", deviceProfileId));

        await().alias("add device -> perform calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode result = testRestClient.getLatestTelemetry(tenantDevice.getId());
                    assertThat(result).isNotNull();
                    assertThat(result.get("result")).isNotNull();
                    assertThat(result.get("result").get(0).get("value").asText()).isEqualTo("150");
                });

        testRestClient.postTelemetryAttribute(customerId, SERVER_SCOPE, JacksonUtil.toJsonNode("{\"attrKey\":80}"));

        await().alias("update telemetry for customer -> perform calculation only for customer device").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode result1 = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(result1).isNotNull();
                    assertThat(result1.get("result")).isNotNull();
                    assertThat(result1.get("result").get(0).get("value").asText()).isEqualTo("180");

                    JsonNode result2 = testRestClient.getLatestTelemetry(tenantDevice.getId());
                    assertThat(result2).isNotNull();
                    assertThat(result2.get("result")).isNotNull();
                    assertThat(result2.get("result").get(0).get("value").asText()).isEqualTo("150");
                });

        testRestClient.deleteCalculatedFieldIfExists(savedCalculatedField.getId());
    }

    private CalculatedField createSimpleCalculatedField(EntityId entityId) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("Test" + RandomStringUtils.randomAlphabetic(5));
        calculatedField.setDebugSettings(DebugSettings.all());

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("attrKey", ArgumentType.ATTRIBUTE, SERVER_SCOPE);
        argument.setRefEntityKey(refEntityKey);
        argument.setRefDynamicSourceConfiguration(new CurrentOwnerDynamicSourceConfiguration());
        config.setArguments(Map.of("a", argument));

        config.setExpression("a + 100");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("result");
        output.setDecimalsByDefault(0);
        config.setOutput(output);

        calculatedField.setConfiguration(config);

        return testRestClient.postCalculatedField(calculatedField);
    }

    private CalculatedField createScriptCalculatedField(EntityId entityId) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SCRIPT);
        calculatedField.setName("Average value:" + RandomStringUtils.randomAlphabetic(5));
        calculatedField.setDebugSettings(DebugSettings.all());

        ScriptCalculatedFieldConfiguration config = new ScriptCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("key", ArgumentType.TS_ROLLING, null);
        argument.setTimeWindow(30000L);
        argument.setLimit(5);
        argument.setRefDynamicSourceConfiguration(new CurrentOwnerDynamicSourceConfiguration());
        argument.setRefEntityKey(refEntityKey);

        config.setArguments(Map.of("rollingKey", argument));

        config.setExpression("return {\"avgValue\": rollingKey.avg()};");

        config.setOutput(new TimeSeriesOutput());

        calculatedField.setConfiguration(config);

        return testRestClient.postCalculatedField(calculatedField);
    }

    private Device createDevice(String name, DeviceProfileId deviceProfileId) {
        Device device = new Device();
        device.setName(name);
        device.setType("default");
        device.setDeviceProfileId(deviceProfileId);
        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        device.setDeviceData(deviceData);
        return device;
    }

}
