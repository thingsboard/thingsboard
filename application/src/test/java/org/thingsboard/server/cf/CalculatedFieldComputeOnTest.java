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
package org.thingsboard.server.cf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.ComputeOn;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DaoSqlTest
@TestPropertySource(properties = {
        "edges.enabled=true"
})
public class CalculatedFieldComputeOnTest extends AbstractControllerTest {

    private static final int TIMEOUT = 60;
    private static final int POLL_INTERVAL = 1;
    private static final String OUTPUT_KEY = "fahrenheitTemp";

    private Tenant savedTenant;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("Compute on tenant");
        savedTenant = saveTenant(tenant);

        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("computeOnTenant@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testDefaultComputeOnIsCalculatedOnCloud() throws Exception {
        Device device = givenDeviceWithTemperature("Default device", "default-1234");

        doPost("/api/calculatedField", cf(device.getId(), null), CalculatedField.class);

        awaitOutput(device.getId(), "77.0");
    }

    @Test
    public void testComputeOnCloudIsCalculatedOnCloud() throws Exception {
        Device device = givenDeviceWithTemperature("Cloud device", "cloud-1234");

        doPost("/api/calculatedField", cf(device.getId(), ComputeOn.CLOUD), CalculatedField.class);

        awaitOutput(device.getId(), "77.0");
    }

    @Test
    public void testComputeOnEdgeIsNotCalculatedOnCloud() throws Exception {
        Device device = givenDeviceWithTemperature("Edge device", "edge-1234");
        // profile bound so that the edge assignment validation does not apply
        CalculatedField calculatedField = cf(device.getDeviceProfileId(), ComputeOn.EDGE);

        doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        awaitNoOutput(device.getId());
    }

    @Test
    public void testSwitchingComputeOnStartsAndStopsCalculationOnCloud() throws Exception {
        Device device = givenDeviceWithTemperature("Switching device", "switch-1234");
        CalculatedField saved = doPost("/api/calculatedField", cf(device.getDeviceProfileId(), ComputeOn.EDGE), CalculatedField.class);

        awaitNoOutput(device.getId());

        saved.setComputeOn(ComputeOn.CLOUD);
        saved = doPost("/api/calculatedField", saved, CalculatedField.class);

        awaitOutput(device.getId(), "77.0");

        saved.setComputeOn(ComputeOn.EDGE);
        doPost("/api/calculatedField", saved, CalculatedField.class);

        postTelemetry(device.getId(), "{\"temperature\":30}");

        await().alias("switch back to EDGE -> cloud stops recalculating").during(10, TimeUnit.SECONDS)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode value = getLatestTelemetry(device.getId(), OUTPUT_KEY).path(OUTPUT_KEY).path(0).path("value");
                    assertThat(value.asText()).isEqualTo("77.0");
                });
    }

    private ObjectNode getLatestTelemetry(EntityId entityId, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/timeseries?keys=" + String.join(",", keys), ObjectNode.class);
    }

    private Device givenDeviceWithTemperature(String name, String accessToken) throws Exception {
        Device device = createDevice(name, accessToken);
        postTelemetry(device.getId(), "{\"temperature\":25}");
        return device;
    }

    private void awaitOutput(EntityId entityId, String expectedValue) {
        await().alias("CF is calculated on the cloud").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode value = getLatestTelemetry(entityId, OUTPUT_KEY).path(OUTPUT_KEY).path(0).path("value");
                    assertThat(value.asText()).isEqualTo(expectedValue);
                });
    }

    private void awaitNoOutput(EntityId entityId) {
        await().alias("CF is not calculated on the cloud").during(10, TimeUnit.SECONDS)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode value = getLatestTelemetry(entityId, OUTPUT_KEY).path(OUTPUT_KEY).path(0).path("value");
                    assertThat(value.isMissingNode() || value.isNull())
                            .as("CF must not be calculated on the cloud, but got value %s", value).isTrue();
                });
    }

    private CalculatedField cf(EntityId entityId, ComputeOn computeOn) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("C to F");
        calculatedField.setComputeOn(computeOn);
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName(OUTPUT_KEY);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();
        config.setArguments(Map.of("T", argument));
        config.setExpression("(T * 9/5) + 32");
        config.setOutput(output);
        calculatedField.setConfiguration(config);

        return calculatedField;
    }

}
