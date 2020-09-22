/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.msa.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class EdgeTest extends AbstractContainerTest {

    private static EdgeImitator edgeImitator;

    @BeforeClass
    public static void init() throws NoSuchFieldException, IllegalAccessException, InterruptedException, IOException {
        restClient.login("tenant@thingsboard.org", "tenant");
        installation();
        edgeImitator = new EdgeImitator("localhost", 7070, "routing", "secret");
        edgeImitator.connect();
        Thread.sleep(10000);
    }

    @Test
    public void testReceivedData() {
        Edge edge = restClient.getTenantEdge("Edge1").get();

        EdgeConfiguration configuration = edgeImitator.getStorage().getConfiguration();
        Assert.assertNotNull(configuration);

        Map<UUID, EdgeEventType> entities = edgeImitator.getStorage().getEntities();
        Assert.assertFalse(entities.isEmpty());

        Set<UUID> devices = edgeImitator.getStorage().getEntitiesByType(EdgeEventType.DEVICE);
        Assert.assertEquals(1, devices.size());
        for (Device device: restClient.getEdgeDevices(edge.getId(), new TextPageLink(1)).getData()) {
            Assert.assertTrue(devices.contains(device.getUuidId()));
        }

        Set<UUID> ruleChains = edgeImitator.getStorage().getEntitiesByType(EdgeEventType.RULE_CHAIN);
        Assert.assertEquals(1, ruleChains.size());
        for (RuleChain ruleChain: restClient.getEdgeRuleChains(edge.getId(), new TimePageLink(1)).getData()) {
            Assert.assertTrue(ruleChains.contains(ruleChain.getUuidId()));
        }

        Set<UUID> assets = edgeImitator.getStorage().getEntitiesByType(EdgeEventType.ASSET);
        Assert.assertEquals(1, assets.size());
        for (Asset asset: restClient.getEdgeAssets(edge.getId(), new TextPageLink(1)).getData()) {
            Assert.assertTrue(assets.contains(asset.getUuidId()));
        }
    }

    @Test
    public void testDevices() throws Exception {
        Edge edge = restClient.getTenantEdge("Edge1").get();

        Device device = new Device();
        device.setName("Edge Device 2");
        device.setType("test");
        Device savedDevice = restClient.saveDevice(device);
        restClient.assignDeviceToEdge(edge.getId(), savedDevice.getId());

        Thread.sleep(1000);
        Assert.assertTrue(restClient.getEdgeDevices(edge.getId(), new TextPageLink(2)).getData().contains(savedDevice));
        Set<UUID> devices = edgeImitator.getStorage().getEntitiesByType(EdgeEventType.DEVICE);
        Assert.assertEquals(2, devices.size());
        Assert.assertTrue(devices.contains(savedDevice.getUuidId()));

        restClient.unassignDeviceFromEdge(edge.getId(), savedDevice.getId());
        Thread.sleep(1000);
        Assert.assertFalse(restClient.getEdgeDevices(edge.getId(), new TextPageLink(2)).getData().contains(savedDevice));
        devices = edgeImitator.getStorage().getEntitiesByType(EdgeEventType.DEVICE);
        Assert.assertEquals(1, devices.size());
        Assert.assertFalse(devices.contains(savedDevice.getUuidId()));

        restClient.deleteDevice(savedDevice.getId());
    }

    @Test
    public void testAssets() throws Exception {
        Edge edge = restClient.getTenantEdge("Edge1").get();

        Asset asset = new Asset();
        asset.setName("Edge Asset 2");
        asset.setType("test");
        Asset savedAsset = restClient.saveAsset(asset);
        restClient.assignAssetToEdge(edge.getId(), savedAsset.getId());

        Thread.sleep(1000);
        Assert.assertTrue(restClient.getEdgeAssets(edge.getId(), new TextPageLink(2)).getData().contains(savedAsset));
        Set<UUID> assets = edgeImitator.getStorage().getEntitiesByType(EdgeEventType.ASSET);
        Assert.assertEquals(2, assets.size());
        Assert.assertTrue(assets.contains(savedAsset.getUuidId()));

        restClient.unassignAssetFromEdge(edge.getId(), savedAsset.getId());
        Thread.sleep(1000);
        Assert.assertFalse(restClient.getEdgeAssets(edge.getId(), new TextPageLink(2)).getData().contains(savedAsset));
        assets = edgeImitator.getStorage().getEntitiesByType(EdgeEventType.ASSET);
        Assert.assertEquals(1, assets.size());
        Assert.assertFalse(assets.contains(savedAsset.getUuidId()));

        restClient.deleteAsset(savedAsset.getId());
    }

    @Test
    public void testRuleChains() throws Exception {
        Edge edge = restClient.getTenantEdge("Edge1").get();

        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Edge Test Rule Chain");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain savedRuleChain = restClient.saveRuleChain(ruleChain);
        restClient.assignRuleChainToEdge(edge.getId(), savedRuleChain.getId());

        Thread.sleep(1000);
        Assert.assertTrue(restClient.getEdgeRuleChains(edge.getId(), new TimePageLink(2)).getData().contains(savedRuleChain));
        Set<UUID> ruleChains = edgeImitator.getStorage().getEntitiesByType(EdgeEventType.RULE_CHAIN);
        Assert.assertEquals(2, ruleChains.size());
        Assert.assertTrue(ruleChains.contains(savedRuleChain.getUuidId()));

        restClient.unassignRuleChainFromEdge(edge.getId(), savedRuleChain.getId());
        Thread.sleep(1000);
        Assert.assertFalse(restClient.getEdgeRuleChains(edge.getId(), new TimePageLink(2)).getData().contains(savedRuleChain));
        ruleChains = edgeImitator.getStorage().getEntitiesByType(EdgeEventType.RULE_CHAIN);
        Assert.assertEquals(1, ruleChains.size());
        Assert.assertFalse(ruleChains.contains(savedRuleChain.getUuidId()));

        restClient.deleteRuleChain(savedRuleChain.getId());

    }

    @Test
    public void testDashboards() throws Exception {
        Edge edge = restClient.getTenantEdge("Edge1").get();

        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Edge Test Dashboard");
        Dashboard savedDashboard = restClient.saveDashboard(dashboard);
        restClient.assignDashboardToEdge(edge.getId(), savedDashboard.getId());

        Thread.sleep(1000);
        Assert.assertTrue(restClient.getEdgeDashboards(edge.getId(), new TimePageLink(2)).getData().stream().allMatch(dashboardInfo -> dashboardInfo.getUuidId().equals(savedDashboard.getUuidId())));
        Set<UUID> dashboards = edgeImitator.getStorage().getEntitiesByType(EdgeEventType.DASHBOARD);
        Assert.assertEquals(1, dashboards.size());
        Assert.assertTrue(dashboards.contains(savedDashboard.getUuidId()));

        restClient.unassignDashboardFromEdge(edge.getId(), savedDashboard.getId());
        Thread.sleep(1000);
        Assert.assertFalse(restClient.getEdgeDashboards(edge.getId(), new TimePageLink(2)).getData().stream().anyMatch(dashboardInfo -> dashboardInfo.getUuidId().equals(savedDashboard.getUuidId())));
        dashboards = edgeImitator.getStorage().getEntitiesByType(EdgeEventType.DASHBOARD);
        Assert.assertEquals(0, dashboards.size());
        Assert.assertFalse(dashboards.contains(savedDashboard.getUuidId()));

        restClient.deleteDashboard(savedDashboard.getId());

    }

    @Test
    public void testRelations() throws InterruptedException {
        Device device = restClient.getTenantDevice("Edge Device 1").get();
        Asset asset = restClient.getTenantAsset("Edge Asset 1").get();

        EntityRelation relation = new EntityRelation();
        relation.setType("test");
        relation.setFrom(device.getId());
        relation.setTo(asset.getId());
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        restClient.saveRelation(relation);

        Thread.sleep(1000);
        List<EntityRelation> relations = edgeImitator.getStorage().getRelations();
        Assert.assertEquals(1, relations.size());
        Assert.assertTrue(relations.contains(relation));
        restClient.deleteRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo());

        Thread.sleep(1000);
        relations = edgeImitator.getStorage().getRelations();
        Assert.assertEquals(0, relations.size());
        Assert.assertFalse(relations.contains(relation));
    }

    @Test
    public void testAlarms() throws Exception {
        Device device = restClient.getTenantDevice("Edge Device 1").get();
        Alarm alarm = new Alarm();
        alarm.setOriginator(device.getId());
        alarm.setStatus(AlarmStatus.ACTIVE_UNACK);
        alarm.setType("alarm");
        alarm.setSeverity(AlarmSeverity.CRITICAL);

        Alarm savedAlarm = restClient.saveAlarm(alarm);
        AlarmInfo alarmInfo = restClient.getAlarmInfoById(savedAlarm.getId()).get();
        Thread.sleep(1000);

        Assert.assertEquals(1, edgeImitator.getStorage().getAlarms().size());
        Assert.assertTrue(edgeImitator.getStorage().getAlarms().containsKey(alarmInfo.getType()));
        Assert.assertEquals(edgeImitator.getStorage().getAlarms().get(alarmInfo.getType()), alarmInfo.getStatus());
        restClient.ackAlarm(savedAlarm.getId());

        Thread.sleep(1000);
        alarmInfo = restClient.getAlarmInfoById(savedAlarm.getId()).get();
        Assert.assertTrue(edgeImitator.getStorage().getAlarms().get(alarmInfo.getType()).isAck());
        Assert.assertEquals(edgeImitator.getStorage().getAlarms().get(alarmInfo.getType()), alarmInfo.getStatus());
        restClient.clearAlarm(savedAlarm.getId());

        Thread.sleep(1000);
        alarmInfo = restClient.getAlarmInfoById(savedAlarm.getId()).get();
        Assert.assertTrue(edgeImitator.getStorage().getAlarms().get(alarmInfo.getType()).isAck());
        Assert.assertTrue(edgeImitator.getStorage().getAlarms().get(alarmInfo.getType()).isCleared());
        Assert.assertEquals(edgeImitator.getStorage().getAlarms().get(alarmInfo.getType()), alarmInfo.getStatus());

        restClient.deleteAlarm(savedAlarm.getId());

    }

    @Ignore
    @Test
    public void testTelemetry() throws Exception {
        Device device = restClient.getTenantDevice("Edge Device 1").get();
        DeviceCredentials deviceCredentials = restClient.getDeviceCredentialsByDeviceId(device.getId()).get();
        ResponseEntity response = restClient.getRestTemplate()
                .postForEntity(HTTPS_URL + "/api/v1/{credentialsId}/telemetry",
                        "{'test': 25}",
                        ResponseEntity.class,
                        deviceCredentials.getCredentialsId());
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Thread.sleep(1000);
        List<String> keys = restClient.getTimeseriesKeys(device.getId());
        List<TsKvEntry> latestTimeseries = restClient.getLatestTimeseries(device.getId(), keys);
        Assert.assertEquals(1, latestTimeseries.size());
        TsKvEntry tsKvEntry = latestTimeseries.get(0);
        Map<UUID, TransportProtos.PostTelemetryMsg> telemetry = edgeImitator.getStorage().getLatestTelemetry();
        Assert.assertEquals(1, telemetry.size());
        Assert.assertTrue(telemetry.containsKey(device.getUuidId()));
        TransportProtos.PostTelemetryMsg telemetryMsg = telemetry.get(device.getUuidId());
        Assert.assertEquals(1, telemetryMsg.getTsKvListCount());
        TransportProtos.TsKvListProto tsKv = telemetryMsg.getTsKvListList().get(0);
        Assert.assertEquals(tsKvEntry.getTs(), tsKv.getTs());
        Assert.assertEquals(1, tsKv.getKvCount());
        TransportProtos.KeyValueProto keyValue = tsKv.getKvList().get(0);
        Assert.assertEquals(tsKvEntry.getKey(), keyValue.getKey());
        Assert.assertEquals(tsKvEntry.getValueAsString(), Long.toString(keyValue.getLongV()));
    }

    @AfterClass
    public static void destroy() throws InterruptedException {
        uninstallation();
        edgeImitator.disconnect();
    }

    private static void installation() throws IOException {
        Edge edge = new Edge();
        edge.setName("Edge1");
        edge.setType("test");
        edge.setRoutingKey("routing");
        edge.setSecret("secret");
        Edge savedEdge = restClient.saveEdge(edge);

        Device device = new Device();
        device.setName("Edge Device 1");
        device.setType("test");
        Device savedDevice = restClient.saveDevice(device);
        restClient.assignDeviceToEdge(savedEdge.getId(), savedDevice.getId());

        Asset asset = new Asset();
        asset.setName("Edge Asset 1");
        asset.setType("test");
        Asset savedAsset = restClient.saveAsset(asset);
        restClient.assignAssetToEdge(savedEdge.getId(), savedAsset.getId());

        ObjectMapper mapper = new ObjectMapper();
        Class edgeTestClass = EdgeTest.class;
        JsonNode configuration = mapper.readTree(edgeTestClass.getClassLoader().getResourceAsStream("RootRuleChain.json"));
        RuleChain ruleChain = mapper.treeToValue(configuration.get("ruleChain"), RuleChain.class);
        RuleChainMetaData ruleChainMetaData = mapper.treeToValue(configuration.get("metadata"), RuleChainMetaData.class);
        RuleChain savedRuleChain = restClient.saveRuleChain(ruleChain);
        ruleChainMetaData.setRuleChainId(savedRuleChain.getId());
        restClient.saveRuleChainMetaData(ruleChainMetaData);
        restClient.setRootRuleChain(savedRuleChain.getId());
    }

    private static void uninstallation() {
        Device device = restClient.getTenantDevice("Edge Device 1").get();
        restClient.deleteDevice(device.getId());

        Asset asset = restClient.getTenantAsset("Edge Asset 1").get();
        restClient.deleteAsset(asset.getId());

        Edge edge = restClient.getTenantEdge("Edge1").get();
        restClient.deleteEdge(edge.getId());

        List<RuleChain> ruleChains = restClient.getRuleChains(new TextPageLink(3)).getData();
        RuleChain oldRoot = ruleChains.stream().filter(ruleChain -> ruleChain.getName().equals("Root Rule Chain")).findAny().get();
        RuleChain newRoot = ruleChains.stream().filter(ruleChain -> ruleChain.getName().equals("Test Root Rule Chain")).findAny().get();
        restClient.setRootRuleChain(oldRoot.getId());
        restClient.deleteRuleChain(newRoot.getId());
    }

}
