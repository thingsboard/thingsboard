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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveOtaPackageInfoRequest;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.MqttDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.edge.imitator.EdgeImitator;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;

@TestPropertySource(properties = {
        "edges.enabled=true",
})
abstract public class AbstractEdgeTest extends AbstractControllerTest {

    private static final String THERMOSTAT_DEVICE_PROFILE_NAME = "Thermostat";

    protected Tenant savedTenant;
    protected TenantId tenantId;
    protected User tenantAdmin;

    protected DeviceProfile thermostatDeviceProfile;

    protected EdgeImitator edgeImitator;
    protected Edge edge;

    @Autowired
    protected EdgeEventService edgeEventService;

    @Autowired
    protected DataDecodingEncodingService dataDecodingEncodingService;

    @Autowired
    protected TbClusterService clusterService;

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
        edgeImitator.expectMessageAmount(16);
        edgeImitator.connect();

        requestEdgeRuleChainMetadata();

        verifyEdgeConnectionAndInitialData();
    }

    private void requestEdgeRuleChainMetadata() throws Exception {
        RuleChainId rootRuleChainId = getEdgeRootRuleChainId();
        RuleChainMetadataRequestMsg.Builder builder = RuleChainMetadataRequestMsg.newBuilder()
                .setRuleChainIdMSB(rootRuleChainId.getId().getMostSignificantBits())
                .setRuleChainIdLSB(rootRuleChainId.getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(builder);
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder()
                .addRuleChainMetadataRequestMsg(builder.build());
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
    }

    private RuleChainId getEdgeRootRuleChainId() throws Exception {
        List<RuleChain> edgeRuleChains = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/ruleChains?",
                new TypeReference<PageData<RuleChain>>() {}, new PageLink(100)).getData();
        for (RuleChain edgeRuleChain : edgeRuleChains) {
            if (edgeRuleChain.isRoot()) {
                return edgeRuleChain.getId();
            }
        }
        throw new RuntimeException("Root rule chain not found");
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

        thermostatDeviceProfile = this.createDeviceProfile(THERMOSTAT_DEVICE_PROFILE_NAME,
                createMqttDeviceProfileTransportConfiguration(new JsonTransportPayloadConfiguration(), false));

        extendDeviceProfileData(thermostatDeviceProfile);
        thermostatDeviceProfile = doPost("/api/deviceProfile", thermostatDeviceProfile, DeviceProfile.class);

        Device savedDevice = saveDevice("Edge Device 1", THERMOSTAT_DEVICE_PROFILE_NAME);
        doPost("/api/edge/" + edge.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);

        Asset savedAsset = saveAsset("Edge Asset 1");
        doPost("/api/edge/" + edge.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);
    }

    protected void extendDeviceProfileData(DeviceProfile deviceProfile) {
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
        Device device = doGet("/api/device/" + deviceUUID, Device.class);
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
        DeviceProfile deviceProfile = doGet("/api/deviceProfile/" + deviceProfileUUID, DeviceProfile.class);
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
        RuleChain ruleChain = doGet("/api/ruleChain/" + ruleChainUUID, RuleChain.class);
        Assert.assertNotNull(ruleChain);
        List<RuleChain> edgeRuleChains = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/ruleChains?",
                new TypeReference<PageData<RuleChain>>() {}, new PageLink(100)).getData();
        Assert.assertTrue(edgeRuleChains.contains(ruleChain));

        testAutoGeneratedCodeByProtobuf(ruleChainUpdateMsg);

        Optional<RuleChainMetadataUpdateMsg> ruleChainMetadataUpdateOpt = edgeImitator.findMessageByType(RuleChainMetadataUpdateMsg.class);
        Assert.assertTrue(ruleChainMetadataUpdateOpt.isPresent());
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = ruleChainMetadataUpdateOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, ruleChainMetadataUpdateMsg.getMsgType());
        Assert.assertEquals(ruleChainUpdateMsg.getIdMSB(), ruleChainMetadataUpdateMsg.getRuleChainIdMSB());
        Assert.assertEquals(ruleChainUpdateMsg.getIdLSB(), ruleChainMetadataUpdateMsg.getRuleChainIdLSB());

        validateAdminSettings();

        Optional<AssetProfileUpdateMsg> assetProfileUpdateMsgOpt = edgeImitator.findMessageByType(AssetProfileUpdateMsg.class);
        Assert.assertTrue(assetProfileUpdateMsgOpt.isPresent());
        AssetProfileUpdateMsg assetProfileUpdateMsg = assetProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());
        UUID assetProfileUUID = new UUID(assetProfileUpdateMsg.getIdMSB(), assetProfileUpdateMsg.getIdLSB());
        AssetProfile assetProfile = doGet("/api/assetProfile/" + assetProfileUUID, AssetProfile.class);
        Assert.assertNotNull(assetProfile);
        Assert.assertEquals("default", assetProfile.getName());
        Assert.assertTrue(assetProfile.isDefault());

        testAutoGeneratedCodeByProtobuf(assetProfileUpdateMsg);
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

    protected Device saveDeviceOnCloudAndVerifyDeliveryToEdge() throws Exception {
        // create ota package
        edgeImitator.expectMessageAmount(1);
        OtaPackageInfo firmwareOtaPackageInfo = saveOtaPackageInfo(thermostatDeviceProfile.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        // create device and assign to edge
        Device savedDevice = saveDevice(StringUtils.randomAlphanumeric(15), thermostatDeviceProfile.getName());
        edgeImitator.expectMessageAmount(1);
        doPost("/api/edge/" + edge.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        DeviceUpdateMsg deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDevice.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getIdLSB());

        // update device
        edgeImitator.expectMessageAmount(1);
        savedDevice.setFirmwareId(firmwareOtaPackageInfo.getId());

        DeviceData deviceData = new DeviceData();
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        MqttDeviceTransportConfiguration transportConfiguration = new MqttDeviceTransportConfiguration();
        transportConfiguration.getProperties().put("topic", "tb_rule_engine.thermostat");
        deviceData.setTransportConfiguration(transportConfiguration);
        savedDevice.setDeviceData(deviceData);

        savedDevice = doPost("/api/device", savedDevice, Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDevice.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getIdLSB());
        Assert.assertEquals(savedDevice.getName(), deviceUpdateMsg.getName());
        Assert.assertEquals(savedDevice.getType(), deviceUpdateMsg.getType());
        Assert.assertEquals(firmwareOtaPackageInfo.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getFirmwareIdMSB());
        Assert.assertEquals(firmwareOtaPackageInfo.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getFirmwareIdLSB());
        Optional<DeviceData> deviceDataOpt =
                dataDecodingEncodingService.decode(deviceUpdateMsg.getDeviceDataBytes().toByteArray());
        Assert.assertTrue(deviceDataOpt.isPresent());
        deviceData = deviceDataOpt.get();
        Assert.assertTrue(deviceData.getTransportConfiguration() instanceof MqttDeviceTransportConfiguration);
        MqttDeviceTransportConfiguration mqttDeviceTransportConfiguration =
                (MqttDeviceTransportConfiguration) deviceData.getTransportConfiguration();
        Assert.assertEquals("tb_rule_engine.thermostat", mqttDeviceTransportConfiguration.getProperties().get("topic"));
        return savedDevice;
    }

    protected Device findDeviceByName(String deviceName) throws Exception {
        List<Device> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/devices?",
                new TypeReference<PageData<Device>>() {
                }, new PageLink(100)).getData();
        Optional<Device> foundDevice = edgeDevices.stream().filter(d -> d.getName().equals(deviceName)).findAny();
        Assert.assertTrue(foundDevice.isPresent());
        Device device = foundDevice.get();
        Assert.assertEquals(deviceName, device.getName());
        return device;
    }

    protected Asset findAssetByName(String assetName) throws Exception {
        List<Asset> edgeAssets = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/assets?",
                new TypeReference<PageData<Asset>>() {
                }, new PageLink(100)).getData();

        Assert.assertEquals(1, edgeAssets.size());
        Asset asset = edgeAssets.get(0);
        Assert.assertEquals(assetName, asset.getName());
        return asset;
    }

    protected Device saveDevice(String deviceName, String type) {
        Device device = new Device();
        device.setName(deviceName);
        device.setType(type);
        return doPost("/api/device", device, Device.class);
    }

    protected Asset saveAsset(String assetName) {
        Asset asset = new Asset();
        asset.setName(assetName);
        return doPost("/api/asset", asset, Asset.class);
    }

    protected OtaPackageInfo saveOtaPackageInfo(DeviceProfileId deviceProfileId) {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle("Firmware Edge " + StringUtils.randomAlphanumeric(3));
        firmwareInfo.setVersion("v1.0");
        firmwareInfo.setTag("My firmware #1 v1.0");
        firmwareInfo.setUsesUrl(true);
        firmwareInfo.setUrl("http://localhost:8080/v1/package");
        firmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());
        firmwareInfo.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        return doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);
    }

    protected EdgeEvent constructEdgeEvent(TenantId tenantId, EdgeId edgeId, EdgeEventActionType edgeEventAction,
                                           UUID entityId, EdgeEventType edgeEventType, JsonNode entityBody) {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setAction(edgeEventAction);
        edgeEvent.setEntityId(entityId);
        edgeEvent.setType(edgeEventType);
        edgeEvent.setBody(entityBody);
        return edgeEvent;
    }

    protected void testAutoGeneratedCodeByProtobuf(MessageLite.Builder builder) throws InvalidProtocolBufferException {
        MessageLite source = builder.build();

        testAutoGeneratedCodeByProtobuf(source);

        MessageLite target = source.getParserForType().parseFrom(source.toByteArray());
        builder.clear().mergeFrom(target);
    }

    protected void testAutoGeneratedCodeByProtobuf(MessageLite source) throws InvalidProtocolBufferException {
        MessageLite target = source.getParserForType().parseFrom(source.toByteArray());
        Assert.assertEquals(source, target);
        Assert.assertEquals(source.hashCode(), target.hashCode());
    }



}
