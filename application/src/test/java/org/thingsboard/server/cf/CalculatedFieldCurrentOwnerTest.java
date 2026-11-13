// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.cf;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CurrentOwnerDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.controller.AbstractWebTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class CalculatedFieldCurrentOwnerTest extends AbstractControllerTest {

    public static final int TIMEOUT = 60;
    public static final int POLL_INTERVAL = 1;

    @Test
    public void testCreateCFWithCurrentOwner() throws Exception {
        loginTenantAdmin();

        postAttributes(customerId, AttributeScope.SERVER_SCOPE, "{\"attrKey\":5}");

        Device testDevice = createDevice("Test device", "1234567890");
        doPost("/api/owner/CUSTOMER/" + customerId.getId() + "/DEVICE/" + testDevice.getId().getId()).andExpect(status().isOk());

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", buildCalculatedFieldWithAttrArg(testDevice.getId()), CalculatedField.class);

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "result");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("result")).isNotNull();
                    assertThat(fahrenheitTemp.get("result").get(0).get("value").asText()).isEqualTo("105");
                });

        postAttributes(customerId, AttributeScope.SERVER_SCOPE, "{\"attrKey\":10}");

        await().alias("update telemetry -> perform calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "result");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("result")).isNotNull();
                    assertThat(fahrenheitTemp.get("result").get(0).get("value").asText()).isEqualTo("110");
                });
    }

    @Test
    public void testChangeOwner() throws Exception {
        loginSysAdmin();

        postAttributes(tenantId, AttributeScope.SERVER_SCOPE, "{\"attrKey\":50}");

        loginTenantAdmin();

        postAttributes(customerId, AttributeScope.SERVER_SCOPE, "{\"attrKey\":5}");
        Device testDevice = createDevice("Test device", "1234567890");
        doPost("/api/owner/CUSTOMER/" + customerId.getId() + "/DEVICE/" + testDevice.getId().getId()).andExpect(status().isOk());

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", buildCalculatedFieldWithAttrArg(testDevice.getId()), CalculatedField.class);

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "result");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("result")).isNotNull();
                    assertThat(fahrenheitTemp.get("result").get(0).get("value").asText()).isEqualTo("105");
                });

        doPost("/api/owner/TENANT/" + tenantId.getId() + "/DEVICE/" + testDevice.getId().getId()).andExpect(status().isOk());

        await().alias("change owner -> perform calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "result");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("result")).isNotNull();
                    assertThat(fahrenheitTemp.get("result").get(0).get("value").asText()).isEqualTo("150");
                });
    }

    @Test
    public void testCreateCFWithCurrentOwnerWhenEntityIsProfile() throws Exception {
        loginSysAdmin();

        postAttributes(tenantId, AttributeScope.SERVER_SCOPE, "{\"attrKey\":50}");

        loginTenantAdmin();

        postAttributes(customerId, AttributeScope.SERVER_SCOPE, "{\"attrKey\":5}");

        AssetProfile assetProfile = doPost("/api/assetProfile", createAssetProfile("Test Asset Profile"), AssetProfile.class);

        Asset asset1 = createAsset("Test asset 1", assetProfile.getId());
        doPost("/api/owner/CUSTOMER/" + customerId.getId() + "/ASSET/" + asset1.getId().getId()).andExpect(status().isOk());

        Asset asset2 = createAsset("Test asset 2", assetProfile.getId()); // owner - TENANT

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", buildCalculatedFieldWithAttrArg(assetProfile.getId()), CalculatedField.class);

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 1
                    ObjectNode result1 = getLatestTelemetry(asset1.getId(), "result");
                    assertThat(result1).isNotNull();
                    assertThat(result1.get("result")).isNotNull();
                    assertThat(result1.get("result").get(0).get("value").asText()).isEqualTo("105");

                    //  result of asset 2
                    ObjectNode result2 = getLatestTelemetry(asset2.getId(), "result");
                    assertThat(result2).isNotNull();
                    assertThat(result2.get("result")).isNotNull();
                    assertThat(result2.get("result").get(0).get("value").asText()).isEqualTo("150");
                });

        doPost("/api/owner/CUSTOMER/" + customerId.getId() + "/ASSET/" + asset2.getId().getId()).andExpect(status().isOk());

        await().alias("change asset2 owner -> recalculate state for asset 2").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 2
                    ObjectNode result2 = getLatestTelemetry(asset2.getId(), "result");
                    assertThat(result2).isNotNull();
                    assertThat(result2.get("result")).isNotNull();
                    assertThat(result2.get("result").get(0).get("value").asText()).isEqualTo("105");
                });
    }

    @Test
    public void testReprocessCalculatedField() throws Exception {
        loginTenantAdmin();

        Device testDevice = createDevice("Test reprocessing device", "11112222");
        doPost("/api/owner/CUSTOMER/" + customerId.getId() + "/DEVICE/" + testDevice.getId().getId()).andExpect(status().isOk());

        long currentTime = System.currentTimeMillis();
        // reprocessing time window(TW)
        long startTs = currentTime - TimeUnit.SECONDS.toMillis(120);
        long endTs = currentTime - TimeUnit.SECONDS.toMillis(45);

        long a1Ts = currentTime - TimeUnit.SECONDS.toMillis(130); // outside the TW (but telemetry will be used for initial processing)
        long a2Ts = currentTime - TimeUnit.SECONDS.toMillis(80); // inside the TW
        long a3Ts = currentTime - TimeUnit.SECONDS.toMillis(70); // inside the TW
        long a4Ts = currentTime - TimeUnit.SECONDS.toMillis(40); // outside the TW

        doPost("/api/plugins/telemetry/CUSTOMER/" + customerId.getId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":10}}", a1Ts)));
        doPost("/api/plugins/telemetry/CUSTOMER/" + customerId.getId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":20}}", a2Ts)));
        doPost("/api/plugins/telemetry/CUSTOMER/" + customerId.getId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":30}}", a3Ts)));
        doPost("/api/plugins/telemetry/CUSTOMER/" + customerId.getId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":40}}", a4Ts)));

        /*      telemetry flow:
                          startTs        endTs
                            |______________|
               |  ts    | 1 |   3     4    |   7
               |customer| 10|-> 20 -> 30 ->|-> 40
                            |______________|
                                   |--- reprocessing time window
               the result should be: 110 -> 120 -> 130
        */

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", buildCalculatedFieldWithTsLatestArg(testDevice.getId()), CalculatedField.class);

        doGet("/api/calculatedField/" + savedCalculatedField.getUuidId() + "/reprocess?startTs={startTs}&endTs={endTs}", Job.class, startTs, endTs);

        await().alias("reprocess -> perform calculation for time window").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = doGetAsync("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/values/timeseries?keys={keys}&startTs={startTs}&endTs={endTs}", ObjectNode.class, String.join(",", "result"), startTs, endTs);
                    assertThat(result).isNotNull();
                    assertThat(result.get("result")).isNotNull();
                    assertThat(result.get("result").size()).isEqualTo(3);

                    assertThat(result.get("result").get(0).get("ts").asText()).isEqualTo(Long.toString(a3Ts));
                    assertThat(result.get("result").get(0).get("value").asText()).isEqualTo("130");

                    assertThat(result.get("result").get(1).get("ts").asText()).isEqualTo(Long.toString(a2Ts));
                    assertThat(result.get("result").get(1).get("value").asText()).isEqualTo("120");

                    assertThat(result.get("result").get(2).get("ts").asText()).isEqualTo(Long.toString(startTs));
                    assertThat(result.get("result").get(2).get("value").asText()).isEqualTo("110"); // we use reprocessing startTs instead of telemetry ts for initial calculation

                    ObjectNode resultLatest = getLatestTelemetry(testDevice.getId(), "result");
                    assertThat(resultLatest).isNotNull();
                    assertThat(resultLatest.get("result")).isNotNull();
                    assertThat(resultLatest.get("result").get(0).get("value").asText()).isEqualTo("140"); // reprocessing result did not overwrite the actual latest value
                });

        await().atMost(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job cfReprocessingJob = findJobs(List.of(JobType.CF_REPROCESSING), List.of(testDevice.getUuidId())).stream().findFirst().orElseThrow();
            assertThat(cfReprocessingJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(cfReprocessingJob.getResult().getSuccessfulCount()).isEqualTo(1);
            assertThat(cfReprocessingJob.getResult().getTotalCount()).isEqualTo(1);
            assertThat(cfReprocessingJob.getEntityId()).isEqualTo(testDevice.getId());
            assertThat(cfReprocessingJob.getEntityName()).isEqualTo(testDevice.getName());
        });
    }

    private CalculatedField buildCalculatedFieldWithTsLatestArg(EntityId entityId) {
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("a", ArgumentType.TS_LATEST, null);
        return buildCalculatedField(entityId, refEntityKey);
    }

    private CalculatedField buildCalculatedFieldWithAttrArg(EntityId entityId) {
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("attrKey", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE);
        return buildCalculatedField(entityId, refEntityKey);
    }

    private CalculatedField buildCalculatedField(EntityId entityId, ReferencedEntityKey refEntityKey) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("Test Calculated Field");

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        argument.setRefEntityKey(refEntityKey);
        argument.setRefDynamicSourceConfiguration(new CurrentOwnerDynamicSourceConfiguration());

        config.setArguments(Map.of("a", argument));

        config.setExpression("a + 100");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("result");
        output.setDecimalsByDefault(0);

        config.setOutput(output);

        calculatedField.setConfiguration(config);
        return calculatedField;
    }

    private ObjectNode getLatestTelemetry(EntityId entityId, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/timeseries?keys=" + String.join(",", keys), ObjectNode.class);
    }

    private Asset createAsset(String name, AssetProfileId assetProfileId) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setAssetProfileId(assetProfileId);
        return doPost("/api/asset", asset, Asset.class);
    }

}
