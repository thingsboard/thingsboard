/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.edge.imitator.EdgeImitator;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.AttributeDeleteMsg;
import org.thingsboard.server.gen.edge.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.RelationRequestMsg;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.RpcResponseMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.edge.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.WidgetTypeUpdateMsg;
import org.thingsboard.server.gen.edge.WidgetsBundleUpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.queue.TbClusterService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
abstract public class BaseEdgeTest extends AbstractControllerTest {

    private static final String CUSTOM_DEVICE_PROFILE_NAME = "Thermostat";

    private Tenant savedTenant;
    private TenantId tenantId;
    private User tenantAdmin;

    private EdgeImitator edgeImitator;
    private Edge edge;

    @Autowired
    private EdgeEventService edgeEventService;

    @Autowired
    private TbClusterService clusterService;

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
        // sleep 1 seconds to avoid CREDENTIALS updated message for the user
        // user credentials is going to be stored and updated event pushed to edge notification service
        // while service will be processing this event edge could be already added and additional message will be pushed
        Thread.sleep(1000);

        installation();

        edgeImitator = new EdgeImitator("localhost", 7070, edge.getRoutingKey(), edge.getSecret());
        // TODO: voba - should be less, but events from SyncEdgeService stack with events from controller. will be fixed in next releases
        // so ideally sync process should check current edge queue and add only missing entities to the edge queue
        edgeImitator.expectMessageAmount(10);
        edgeImitator.connect();
    }

    @After
    public void afterTest() throws Exception {
        try {
            edgeImitator.disconnect();
        } catch (Exception ignored) {}

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void test() throws Exception {
        testReceivedInitialData();
        int expectedDownlinkSize = 10;
        Assert.assertEquals(expectedDownlinkSize, edgeImitator.getDownlinkMsgs().size());

        testDevices();
        expectedDownlinkSize = expectedDownlinkSize + 4;
        Assert.assertEquals(expectedDownlinkSize, edgeImitator.getDownlinkMsgs().size());

        testAssets();
        expectedDownlinkSize = expectedDownlinkSize + 4;
        Assert.assertEquals(expectedDownlinkSize, edgeImitator.getDownlinkMsgs().size());

        testRuleChains();
        expectedDownlinkSize = expectedDownlinkSize + 3;
        Assert.assertEquals(expectedDownlinkSize, edgeImitator.getDownlinkMsgs().size());

        testDashboards();
        expectedDownlinkSize = expectedDownlinkSize + 3;
        Assert.assertEquals(expectedDownlinkSize, edgeImitator.getDownlinkMsgs().size());

        testRelations();
        expectedDownlinkSize = expectedDownlinkSize + 2;
        Assert.assertEquals(expectedDownlinkSize, edgeImitator.getDownlinkMsgs().size());

        testAlarms();
        expectedDownlinkSize = expectedDownlinkSize + 3;
        Assert.assertEquals(expectedDownlinkSize, edgeImitator.getDownlinkMsgs().size());

        testEntityView();
        expectedDownlinkSize = expectedDownlinkSize + 2;
        Assert.assertEquals(expectedDownlinkSize, edgeImitator.getDownlinkMsgs().size());

        testCustomer();
        expectedDownlinkSize = expectedDownlinkSize + 2;
        Assert.assertEquals(expectedDownlinkSize, edgeImitator.getDownlinkMsgs().size());

        testWidgetsBundleAndWidgetType();
        expectedDownlinkSize = expectedDownlinkSize + 4;
        Assert.assertEquals(expectedDownlinkSize, edgeImitator.getDownlinkMsgs().size());

        testTimeseries();
        expectedDownlinkSize = expectedDownlinkSize + 1;
        Assert.assertEquals(expectedDownlinkSize, edgeImitator.getDownlinkMsgs().size());

        testAttributes();
        expectedDownlinkSize = expectedDownlinkSize + 3;
        Assert.assertEquals(expectedDownlinkSize, edgeImitator.getDownlinkMsgs().size());

        testSendMessagesToCloud();
        expectedDownlinkSize = expectedDownlinkSize + 9;
        Assert.assertEquals(expectedDownlinkSize, edgeImitator.getDownlinkMsgs().size());

        testRpcCall();
        expectedDownlinkSize = expectedDownlinkSize + 1;
        Assert.assertEquals(expectedDownlinkSize, edgeImitator.getDownlinkMsgs().size());
    }

    private Device findDeviceByName(String deviceName) throws Exception {
        List<Device> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/devices?",
                new TypeReference<PageData<Device>>() {
                }, new PageLink(100)).getData();
        Optional<Device> foundDevice = edgeDevices.stream().filter(d -> d.getName().equals(deviceName)).findAny();
        Assert.assertTrue(foundDevice.isPresent());
        Device device = foundDevice.get();
        Assert.assertEquals(deviceName, device.getName());
        return device;
    }

    private Asset findAssetByName(String assetName) throws Exception {
        List<Asset> edgeAssets = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/assets?",
                new TypeReference<PageData<Asset>>() {
                }, new PageLink(100)).getData();

        Assert.assertEquals(1, edgeAssets.size());
        Asset asset = edgeAssets.get(0);
        Assert.assertEquals(assetName, asset.getName());
        return asset;
    }

    private Device saveDevice(String deviceName, String type) throws Exception {
        Device device = new Device();
        device.setName(deviceName);
        device.setType(type);
        return doPost("/api/device", device, Device.class);
    }

    private Asset saveAsset(String assetName) throws Exception {
        Asset asset = new Asset();
        asset.setName(assetName);
        asset.setType("test");
        return doPost("/api/asset", asset, Asset.class);
    }

    private void testRpcCall() throws Exception {
        Device device = findDeviceByName("Edge Device 1");

        ObjectNode body = mapper.createObjectNode();
        body.put("requestId", new Random().nextInt());
        body.put("requestUUID", Uuids.timeBased().toString());
        body.put("oneway", false);
        body.put("expirationTime", System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10));
        body.put("method", "test_method");
        body.put("params", "{\"param1\":\"value1\"}");

        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.RPC_CALL, device.getId().getId(), EdgeEventType.DEVICE, body);
        edgeImitator.expectMessageAmount(1);
        edgeEventService.saveAsync(edgeEvent);
        clusterService.onEdgeEventUpdate(tenantId, edge.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceRpcCallMsg);
        DeviceRpcCallMsg latestDeviceRpcCallMsg = (DeviceRpcCallMsg) latestMessage;
        Assert.assertEquals("test_method", latestDeviceRpcCallMsg.getRequestMsg().getMethod());
    }

    private void testReceivedInitialData() throws Exception {
        log.info("Checking received data");
        Assert.assertTrue(edgeImitator.waitForMessages());

        EdgeConfiguration configuration = edgeImitator.getConfiguration();
        Assert.assertNotNull(configuration);

        testAutoGeneratedCodeByProtobuf(configuration);

        UserId userId = edgeImitator.getUserId();
        Assert.assertNotNull(userId);

        Optional<DeviceUpdateMsg> deviceUpdateMsgOpt = edgeImitator.findMessageByType(DeviceUpdateMsg.class);
        Assert.assertTrue(deviceUpdateMsgOpt.isPresent());
        DeviceUpdateMsg deviceUpdateMsg = deviceUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        UUID deviceUUID = new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB());
        Device device = doGet("/api/device/" + deviceUUID.toString(), Device.class);
        Assert.assertNotNull(device);
        List<Device> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/devices?",
                new TypeReference<PageData<Device>>() {}, new PageLink(100)).getData();
        Assert.assertTrue(edgeDevices.contains(device));

        List<DeviceProfileUpdateMsg> deviceProfileUpdateMsgList = edgeImitator.findAllMessagesByType(DeviceProfileUpdateMsg.class);
        Assert.assertEquals(3, deviceProfileUpdateMsgList.size());
        Optional<DeviceProfileUpdateMsg> deviceProfileUpdateMsgOpt =
                deviceProfileUpdateMsgList.stream().filter(dfum -> CUSTOM_DEVICE_PROFILE_NAME.equals(dfum.getName())).findAny();
        Assert.assertTrue(deviceProfileUpdateMsgOpt.isPresent());
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = deviceProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        UUID deviceProfileUUID = new UUID(deviceProfileUpdateMsg.getIdMSB(), deviceProfileUpdateMsg.getIdLSB());
        DeviceProfile deviceProfile = doGet("/api/deviceProfile/" + deviceProfileUUID.toString(), DeviceProfile.class);
        Assert.assertNotNull(deviceProfile);
        Assert.assertNotNull(deviceProfile.getProfileData());
        Assert.assertNotNull(deviceProfile.getProfileData().getAlarms());
        Assert.assertNotNull(deviceProfile.getProfileData().getAlarms().get(0).getClearRule());

        testAutoGeneratedCodeByProtobuf(deviceProfileUpdateMsg);

        Optional<AssetUpdateMsg> assetUpdateMsgOpt = edgeImitator.findMessageByType(AssetUpdateMsg.class);
        Assert.assertTrue(assetUpdateMsgOpt.isPresent());
        AssetUpdateMsg assetUpdateMsg = assetUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        UUID assetUUID = new UUID(assetUpdateMsg.getIdMSB(), assetUpdateMsg.getIdLSB());
        Asset asset = doGet("/api/asset/" + assetUUID.toString(), Asset.class);
        Assert.assertNotNull(asset);
        List<Asset> edgeAssets = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/assets?",
                new TypeReference<PageData<Asset>>() {}, new PageLink(100)).getData();
        Assert.assertTrue(edgeAssets.contains(asset));

        testAutoGeneratedCodeByProtobuf(assetUpdateMsg);

        Optional<RuleChainUpdateMsg> ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        RuleChainUpdateMsg ruleChainUpdateMsg = ruleChainUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, ruleChainUpdateMsg.getMsgType());
        UUID ruleChainUUID = new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB());
        RuleChain ruleChain = doGet("/api/ruleChain/" + ruleChainUUID.toString(), RuleChain.class);
        Assert.assertNotNull(ruleChain);
        List<RuleChain> edgeRuleChains = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/ruleChains?",
                new TypeReference<PageData<RuleChain>>() {}, new PageLink(100)).getData();
        Assert.assertTrue(edgeRuleChains.contains(ruleChain));

        testAutoGeneratedCodeByProtobuf(ruleChainUpdateMsg);

        log.info("Received data checked");
    }

    private void testDevices() throws Exception {
        log.info("Testing devices");

        // 1
        edgeImitator.expectMessageAmount(1);
        Device savedDevice = saveDevice("Edge Device 2", "Default");
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/device/" + savedDevice.getId().getId().toString(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        DeviceUpdateMsg deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(deviceUpdateMsg.getIdMSB(), savedDevice.getUuidId().getMostSignificantBits());
        Assert.assertEquals(deviceUpdateMsg.getIdLSB(), savedDevice.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(deviceUpdateMsg.getName(), savedDevice.getName());
        Assert.assertEquals(deviceUpdateMsg.getType(), savedDevice.getType());

        // 2
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getId().getId().toString()
                + "/device/" + savedDevice.getId().getId().toString(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(deviceUpdateMsg.getIdMSB(), savedDevice.getUuidId().getMostSignificantBits());
        Assert.assertEquals(deviceUpdateMsg.getIdLSB(), savedDevice.getUuidId().getLeastSignificantBits());

        // 3
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/device/" + savedDevice.getId().getId().toString())
                .andExpect(status().isOk());
        // we should not get any message because device is not assigned to edge any more
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // 4
        edgeImitator.expectMessageAmount(1);
        savedDevice = saveDevice("Edge Device 3", "Default");
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/device/" + savedDevice.getId().getId().toString(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(deviceUpdateMsg.getIdMSB(), savedDevice.getUuidId().getMostSignificantBits());
        Assert.assertEquals(deviceUpdateMsg.getIdLSB(), savedDevice.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(deviceUpdateMsg.getName(), savedDevice.getName());
        Assert.assertEquals(deviceUpdateMsg.getType(), savedDevice.getType());

        // 5
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/device/" + savedDevice.getId().getId().toString())
                .andExpect(status().isOk());
        // in this case we should get messages because device was assigned to edge
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(deviceUpdateMsg.getIdMSB(), savedDevice.getUuidId().getMostSignificantBits());
        Assert.assertEquals(deviceUpdateMsg.getIdLSB(), savedDevice.getUuidId().getLeastSignificantBits());

        log.info("Devices tested successfully");
    }


    private void testAssets() throws Exception {
        log.info("Testing assets");

        // 1
        edgeImitator.expectMessageAmount(1);
        Asset savedAsset = saveAsset("Edge Asset 2");
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetUpdateMsg);
        AssetUpdateMsg assetUpdateMsg = (AssetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(assetUpdateMsg.getIdMSB(), savedAsset.getUuidId().getMostSignificantBits());
        Assert.assertEquals(assetUpdateMsg.getIdLSB(), savedAsset.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(assetUpdateMsg.getName(), savedAsset.getName());
        Assert.assertEquals(assetUpdateMsg.getType(), savedAsset.getType());

        // 2
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetUpdateMsg);
        assetUpdateMsg = (AssetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(assetUpdateMsg.getIdMSB(), savedAsset.getUuidId().getMostSignificantBits());
        Assert.assertEquals(assetUpdateMsg.getIdLSB(), savedAsset.getUuidId().getLeastSignificantBits());

        // 3
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isOk());
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // 4
        edgeImitator.expectMessageAmount(1);
        savedAsset = saveAsset("Edge Asset 3");
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetUpdateMsg);
        assetUpdateMsg = (AssetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(assetUpdateMsg.getIdMSB(), savedAsset.getUuidId().getMostSignificantBits());
        Assert.assertEquals(assetUpdateMsg.getIdLSB(), savedAsset.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(assetUpdateMsg.getName(), savedAsset.getName());
        Assert.assertEquals(assetUpdateMsg.getType(), savedAsset.getType());

        // 5
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetUpdateMsg);
        assetUpdateMsg = (AssetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(assetUpdateMsg.getIdMSB(), savedAsset.getUuidId().getMostSignificantBits());
        Assert.assertEquals(assetUpdateMsg.getIdLSB(), savedAsset.getUuidId().getLeastSignificantBits());

        log.info("Assets tested successfully");
    }

    private void testRuleChains() throws Exception {
        log.info("Testing RuleChains");

        // 1
        edgeImitator.expectMessageAmount(1);
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Edge Test Rule Chain");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        createRuleChainMetadata(savedRuleChain);
        // sleep 1 seconds to avoid ENTITY_UPDATED_RPC_MESSAGE for the rule chain
        // rule chain metadata is going to be stored and updated event pushed to edge notification service
        // while service will be processing this event assignment rule chain to edge will be completed if bad timing
        Thread.sleep(1000);
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/ruleChain/" + savedRuleChain.getId().getId().toString(), RuleChain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RuleChainUpdateMsg);
        RuleChainUpdateMsg ruleChainUpdateMsg = (RuleChainUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, ruleChainUpdateMsg.getMsgType());
        Assert.assertEquals(ruleChainUpdateMsg.getIdMSB(), savedRuleChain.getUuidId().getMostSignificantBits());
        Assert.assertEquals(ruleChainUpdateMsg.getIdLSB(), savedRuleChain.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(ruleChainUpdateMsg.getName(), savedRuleChain.getName());

        // 2
        testRuleChainMetadataRequestMsg(savedRuleChain.getId());

        // 3
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getId().getId().toString()
                + "/ruleChain/" + savedRuleChain.getId().getId().toString(), RuleChain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RuleChainUpdateMsg);
        ruleChainUpdateMsg = (RuleChainUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, ruleChainUpdateMsg.getMsgType());
        Assert.assertEquals(ruleChainUpdateMsg.getIdMSB(), savedRuleChain.getUuidId().getMostSignificantBits());
        Assert.assertEquals(ruleChainUpdateMsg.getIdLSB(), savedRuleChain.getUuidId().getLeastSignificantBits());

        // 4
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/ruleChain/" + savedRuleChain.getId().getId().toString())
                .andExpect(status().isOk());
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        log.info("RuleChains tested successfully");
    }

    private void testRuleChainMetadataRequestMsg(RuleChainId ruleChainId) throws Exception {
        RuleChainMetadataRequestMsg.Builder ruleChainMetadataRequestMsgBuilder = RuleChainMetadataRequestMsg.newBuilder()
                .setRuleChainIdMSB(ruleChainId.getId().getMostSignificantBits())
                .setRuleChainIdLSB(ruleChainId.getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(ruleChainMetadataRequestMsgBuilder);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder()
                .addRuleChainMetadataRequestMsg(ruleChainMetadataRequestMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RuleChainMetadataUpdateMsg);
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = (RuleChainMetadataUpdateMsg) latestMessage;
        RuleChainId receivedRuleChainId =
                new RuleChainId(new UUID(ruleChainMetadataUpdateMsg.getRuleChainIdMSB(), ruleChainMetadataUpdateMsg.getRuleChainIdLSB()));
        Assert.assertEquals(ruleChainId, receivedRuleChainId);
    }

    private void createRuleChainMetadata(RuleChain ruleChain) throws Exception {
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());

        ObjectMapper mapper = new ObjectMapper();

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("name1");
        ruleNode1.setType("type1");
        ruleNode1.setConfiguration(mapper.readTree("\"key1\": \"val1\""));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("name2");
        ruleNode2.setType("type2");
        ruleNode2.setConfiguration(mapper.readTree("\"key2\": \"val2\""));

        RuleNode ruleNode3 = new RuleNode();
        ruleNode3.setName("name3");
        ruleNode3.setType("type3");
        ruleNode3.setConfiguration(mapper.readTree("\"key3\": \"val3\""));

        List<RuleNode> ruleNodes = new ArrayList<>();
        ruleNodes.add(ruleNode1);
        ruleNodes.add(ruleNode2);
        ruleNodes.add(ruleNode3);
        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);

        ruleChainMetaData.addConnectionInfo(0, 1, "success");
        ruleChainMetaData.addConnectionInfo(0, 2, "fail");
        ruleChainMetaData.addConnectionInfo(1, 2, "success");

        ruleChainMetaData.addRuleChainConnectionInfo(2, edge.getRootRuleChainId(), "success", mapper.createObjectNode());

        doPost("/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class);
    }

    private void testDashboards() throws Exception {
        log.info("Testing Dashboards");

        // 1
        edgeImitator.expectMessageAmount(1);
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Edge Test Dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/dashboard/" + savedDashboard.getId().getId().toString(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        DashboardUpdateMsg dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(dashboardUpdateMsg.getIdMSB(), savedDashboard.getUuidId().getMostSignificantBits());
        Assert.assertEquals(dashboardUpdateMsg.getIdLSB(), savedDashboard.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(dashboardUpdateMsg.getTitle(), savedDashboard.getName());
        testAutoGeneratedCodeByProtobuf(dashboardUpdateMsg);

        // 2
        edgeImitator.expectMessageAmount(1);
        savedDashboard.setTitle("Updated Edge Test Dashboard");
        doPost("/api/dashboard", savedDashboard, Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(dashboardUpdateMsg.getTitle(), savedDashboard.getName());

        // 3
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getId().getId().toString()
                + "/dashboard/" + savedDashboard.getId().getId().toString(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(dashboardUpdateMsg.getIdMSB(), savedDashboard.getUuidId().getMostSignificantBits());
        Assert.assertEquals(dashboardUpdateMsg.getIdLSB(), savedDashboard.getUuidId().getLeastSignificantBits());

        // 4
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/dashboard/" + savedDashboard.getId().getId().toString())
                .andExpect(status().isOk());
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        log.info("Dashboards tested successfully");
    }

    private void testRelations() throws Exception {
        log.info("Testing Relations");

        // 1
        edgeImitator.expectMessageAmount(1);
        Device device = findDeviceByName("Edge Device 1");
        Asset asset = findAssetByName("Edge Asset 1");
        EntityRelation relation = new EntityRelation();
        relation.setType("test");
        relation.setFrom(device.getId());
        relation.setTo(asset.getId());
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        doPost("/api/relation", relation);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RelationUpdateMsg);
        RelationUpdateMsg relationUpdateMsg = (RelationUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, relationUpdateMsg.getMsgType());
        Assert.assertEquals(relationUpdateMsg.getType(), relation.getType());
        Assert.assertEquals(relationUpdateMsg.getFromIdMSB(), relation.getFrom().getId().getMostSignificantBits());
        Assert.assertEquals(relationUpdateMsg.getFromIdLSB(), relation.getFrom().getId().getLeastSignificantBits());
        Assert.assertEquals(relationUpdateMsg.getToEntityType(), relation.getTo().getEntityType().name());
        Assert.assertEquals(relationUpdateMsg.getFromIdMSB(), relation.getFrom().getId().getMostSignificantBits());
        Assert.assertEquals(relationUpdateMsg.getToIdLSB(), relation.getTo().getId().getLeastSignificantBits());
        Assert.assertEquals(relationUpdateMsg.getToEntityType(), relation.getTo().getEntityType().name());
        Assert.assertEquals(relationUpdateMsg.getTypeGroup(), relation.getTypeGroup().name());

        // 2
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/relation?" +
                "fromId=" + relation.getFrom().getId().toString() +
                "&fromType=" + relation.getFrom().getEntityType().name() +
                "&relationType=" + relation.getType() +
                "&relationTypeGroup=" + relation.getTypeGroup().name() +
                "&toId=" + relation.getTo().getId().toString() +
                "&toType=" + relation.getTo().getEntityType().name())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RelationUpdateMsg);
        relationUpdateMsg = (RelationUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, relationUpdateMsg.getMsgType());
        Assert.assertEquals(relationUpdateMsg.getType(), relation.getType());
        Assert.assertEquals(relationUpdateMsg.getFromIdMSB(), relation.getFrom().getId().getMostSignificantBits());
        Assert.assertEquals(relationUpdateMsg.getFromIdLSB(), relation.getFrom().getId().getLeastSignificantBits());
        Assert.assertEquals(relationUpdateMsg.getToEntityType(), relation.getTo().getEntityType().name());
        Assert.assertEquals(relationUpdateMsg.getFromIdMSB(), relation.getFrom().getId().getMostSignificantBits());
        Assert.assertEquals(relationUpdateMsg.getToIdLSB(), relation.getTo().getId().getLeastSignificantBits());
        Assert.assertEquals(relationUpdateMsg.getToEntityType(), relation.getTo().getEntityType().name());
        Assert.assertEquals(relationUpdateMsg.getTypeGroup(), relation.getTypeGroup().name());

        log.info("Relations tested successfully");
    }

    private void testAlarms() throws Exception {
        log.info("Testing Alarms");

        // 1
        edgeImitator.expectMessageAmount(1);
        Device device = findDeviceByName("Edge Device 1");
        Alarm alarm = new Alarm();
        alarm.setOriginator(device.getId());
        alarm.setStatus(AlarmStatus.ACTIVE_UNACK);
        alarm.setType("alarm");
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        Alarm savedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AlarmUpdateMsg);
        AlarmUpdateMsg alarmUpdateMsg = (AlarmUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, alarmUpdateMsg.getMsgType());
        Assert.assertEquals(alarmUpdateMsg.getType(), savedAlarm.getType());
        Assert.assertEquals(alarmUpdateMsg.getName(), savedAlarm.getName());
        Assert.assertEquals(alarmUpdateMsg.getOriginatorName(), device.getName());
        Assert.assertEquals(alarmUpdateMsg.getStatus(), savedAlarm.getStatus().name());
        Assert.assertEquals(alarmUpdateMsg.getSeverity(), savedAlarm.getSeverity().name());

        // 2
        edgeImitator.expectMessageAmount(1);
        doPost("/api/alarm/" + savedAlarm.getId().getId().toString() + "/ack");
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AlarmUpdateMsg);
        alarmUpdateMsg = (AlarmUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ALARM_ACK_RPC_MESSAGE, alarmUpdateMsg.getMsgType());
        Assert.assertEquals(alarmUpdateMsg.getType(), savedAlarm.getType());
        Assert.assertEquals(alarmUpdateMsg.getName(), savedAlarm.getName());
        Assert.assertEquals(alarmUpdateMsg.getOriginatorName(), device.getName());
        Assert.assertEquals(alarmUpdateMsg.getStatus(), AlarmStatus.ACTIVE_ACK.name());

        // 3
        edgeImitator.expectMessageAmount(1);
        doPost("/api/alarm/" + savedAlarm.getId().getId().toString() + "/clear");
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AlarmUpdateMsg);
        alarmUpdateMsg = (AlarmUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ALARM_CLEAR_RPC_MESSAGE, alarmUpdateMsg.getMsgType());
        Assert.assertEquals(alarmUpdateMsg.getType(), savedAlarm.getType());
        Assert.assertEquals(alarmUpdateMsg.getName(), savedAlarm.getName());
        Assert.assertEquals(alarmUpdateMsg.getOriginatorName(), device.getName());
        Assert.assertEquals(alarmUpdateMsg.getStatus(), AlarmStatus.CLEARED_ACK.name());

        // 4
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/alarm/" + savedAlarm.getId().getId().toString())
                .andExpect(status().isOk());
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        log.info("Alarms tested successfully");
    }

    private void testEntityView() throws Exception {
        log.info("Testing EntityView");

        // 1
        edgeImitator.expectMessageAmount(1);
        Device device = findDeviceByName("Edge Device 1");
        EntityView entityView = new EntityView();
        entityView.setName("Edge EntityView 1");
        entityView.setType("test");
        entityView.setEntityId(device.getId());
        EntityView savedEntityView = doPost("/api/entityView", entityView, EntityView.class);
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/entityView/" + savedEntityView.getId().getId().toString(), EntityView.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityViewUpdateMsg);
        EntityViewUpdateMsg entityViewUpdateMsg = (EntityViewUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, entityViewUpdateMsg.getMsgType());
        Assert.assertEquals(entityViewUpdateMsg.getType(), savedEntityView.getType());
        Assert.assertEquals(entityViewUpdateMsg.getName(), savedEntityView.getName());
        Assert.assertEquals(entityViewUpdateMsg.getIdMSB(), savedEntityView.getUuidId().getMostSignificantBits());
        Assert.assertEquals(entityViewUpdateMsg.getIdLSB(), savedEntityView.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(entityViewUpdateMsg.getEntityIdMSB(), device.getUuidId().getMostSignificantBits());
        Assert.assertEquals(entityViewUpdateMsg.getEntityIdLSB(), device.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(entityViewUpdateMsg.getEntityType().name(), device.getId().getEntityType().name());

        // 2
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getId().getId().toString()
                + "/entityView/" + savedEntityView.getId().getId().toString(), EntityView.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityViewUpdateMsg);
        entityViewUpdateMsg = (EntityViewUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, entityViewUpdateMsg.getMsgType());
        Assert.assertEquals(entityViewUpdateMsg.getIdMSB(), savedEntityView.getUuidId().getMostSignificantBits());
        Assert.assertEquals(entityViewUpdateMsg.getIdLSB(), savedEntityView.getUuidId().getLeastSignificantBits());

        // 3
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/entityView/" + savedEntityView.getId().getId().toString())
                .andExpect(status().isOk());
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        log.info("EntityView tested successfully");
    }

    private void testCustomer() throws Exception {
        log.info("Testing Customer");

        // 1
        edgeImitator.expectMessageAmount(1);
        Customer customer = new Customer();
        customer.setTitle("Edge Customer 1");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        doPost("/api/customer/" + savedCustomer.getId().getId().toString()
                + "/edge/" + edge.getId().getId().toString(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof CustomerUpdateMsg);
        CustomerUpdateMsg customerUpdateMsg = (CustomerUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, customerUpdateMsg.getMsgType());
        Assert.assertEquals(customerUpdateMsg.getIdMSB(), savedCustomer.getUuidId().getMostSignificantBits());
        Assert.assertEquals(customerUpdateMsg.getIdLSB(), savedCustomer.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(customerUpdateMsg.getTitle(), savedCustomer.getTitle());
        testAutoGeneratedCodeByProtobuf(customerUpdateMsg);

        // 2
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/customer/edge/" + edge.getId().getId().toString(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof CustomerUpdateMsg);
        customerUpdateMsg = (CustomerUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, customerUpdateMsg.getMsgType());
        Assert.assertEquals(customerUpdateMsg.getIdMSB(), savedCustomer.getUuidId().getMostSignificantBits());
        Assert.assertEquals(customerUpdateMsg.getIdLSB(), savedCustomer.getUuidId().getLeastSignificantBits());

        // 3
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/customer/" + savedCustomer.getId().getId().toString())
                .andExpect(status().isOk());
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        log.info("Customer tested successfully");
    }

    private void testWidgetsBundleAndWidgetType() throws Exception {
        log.info("Testing WidgetsBundle and WidgetType");

        // 1
        edgeImitator.expectMessageAmount(1);
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("Test Widget Bundle");
        WidgetsBundle savedWidgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof WidgetsBundleUpdateMsg);
        WidgetsBundleUpdateMsg widgetsBundleUpdateMsg = (WidgetsBundleUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, widgetsBundleUpdateMsg.getMsgType());
        Assert.assertEquals(widgetsBundleUpdateMsg.getIdMSB(), savedWidgetsBundle.getUuidId().getMostSignificantBits());
        Assert.assertEquals(widgetsBundleUpdateMsg.getIdLSB(), savedWidgetsBundle.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(widgetsBundleUpdateMsg.getAlias(), savedWidgetsBundle.getAlias());
        Assert.assertEquals(widgetsBundleUpdateMsg.getTitle(), savedWidgetsBundle.getTitle());
        testAutoGeneratedCodeByProtobuf(widgetsBundleUpdateMsg);

        // 2
        edgeImitator.expectMessageAmount(1);
        WidgetType widgetType = new WidgetType();
        widgetType.setName("Test Widget Type");
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        ObjectNode descriptor = mapper.createObjectNode();
        descriptor.put("key", "value");
        widgetType.setDescriptor(descriptor);
        WidgetType savedWidgetType = doPost("/api/widgetType", widgetType, WidgetType.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof WidgetTypeUpdateMsg);
        WidgetTypeUpdateMsg widgetTypeUpdateMsg = (WidgetTypeUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, widgetTypeUpdateMsg.getMsgType());
        Assert.assertEquals(widgetTypeUpdateMsg.getIdMSB(), savedWidgetType.getUuidId().getMostSignificantBits());
        Assert.assertEquals(widgetTypeUpdateMsg.getIdLSB(), savedWidgetType.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(widgetTypeUpdateMsg.getAlias(), savedWidgetType.getAlias());
        Assert.assertEquals(widgetTypeUpdateMsg.getName(), savedWidgetType.getName());
        Assert.assertEquals(JacksonUtil.toJsonNode(widgetTypeUpdateMsg.getDescriptorJson()), savedWidgetType.getDescriptor());

        // 3
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/widgetType/" + savedWidgetType.getId().getId().toString())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof WidgetTypeUpdateMsg);
        widgetTypeUpdateMsg = (WidgetTypeUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, widgetTypeUpdateMsg.getMsgType());
        Assert.assertEquals(widgetTypeUpdateMsg.getIdMSB(), savedWidgetType.getUuidId().getMostSignificantBits());
        Assert.assertEquals(widgetTypeUpdateMsg.getIdLSB(), savedWidgetType.getUuidId().getLeastSignificantBits());

        // 4
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/widgetsBundle/" + savedWidgetsBundle.getId().getId().toString())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof WidgetsBundleUpdateMsg);
        widgetsBundleUpdateMsg = (WidgetsBundleUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, widgetsBundleUpdateMsg.getMsgType());
        Assert.assertEquals(widgetsBundleUpdateMsg.getIdMSB(), savedWidgetsBundle.getUuidId().getMostSignificantBits());
        Assert.assertEquals(widgetsBundleUpdateMsg.getIdLSB(), savedWidgetsBundle.getUuidId().getLeastSignificantBits());

        log.info("WidgetsBundle and WidgetType tested successfully");
    }

    private void testTimeseries() throws Exception {
        log.info("Testing timeseries");

        edgeImitator.expectMessageAmount(1);
        Device device = findDeviceByName("Edge Device 1");
        String timeseriesData = "{\"data\":{\"temperature\":25},\"ts\":" + System.currentTimeMillis() + "}";
        JsonNode timeseriesEntityData = mapper.readTree(timeseriesData);
        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.TIMESERIES_UPDATED, device.getId().getId(), EdgeEventType.DEVICE, timeseriesEntityData);
        edgeEventService.saveAsync(edgeEvent);
        clusterService.onEdgeEventUpdate(tenantId, edge.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityDataProto);
        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
        Assert.assertEquals(latestEntityDataMsg.getEntityIdMSB(), device.getUuidId().getMostSignificantBits());
        Assert.assertEquals(latestEntityDataMsg.getEntityIdLSB(), device.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(latestEntityDataMsg.getEntityType(), device.getId().getEntityType().name());
        Assert.assertTrue(latestEntityDataMsg.hasPostTelemetryMsg());

        TransportProtos.PostTelemetryMsg postTelemetryMsg = latestEntityDataMsg.getPostTelemetryMsg();
        Assert.assertEquals(1, postTelemetryMsg.getTsKvListCount());
        TransportProtos.TsKvListProto tsKvListProto = postTelemetryMsg.getTsKvList(0);
        Assert.assertEquals(timeseriesEntityData.get("ts").asLong(), tsKvListProto.getTs());
        Assert.assertEquals(1, tsKvListProto.getKvCount());
        TransportProtos.KeyValueProto keyValueProto = tsKvListProto.getKv(0);
        Assert.assertEquals("temperature", keyValueProto.getKey());
        Assert.assertEquals(25, keyValueProto.getLongV());
        log.info("Timeseries tested successfully");
    }

    private void testAttributes() throws Exception {
        log.info("Testing attributes");
        Device device = findDeviceByName("Edge Device 1");

        testAttributesUpdatedMsg(device);
        testPostAttributesMsg(device);
        testAttributesDeleteMsg(device);

        log.info("Attributes tested successfully");
    }

    private void testAttributesDeleteMsg(Device device) throws JsonProcessingException, InterruptedException {
        String deleteAttributesData = "{\"scope\":\"SERVER_SCOPE\",\"keys\":[\"key1\",\"key2\"]}";
        JsonNode deleteAttributesEntityData = mapper.readTree(deleteAttributesData);
        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.ATTRIBUTES_DELETED, device.getId().getId(), EdgeEventType.DEVICE, deleteAttributesEntityData);
        edgeImitator.expectMessageAmount(1);
        edgeEventService.saveAsync(edgeEvent);
        clusterService.onEdgeEventUpdate(tenantId, edge.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityDataProto);
        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
        Assert.assertEquals(device.getUuidId().getMostSignificantBits(), latestEntityDataMsg.getEntityIdMSB());
        Assert.assertEquals(device.getUuidId().getLeastSignificantBits(), latestEntityDataMsg.getEntityIdLSB());
        Assert.assertEquals(device.getId().getEntityType().name(), latestEntityDataMsg.getEntityType());

        Assert.assertTrue(latestEntityDataMsg.hasAttributeDeleteMsg());

        AttributeDeleteMsg attributeDeleteMsg = latestEntityDataMsg.getAttributeDeleteMsg();
        Assert.assertEquals(attributeDeleteMsg.getScope(), deleteAttributesEntityData.get("scope").asText());

        Assert.assertEquals(2, attributeDeleteMsg.getAttributeNamesCount());
        Assert.assertEquals("key1", attributeDeleteMsg.getAttributeNames(0));
        Assert.assertEquals("key2", attributeDeleteMsg.getAttributeNames(1));
    }

    private void testPostAttributesMsg(Device device) throws JsonProcessingException, InterruptedException {
        String postAttributesData = "{\"scope\":\"SERVER_SCOPE\",\"kv\":{\"key2\":\"value2\"}}";
        JsonNode postAttributesEntityData = mapper.readTree(postAttributesData);
        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.POST_ATTRIBUTES, device.getId().getId(), EdgeEventType.DEVICE, postAttributesEntityData);
        edgeImitator.expectMessageAmount(1);
        edgeEventService.saveAsync(edgeEvent);
        clusterService.onEdgeEventUpdate(tenantId, edge.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityDataProto);
        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
        Assert.assertEquals(device.getUuidId().getMostSignificantBits(), latestEntityDataMsg.getEntityIdMSB());
        Assert.assertEquals(device.getUuidId().getLeastSignificantBits(), latestEntityDataMsg.getEntityIdLSB());
        Assert.assertEquals(device.getId().getEntityType().name(), latestEntityDataMsg.getEntityType());
        Assert.assertEquals("SERVER_SCOPE", latestEntityDataMsg.getPostAttributeScope());
        Assert.assertTrue(latestEntityDataMsg.hasPostAttributesMsg());

        TransportProtos.PostAttributeMsg postAttributesMsg = latestEntityDataMsg.getPostAttributesMsg();
        Assert.assertEquals(1, postAttributesMsg.getKvCount());
        TransportProtos.KeyValueProto keyValueProto = postAttributesMsg.getKv(0);
        Assert.assertEquals("key2", keyValueProto.getKey());
        Assert.assertEquals("value2", keyValueProto.getStringV());
    }

    private void testAttributesUpdatedMsg(Device device) throws JsonProcessingException, InterruptedException {
        String attributesData = "{\"scope\":\"SERVER_SCOPE\",\"kv\":{\"key1\":\"value1\"}}";
        JsonNode attributesEntityData = mapper.readTree(attributesData);
        EdgeEvent edgeEvent1 = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.ATTRIBUTES_UPDATED, device.getId().getId(), EdgeEventType.DEVICE, attributesEntityData);
        edgeImitator.expectMessageAmount(1);
        edgeEventService.saveAsync(edgeEvent1);
        clusterService.onEdgeEventUpdate(tenantId, edge.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityDataProto);
        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
        Assert.assertEquals(device.getUuidId().getMostSignificantBits(), latestEntityDataMsg.getEntityIdMSB());
        Assert.assertEquals(device.getUuidId().getLeastSignificantBits(), latestEntityDataMsg.getEntityIdLSB());
        Assert.assertEquals(device.getId().getEntityType().name(), latestEntityDataMsg.getEntityType());
        Assert.assertEquals("SERVER_SCOPE", latestEntityDataMsg.getPostAttributeScope());
        Assert.assertTrue(latestEntityDataMsg.hasAttributesUpdatedMsg());

        TransportProtos.PostAttributeMsg attributesUpdatedMsg = latestEntityDataMsg.getAttributesUpdatedMsg();
        Assert.assertEquals(1, attributesUpdatedMsg.getKvCount());
        TransportProtos.KeyValueProto keyValueProto = attributesUpdatedMsg.getKv(0);
        Assert.assertEquals("key1", keyValueProto.getKey());
        Assert.assertEquals("value1", keyValueProto.getStringV());
    }

    private void testSendMessagesToCloud() throws Exception {
        log.info("Sending messages to cloud");
        sendDevice();
        sendDeviceWithNameThatAlreadyExistsOnCloud();
        sendRelationRequest();
        sendAlarm();
        sendTelemetry();
        sendRelation();
        sendDeleteDeviceOnEdge();
        sendRuleChainMetadataRequest();
        sendUserCredentialsRequest();
        sendDeviceCredentialsRequest();
        sendDeviceRpcResponse();
        sendDeviceCredentialsUpdate();
        sendAttributesRequest();
        log.info("Messages were sent successfully");
    }

    private void sendDevice() throws Exception {
        UUID uuid = Uuids.timeBased();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceUpdateMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        deviceUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        deviceUpdateMsgBuilder.setName("Edge Device 2");
        deviceUpdateMsgBuilder.setType("test");
        deviceUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        testAutoGeneratedCodeByProtobuf(deviceUpdateMsgBuilder);
        uplinkMsgBuilder.addDeviceUpdateMsg(deviceUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        DeviceUpdateMsg latestDeviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals("Edge Device 2", latestDeviceUpdateMsg.getName());

        UUID newDeviceId = new UUID(latestDeviceUpdateMsg.getIdMSB(), latestDeviceUpdateMsg.getIdLSB());

        Device device = doGet("/api/device/" + newDeviceId, Device.class);
        Assert.assertNotNull(device);
        Assert.assertEquals("Edge Device 2", device.getName());
    }

    private void sendDeviceWithNameThatAlreadyExistsOnCloud() throws Exception {
        String deviceOnCloudName = RandomStringUtils.randomAlphanumeric(15);
        Device deviceOnCloud = saveDevice(deviceOnCloudName, "Default");

        UUID uuid = Uuids.timeBased();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceUpdateMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        deviceUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        deviceUpdateMsgBuilder.setName(deviceOnCloudName);
        deviceUpdateMsgBuilder.setType("test");
        deviceUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        testAutoGeneratedCodeByProtobuf(deviceUpdateMsgBuilder);
        uplinkMsgBuilder.addDeviceUpdateMsg(deviceUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        DeviceUpdateMsg latestDeviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertNotEquals(deviceOnCloudName, latestDeviceUpdateMsg.getName());
        Assert.assertEquals(deviceOnCloudName, latestDeviceUpdateMsg.getConflictName());

        UUID newDeviceId = new UUID(latestDeviceUpdateMsg.getIdMSB(), latestDeviceUpdateMsg.getIdLSB());

        Assert.assertNotEquals(deviceOnCloud.getId().getId(), newDeviceId);

        Device device = doGet("/api/device/" + newDeviceId, Device.class);
        Assert.assertNotNull(device);
        Assert.assertNotEquals(deviceOnCloudName, device.getName());
    }

    private void sendRelationRequest() throws Exception {
        Device device = findDeviceByName("Edge Device 1");
        Asset asset = findAssetByName("Edge Asset 1");

        EntityRelation relation = new EntityRelation();
        relation.setType("test");
        relation.setFrom(device.getId());
        relation.setTo(asset.getId());
        relation.setTypeGroup(RelationTypeGroup.COMMON);

        edgeImitator.expectMessageAmount(1);
        doPost("/api/relation", relation);
        Assert.assertTrue(edgeImitator.waitForMessages());

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        RelationRequestMsg.Builder relationRequestMsgBuilder = RelationRequestMsg.newBuilder();
        relationRequestMsgBuilder.setEntityIdMSB(device.getId().getId().getMostSignificantBits());
        relationRequestMsgBuilder.setEntityIdLSB(device.getId().getId().getLeastSignificantBits());
        relationRequestMsgBuilder.setEntityType(device.getId().getEntityType().name());
        testAutoGeneratedCodeByProtobuf(relationRequestMsgBuilder);

        uplinkMsgBuilder.addRelationRequestMsg(relationRequestMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RelationUpdateMsg);
        RelationUpdateMsg relationUpdateMsg = (RelationUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, relationUpdateMsg.getMsgType());
        Assert.assertEquals(relation.getType(), relationUpdateMsg.getType());

        UUID fromUUID = new UUID(relationUpdateMsg.getFromIdMSB(), relationUpdateMsg.getFromIdLSB());
        EntityId fromEntityId = EntityIdFactory.getByTypeAndUuid(relationUpdateMsg.getFromEntityType(), fromUUID);
        Assert.assertEquals(relation.getFrom(), fromEntityId);

        UUID toUUID = new UUID(relationUpdateMsg.getToIdMSB(), relationUpdateMsg.getToIdLSB());
        EntityId toEntityId = EntityIdFactory.getByTypeAndUuid(relationUpdateMsg.getToEntityType(), toUUID);
        Assert.assertEquals(relation.getTo(), toEntityId);

        Assert.assertEquals(relation.getTypeGroup().name(), relationUpdateMsg.getTypeGroup());
    }

    private void sendAlarm() throws Exception {
        Device device = findDeviceByName("Edge Device 2");

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        AlarmUpdateMsg.Builder alarmUpdateMgBuilder = AlarmUpdateMsg.newBuilder();
        alarmUpdateMgBuilder.setName("alarm from edge");
        alarmUpdateMgBuilder.setStatus(AlarmStatus.ACTIVE_UNACK.name());
        alarmUpdateMgBuilder.setSeverity(AlarmSeverity.CRITICAL.name());
        alarmUpdateMgBuilder.setOriginatorName(device.getName());
        alarmUpdateMgBuilder.setOriginatorType(EntityType.DEVICE.name());
        testAutoGeneratedCodeByProtobuf(alarmUpdateMgBuilder);
        uplinkMsgBuilder.addAlarmUpdateMsg(alarmUpdateMgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());


        List<AlarmInfo> alarms = doGetTypedWithPageLink("/api/alarm/{entityType}/{entityId}?",
                new TypeReference<PageData<AlarmInfo>>() {},
                new PageLink(100), device.getId().getEntityType().name(), device.getId().getId().toString())
                .getData();
        Optional<AlarmInfo> foundAlarm = alarms.stream().filter(alarm -> alarm.getType().equals("alarm from edge")).findAny();
        Assert.assertTrue(foundAlarm.isPresent());
        AlarmInfo alarmInfo = foundAlarm.get();
        Assert.assertEquals(device.getId(), alarmInfo.getOriginator());
        Assert.assertEquals(AlarmStatus.ACTIVE_UNACK, alarmInfo.getStatus());
        Assert.assertEquals(AlarmSeverity.CRITICAL, alarmInfo.getSeverity());
    }

    private void sendRelation() throws Exception {
        List<Device> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/devices?",
                new TypeReference<PageData<Device>>() {}, new PageLink(100)).getData();
        Optional<Device> foundDevice1 = edgeDevices.stream().filter(device1 -> device1.getName().equals("Edge Device 1")).findAny();
        Assert.assertTrue(foundDevice1.isPresent());
        Device device1 = foundDevice1.get();
        Optional<Device> foundDevice2 = edgeDevices.stream().filter(device2 -> device2.getName().equals("Edge Device 2")).findAny();
        Assert.assertTrue(foundDevice2.isPresent());
        Device device2 = foundDevice2.get();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        RelationUpdateMsg.Builder relationUpdateMsgBuilder = RelationUpdateMsg.newBuilder();
        relationUpdateMsgBuilder.setType("test");
        relationUpdateMsgBuilder.setTypeGroup(RelationTypeGroup.COMMON.name());
        relationUpdateMsgBuilder.setToIdMSB(device1.getId().getId().getMostSignificantBits());
        relationUpdateMsgBuilder.setToIdLSB(device1.getId().getId().getLeastSignificantBits());
        relationUpdateMsgBuilder.setToEntityType(device1.getId().getEntityType().name());
        relationUpdateMsgBuilder.setFromIdMSB(device2.getId().getId().getMostSignificantBits());
        relationUpdateMsgBuilder.setFromIdLSB(device2.getId().getId().getLeastSignificantBits());
        relationUpdateMsgBuilder.setFromEntityType(device2.getId().getEntityType().name());
        relationUpdateMsgBuilder.setAdditionalInfo("{}");
        testAutoGeneratedCodeByProtobuf(relationUpdateMsgBuilder);
        uplinkMsgBuilder.addRelationUpdateMsg(relationUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        EntityRelation relation = doGet("/api/relation?" +
                "&fromId=" + device2.getId().getId().toString() +
                "&fromType=" + device2.getId().getEntityType().name() +
                "&relationType=" + "test" +
                "&relationTypeGroup=" + RelationTypeGroup.COMMON.name() +
                "&toId=" + device1.getId().getId().toString() +
                "&toType=" + device1.getId().getEntityType().name(), EntityRelation.class);
        Assert.assertNotNull(relation);
    }

    private void sendTelemetry() throws Exception {
        List<Device> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/devices?",
                new TypeReference<PageData<Device>>() {}, new PageLink(100)).getData();
        Optional<Device> foundDevice = edgeDevices.stream().filter(device1 -> device1.getName().equals("Edge Device 2")).findAny();
        Assert.assertTrue(foundDevice.isPresent());
        Device device = foundDevice.get();

        edgeImitator.expectResponsesAmount(2);

        JsonObject data = new JsonObject();
        String timeseriesKey = "key";
        String timeseriesValue = "25";
        data.addProperty(timeseriesKey, timeseriesValue);
        UplinkMsg.Builder uplinkMsgBuilder1 = UplinkMsg.newBuilder();
        EntityDataProto.Builder entityDataBuilder = EntityDataProto.newBuilder();
        entityDataBuilder.setPostTelemetryMsg(JsonConverter.convertToTelemetryProto(data, System.currentTimeMillis()));
        entityDataBuilder.setEntityType(device.getId().getEntityType().name());
        entityDataBuilder.setEntityIdMSB(device.getUuidId().getMostSignificantBits());
        entityDataBuilder.setEntityIdLSB(device.getUuidId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(entityDataBuilder);
        uplinkMsgBuilder1.addEntityData(entityDataBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder1.build());

        JsonObject attributesData = new JsonObject();
        String attributesKey = "test_attr";
        String attributesValue = "test_value";
        attributesData.addProperty(attributesKey, attributesValue);
        UplinkMsg.Builder uplinkMsgBuilder2 = UplinkMsg.newBuilder();
        EntityDataProto.Builder entityDataBuilder2 = EntityDataProto.newBuilder();
        entityDataBuilder2.setEntityType(device.getId().getEntityType().name());
        entityDataBuilder2.setEntityIdMSB(device.getId().getId().getMostSignificantBits());
        entityDataBuilder2.setEntityIdLSB(device.getId().getId().getLeastSignificantBits());
        entityDataBuilder2.setAttributesUpdatedMsg(JsonConverter.convertToAttributesProto(attributesData));
        entityDataBuilder2.setPostAttributeScope(DataConstants.SERVER_SCOPE);
        testAutoGeneratedCodeByProtobuf(entityDataBuilder2);

        uplinkMsgBuilder2.addEntityData(entityDataBuilder2.build());
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder2);

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder2.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        // Wait before device attributes saved to database before requesting them from controller
        Thread.sleep(1000);
        Map<String, List<Map<String, String>>> timeseries = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/values/timeseries?keys=" + timeseriesKey, new TypeReference<>() {});
        Assert.assertTrue(timeseries.containsKey(timeseriesKey));
        Assert.assertEquals(1, timeseries.get(timeseriesKey).size());
        Assert.assertEquals(timeseriesValue, timeseries.get(timeseriesKey).get(0).get("value"));

        List<Map<String, String>> attributes = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/attributes/" + DataConstants.SERVER_SCOPE, new TypeReference<>() {});
        Assert.assertEquals(1, attributes.size());
        Assert.assertEquals(attributes.get(0).get("key"), attributesKey);
        Assert.assertEquals(attributes.get(0).get("value"), attributesValue);

    }

    private void sendRuleChainMetadataRequest() throws Exception {
        RuleChainId edgeRootRuleChainId = edge.getRootRuleChainId();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        RuleChainMetadataRequestMsg.Builder ruleChainMetadataRequestMsgBuilder = RuleChainMetadataRequestMsg.newBuilder();
        ruleChainMetadataRequestMsgBuilder.setRuleChainIdMSB(edgeRootRuleChainId.getId().getMostSignificantBits());
        ruleChainMetadataRequestMsgBuilder.setRuleChainIdLSB(edgeRootRuleChainId.getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(ruleChainMetadataRequestMsgBuilder);
        uplinkMsgBuilder.addRuleChainMetadataRequestMsg(ruleChainMetadataRequestMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());;

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RuleChainMetadataUpdateMsg);
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = (RuleChainMetadataUpdateMsg) latestMessage;
        Assert.assertEquals(ruleChainMetadataUpdateMsg.getRuleChainIdMSB(), edgeRootRuleChainId.getId().getMostSignificantBits());
        Assert.assertEquals(ruleChainMetadataUpdateMsg.getRuleChainIdLSB(), edgeRootRuleChainId.getId().getLeastSignificantBits());

        testAutoGeneratedCodeByProtobuf(ruleChainMetadataUpdateMsg);
    }

    private void sendUserCredentialsRequest() throws Exception {
        UserId userId = edgeImitator.getUserId();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        UserCredentialsRequestMsg.Builder userCredentialsRequestMsgBuilder = UserCredentialsRequestMsg.newBuilder();
        userCredentialsRequestMsgBuilder.setUserIdMSB(userId.getId().getMostSignificantBits());
        userCredentialsRequestMsgBuilder.setUserIdLSB(userId.getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(userCredentialsRequestMsgBuilder);
        uplinkMsgBuilder.addUserCredentialsRequestMsg(userCredentialsRequestMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserCredentialsUpdateMsg);
        UserCredentialsUpdateMsg userCredentialsUpdateMsg = (UserCredentialsUpdateMsg) latestMessage;
        Assert.assertEquals(userCredentialsUpdateMsg.getUserIdMSB(), userId.getId().getMostSignificantBits());
        Assert.assertEquals(userCredentialsUpdateMsg.getUserIdLSB(), userId.getId().getLeastSignificantBits());

        testAutoGeneratedCodeByProtobuf(userCredentialsUpdateMsg);
    }

    private void sendDeviceCredentialsRequest() throws Exception {
        Device device = findDeviceByName("Edge Device 1");

        DeviceCredentials deviceCredentials = doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceCredentialsRequestMsg.Builder deviceCredentialsRequestMsgBuilder = DeviceCredentialsRequestMsg.newBuilder();
        deviceCredentialsRequestMsgBuilder.setDeviceIdMSB(device.getUuidId().getMostSignificantBits());
        deviceCredentialsRequestMsgBuilder.setDeviceIdLSB(device.getUuidId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(deviceCredentialsRequestMsgBuilder);
        uplinkMsgBuilder.addDeviceCredentialsRequestMsg(deviceCredentialsRequestMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceCredentialsUpdateMsg);
        DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg = (DeviceCredentialsUpdateMsg) latestMessage;
        Assert.assertEquals(deviceCredentialsUpdateMsg.getDeviceIdMSB(), device.getUuidId().getMostSignificantBits());
        Assert.assertEquals(deviceCredentialsUpdateMsg.getDeviceIdLSB(), device.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(deviceCredentialsUpdateMsg.getCredentialsType(), deviceCredentials.getCredentialsType().name());
        Assert.assertEquals(deviceCredentialsUpdateMsg.getCredentialsId(), deviceCredentials.getCredentialsId());
    }

    private void sendDeviceCredentialsUpdate() throws Exception {
        Device device = findDeviceByName("Edge Device 1");

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceCredentialsUpdateMsg.Builder deviceCredentialsUpdateMsgBuilder = DeviceCredentialsUpdateMsg.newBuilder();
        deviceCredentialsUpdateMsgBuilder.setDeviceIdMSB(device.getUuidId().getMostSignificantBits());
        deviceCredentialsUpdateMsgBuilder.setDeviceIdLSB(device.getUuidId().getLeastSignificantBits());
        deviceCredentialsUpdateMsgBuilder.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN.name());
        deviceCredentialsUpdateMsgBuilder.setCredentialsId("NEW_TOKEN");
        testAutoGeneratedCodeByProtobuf(deviceCredentialsUpdateMsgBuilder);
        uplinkMsgBuilder.addDeviceCredentialsUpdateMsg(deviceCredentialsUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
    }

    private void sendDeviceRpcResponse() throws Exception {
        Device device = findDeviceByName("Edge Device 1");

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceRpcCallMsg.Builder deviceRpcCallResponseBuilder = DeviceRpcCallMsg.newBuilder();
        deviceRpcCallResponseBuilder.setDeviceIdMSB(device.getUuidId().getMostSignificantBits());
        deviceRpcCallResponseBuilder.setDeviceIdLSB(device.getUuidId().getLeastSignificantBits());
        deviceRpcCallResponseBuilder.setOneway(true);
        deviceRpcCallResponseBuilder.setRequestId(0);
        deviceRpcCallResponseBuilder.setExpirationTime(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10));
        RpcResponseMsg.Builder responseBuilder =
                RpcResponseMsg.newBuilder().setResponse("{}");
        testAutoGeneratedCodeByProtobuf(responseBuilder);

        deviceRpcCallResponseBuilder.setResponseMsg(responseBuilder.build());
        testAutoGeneratedCodeByProtobuf(deviceRpcCallResponseBuilder);

        uplinkMsgBuilder.addDeviceRpcCallMsg(deviceRpcCallResponseBuilder.build());
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
    }

    private void sendAttributesRequest() throws Exception {
        Device device = findDeviceByName("Edge Device 1");
        sendAttributesRequest(device, DataConstants.SERVER_SCOPE, "{\"key1\":\"value1\"}", "key1", "value1");
        sendAttributesRequest(device, DataConstants.SHARED_SCOPE, "{\"key2\":\"value2\"}", "key2", "value2");
    }

    private void sendAttributesRequest(Device device, String scope, String attributesDataStr, String expectedKey, String expectedValue) throws Exception {
        JsonNode attributesData = mapper.readTree(attributesDataStr);

        doPost("/api/plugins/telemetry/DEVICE/" + device.getId().getId().toString() + "/attributes/" + scope,
                attributesData);

        // Wait before device attributes saved to database before requesting them from edge
        Thread.sleep(1000);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        AttributesRequestMsg.Builder attributesRequestMsgBuilder = AttributesRequestMsg.newBuilder();
        attributesRequestMsgBuilder.setEntityIdMSB(device.getUuidId().getMostSignificantBits());
        attributesRequestMsgBuilder.setEntityIdLSB(device.getUuidId().getLeastSignificantBits());
        attributesRequestMsgBuilder.setEntityType(EntityType.DEVICE.name());
        attributesRequestMsgBuilder.setScope(scope);
        testAutoGeneratedCodeByProtobuf(attributesRequestMsgBuilder);
        uplinkMsgBuilder.addAttributesRequestMsg(attributesRequestMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityDataProto);
        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
        Assert.assertEquals(device.getUuidId().getMostSignificantBits(), latestEntityDataMsg.getEntityIdMSB());
        Assert.assertEquals(device.getUuidId().getLeastSignificantBits(), latestEntityDataMsg.getEntityIdLSB());
        Assert.assertEquals(device.getId().getEntityType().name(), latestEntityDataMsg.getEntityType());
        Assert.assertEquals(scope, latestEntityDataMsg.getPostAttributeScope());
        Assert.assertTrue(latestEntityDataMsg.hasAttributesUpdatedMsg());

        TransportProtos.PostAttributeMsg attributesUpdatedMsg = latestEntityDataMsg.getAttributesUpdatedMsg();
        Assert.assertEquals(1, attributesUpdatedMsg.getKvCount());
        TransportProtos.KeyValueProto keyValueProto = attributesUpdatedMsg.getKv(0);
        Assert.assertEquals(expectedKey, keyValueProto.getKey());
        Assert.assertEquals(expectedValue, keyValueProto.getStringV());
    }

    private void sendDeleteDeviceOnEdge() throws Exception {
        Device device = findDeviceByName("Edge Device 2");
        UplinkMsg.Builder upLinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceDeleteMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceDeleteMsgBuilder.setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE);
        deviceDeleteMsgBuilder.setIdMSB(device.getId().getId().getMostSignificantBits());
        deviceDeleteMsgBuilder.setIdLSB(device.getId().getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(deviceDeleteMsgBuilder);

        upLinkMsgBuilder.addDeviceUpdateMsg(deviceDeleteMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(upLinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(upLinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        device = doGet("/api/device/" + device.getId().getId().toString(), Device.class);
        Assert.assertNotNull(device);
        List<Device> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId().toString() + "/devices?",
                new TypeReference<PageData<Device>>() {
                }, new PageLink(100)).getData();
        Assert.assertFalse(edgeDevices.contains(device));
    }

    private void installation() throws Exception {
        edge = doPost("/api/edge", constructEdge("Test Edge", "test"), Edge.class);

        DeviceProfile deviceProfile = this.createDeviceProfile(CUSTOM_DEVICE_PROFILE_NAME, null);
        extendDeviceProfileData(deviceProfile);
        doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);

        Device savedDevice = saveDevice("Edge Device 1", CUSTOM_DEVICE_PROFILE_NAME);
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/device/" + savedDevice.getId().getId().toString(), Device.class);

        Asset savedAsset = saveAsset("Edge Asset 1");
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);
    }

    private void extendDeviceProfileData(DeviceProfile deviceProfile) {
        DeviceProfileData profileData = deviceProfile.getProfileData();
        List<DeviceProfileAlarm> alarms = new ArrayList<>();
        DeviceProfileAlarm deviceProfileAlarm = new DeviceProfileAlarm();
        deviceProfileAlarm.setAlarmType("High Temperature");
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setAlarmDetails("Alarm Details");
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setSpec(new SimpleAlarmConditionSpec());
        List<AlarmConditionFilter> condition = new ArrayList<>();
        AlarmConditionFilter alarmConditionFilter = new AlarmConditionFilter();
        alarmConditionFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "temperature"));
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        predicate.setValue(new FilterPredicateValue<>(55.0));
        alarmConditionFilter.setPredicate(predicate);
        alarmConditionFilter.setValueType(EntityKeyValueType.NUMERIC);
        condition.add(alarmConditionFilter);
        alarmCondition.setCondition(condition);
        alarmRule.setCondition(alarmCondition);
        deviceProfileAlarm.setClearRule(alarmRule);
        TreeMap<AlarmSeverity, AlarmRule> createRules = new TreeMap<>();
        createRules.put(AlarmSeverity.CRITICAL, alarmRule);
        deviceProfileAlarm.setCreateRules(createRules);
        alarms.add(deviceProfileAlarm);
        profileData.setAlarms(alarms);
        profileData.setProvisionConfiguration(new AllowCreateNewDevicesDeviceProfileProvisionConfiguration("123"));
    }

    private EdgeEvent constructEdgeEvent(TenantId tenantId, EdgeId edgeId, EdgeEventActionType edgeEventAction, UUID entityId, EdgeEventType edgeEventType, JsonNode entityBody) {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setAction(edgeEventAction);
        edgeEvent.setEntityId(entityId);
        edgeEvent.setType(edgeEventType);
        edgeEvent.setBody(entityBody);
        return edgeEvent;
    }

    private void testAutoGeneratedCodeByProtobuf(MessageLite.Builder builder) throws InvalidProtocolBufferException {
        MessageLite source = builder.build();

        testAutoGeneratedCodeByProtobuf(source);

        MessageLite target = source.getParserForType().parseFrom(source.toByteArray());
        builder.clear().mergeFrom(target);
    }

    private void testAutoGeneratedCodeByProtobuf(MessageLite source) throws InvalidProtocolBufferException {
        MessageLite target = source.getParserForType().parseFrom(source.toByteArray());
        Assert.assertEquals(source, target);
        Assert.assertEquals(source.hashCode(), target.hashCode());
    }
}
