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
package org.thingsboard.server.edge;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.edge.imitator.EdgeImitator;
import org.thingsboard.server.gen.edge.EdgeConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
abstract public class BaseEdgeTest extends AbstractControllerTest {

    private Tenant savedTenant;
    private TenantId tenantId;
    private User tenantAdmin;

    private EdgeImitator edgeImitator;
    private Edge edge;

    @Autowired
    RuleChainService ruleChainService;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        tenantId = savedTenant.getId();
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        installation();

        edgeImitator = new EdgeImitator("localhost", 7070, edge.getRoutingKey(), edge.getSecret());
        edgeImitator.connect();
    }

    @After
    public void afterTest() throws Exception {
        edgeImitator.disconnect();
        uninstallation();

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }


    @Test
    public void test() throws Exception {
        testReceivedInitialData();
        testDevices();
        testAssets();
        testRuleChains();
        testDashboards();
        testRelations();
        testAlarms();
    }

    private void testReceivedInitialData() throws Exception {
        log.info("Checking received data");
        waitForMessages(6); // should be 3, but 3 events from sync service + 3 from controller. will be fixed in next releases

        EdgeConfiguration configuration = edgeImitator.getStorage().getConfiguration();
        Assert.assertNotNull(configuration);

        Map<UUID, EntityType> entities = edgeImitator.getStorage().getEntities();
        Assert.assertFalse(entities.isEmpty());

        Set<UUID> devices = edgeImitator.getStorage().getEntitiesByType(EntityType.DEVICE);
        Assert.assertEquals(1, devices.size());
        TimePageData<Device> pageDataDevices = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/devices?",
                new TypeReference<TimePageData<Device>>() {}, new TextPageLink(100));
        for (Device device: pageDataDevices.getData()) {
            Assert.assertTrue(devices.contains(device.getUuidId()));
        }

        Set<UUID> assets = edgeImitator.getStorage().getEntitiesByType(EntityType.ASSET);
        Assert.assertEquals(1, assets.size());
        TimePageData<Asset> pageDataAssets = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/assets?",
                new TypeReference<TimePageData<Asset>>() {}, new TextPageLink(100));
        for (Asset asset: pageDataAssets.getData()) {
            Assert.assertTrue(assets.contains(asset.getUuidId()));
        }

        Set<UUID> ruleChains = edgeImitator.getStorage().getEntitiesByType(EntityType.RULE_CHAIN);
        Assert.assertEquals(1, ruleChains.size());
        TimePageData<RuleChain> pageDataRuleChains = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/ruleChains?",
                new TypeReference<TimePageData<RuleChain>>() {}, new TextPageLink(100));
        for (RuleChain ruleChain: pageDataRuleChains.getData()) {
            Assert.assertTrue(ruleChains.contains(ruleChain.getUuidId()));
        }
        log.info("Received data checked");
    }

    private void testDevices() throws Exception {
        log.info("Testing devices");
        Device device = new Device();
        device.setName("Edge Device 2");
        device.setType("test");
        Device savedDevice = doPost("/api/device", device, Device.class);
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/device/" + savedDevice.getId().getId().toString(), Device.class);

        TimePageData<Device> pageDataDevices = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/devices?",
                new TypeReference<TimePageData<Device>>() {}, new TextPageLink(100));
        Assert.assertTrue(pageDataDevices.getData().contains(savedDevice));
        waitForMessages(1);
        Set<UUID> devices = edgeImitator.getStorage().getEntitiesByType(EntityType.DEVICE);
        Assert.assertEquals(2, devices.size());
        Assert.assertTrue(devices.contains(savedDevice.getUuidId()));

        doDelete("/api/edge/" + edge.getId().getId().toString()
                + "/device/" + savedDevice.getId().getId().toString(), Device.class);
        pageDataDevices = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/devices?",
                new TypeReference<TimePageData<Device>>() {}, new TextPageLink(100));
        Assert.assertFalse(pageDataDevices.getData().contains(savedDevice));
        waitForMessages(1);
        devices = edgeImitator.getStorage().getEntitiesByType(EntityType.DEVICE);
        Assert.assertEquals(1, devices.size());
        Assert.assertFalse(devices.contains(savedDevice.getUuidId()));

        doDelete("/api/device/" + savedDevice.getId().getId().toString())
                .andExpect(status().isOk());
        log.info("Devices tested successfully");
    }

    private void testAssets() throws Exception {
        log.info("Testing assets");
        Asset asset = new Asset();
        asset.setName("Edge Asset 2");
        asset.setType("test");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);

        TimePageData<Asset> pageDataAssets = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/assets?",
                new TypeReference<TimePageData<Asset>>() {}, new TextPageLink(100));
        Assert.assertTrue(pageDataAssets.getData().contains(savedAsset));
        waitForMessages(1);
        Set<UUID> assets = edgeImitator.getStorage().getEntitiesByType(EntityType.ASSET);
        Assert.assertEquals(2, assets.size());
        Assert.assertTrue(assets.contains(savedAsset.getUuidId()));

        doDelete("/api/edge/" + edge.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        pageDataAssets = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/assets?",
                new TypeReference<TimePageData<Asset>>() {}, new TextPageLink(100));
        Assert.assertFalse(pageDataAssets.getData().contains(savedAsset));
        waitForMessages(1);
        assets = edgeImitator.getStorage().getEntitiesByType(EntityType.ASSET);
        Assert.assertEquals(1, assets.size());
        Assert.assertFalse(assets.contains(savedAsset.getUuidId()));

        doDelete("/api/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isOk());
        log.info("Assets tested successfully");
    }

    private void testRuleChains() throws Exception {
        log.info("Testing RuleChains");
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Edge Test Rule Chain");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/ruleChain/" + savedRuleChain.getId().getId().toString(), RuleChain.class);

        TimePageData<RuleChain> pageDataRuleChain = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/ruleChains?",
                new TypeReference<TimePageData<RuleChain>>() {}, new TextPageLink(100));
        Assert.assertTrue(pageDataRuleChain.getData().contains(savedRuleChain));
        waitForMessages(1);
        Set<UUID> ruleChains = edgeImitator.getStorage().getEntitiesByType(EntityType.RULE_CHAIN);
        Assert.assertEquals(2, ruleChains.size());
        Assert.assertTrue(ruleChains.contains(savedRuleChain.getUuidId()));

        doDelete("/api/edge/" + edge.getId().getId().toString()
                + "/ruleChain/" + savedRuleChain.getId().getId().toString(), RuleChain.class);
        pageDataRuleChain = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/ruleChains?",
                new TypeReference<TimePageData<RuleChain>>() {}, new TextPageLink(100));
        Assert.assertFalse(pageDataRuleChain.getData().contains(savedRuleChain));
        waitForMessages(1);
        ruleChains = edgeImitator.getStorage().getEntitiesByType(EntityType.RULE_CHAIN);
        Assert.assertEquals(1, ruleChains.size());
        Assert.assertFalse(ruleChains.contains(savedRuleChain.getUuidId()));

        doDelete("/api/ruleChain/" + savedRuleChain.getId().getId().toString())
                .andExpect(status().isOk());
        log.info("RuleChains tested successfully");

    }

    private void testDashboards() throws Exception {
        log.info("Testing Dashboards");
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Edge Test Dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/dashboard/" + savedDashboard.getId().getId().toString(), Dashboard.class);

        TimePageData<DashboardInfo> pageDataDashboard = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/dashboards?",
                new TypeReference<TimePageData<DashboardInfo>>() {}, new TextPageLink(100));
        Assert.assertTrue(pageDataDashboard.getData().stream().allMatch(dashboardInfo -> dashboardInfo.getUuidId().equals(savedDashboard.getUuidId())));
        waitForMessages(1);
        Set<UUID> dashboards = edgeImitator.getStorage().getEntitiesByType(EntityType.DASHBOARD);
        Assert.assertEquals(1, dashboards.size());
        Assert.assertTrue(dashboards.contains(savedDashboard.getUuidId()));

        doDelete("/api/edge/" + edge.getId().getId().toString()
                + "/dashboard/" + savedDashboard.getId().getId().toString(), Dashboard.class);
        pageDataDashboard = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/dashboards?",
                new TypeReference<TimePageData<DashboardInfo>>() {}, new TextPageLink(100));
        Assert.assertFalse(pageDataDashboard.getData().stream().anyMatch(dashboardInfo -> dashboardInfo.getUuidId().equals(savedDashboard.getUuidId())));
        waitForMessages(1);
        dashboards = edgeImitator.getStorage().getEntitiesByType(EntityType.DASHBOARD);
        Assert.assertEquals(0, dashboards.size());
        Assert.assertFalse(dashboards.contains(savedDashboard.getUuidId()));

        doDelete("/api/dashboard/" + savedDashboard.getId().getId().toString())
                .andExpect(status().isOk());
        log.info("Dashboards tested successfully");

    }

    private void testRelations() throws Exception {
        log.info("Testing Relations");
        List<Device> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/devices?",
                new TypeReference<TimePageData<Device>>() {}, new TextPageLink(100)).getData();
        List<Asset> edgeAssets = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/assets?",
                new TypeReference<TimePageData<Asset>>() {}, new TextPageLink(100)).getData();

        Assert.assertEquals(1, edgeDevices.size());
        Assert.assertEquals(1, edgeAssets.size());
        Device device = edgeDevices.get(0);
        Asset asset = edgeAssets.get(0);
        Assert.assertEquals("Edge Device 1", device.getName());
        Assert.assertEquals("Edge Asset 1", asset.getName());

        EntityRelation relation = new EntityRelation();
        relation.setType("test");
        relation.setFrom(device.getId());
        relation.setTo(asset.getId());
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        doPost("/api/relation", relation);

        waitForMessages(1);
        List<EntityRelation> relations = edgeImitator.getStorage().getRelations();
        Assert.assertEquals(1, relations.size());
        Assert.assertTrue(relations.contains(relation));
        doDelete("/api/relation?" +
                "fromId=" + relation.getFrom().getId().toString() +
                "&fromType=" + relation.getFrom().getEntityType().name() +
                "&relationType=" + relation.getType() +
                "&relationTypeGroup=" + relation.getTypeGroup().name() +
                "&toId=" + relation.getTo().getId().toString() +
                "&toType=" + relation.getTo().getEntityType().name())
                .andExpect(status().isOk());

        waitForMessages(1);
        relations = edgeImitator.getStorage().getRelations();
        Assert.assertEquals(0, relations.size());
        Assert.assertFalse(relations.contains(relation));
        log.info("Relations tested successfully");
    }


    private void testAlarms() throws Exception {
        log.info("Testing Alarms");
        List<Device> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/devices?",
                new TypeReference<TimePageData<Device>>() {}, new TextPageLink(100)).getData();
        Assert.assertEquals(1, edgeDevices.size());
        Device device = edgeDevices.get(0);
        Assert.assertEquals("Edge Device 1", device.getName());

        Alarm alarm = new Alarm();
        alarm.setOriginator(device.getId());
        alarm.setStatus(AlarmStatus.ACTIVE_UNACK);
        alarm.setType("alarm");
        alarm.setSeverity(AlarmSeverity.CRITICAL);

        Alarm savedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        AlarmInfo alarmInfo = doGet("/api/alarm/info/" + savedAlarm.getId().getId().toString(), AlarmInfo.class);
        waitForMessages(1);

        Assert.assertEquals(1, edgeImitator.getStorage().getAlarms().size());
        Assert.assertTrue(edgeImitator.getStorage().getAlarms().containsKey(alarmInfo.getType()));
        Assert.assertEquals(edgeImitator.getStorage().getAlarms().get(alarmInfo.getType()), alarmInfo.getStatus());
        doPost("/api/alarm/" + savedAlarm.getId().getId().toString() + "/ack");

        waitForMessages(1);
        alarmInfo = doGet("/api/alarm/info/" + savedAlarm.getId().getId().toString(), AlarmInfo.class);
        Assert.assertTrue(edgeImitator.getStorage().getAlarms().get(alarmInfo.getType()).isAck());
        Assert.assertEquals(edgeImitator.getStorage().getAlarms().get(alarmInfo.getType()), alarmInfo.getStatus());
        doPost("/api/alarm/" + savedAlarm.getId().getId().toString() + "/clear");

        waitForMessages(1);
        alarmInfo = doGet("/api/alarm/info/" + savedAlarm.getId().getId().toString(), AlarmInfo.class);
        Assert.assertTrue(edgeImitator.getStorage().getAlarms().get(alarmInfo.getType()).isAck());
        Assert.assertTrue(edgeImitator.getStorage().getAlarms().get(alarmInfo.getType()).isCleared());
        Assert.assertEquals(edgeImitator.getStorage().getAlarms().get(alarmInfo.getType()), alarmInfo.getStatus());

        doDelete("/api/alarm/" + savedAlarm.getId().getId().toString())
            .andExpect(status().isOk());
        log.info("Alarms tested successfully");
    }

    private void installation() throws Exception {
        edge = doPost("/api/edge", constructEdge("Test Edge", "test"), Edge.class);

        Device device = new Device();
        device.setName("Edge Device 1");
        device.setType("test");
        Device savedDevice = doPost("/api/device", device, Device.class);
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/device/" + savedDevice.getId().getId().toString(), Device.class);

        Asset asset = new Asset();
        asset.setName("Edge Asset 1");
        asset.setType("test");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);
    }

    private void uninstallation() throws Exception {

        TimePageData<Device> pageDataDevices = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/devices?",
                new TypeReference<TimePageData<Device>>() {}, new TextPageLink(100));
        for (Device device: pageDataDevices.getData()) {
            doDelete("/api/device/" + device.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        TimePageData<Asset> pageDataAssets = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/assets?",
                new TypeReference<TimePageData<Asset>>() {}, new TextPageLink(100));
        for (Asset asset: pageDataAssets.getData()) {
            doDelete("/api/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        doDelete("/api/edge/" + edge.getId().getId().toString())
                .andExpect(status().isOk());
    }

    private void waitForMessages(int messageAmount) throws InterruptedException {
        edgeImitator.getStorage().setLatch(new CountDownLatch(messageAmount));
        while (!edgeImitator.getStorage().getLatch().await(1, TimeUnit.SECONDS)) {
            log.warn("Waiting for messages..");
        }
    }

}
