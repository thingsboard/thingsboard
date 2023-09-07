/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor;

import org.junit.jupiter.params.provider.Arguments;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.edge.EdgeSynchronizationManager;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.service.edge.rpc.constructor.AdminSettingsMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.AlarmMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.AssetMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.AssetProfileMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.CustomerMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DashboardMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DeviceMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DeviceProfileMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EdgeMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityDataMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityViewMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.OtaPackageMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.QueueMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RelationMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RuleChainMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.TenantMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.TenantProfileMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.UserMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.WidgetTypeMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.WidgetsBundleMsgConstructor;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.UUID;
import java.util.stream.Stream;

public abstract class BaseEdgeProcessorTest {

    @MockBean
    protected TelemetrySubscriptionService tsSubService;

    @MockBean
    protected TbNotificationEntityService notificationEntityService;

    @MockBean
    protected RuleChainService ruleChainService;

    @MockBean
    protected AlarmService alarmService;

    @MockBean
    protected DeviceService deviceService;

    @MockBean
    protected TbDeviceProfileCache deviceProfileCache;

    @MockBean
    protected TbAssetProfileCache assetProfileCache;

    @MockBean
    protected DashboardService dashboardService;

    @MockBean
    protected AssetService assetService;

    @MockBean
    protected EntityViewService entityViewService;

    @MockBean
    protected TenantService tenantService;

    @MockBean
    protected TenantProfileService tenantProfileService;

    @MockBean
    protected EdgeService edgeService;

    @MockBean
    protected CustomerService customerService;

    @MockBean
    protected UserService userService;

    @MockBean
    protected DeviceProfileService deviceProfileService;

    @MockBean
    protected AssetProfileService assetProfileService;

    @MockBean
    protected RelationService relationService;

    @MockBean
    protected DeviceCredentialsService deviceCredentialsService;

    @MockBean
    protected AttributesService attributesService;

    @MockBean
    protected TbClusterService tbClusterService;

    @MockBean
    protected DeviceStateService deviceStateService;

    @MockBean
    protected EdgeEventService edgeEventService;

    @MockBean
    protected WidgetsBundleService widgetsBundleService;

    @MockBean
    protected WidgetTypeService widgetTypeService;

    @MockBean
    protected OtaPackageService otaPackageService;

    @MockBean
    protected QueueService queueService;

    @MockBean
    protected PartitionService partitionService;

    @MockBean
    @Lazy
    protected TbQueueProducerProvider producerProvider;

    @MockBean
    protected DataValidator<Device> deviceValidator;

    @MockBean
    protected DataValidator<DeviceProfile> deviceProfileValidator;

    @MockBean
    protected DataValidator<Asset> assetValidator;

    @MockBean
    protected DataValidator<AssetProfile> assetProfileValidator;

    @MockBean
    protected DataValidator<Dashboard> dashboardValidator;

    @MockBean
    protected DataValidator<EntityView> entityViewValidator;

    @MockBean
    protected EdgeMsgConstructor edgeMsgConstructor;

    @MockBean
    protected EntityDataMsgConstructor entityDataMsgConstructor;

    @MockBean
    protected RuleChainMsgConstructor ruleChainMsgConstructor;

    @MockBean
    protected AlarmMsgConstructor alarmMsgConstructor;

    @SpyBean
    protected DeviceMsgConstructor deviceMsgConstructor;

    @SpyBean
    protected AssetMsgConstructor assetMsgConstructor;

    @MockBean
    protected EntityViewMsgConstructor entityViewMsgConstructor;

    @MockBean
    protected DashboardMsgConstructor dashboardMsgConstructor;

    @MockBean
    protected RelationMsgConstructor relationMsgConstructor;

    @MockBean
    protected UserMsgConstructor userMsgConstructor;

    @MockBean
    protected CustomerMsgConstructor customerMsgConstructor;

    @SpyBean
    protected DeviceProfileMsgConstructor deviceProfileMsgConstructor;

    @SpyBean
    protected AssetProfileMsgConstructor assetProfileMsgConstructor;

    @MockBean
    protected TenantMsgConstructor tenantMsgConstructor;

    @MockBean
    protected TenantProfileMsgConstructor tenantProfileMsgConstructor;

    @MockBean
    protected WidgetsBundleMsgConstructor widgetsBundleMsgConstructor;

    @MockBean
    protected WidgetTypeMsgConstructor widgetTypeMsgConstructor;

    @MockBean
    protected AdminSettingsMsgConstructor adminSettingsMsgConstructor;

    @MockBean
    protected OtaPackageMsgConstructor otaPackageMsgConstructor;

    @MockBean
    protected QueueMsgConstructor queueMsgConstructor;

    @MockBean
    protected EdgeSynchronizationManager edgeSynchronizationManager;

    @MockBean
    protected DbCallbackExecutorService dbCallbackExecutorService;
    
    @MockBean
    protected DataDecodingEncodingService dataDecodingEncodingService;

    protected EdgeId edgeId;
    protected TenantId tenantId;
    protected EdgeEvent edgeEvent;

    protected DashboardId getDashboardId(long expectedDashboardIdMSB, long expectedDashboardIdLSB) {
        DashboardId dashboardId;
        if (expectedDashboardIdMSB != 0 && expectedDashboardIdLSB != 0) {
            dashboardId = new DashboardId(new UUID(expectedDashboardIdMSB, expectedDashboardIdLSB));
        } else {
            dashboardId = new DashboardId(UUID.randomUUID());
        }
        return dashboardId;
    }

    protected RuleChainId getRuleChainId(long expectedRuleChainIdMSB, long expectedRuleChainIdLSB) {
        RuleChainId ruleChainId;
        if (expectedRuleChainIdMSB != 0 && expectedRuleChainIdLSB != 0) {
            ruleChainId = new RuleChainId(new UUID(expectedRuleChainIdMSB, expectedRuleChainIdLSB));
        } else {
            ruleChainId = new RuleChainId(UUID.randomUUID());
        }
        return ruleChainId;
    }

    protected static Stream<Arguments> provideParameters() {
        UUID dashoboardUUID = UUID.randomUUID();
        UUID ruleChaindUUID = UUID.randomUUID();
        return Stream.of(
                Arguments.of(EdgeVersion.V_3_3_0, 0, 0, 0, 0),
                Arguments.of(EdgeVersion.V_3_3_3, 0, 0, 0, 0),
                Arguments.of(EdgeVersion.V_3_4_0, 0, 0, 0, 0),
                Arguments.of(EdgeVersion.V_3_6_0,
                        dashoboardUUID.getMostSignificantBits(),
                        dashoboardUUID.getLeastSignificantBits(),
                        ruleChaindUUID.getMostSignificantBits(),
                        ruleChaindUUID.getLeastSignificantBits())
        );
    }
}
