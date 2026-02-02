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
package org.thingsboard.server.edge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.TestSocketUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveOtaPackageInfoRequest;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.debug.DebugSettings;
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
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.edge.imitator.EdgeImitator;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.OAuth2ClientUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OAuth2DomainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.QueueUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.SyncCompletedMsg;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "edges.enabled=true",
        "queue.rule-engine.stats.enabled=false",
        "edges.storage.sleep_between_batches=1000"
})
@Slf4j
abstract public class AbstractEdgeTest extends AbstractControllerTest {

    public static final String EDGE_HOST = "localhost";
    public static final int EDGE_PORT = TestSocketUtils.findAvailableTcpPort();
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        log.debug("edges.rpc.port = {}", EDGE_PORT);
        registry.add("edges.rpc.port", () -> EDGE_PORT);
    }

    public static final Integer CONNECT_MESSAGE_COUNT = 17;
    public static final Integer INSTALLATION_MESSAGE_COUNT = 8;
    public static final Integer SYNC_MESSAGE_COUNT = CONNECT_MESSAGE_COUNT + INSTALLATION_MESSAGE_COUNT;
    private static final String THERMOSTAT_DEVICE_PROFILE_NAME = "Thermostat";

    protected DeviceProfile thermostatDeviceProfile;

    protected EdgeImitator edgeImitator;
    protected Edge edge;

    @Autowired
    protected EdgeEventService edgeEventService;

    @Before
    public void setupEdgeTest() throws Exception {
        loginSysAdmin();

        // get jwt settings from yaml config
        JwtSettings settings = doGet("/api/admin/jwtSettings", JwtSettings.class);
        // save jwt settings into db
        doPost("/api/admin/jwtSettings", settings).andExpect(status().isOk());

        loginTenantAdmin();
        //8 installation messages
        installation();

        edgeImitator = new EdgeImitator(EDGE_HOST, EDGE_PORT, edge.getRoutingKey(), edge.getSecret());
        // 17 connect messages + 8 installation messages
        edgeImitator.expectMessageAmount(SYNC_MESSAGE_COUNT);
        edgeImitator.ignoreType(OAuth2ClientUpdateMsg.class);
        edgeImitator.ignoreType(OAuth2DomainUpdateMsg.class);
        edgeImitator.connect();

        verifyEdgeConnectionAndInitialData();
    }

    @After
    public void teardownEdgeTest() {
        try {
            loginTenantAdmin();

            doDelete("/api/edge/" + edge.getId().toString())
                    .andExpect(status().isOk());
            edgeImitator.disconnect();
        } catch (Exception ignored) {
        }
    }

    private void installation() throws Exception {
        thermostatDeviceProfile = this.createDeviceProfile(THERMOSTAT_DEVICE_PROFILE_NAME,
                createMqttDeviceProfileTransportConfiguration(new JsonTransportPayloadConfiguration(), false));
        extendDeviceProfileData(thermostatDeviceProfile);
        //2 messages DeviceProfile
        thermostatDeviceProfile = doPost("/api/deviceProfile", thermostatDeviceProfile, DeviceProfile.class);

        Device savedDevice = saveDevice("Edge Device 1", THERMOSTAT_DEVICE_PROFILE_NAME);

        // create public customer
        //1 message
        // Customer
        doPost("/api/customer/public/device/" + savedDevice.getId().getId(), Device.class);
        doDelete("/api/customer/device/" + savedDevice.getId().getId(), Device.class);


        Asset savedAsset = saveAsset("Edge Asset 1");
        updateRootRuleChainMetadata();

        edge = doPost("/api/edge", constructEdge("Test Edge", "test"), Edge.class);

        //3 messages
        // Device
        // DeviceProfile
        // DeviceCredentials
        doPost("/api/edge/" + edge.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);
        //2 messages
        // Asset
        // AssetProfile
        doPost("/api/edge/" + edge.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);

        // wait until assign device and asset events are fully processed by edge notification service
        TimeUnit.MILLISECONDS.sleep(1000);
    }

    protected void updateRootRuleChainMetadata() throws Exception {
        RuleChainId rootRuleChainId = getEdgeRootRuleChainId();
        RuleChainMetaData rootRuleChainMetadata = doGet("/api/ruleChain/" + rootRuleChainId.getId().toString() + "/metadata", RuleChainMetaData.class);
        rootRuleChainMetadata.getNodes().forEach(n -> n.setDebugSettings(DebugSettings.all()));
        doPost("/api/ruleChain/metadata", rootRuleChainMetadata, RuleChainMetaData.class);
    }

    private RuleChainId getEdgeRootRuleChainId() throws Exception {
        List<RuleChain> edgeRuleChains = doGetTypedWithPageLink("/api/ruleChains?type={type}&",
                new TypeReference<PageData<RuleChain>>() {},
                new PageLink(100, 0, "Edge Root Rule Chain"),
                "EDGE").getData();
        for (RuleChain edgeRuleChain : edgeRuleChains) {
            if (edgeRuleChain.isRoot()) {
                return edgeRuleChain.getId();
            }
        }
        throw new RuntimeException("Root rule chain not found");
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

        validateEdgeConfiguration();

        // 1 message from queue fetcher
        validateMsgsCnt(QueueUpdateMsg.class, 1);
        validateQueues();

        // 1 from rule chain fetcher
        validateMsgsCnt(RuleChainUpdateMsg.class, 1);
        UUID ruleChainUUID = validateRuleChains();

        // 1 from rule chain fetcher
        validateMsgsCnt(RuleChainMetadataUpdateMsg.class, 1);
        validateRuleChainMetadataUpdates(ruleChainUUID);

        // 4 messages ('general', 'mail', 'connectivity', 'jwt')
        validateMsgsCnt(AdminSettingsUpdateMsg.class, 4);
        validateAdminSettings(4);

        // 4 messages
        // - 1 from default profile fetcher
        // - 4 from device profile fetcher (2 * (default and thermostat) before and after ota packages fetcher
        // - 1 from device fetcher
        validateMsgsCnt(DeviceProfileUpdateMsg.class, 6);
        validateDeviceProfiles(6);

        // 3 messages
        // - 1 from default profile fetcher
        // - 1 message from asset profile fetcher
        // - 1 message from asset fetcher
        validateMsgsCnt(AssetProfileUpdateMsg.class, 3);
        validateAssetProfiles(3);

        // 1 from device fetcher
        validateMsgsCnt(DeviceUpdateMsg.class, 1);
        validateDevices();

        validateMsgsCnt(DeviceCredentialsUpdateMsg.class, 1);

        // 1 from asset fetcher
        validateMsgsCnt(AssetUpdateMsg.class, 1);
        validateAssets();

        // 1 message from public customer fetcher
        validateMsgsCnt(CustomerUpdateMsg.class, 1);
        validatePublicCustomer();

        // 1 message from user fetcher
        validateMsgsCnt(UserUpdateMsg.class, 1);
        validateUsers();

        validateMsgsCnt(UserCredentialsUpdateMsg.class, 1);

        // 1 from tenant fetcher
        validateMsgsCnt(TenantUpdateMsg.class, 1);
        validateTenant();

        // 1 from tenant profile fetcher
        validateMsgsCnt(TenantProfileUpdateMsg.class, 1);
        validateTenantProfile();

        // 1 message sync completed
        validateMsgsCnt(SyncCompletedMsg.class, 1);
        validateSyncCompleted();
    }

    private <T extends AbstractMessage> void validateMsgsCnt(Class<T> clazz, int expectedMsgCnt) {
        List<T> downlinkMsgsByType = edgeImitator.findAllMessagesByType(clazz);
        if (downlinkMsgsByType.size() != expectedMsgCnt) {
            List<AbstractMessage> downlinkMsgs = edgeImitator.getDownlinkMsgs();
            for (AbstractMessage downlinkMsg : downlinkMsgs) {
                log.error("{}\n{}", downlinkMsg.getClass(), downlinkMsg);
            }
            Assert.fail("Unexpected message count for " + clazz + "! Expected: " + expectedMsgCnt + ", but found: " + downlinkMsgsByType.size());
        }
    }

    private void validateEdgeConfiguration() throws Exception {
        EdgeConfiguration configuration = edgeImitator.getConfiguration();
        Assert.assertNotNull(configuration);
        testAutoGeneratedCodeByProtobuf(configuration);
    }

    private void validateTenant() throws Exception {
        Optional<TenantUpdateMsg> tenantUpdateMsgOpt = edgeImitator.findMessageByType(TenantUpdateMsg.class);
        Assert.assertTrue(tenantUpdateMsgOpt.isPresent());
        TenantUpdateMsg tenantUpdateMsg = tenantUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, tenantUpdateMsg.getMsgType());
        Tenant tenantMsg = JacksonUtil.fromString(tenantUpdateMsg.getEntity(), Tenant.class, true);
        Assert.assertNotNull(tenantMsg);
        Tenant tenant = doGet("/api/tenant/" + tenantMsg.getUuidId(), Tenant.class);
        Assert.assertNotNull(tenant);
        testAutoGeneratedCodeByProtobuf(tenantUpdateMsg);
    }

    private void validateTenantProfile() throws Exception {
        Optional<TenantProfileUpdateMsg> tenantProfileUpdateMsgOpt = edgeImitator.findMessageByType(TenantProfileUpdateMsg.class);
        Assert.assertTrue(tenantProfileUpdateMsgOpt.isPresent());
        TenantProfileUpdateMsg tenantProfileUpdateMsg = tenantProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, tenantProfileUpdateMsg.getMsgType());
        TenantProfile tenantProfile = JacksonUtil.fromString(tenantProfileUpdateMsg.getEntity(), TenantProfile.class, true);
        Assert.assertNotNull(tenantProfile);
        Tenant tenant = doGet("/api/tenant/" + tenantId.getId(), Tenant.class);
        Assert.assertNotNull(tenant);
        Assert.assertEquals(tenantProfile.getId(), tenant.getTenantProfileId());
        testAutoGeneratedCodeByProtobuf(tenantProfileUpdateMsg);
    }

    private void validateDeviceProfiles(int expectedMsgCnt) throws Exception {
        List<DeviceProfileUpdateMsg> deviceProfileUpdateMsgList = edgeImitator.findAllMessagesByType(DeviceProfileUpdateMsg.class);
        // default msg default device profile from fetcher
        // default msg device profile from fetcher
        // thermostat msg from device profile fetcher
        // thermostat msg from device fetcher
        Assert.assertEquals(expectedMsgCnt, deviceProfileUpdateMsgList.size());
        Optional<DeviceProfileUpdateMsg> thermostatProfileUpdateMsgOpt =
                deviceProfileUpdateMsgList.stream().filter(dfum -> {
                    DeviceProfile deviceProfile = JacksonUtil.fromString(dfum.getEntity(), DeviceProfile.class, true);
                    Assert.assertNotNull(deviceProfile);
                    return THERMOSTAT_DEVICE_PROFILE_NAME.equals(deviceProfile.getName());
                }).findAny();
        Assert.assertTrue(thermostatProfileUpdateMsgOpt.isPresent());
        DeviceProfileUpdateMsg thermostatProfileUpdateMsg = thermostatProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, thermostatProfileUpdateMsg.getMsgType());
        UUID deviceProfileUUID = new UUID(thermostatProfileUpdateMsg.getIdMSB(), thermostatProfileUpdateMsg.getIdLSB());
        DeviceProfile deviceProfile = doGet("/api/deviceProfile/" + deviceProfileUUID, DeviceProfile.class);
        Assert.assertNotNull(deviceProfile);
        Assert.assertNotNull(deviceProfile.getProfileData());
        Assert.assertNotNull(deviceProfile.getProfileData().getAlarms());
        Assert.assertNotNull(deviceProfile.getProfileData().getAlarms().get(0).getClearRule());

        testAutoGeneratedCodeByProtobuf(thermostatProfileUpdateMsg);
    }

    private void validateDevices() throws Exception {
        Optional<DeviceUpdateMsg> deviceUpdateMsgOpt = edgeImitator.findMessageByType(DeviceUpdateMsg.class);
        Assert.assertTrue(deviceUpdateMsgOpt.isPresent());
        validateDevice(deviceUpdateMsgOpt.get());
    }

    private void validateDevice(DeviceUpdateMsg deviceUpdateMsg) throws Exception {
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        UUID deviceUUID = new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB());
        Device device = doGet("/api/device/" + deviceUUID, Device.class);
        Assert.assertNotNull(device);
        List<DeviceInfo> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/devices?",
                new TypeReference<PageData<DeviceInfo>>() {
                }, new PageLink(100)).getData();
        Assert.assertTrue(edgeDevices.stream().map(DeviceInfo::getId).anyMatch(id -> id.equals(device.getId())));

        testAutoGeneratedCodeByProtobuf(deviceUpdateMsg);
    }

    private void validateAssets() throws Exception {
        Optional<AssetUpdateMsg> assetUpdateMsgOpt = edgeImitator.findMessageByType(AssetUpdateMsg.class);
        Assert.assertTrue(assetUpdateMsgOpt.isPresent());
        validateAsset(assetUpdateMsgOpt.get());
    }

    private void validateAsset(AssetUpdateMsg assetUpdateMsg) throws Exception {
        Assert.assertNotNull(assetUpdateMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        UUID assetUUID = new UUID(assetUpdateMsg.getIdMSB(), assetUpdateMsg.getIdLSB());
        Asset asset = doGet("/api/asset/" + assetUUID, Asset.class);
        Assert.assertNotNull(asset);
        List<Asset> edgeAssets = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/assets?",
                new TypeReference<PageData<Asset>>() {
                }, new PageLink(100)).getData();
        Assert.assertTrue(edgeAssets.contains(asset));

        testAutoGeneratedCodeByProtobuf(assetUpdateMsg);
    }

    private UUID validateRuleChains() throws Exception {
        Optional<RuleChainUpdateMsg> ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        RuleChainUpdateMsg ruleChainUpdateMsg = ruleChainUpdateMsgOpt.get();
        validateRuleChain(ruleChainUpdateMsg, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        return new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB());
    }

    private void validateRuleChain(RuleChainUpdateMsg ruleChainUpdateMsg, UpdateMsgType expectedMsgType) throws Exception {
        Assert.assertEquals(expectedMsgType, ruleChainUpdateMsg.getMsgType());
        UUID ruleChainUUID = new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB());
        RuleChain ruleChain = doGet("/api/ruleChain/" + ruleChainUUID, RuleChain.class);
        Assert.assertNotNull(ruleChain);
        List<RuleChain> edgeRuleChains = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/ruleChains?",
                new TypeReference<PageData<RuleChain>>() {
                }, new PageLink(100)).getData();
        Assert.assertTrue(edgeRuleChains.contains(ruleChain));
        testAutoGeneratedCodeByProtobuf(ruleChainUpdateMsg);
    }

    private void validateRuleChainMetadataUpdates(UUID expectedRuleChainUUID) {
        Optional<RuleChainMetadataUpdateMsg> ruleChainMetadataUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainMetadataUpdateMsg.class);
        Assert.assertTrue(ruleChainMetadataUpdateMsgOpt.isPresent());
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = ruleChainMetadataUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, ruleChainMetadataUpdateMsg.getMsgType());
        RuleChainMetaData ruleChainMetaData = JacksonUtil.fromString(ruleChainMetadataUpdateMsg.getEntity(), RuleChainMetaData.class, true);
        Assert.assertEquals(expectedRuleChainUUID, ruleChainMetaData.getRuleChainId().getId());
    }

    private void validateAdminSettings(int expectedMsgCnt) {
        List<AdminSettingsUpdateMsg> adminSettingsUpdateMsgs = edgeImitator.findAllMessagesByType(AdminSettingsUpdateMsg.class);
        Assert.assertEquals(expectedMsgCnt, adminSettingsUpdateMsgs.size());

        for (AdminSettingsUpdateMsg adminSettingsUpdateMsg : adminSettingsUpdateMsgs) {
            AdminSettings adminSettings = JacksonUtil.fromString(adminSettingsUpdateMsg.getEntity(), AdminSettings.class, true);
            Assert.assertNotNull(adminSettings);
            if (adminSettings.getKey().equals("general")) {
                validateGeneralAdminSettings(adminSettings);
            }
            if (adminSettings.getKey().equals("mail")) {
                validateMailAdminSettings(adminSettings);
            }
            if (adminSettings.getKey().equals("connectivity")) {
                validateConnectivityAdminSettings(adminSettings);
            }
            if (adminSettings.getKey().equals("jwt")) {
                validateJwtAdminSettings(adminSettings);
            }
        }
    }

    private void validateGeneralAdminSettings(AdminSettings adminSettings) {
        Assert.assertNotNull(adminSettings.getJsonValue().get("baseUrl"));
    }

    private void validateMailAdminSettings(AdminSettings adminSettings) {
        JsonNode jsonNode = adminSettings.getJsonValue();
        Assert.assertNotNull(jsonNode.get("mailFrom"));
        Assert.assertNotNull(jsonNode.get("smtpProtocol"));
        Assert.assertNotNull(jsonNode.get("smtpHost"));
        Assert.assertNotNull(jsonNode.get("smtpPort"));
        Assert.assertNotNull(jsonNode.get("timeout"));
    }

    private void validateConnectivityAdminSettings(AdminSettings adminSettings) {
        JsonNode jsonNode = adminSettings.getJsonValue();
        Assert.assertNotNull(jsonNode.get("http"));
        Assert.assertNotNull(jsonNode.get("https"));
        Assert.assertNotNull(jsonNode.get("mqtt"));
        Assert.assertNotNull(jsonNode.get("mqtts"));
        Assert.assertNotNull(jsonNode.get("coap"));
        Assert.assertNotNull(jsonNode.get("coaps"));
    }

    private void validateJwtAdminSettings(AdminSettings adminSettings) {
        JsonNode jsonNode = adminSettings.getJsonValue();
        Assert.assertNotNull(jsonNode.get("tokenExpirationTime"));
        Assert.assertNotNull(jsonNode.get("refreshTokenExpTime"));
        Assert.assertNotNull(jsonNode.get("tokenIssuer"));
        Assert.assertNotNull(jsonNode.get("tokenSigningKey"));
    }

    private void validateAssetProfiles(int expectedMsgCnt) throws Exception {
        List<AssetProfileUpdateMsg> assetProfileUpdateMsgs = edgeImitator.findAllMessagesByType(AssetProfileUpdateMsg.class);
        Assert.assertEquals(expectedMsgCnt, assetProfileUpdateMsgs.size());
        AssetProfileUpdateMsg assetProfileUpdateMsg = assetProfileUpdateMsgs.get(0);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());
        UUID assetProfileUUID = new UUID(assetProfileUpdateMsg.getIdMSB(), assetProfileUpdateMsg.getIdLSB());
        AssetProfile assetProfile = doGet("/api/assetProfile/" + assetProfileUUID, AssetProfile.class);
        Assert.assertNotNull(assetProfile);
        Assert.assertEquals("default", assetProfile.getName());
        Assert.assertTrue(assetProfile.isDefault());
        testAutoGeneratedCodeByProtobuf(assetProfileUpdateMsg);
    }

    private void validateQueues() throws Exception {
        Optional<QueueUpdateMsg> queueUpdateMsgOpt = edgeImitator.findMessageByType(QueueUpdateMsg.class);
        Assert.assertTrue(queueUpdateMsgOpt.isPresent());
        QueueUpdateMsg queueUpdateMsg = queueUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, queueUpdateMsg.getMsgType());
        UUID queueUUID = new UUID(queueUpdateMsg.getIdMSB(), queueUpdateMsg.getIdLSB());
        Queue queue = doGet("/api/queues/" + queueUUID, Queue.class);
        Assert.assertNotNull(queue);
        Assert.assertEquals(DataConstants.MAIN_QUEUE_NAME, queue.getName());
        Assert.assertEquals(DataConstants.MAIN_QUEUE_TOPIC, queue.getTopic());
        Assert.assertEquals(10, queue.getPartitions());
        Assert.assertEquals(25, queue.getPollInterval());
        testAutoGeneratedCodeByProtobuf(queueUpdateMsg);
    }

    private void validateUsers() throws Exception {
        Optional<UserUpdateMsg> userUpdateMsgOpt = edgeImitator.findMessageByType(UserUpdateMsg.class);
        Assert.assertTrue(userUpdateMsgOpt.isPresent());
        UserUpdateMsg userUpdateMsg = userUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        UUID userUUID = new UUID(userUpdateMsg.getIdMSB(), userUpdateMsg.getIdLSB());
        User user = doGet("/api/user/" + userUUID, User.class);
        Assert.assertNotNull(user);
        Assert.assertEquals("testtenant@thingsboard.org", user.getEmail());
        testAutoGeneratedCodeByProtobuf(userUpdateMsg);
    }

    private void validatePublicCustomer() throws Exception {
        Optional<CustomerUpdateMsg> customerUpdateMsgOpt = edgeImitator.findMessageByType(CustomerUpdateMsg.class);
        Assert.assertTrue(customerUpdateMsgOpt.isPresent());
        CustomerUpdateMsg customerUpdateMsg = customerUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, customerUpdateMsg.getMsgType());
        UUID customerUUID = new UUID(customerUpdateMsg.getIdMSB(), customerUpdateMsg.getIdLSB());
        Customer customer = doGet("/api/customer/" + customerUUID, Customer.class);
        Assert.assertNotNull(customer);
        Assert.assertTrue(customer.isPublic());
    }

    private void validateSyncCompleted() {
        Optional<SyncCompletedMsg> syncCompletedMsgOpt = edgeImitator.findMessageByType(SyncCompletedMsg.class);
        Assert.assertTrue(syncCompletedMsgOpt.isPresent());
    }

    protected Device saveDeviceOnCloudAndVerifyDeliveryToEdge() throws Exception {
        // create device and assign to edge
        Device savedDevice = saveDevice(StringUtils.randomAlphanumeric(15), thermostatDeviceProfile.getName());
        DeviceCredentials deviceCredentials = doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        edgeImitator.expectMessageAmount(3); // device and device profile messages and device credentials
        doPost("/api/edge/" + edge.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<DeviceUpdateMsg> deviceUpdateMsgOpt = edgeImitator.findMessageByType(DeviceUpdateMsg.class);
        Assert.assertTrue(deviceUpdateMsgOpt.isPresent());
        DeviceUpdateMsg deviceUpdateMsg = deviceUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDevice.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getIdLSB());

        Optional<DeviceProfileUpdateMsg> deviceProfileUpdateMsgOpt = edgeImitator.findMessageByType(DeviceProfileUpdateMsg.class);
        Assert.assertTrue(deviceProfileUpdateMsgOpt.isPresent());
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = deviceProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(thermostatDeviceProfile.getUuidId().getMostSignificantBits(), deviceProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(thermostatDeviceProfile.getUuidId().getLeastSignificantBits(), deviceProfileUpdateMsg.getIdLSB());

        Optional<DeviceCredentialsUpdateMsg> deviceCredentialsUpdateMsgOpt = edgeImitator.findMessageByType(DeviceCredentialsUpdateMsg.class);
        Assert.assertTrue(deviceCredentialsUpdateMsgOpt.isPresent());
        DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg = deviceCredentialsUpdateMsgOpt.get();
        DeviceCredentials deviceCredentialsMsg = JacksonUtil.fromString(deviceCredentialsUpdateMsg.getEntity(), DeviceCredentials.class, true);
        Assert.assertNotNull(deviceCredentialsMsg);
        Assert.assertEquals(savedDevice.getId(), deviceCredentialsMsg.getDeviceId());
        Assert.assertEquals(deviceCredentials, deviceCredentialsMsg);

        return savedDevice;
    }

    protected Device findDeviceByName(String deviceName) throws Exception {
        List<DeviceInfo> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/devices?",
                new TypeReference<PageData<DeviceInfo>>() {
                }, new PageLink(100)).getData();
        Optional<DeviceInfo> foundDevice = edgeDevices.stream().filter(d -> d.getName().equals(deviceName)).findAny();
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

    protected OtaPackageInfo saveOtaPackageInfo(DeviceProfileId deviceProfileId, OtaPackageType type) {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(type);
        firmwareInfo.setTitle(type.name() + " Edge " + StringUtils.randomAlphanumeric(3));
        firmwareInfo.setVersion("v1.0");
        firmwareInfo.setTag("My " + type.name() + " #1 v1.0");
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

    protected RuleChainId createEdgeRuleChainAndAssignToEdge(String ruleChainName) throws Exception {
        edgeImitator.expectMessageAmount(2);
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(ruleChainName);
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + savedRuleChain.getUuidId(), RuleChain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        return savedRuleChain.getId();
    }

    protected void unAssignFromEdgeAndDeleteRuleChain(RuleChainId ruleChainId) throws Exception {
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + ruleChainId.getId(), RuleChain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // delete rule chain
        doDelete("/api/ruleChain/" + ruleChainId.getId())
                .andExpect(status().isOk());
    }

    protected DashboardId createDashboardAndAssignToEdge(String dashboardName) throws Exception {
        edgeImitator.expectMessageAmount(1);
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(dashboardName);
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        return savedDashboard.getId();
    }

    protected void unAssignFromEdgeAndDeleteDashboard(DashboardId dashboardId) throws Exception {
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/dashboard/" + dashboardId.getId(), RuleChain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // delete dashboard
        doDelete("/api/dashboard/" + dashboardId.getId())
                .andExpect(status().isOk());
    }


    protected ObjectNode createDefaultRpc() {
        return createDefaultRpc(1);
    }

    protected ObjectNode createDefaultRpc(Integer value) {
        ObjectNode rpc = JacksonUtil.newObjectNode();
        rpc.put("method", "setGpio");

        ObjectNode params = JacksonUtil.newObjectNode();

        params.put("pin", 7);
        params.put("value", value);

        rpc.set("params", params);
        rpc.put("persistent", true);
        rpc.put("timeout", 5000);

        return rpc;
    }

}
