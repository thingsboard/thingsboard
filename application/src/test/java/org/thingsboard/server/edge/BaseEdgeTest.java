/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveOtaPackageInfoRequest;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
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
import org.thingsboard.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
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
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
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
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.edge.imitator.EdgeImitator;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AttributeDeleteMsg;
import org.thingsboard.server.gen.edge.v1.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityViewsRequestMsg;
import org.thingsboard.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RelationRequestMsg;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RpcResponseMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.gen.edge.v1.WidgetTypeUpdateMsg;
import org.thingsboard.server.gen.edge.v1.WidgetsBundleUpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;

@TestPropertySource(properties = {
        "edges.enabled=true",
})
abstract public class BaseEdgeTest extends AbstractControllerTest {

    private static final String THERMOSTAT_DEVICE_PROFILE_NAME = "Thermostat";

    private Tenant savedTenant;
    private TenantId tenantId;
    private User tenantAdmin;

    private DeviceProfile thermostatDeviceProfile;

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
        // sleep 0.5 second to avoid CREDENTIALS updated message for the user
        // user credentials is going to be stored and updated event pushed to edge notification service
        // while service will be processing this event edge could be already added and additional message will be pushed
        Thread.sleep(500);

        installation();

        edgeImitator = new EdgeImitator("localhost", 7070, edge.getRoutingKey(), edge.getSecret());
        edgeImitator.expectMessageAmount(13);
        edgeImitator.connect();

        verifyEdgeConnectionAndInitialData();
    }

    @After
    public void afterTest() throws Exception {
        try {
            edgeImitator.disconnect();
        } catch (Exception ignored) {}

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getUuidId())
                .andExpect(status().isOk());
    }

    private void installation() throws Exception {
        edge = doPost("/api/edge", constructEdge("Test Edge", "test"), Edge.class);

        MqttDeviceProfileTransportConfiguration transportConfiguration = new MqttDeviceProfileTransportConfiguration();
        transportConfiguration.setTransportPayloadTypeConfiguration(new JsonTransportPayloadConfiguration());

        thermostatDeviceProfile = this.createDeviceProfile(THERMOSTAT_DEVICE_PROFILE_NAME, transportConfiguration);

        extendDeviceProfileData(thermostatDeviceProfile);
        thermostatDeviceProfile = doPost("/api/deviceProfile", thermostatDeviceProfile, DeviceProfile.class);

        Device savedDevice = saveDevice("Edge Device 1", THERMOSTAT_DEVICE_PROFILE_NAME);
        doPost("/api/edge/" + edge.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);

        Asset savedAsset = saveAsset("Edge Asset 1");
        doPost("/api/edge/" + edge.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);
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

    private void verifyEdgeConnectionAndInitialData() throws Exception {
        Assert.assertTrue(edgeImitator.waitForMessages());

        EdgeConfiguration configuration = edgeImitator.getConfiguration();
        Assert.assertNotNull(configuration);

        testAutoGeneratedCodeByProtobuf(configuration);

        Optional<DeviceUpdateMsg> deviceUpdateMsgOpt = edgeImitator.findMessageByType(DeviceUpdateMsg.class);
        Assert.assertTrue(deviceUpdateMsgOpt.isPresent());
        DeviceUpdateMsg deviceUpdateMsg = deviceUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        UUID deviceUUID = new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB());
        Device device = doGet("/api/device/" + deviceUUID.toString(), Device.class);
        Assert.assertNotNull(device);
        List<Device> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/devices?",
                new TypeReference<PageData<Device>>() {}, new PageLink(100)).getData();
        Assert.assertTrue(edgeDevices.contains(device));

        List<DeviceProfileUpdateMsg> deviceProfileUpdateMsgList = edgeImitator.findAllMessagesByType(DeviceProfileUpdateMsg.class);
        Assert.assertEquals(3, deviceProfileUpdateMsgList.size());
        Optional<DeviceProfileUpdateMsg> deviceProfileUpdateMsgOpt =
                deviceProfileUpdateMsgList.stream().filter(dfum -> THERMOSTAT_DEVICE_PROFILE_NAME.equals(dfum.getName())).findAny();
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
        List<Asset> edgeAssets = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/assets?",
                new TypeReference<PageData<Asset>>() {}, new PageLink(100)).getData();
        Assert.assertTrue(edgeAssets.contains(asset));

        testAutoGeneratedCodeByProtobuf(assetUpdateMsg);

        Optional<RuleChainUpdateMsg> ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        RuleChainUpdateMsg ruleChainUpdateMsg = ruleChainUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, ruleChainUpdateMsg.getMsgType());
        UUID ruleChainUUID = new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB());
        RuleChain ruleChain = doGet("/api/ruleChain/" + ruleChainUUID.toString(), RuleChain.class);
        Assert.assertNotNull(ruleChain);
        List<RuleChain> edgeRuleChains = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/ruleChains?",
                new TypeReference<PageData<RuleChain>>() {}, new PageLink(100)).getData();
        Assert.assertTrue(edgeRuleChains.contains(ruleChain));

        testAutoGeneratedCodeByProtobuf(ruleChainUpdateMsg);

        validateAdminSettings();
    }

    private void validateAdminSettings() throws JsonProcessingException {
        List<AdminSettingsUpdateMsg> adminSettingsUpdateMsgs = edgeImitator.findAllMessagesByType(AdminSettingsUpdateMsg.class);
        Assert.assertEquals(4, adminSettingsUpdateMsgs.size());

        for (AdminSettingsUpdateMsg adminSettingsUpdateMsg : adminSettingsUpdateMsgs) {
            if (adminSettingsUpdateMsg.getKey().equals("mail")) {
                validateMailAdminSettings(adminSettingsUpdateMsg);
            }
            if (adminSettingsUpdateMsg.getKey().equals("mailTemplates")) {
                validateMailTemplatesAdminSettings(adminSettingsUpdateMsg);
            }
        }
    }

    private void validateMailAdminSettings(AdminSettingsUpdateMsg adminSettingsUpdateMsg) throws JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(adminSettingsUpdateMsg.getJsonValue());
        Assert.assertNotNull(jsonNode.get("mailFrom"));
        Assert.assertNotNull(jsonNode.get("smtpProtocol"));
        Assert.assertNotNull(jsonNode.get("smtpHost"));
        Assert.assertNotNull(jsonNode.get("smtpPort"));
        Assert.assertNotNull(jsonNode.get("timeout"));
    }

    private void validateMailTemplatesAdminSettings(AdminSettingsUpdateMsg adminSettingsUpdateMsg) throws JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(adminSettingsUpdateMsg.getJsonValue());
        Assert.assertNotNull(jsonNode.get("accountActivated"));
        Assert.assertNotNull(jsonNode.get("accountLockout"));
        Assert.assertNotNull(jsonNode.get("activation"));
        Assert.assertNotNull(jsonNode.get("passwordWasReset"));
        Assert.assertNotNull(jsonNode.get("resetPassword"));
        Assert.assertNotNull(jsonNode.get("test"));
    }

    @Test
    public void testDeviceProfiles() throws Exception {
        // 1
        DeviceProfile deviceProfile = this.createDeviceProfile("ONE_MORE_DEVICE_PROFILE", null);
        extendDeviceProfileData(deviceProfile);
        edgeImitator.expectMessageAmount(1);
        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceProfileUpdateMsg);
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(deviceProfileUpdateMsg.getIdMSB(), deviceProfile.getUuidId().getMostSignificantBits());
        Assert.assertEquals(deviceProfileUpdateMsg.getIdLSB(), deviceProfile.getUuidId().getLeastSignificantBits());

        // 2
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/deviceProfile/" + deviceProfile.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceProfileUpdateMsg);
        deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(deviceProfileUpdateMsg.getIdMSB(), deviceProfile.getUuidId().getMostSignificantBits());
        Assert.assertEquals(deviceProfileUpdateMsg.getIdLSB(), deviceProfile.getUuidId().getLeastSignificantBits());
    }

    @Test
    public void testDevices() throws Exception {
        // 1
        Device savedDevice = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        // 2
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        DeviceUpdateMsg deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(deviceUpdateMsg.getIdMSB(), savedDevice.getUuidId().getMostSignificantBits());
        Assert.assertEquals(deviceUpdateMsg.getIdLSB(), savedDevice.getUuidId().getLeastSignificantBits());

        // 3
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/device/" + savedDevice.getUuidId())
                .andExpect(status().isOk());
        // we should not get any message because device is not assigned to edge any more
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // 4
        edgeImitator.expectMessageAmount(1);
        savedDevice = saveDevice("Edge Device 3", "Default");
        doPost("/api/edge/" + edge.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);
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
        doDelete("/api/device/" + savedDevice.getUuidId())
                .andExpect(status().isOk());
        // in this case we should get messages because device was assigned to edge
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(deviceUpdateMsg.getIdMSB(), savedDevice.getUuidId().getMostSignificantBits());
        Assert.assertEquals(deviceUpdateMsg.getIdLSB(), savedDevice.getUuidId().getLeastSignificantBits());

    }

    @Test
    public void testDeviceReachedMaximumAllowedOnCloud() throws Exception {
        // update tenant profile configuration
        loginSysAdmin();
        TenantProfile tenantProfile = doGet("/api/tenantProfile/" + savedTenant.getTenantProfileId().getId(), TenantProfile.class);
        DefaultTenantProfileConfiguration profileConfiguration =
                (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();
        profileConfiguration.setMaxDevices(1);
        tenantProfile.getProfileData().setConfiguration(profileConfiguration);
        doPost("/api/tenantProfile/", tenantProfile, TenantProfile.class);

        loginTenantAdmin();

        UUID uuid = Uuids.timeBased();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceUpdateMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        deviceUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        deviceUpdateMsgBuilder.setName("Edge Device");
        deviceUpdateMsgBuilder.setType("default");
        deviceUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        uplinkMsgBuilder.addDeviceUpdateMsg(deviceUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());

        UplinkResponseMsg latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());
    }

    @Test
    public void testAssets() throws Exception {
        // 1
        edgeImitator.expectMessageAmount(1);
        Asset savedAsset = saveAsset("Edge Asset 2");
        doPost("/api/edge/" + edge.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);
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
        doDelete("/api/edge/" + edge.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetUpdateMsg);
        assetUpdateMsg = (AssetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(assetUpdateMsg.getIdMSB(), savedAsset.getUuidId().getMostSignificantBits());
        Assert.assertEquals(assetUpdateMsg.getIdLSB(), savedAsset.getUuidId().getLeastSignificantBits());

        // 3
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/asset/" + savedAsset.getUuidId())
                .andExpect(status().isOk());
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // 4
        edgeImitator.expectMessageAmount(1);
        savedAsset = saveAsset("Edge Asset 3");
        doPost("/api/edge/" + edge.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);
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
        doDelete("/api/asset/" + savedAsset.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetUpdateMsg);
        assetUpdateMsg = (AssetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(assetUpdateMsg.getIdMSB(), savedAsset.getUuidId().getMostSignificantBits());
        Assert.assertEquals(assetUpdateMsg.getIdLSB(), savedAsset.getUuidId().getLeastSignificantBits());
    }

    @Test
    public void testRuleChains() throws Exception {
        // 1
        edgeImitator.expectMessageAmount(2);
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Edge Test Rule Chain");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + savedRuleChain.getUuidId(), RuleChain.class);
        createRuleChainMetadata(savedRuleChain);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<RuleChainUpdateMsg> ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        RuleChainUpdateMsg ruleChainUpdateMsg = ruleChainUpdateMsgOpt.get();
        Assert.assertTrue(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(ruleChainUpdateMsg.getMsgType()) ||
                UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(ruleChainUpdateMsg.getMsgType()));
        Assert.assertEquals(ruleChainUpdateMsg.getIdMSB(), savedRuleChain.getUuidId().getMostSignificantBits());
        Assert.assertEquals(ruleChainUpdateMsg.getIdLSB(), savedRuleChain.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(ruleChainUpdateMsg.getName(), savedRuleChain.getName());

        // 2
        testRuleChainMetadataRequestMsg(savedRuleChain.getId());

        // 3
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + savedRuleChain.getUuidId(), RuleChain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        ruleChainUpdateMsg = ruleChainUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, ruleChainUpdateMsg.getMsgType());
        Assert.assertEquals(ruleChainUpdateMsg.getIdMSB(), savedRuleChain.getUuidId().getMostSignificantBits());
        Assert.assertEquals(ruleChainUpdateMsg.getIdLSB(), savedRuleChain.getUuidId().getLeastSignificantBits());

        // 4
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/ruleChain/" + savedRuleChain.getUuidId())
                .andExpect(status().isOk());
        Assert.assertFalse(edgeImitator.waitForMessages(1));
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

        doPost("/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class);
    }

    @Test
    public void testDashboards() throws Exception {
        // 1
        edgeImitator.expectMessageAmount(1);
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Edge Test Dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
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
        doDelete("/api/edge/" + edge.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(dashboardUpdateMsg.getIdMSB(), savedDashboard.getUuidId().getMostSignificantBits());
        Assert.assertEquals(dashboardUpdateMsg.getIdLSB(), savedDashboard.getUuidId().getLeastSignificantBits());

        // 4
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/dashboard/" + savedDashboard.getUuidId())
                .andExpect(status().isOk());
        Assert.assertFalse(edgeImitator.waitForMessages(1));
    }

    @Test
    public void testRelations() throws Exception {
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
    }

    @Test
    public void testAlarms() throws Exception {
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
        doPost("/api/alarm/" + savedAlarm.getUuidId() + "/ack");
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
        doPost("/api/alarm/" + savedAlarm.getUuidId() + "/clear");
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
        doDelete("/api/alarm/" + savedAlarm.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AlarmUpdateMsg);
        alarmUpdateMsg = (AlarmUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, alarmUpdateMsg.getMsgType());
        Assert.assertEquals(alarmUpdateMsg.getType(), savedAlarm.getType());
        Assert.assertEquals(alarmUpdateMsg.getName(), savedAlarm.getName());
        Assert.assertEquals(alarmUpdateMsg.getOriginatorName(), device.getName());
        Assert.assertEquals(alarmUpdateMsg.getStatus(), AlarmStatus.CLEARED_ACK.name());
    }

    @Test
    public void testEntityView() throws Exception {
        // 1
        edgeImitator.expectMessageAmount(1);
        Device device = findDeviceByName("Edge Device 1");
        EntityView entityView = new EntityView();
        entityView.setName("Edge EntityView 1");
        entityView.setType("test");
        entityView.setEntityId(device.getId());
        EntityView savedEntityView = doPost("/api/entityView", entityView, EntityView.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/entityView/" + savedEntityView.getUuidId(), EntityView.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        verifyEntityViewUpdateMsg(savedEntityView, device);


        // 2
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        EntityViewsRequestMsg.Builder entityViewsRequestBuilder = EntityViewsRequestMsg.newBuilder();
        entityViewsRequestBuilder.setEntityIdMSB(device.getUuidId().getMostSignificantBits());
        entityViewsRequestBuilder.setEntityIdLSB(device.getUuidId().getLeastSignificantBits());
        entityViewsRequestBuilder.setEntityType(device.getId().getEntityType().name());
        testAutoGeneratedCodeByProtobuf(entityViewsRequestBuilder);
        uplinkMsgBuilder.addEntityViewsRequestMsg(entityViewsRequestBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());
        verifyEntityViewUpdateMsg(savedEntityView, device);


        // 3
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/entityView/" + savedEntityView.getUuidId(), EntityView.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityViewUpdateMsg);
        EntityViewUpdateMsg entityViewUpdateMsg = (EntityViewUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, entityViewUpdateMsg.getMsgType());
        Assert.assertEquals(entityViewUpdateMsg.getIdMSB(), savedEntityView.getUuidId().getMostSignificantBits());
        Assert.assertEquals(entityViewUpdateMsg.getIdLSB(), savedEntityView.getUuidId().getLeastSignificantBits());


        edgeImitator.expectMessageAmount(1);
        doDelete("/api/entityView/" + savedEntityView.getUuidId())
                .andExpect(status().isOk());
        Assert.assertFalse(edgeImitator.waitForMessages(1));
    }

    private void verifyEntityViewUpdateMsg(EntityView entityView, Device device) {
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityViewUpdateMsg);
        EntityViewUpdateMsg entityViewUpdateMsg = (EntityViewUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, entityViewUpdateMsg.getMsgType());
        Assert.assertEquals(entityViewUpdateMsg.getType(), entityView.getType());
        Assert.assertEquals(entityViewUpdateMsg.getName(), entityView.getName());
        Assert.assertEquals(entityViewUpdateMsg.getIdMSB(), entityView.getUuidId().getMostSignificantBits());
        Assert.assertEquals(entityViewUpdateMsg.getIdLSB(), entityView.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(entityViewUpdateMsg.getEntityIdMSB(), device.getUuidId().getMostSignificantBits());
        Assert.assertEquals(entityViewUpdateMsg.getEntityIdLSB(), device.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(entityViewUpdateMsg.getEntityType().name(), device.getId().getEntityType().name());
    }

    @Test
    public void testCustomerAndNewUser() throws Exception {
        // 1
        edgeImitator.expectMessageAmount(1);
        Customer customer = new Customer();
        customer.setTitle("Edge Customer 1");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/edge/" + edge.getUuidId(), Edge.class);
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
        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(savedTenant.getId());
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setEmail("customerUser@thingsboard.org");
        customerUser.setFirstName("John");
        customerUser.setLastName("Edwards");
        User savedUser = doPost("/api/user", customerUser, User.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserUpdateMsg);
        UserUpdateMsg userUpdateMsg = (UserUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(userUpdateMsg.getIdMSB(), savedUser.getUuidId().getMostSignificantBits());
        Assert.assertEquals(userUpdateMsg.getIdLSB(), savedUser.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(userUpdateMsg.getEmail(), savedUser.getEmail());
        testAutoGeneratedCodeByProtobuf(userUpdateMsg);

        // 3
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/customer/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof CustomerUpdateMsg);
        customerUpdateMsg = (CustomerUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, customerUpdateMsg.getMsgType());
        Assert.assertEquals(customerUpdateMsg.getIdMSB(), savedCustomer.getUuidId().getMostSignificantBits());
        Assert.assertEquals(customerUpdateMsg.getIdLSB(), savedCustomer.getUuidId().getLeastSignificantBits());

        edgeImitator.expectMessageAmount(1);
        doDelete("/api/customer/" + savedCustomer.getUuidId())
                .andExpect(status().isOk());
        Assert.assertFalse(edgeImitator.waitForMessages(1));
    }

    @Test
    public void testWidgetsBundleAndWidgetType() throws Exception {
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
        doDelete("/api/widgetType/" + savedWidgetType.getUuidId())
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
        doDelete("/api/widgetsBundle/" + savedWidgetsBundle.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof WidgetsBundleUpdateMsg);
        widgetsBundleUpdateMsg = (WidgetsBundleUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, widgetsBundleUpdateMsg.getMsgType());
        Assert.assertEquals(widgetsBundleUpdateMsg.getIdMSB(), savedWidgetsBundle.getUuidId().getMostSignificantBits());
        Assert.assertEquals(widgetsBundleUpdateMsg.getIdLSB(), savedWidgetsBundle.getUuidId().getLeastSignificantBits());
    }

    @Test
    public void testTimeseries() throws Exception {
        edgeImitator.expectMessageAmount(1);
        Device device = findDeviceByName("Edge Device 1");
        String timeseriesData = "{\"data\":{\"temperature\":25},\"ts\":" + System.currentTimeMillis() + "}";
        JsonNode timeseriesEntityData = mapper.readTree(timeseriesData);
        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.TIMESERIES_UPDATED, device.getId().getId(), EdgeEventType.DEVICE, timeseriesEntityData);
        edgeEventService.saveAsync(edgeEvent).get();
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
    }

    @Test
    public void testAttributes() throws Exception {
        Device device = findDeviceByName("Edge Device 1");

        testAttributesUpdatedMsg(device);
        testPostAttributesMsg(device);
        testAttributesDeleteMsg(device);
    }

    private void testAttributesUpdatedMsg(Device device) throws Exception {
        String attributesData = "{\"scope\":\"SERVER_SCOPE\",\"kv\":{\"key1\":\"value1\"}}";
        JsonNode attributesEntityData = mapper.readTree(attributesData);
        EdgeEvent edgeEvent1 = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.ATTRIBUTES_UPDATED, device.getId().getId(), EdgeEventType.DEVICE, attributesEntityData);
        edgeImitator.expectMessageAmount(1);
        edgeEventService.saveAsync(edgeEvent1).get();
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

    private void testPostAttributesMsg(Device device) throws Exception {
        String postAttributesData = "{\"scope\":\"SERVER_SCOPE\",\"kv\":{\"key2\":\"value2\"}}";
        JsonNode postAttributesEntityData = mapper.readTree(postAttributesData);
        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.POST_ATTRIBUTES, device.getId().getId(), EdgeEventType.DEVICE, postAttributesEntityData);
        edgeImitator.expectMessageAmount(1);
        edgeEventService.saveAsync(edgeEvent).get();
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

    private void testAttributesDeleteMsg(Device device) throws Exception {
        String deleteAttributesData = "{\"scope\":\"SERVER_SCOPE\",\"keys\":[\"key1\",\"key2\"]}";
        JsonNode deleteAttributesEntityData = mapper.readTree(deleteAttributesData);
        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.ATTRIBUTES_DELETED, device.getId().getId(), EdgeEventType.DEVICE, deleteAttributesEntityData);
        edgeImitator.expectMessageAmount(1);
        edgeEventService.saveAsync(edgeEvent).get();
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

    @Test
    public void testRpcCall() throws Exception {
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
        edgeEventService.saveAsync(edgeEvent).get();
        clusterService.onEdgeEventUpdate(tenantId, edge.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceRpcCallMsg);
        DeviceRpcCallMsg latestDeviceRpcCallMsg = (DeviceRpcCallMsg) latestMessage;
        Assert.assertEquals("test_method", latestDeviceRpcCallMsg.getRequestMsg().getMethod());
    }

    @Test
    public void testTimeseriesWithFailures() throws Exception {
        int numberOfTimeseriesToSend = 1000;

        edgeImitator.setRandomFailuresOnTimeseriesDownlink(true);
        // imitator will generate failure in 5% of cases
        edgeImitator.setFailureProbability(5.0);

        edgeImitator.expectMessageAmount(numberOfTimeseriesToSend);
        Device device = findDeviceByName("Edge Device 1");
        for (int idx = 1; idx <= numberOfTimeseriesToSend; idx++) {
            String timeseriesData = "{\"data\":{\"idx\":" + idx + "},\"ts\":" + System.currentTimeMillis() + "}";
            JsonNode timeseriesEntityData = mapper.readTree(timeseriesData);
            EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.TIMESERIES_UPDATED,
                    device.getId().getId(), EdgeEventType.DEVICE, timeseriesEntityData);
            edgeEventService.saveAsync(edgeEvent).get();
            clusterService.onEdgeEventUpdate(tenantId, edge.getId());
        }

        Assert.assertTrue(edgeImitator.waitForMessages(120));

        List<EntityDataProto> allTelemetryMsgs = edgeImitator.findAllMessagesByType(EntityDataProto.class);
        Assert.assertEquals(numberOfTimeseriesToSend, allTelemetryMsgs.size());

        for (int idx = 1; idx <= numberOfTimeseriesToSend; idx++) {
            Assert.assertTrue(isIdxExistsInTheDownlinkList(idx, allTelemetryMsgs));
        }

        edgeImitator.setRandomFailuresOnTimeseriesDownlink(false);
    }

    private boolean isIdxExistsInTheDownlinkList(int idx, List<EntityDataProto> allTelemetryMsgs) {
        for (EntityDataProto proto : allTelemetryMsgs) {
            TransportProtos.PostTelemetryMsg postTelemetryMsg = proto.getPostTelemetryMsg();
            Assert.assertEquals(1, postTelemetryMsg.getTsKvListCount());
            TransportProtos.TsKvListProto tsKvListProto = postTelemetryMsg.getTsKvList(0);
            Assert.assertEquals(1, tsKvListProto.getKvCount());
            TransportProtos.KeyValueProto keyValueProto = tsKvListProto.getKv(0);
            Assert.assertEquals("idx", keyValueProto.getKey());
            if (keyValueProto.getLongV() == idx) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testSendDeviceToCloud() throws Exception {
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
        Assert.assertTrue(latestMessage instanceof DeviceCredentialsRequestMsg);
        DeviceCredentialsRequestMsg latestDeviceCredentialsRequestMsg = (DeviceCredentialsRequestMsg) latestMessage;
        Assert.assertEquals(uuid.getMostSignificantBits(), latestDeviceCredentialsRequestMsg.getDeviceIdMSB());
        Assert.assertEquals(uuid.getLeastSignificantBits(), latestDeviceCredentialsRequestMsg.getDeviceIdLSB());

        UUID newDeviceId = new UUID(latestDeviceCredentialsRequestMsg.getDeviceIdMSB(), latestDeviceCredentialsRequestMsg.getDeviceIdLSB());

        Device device = doGet("/api/device/" + newDeviceId, Device.class);
        Assert.assertNotNull(device);
        Assert.assertEquals("Edge Device 2", device.getName());
    }

    @Test
    public void testSendDeviceToCloudWithNameThatAlreadyExistsOnCloud() throws Exception {
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
        edgeImitator.expectMessageAmount(2);
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getMessageFromTail(2);
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        DeviceUpdateMsg latestDeviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertNotEquals(deviceOnCloudName, latestDeviceUpdateMsg.getName());
        Assert.assertEquals(deviceOnCloudName, latestDeviceUpdateMsg.getConflictName());

        UUID newDeviceId = new UUID(latestDeviceUpdateMsg.getIdMSB(), latestDeviceUpdateMsg.getIdLSB());

        Assert.assertNotEquals(deviceOnCloud.getId().getId(), newDeviceId);

        Device device = doGet("/api/device/" + newDeviceId, Device.class);
        Assert.assertNotNull(device);
        Assert.assertNotEquals(deviceOnCloudName, device.getName());

        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceCredentialsRequestMsg);
        DeviceCredentialsRequestMsg latestDeviceCredentialsRequestMsg = (DeviceCredentialsRequestMsg) latestMessage;
        Assert.assertEquals(uuid.getMostSignificantBits(), latestDeviceCredentialsRequestMsg.getDeviceIdMSB());
        Assert.assertEquals(uuid.getLeastSignificantBits(), latestDeviceCredentialsRequestMsg.getDeviceIdLSB());

        newDeviceId = new UUID(latestDeviceCredentialsRequestMsg.getDeviceIdMSB(), latestDeviceCredentialsRequestMsg.getDeviceIdLSB());

        device = doGet("/api/device/" + newDeviceId, Device.class);
        Assert.assertNotNull(device);
        Assert.assertNotEquals(deviceOnCloudName, device.getName());
    }

    @Test
    public void testSendRelationRequestToCloud() throws Exception {
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

    @Test
    public void testSendAlarmToCloud() throws Exception {
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();

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
                new PageLink(100), device.getId().getEntityType().name(), device.getUuidId())
                .getData();
        Optional<AlarmInfo> foundAlarm = alarms.stream().filter(alarm -> alarm.getType().equals("alarm from edge")).findAny();
        Assert.assertTrue(foundAlarm.isPresent());
        AlarmInfo alarmInfo = foundAlarm.get();
        Assert.assertEquals(device.getId(), alarmInfo.getOriginator());
        Assert.assertEquals(AlarmStatus.ACTIVE_UNACK, alarmInfo.getStatus());
        Assert.assertEquals(AlarmSeverity.CRITICAL, alarmInfo.getSeverity());
    }

    @Test
    public void testSendTelemetryToCloud() throws Exception {
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();

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

        Awaitility.await()
                .atMost(2, TimeUnit.SECONDS)
                .until(() -> loadDeviceTimeseries(device, timeseriesKey).containsKey(timeseriesKey));

        Map<String, List<Map<String, String>>> timeseries = loadDeviceTimeseries(device, timeseriesKey);
        Assert.assertTrue(timeseries.containsKey(timeseriesKey));
        Assert.assertEquals(1, timeseries.get(timeseriesKey).size());
        Assert.assertEquals(timeseriesValue, timeseries.get(timeseriesKey).get(0).get("value"));

        String attributeValuesUrl = "/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/attributes/" + DataConstants.SERVER_SCOPE;
        List<Map<String, String>> attributes = doGetAsyncTyped(attributeValuesUrl, new TypeReference<>() {});
        Assert.assertEquals(2, attributes.size());
        var result = attributes.stream().filter(kv -> kv.get("key").equals(attributesKey)).filter(kv -> kv.get("value").equals(attributesValue)).findFirst();
        Assert.assertTrue(result.isPresent());
    }

    private Map<String, List<Map<String, String>>> loadDeviceTimeseries(Device device, String timeseriesKey) throws Exception {
        return doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/values/timeseries?keys=" + timeseriesKey,
                new TypeReference<>() {});
    }

    @Test
    public void testSendRelationToCloud() throws Exception {
        Device device1 = saveDeviceOnCloudAndVerifyDeliveryToEdge();
        Device device2 = saveDeviceOnCloudAndVerifyDeliveryToEdge();

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
                "&fromId=" + device2.getUuidId() +
                "&fromType=" + device2.getId().getEntityType().name() +
                "&relationType=" + "test" +
                "&relationTypeGroup=" + RelationTypeGroup.COMMON.name() +
                "&toId=" + device1.getUuidId() +
                "&toType=" + device1.getId().getEntityType().name(), EntityRelation.class);
        Assert.assertNotNull(relation);
    }

    @Test
    public void testSendDeleteDeviceOnEdgeToCloud() throws Exception {
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();
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
        device = doGet("/api/device/" + device.getUuidId(), Device.class);
        Assert.assertNotNull(device);
        List<Device> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/devices?",
                new TypeReference<PageData<Device>>() {
                }, new PageLink(100)).getData();
        Assert.assertFalse(edgeDevices.contains(device));
    }

    @Test
    public void testSendRuleChainMetadataRequestToCloud() throws Exception {
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
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RuleChainMetadataUpdateMsg);
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = (RuleChainMetadataUpdateMsg) latestMessage;
        Assert.assertEquals(ruleChainMetadataUpdateMsg.getRuleChainIdMSB(), edgeRootRuleChainId.getId().getMostSignificantBits());
        Assert.assertEquals(ruleChainMetadataUpdateMsg.getRuleChainIdLSB(), edgeRootRuleChainId.getId().getLeastSignificantBits());

        testAutoGeneratedCodeByProtobuf(ruleChainMetadataUpdateMsg);
    }

    @Test
    public void testSendUserCredentialsRequestToCloud() throws Exception {
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        UserCredentialsRequestMsg.Builder userCredentialsRequestMsgBuilder = UserCredentialsRequestMsg.newBuilder();
        userCredentialsRequestMsgBuilder.setUserIdMSB(tenantAdmin.getId().getId().getMostSignificantBits());
        userCredentialsRequestMsgBuilder.setUserIdLSB(tenantAdmin.getId().getId().getLeastSignificantBits());
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
        Assert.assertEquals(userCredentialsUpdateMsg.getUserIdMSB(), tenantAdmin.getId().getId().getMostSignificantBits());
        Assert.assertEquals(userCredentialsUpdateMsg.getUserIdLSB(), tenantAdmin.getId().getId().getLeastSignificantBits());

        testAutoGeneratedCodeByProtobuf(userCredentialsUpdateMsg);
    }

    @Test
    public void testSendDeviceCredentialsRequestToCloud() throws Exception {
        Device device = findDeviceByName("Edge Device 1");

        DeviceCredentials deviceCredentials = doGet("/api/device/" + device.getUuidId() + "/credentials", DeviceCredentials.class);

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

    @Test
    public void testSendDeviceRpcResponseToCloud() throws Exception {
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

    @Test
    public void testSendDeviceCredentialsUpdateToCloud() throws Exception {
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

    @Test
    public void testSendAttributesRequestToCloud() throws Exception {
        Device device = findDeviceByName("Edge Device 1");
        sendAttributesRequestAndVerify(device, DataConstants.SERVER_SCOPE, "{\"key1\":\"value1\"}",
                "key1", "value1");
        sendAttributesRequestAndVerify(device, DataConstants.SHARED_SCOPE, "{\"key2\":\"value2\"}",
                "key2", "value2");
    }

    private void sendAttributesRequestAndVerify(Device device, String scope, String attributesDataStr, String expectedKey,
                                                String expectedValue) throws Exception {
        JsonNode attributesData = mapper.readTree(attributesDataStr);

        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/attributes/" + scope,
                attributesData);

        // Wait before device attributes saved to database before requesting them from edge
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    String urlTemplate = "/api/plugins/telemetry/DEVICE/" + device.getId() + "/keys/attributes/" + scope;
                    List<String> actualKeys = doGetAsyncTyped(urlTemplate, new TypeReference<>() {});
                    return actualKeys != null && !actualKeys.isEmpty() && actualKeys.contains(expectedKey);
                });

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

        boolean found = false;
        for (TransportProtos.KeyValueProto keyValueProto : attributesUpdatedMsg.getKvList()) {
            if (keyValueProto.getKey().equals(expectedKey)) {
                Assert.assertEquals(expectedKey, keyValueProto.getKey());
                Assert.assertEquals(expectedValue, keyValueProto.getStringV());
                found = true;
            }
        }
        Assert.assertTrue("Expected key and value must be found", found);
    }

    @Test
    public void testOtaPackages_usesUrl() throws Exception {
        // 1
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(thermostatDeviceProfile.getId());
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle("My firmware #1");
        firmwareInfo.setVersion("v1.0");
        firmwareInfo.setTag("My firmware #1 v1.0");
        firmwareInfo.setUsesUrl(true);
        firmwareInfo.setUrl("http://localhost:8080/v1/package");
        firmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());

        edgeImitator.expectMessageAmount(1);
        OtaPackageInfo savedFirmwareInfo = doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OtaPackageUpdateMsg);
        OtaPackageUpdateMsg otaPackageUpdateMsg = (OtaPackageUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, otaPackageUpdateMsg.getMsgType());
        Assert.assertEquals(savedFirmwareInfo.getUuidId().getMostSignificantBits(), otaPackageUpdateMsg.getIdMSB());
        Assert.assertEquals(savedFirmwareInfo.getUuidId().getLeastSignificantBits(), otaPackageUpdateMsg.getIdLSB());
        Assert.assertEquals(thermostatDeviceProfile.getUuidId().getMostSignificantBits(), otaPackageUpdateMsg.getDeviceProfileIdMSB());
        Assert.assertEquals(thermostatDeviceProfile.getUuidId().getLeastSignificantBits(), otaPackageUpdateMsg.getDeviceProfileIdLSB());
        Assert.assertEquals(FIRMWARE, OtaPackageType.valueOf(otaPackageUpdateMsg.getType()));
        Assert.assertEquals("My firmware #1", otaPackageUpdateMsg.getTitle());
        Assert.assertEquals("v1.0", otaPackageUpdateMsg.getVersion());
        Assert.assertEquals("My firmware #1 v1.0", otaPackageUpdateMsg.getTag());
        Assert.assertEquals("http://localhost:8080/v1/package", otaPackageUpdateMsg.getUrl());
        Assert.assertFalse(otaPackageUpdateMsg.hasData());
        Assert.assertFalse(otaPackageUpdateMsg.hasFileName());
        Assert.assertFalse(otaPackageUpdateMsg.hasContentType());
        Assert.assertFalse(otaPackageUpdateMsg.hasChecksumAlgorithm());
        Assert.assertFalse(otaPackageUpdateMsg.hasChecksum());
        Assert.assertFalse(otaPackageUpdateMsg.hasDataSize());

        // 2
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/otaPackage/" + savedFirmwareInfo.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OtaPackageUpdateMsg);
        otaPackageUpdateMsg = (OtaPackageUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, otaPackageUpdateMsg.getMsgType());
        Assert.assertEquals(otaPackageUpdateMsg.getIdMSB(), savedFirmwareInfo.getUuidId().getMostSignificantBits());
        Assert.assertEquals(otaPackageUpdateMsg.getIdLSB(), savedFirmwareInfo.getUuidId().getLeastSignificantBits());
    }

    @Test
    public void testOtaPackages_hasData() throws Exception {
        // 1
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(thermostatDeviceProfile.getId());
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle("My firmware #2");
        firmwareInfo.setVersion("v2.0");
        firmwareInfo.setTag("My firmware #2 v2.0");
        firmwareInfo.setUsesUrl(false);
        firmwareInfo.setHasData(false);
        firmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());

        edgeImitator.expectMessageAmount(1);

        OtaPackageInfo savedFirmwareInfo = doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);
        MockMultipartFile testData = new MockMultipartFile("file", "firmware.bin", "image/png", ByteBuffer.wrap(new byte[]{1, 3, 5}).array());
        savedFirmwareInfo = saveData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksumAlgorithm={checksumAlgorithm}", testData, ChecksumAlgorithm.SHA256.name());

        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OtaPackageUpdateMsg);
        OtaPackageUpdateMsg otaPackageUpdateMsg = (OtaPackageUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, otaPackageUpdateMsg.getMsgType());
        Assert.assertEquals(savedFirmwareInfo.getUuidId().getMostSignificantBits(), otaPackageUpdateMsg.getIdMSB());
        Assert.assertEquals(savedFirmwareInfo.getUuidId().getLeastSignificantBits(), otaPackageUpdateMsg.getIdLSB());
        Assert.assertEquals(thermostatDeviceProfile.getUuidId().getMostSignificantBits(), otaPackageUpdateMsg.getDeviceProfileIdMSB());
        Assert.assertEquals(thermostatDeviceProfile.getUuidId().getLeastSignificantBits(), otaPackageUpdateMsg.getDeviceProfileIdLSB());
        Assert.assertEquals(FIRMWARE, OtaPackageType.valueOf(otaPackageUpdateMsg.getType()));
        Assert.assertEquals("My firmware #2", otaPackageUpdateMsg.getTitle());
        Assert.assertEquals("v2.0", otaPackageUpdateMsg.getVersion());
        Assert.assertEquals("My firmware #2 v2.0", otaPackageUpdateMsg.getTag());
        Assert.assertFalse(otaPackageUpdateMsg.hasUrl());
        Assert.assertEquals("firmware.bin", otaPackageUpdateMsg.getFileName());
        Assert.assertEquals("image/png", otaPackageUpdateMsg.getContentType());
        Assert.assertEquals(ChecksumAlgorithm.SHA256.name(), otaPackageUpdateMsg.getChecksumAlgorithm());
        Assert.assertEquals("62467691cf583d4fa78b18fafaf9801f505e0ef03baf0603fd4b0cd004cd1e75", otaPackageUpdateMsg.getChecksum());
        Assert.assertEquals(3L, otaPackageUpdateMsg.getDataSize());
        Assert.assertEquals(ByteString.copyFrom(new byte[]{1, 3, 5}), otaPackageUpdateMsg.getData());

        // 2
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/otaPackage/" + savedFirmwareInfo.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OtaPackageUpdateMsg);
        otaPackageUpdateMsg = (OtaPackageUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, otaPackageUpdateMsg.getMsgType());
        Assert.assertEquals(otaPackageUpdateMsg.getIdMSB(), savedFirmwareInfo.getUuidId().getMostSignificantBits());
        Assert.assertEquals(otaPackageUpdateMsg.getIdLSB(), savedFirmwareInfo.getUuidId().getLeastSignificantBits());
    }

    // Utility methods

    private Device saveDeviceOnCloudAndVerifyDeliveryToEdge() throws Exception {
        edgeImitator.expectMessageAmount(1);
        Device savedDevice = saveDevice(RandomStringUtils.randomAlphanumeric(15), "Default");
        doPost("/api/edge/" + edge.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        DeviceUpdateMsg deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(deviceUpdateMsg.getIdMSB(), savedDevice.getUuidId().getMostSignificantBits());
        Assert.assertEquals(deviceUpdateMsg.getIdLSB(), savedDevice.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(deviceUpdateMsg.getName(), savedDevice.getName());
        Assert.assertEquals(deviceUpdateMsg.getType(), savedDevice.getType());
        return savedDevice;
    }

    private Device findDeviceByName(String deviceName) throws Exception {
        List<Device> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/devices?",
                new TypeReference<PageData<Device>>() {
                }, new PageLink(100)).getData();
        Optional<Device> foundDevice = edgeDevices.stream().filter(d -> d.getName().equals(deviceName)).findAny();
        Assert.assertTrue(foundDevice.isPresent());
        Device device = foundDevice.get();
        Assert.assertEquals(deviceName, device.getName());
        return device;
    }

    private Asset findAssetByName(String assetName) throws Exception {
        List<Asset> edgeAssets = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/assets?",
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

    private OtaPackageInfo saveData(String urlTemplate, MockMultipartFile content, String... params) throws Exception {
        MockMultipartHttpServletRequestBuilder postRequest = MockMvcRequestBuilders.multipart(urlTemplate, params);
        postRequest.file(content);
        setJwtToken(postRequest);
        return readResponse(mockMvc.perform(postRequest).andExpect(status().isOk()), OtaPackageInfo.class);
    }

}
