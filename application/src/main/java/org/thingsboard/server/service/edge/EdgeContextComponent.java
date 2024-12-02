/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.edge;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.EdgeEventStorageSettings;
import org.thingsboard.server.service.edge.rpc.EdgeRpcService;
import org.thingsboard.server.service.edge.rpc.constructor.asset.AssetMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.device.DeviceMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.edge.EdgeMsgConstructor;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.asset.profile.AssetProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.profile.AssetProfileEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.customer.CustomerEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.DashboardEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.DashboardEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.device.profile.DeviceProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.profile.DeviceProfileEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.edge.EdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.notification.NotificationEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.oauth2.OAuth2EdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.ota.OtaPackageEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.queue.QueueEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.relation.RelationEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.relation.RelationEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.resource.ResourceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.resource.ResourceEdgeProcessorFactory;
import org.thingsboard.server.service.edge.rpc.processor.rule.RuleChainEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.settings.AdminSettingsEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.telemetry.TelemetryEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.tenant.TenantEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.tenant.TenantProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.user.UserEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.widget.WidgetBundleEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.widget.WidgetTypeEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.sync.EdgeRequestsService;
import org.thingsboard.server.service.executors.GrpcCallbackExecutorService;

@Lazy
@Data
@Component
@TbCoreComponent
public class EdgeContextComponent {

    // services
    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private AlarmCommentService alarmCommentService;

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private AssetProfileService assetProfileService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DomainService domainService;

    @Autowired
    private EdgeEventService edgeEventService;

    @Autowired
    private EdgeRequestsService edgeRequestsService;

    @Autowired(required = false)
    private EdgeRpcService edgeRpcService;

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private NotificationRuleService notificationRuleService;

    @Autowired
    private NotificationTargetService notificationTargetService;

    @Autowired
    private NotificationTemplateService notificationTemplateService;

    @Autowired
    private OAuth2ClientService oAuth2ClientService;

    @Autowired
    private OtaPackageService otaPackageService;

    @Autowired
    private QueueService queueService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private TbClusterService clusterService;

    @Autowired
    private TenantProfileService tenantProfileService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private UserService userService;

    @Autowired
    private WidgetTypeService widgetTypeService;

    @Autowired
    private WidgetsBundleService widgetsBundleService;


    // processors
    @Autowired
    private AdminSettingsEdgeProcessor adminSettingsProcessor;

    @Autowired
    private AlarmEdgeProcessor alarmProcessor;

    @Autowired
    private AssetEdgeProcessor assetProcessor;

    @Autowired
    private AssetProfileEdgeProcessor assetProfileProcessor;

    @Autowired
    private CustomerEdgeProcessor customerProcessor;

    @Autowired
    private DashboardEdgeProcessor dashboardProcessor;

    @Autowired
    private DeviceEdgeProcessor deviceProcessor;

    @Autowired
    private DeviceProfileEdgeProcessor deviceProfileProcessor;

    @Autowired
    private EdgeProcessor edgeProcessor;

    @Autowired
    private EntityViewEdgeProcessor entityViewProcessor;

    @Autowired
    private NotificationEdgeProcessor notificationEdgeProcessor;

    @Autowired
    private NotificationRuleProcessor notificationRuleProcessor;

    @Autowired
    private OAuth2EdgeProcessor oAuth2EdgeProcessor;

    @Autowired
    private OtaPackageEdgeProcessor otaPackageProcessor;

    @Autowired
    private QueueEdgeProcessor queueProcessor;

    @Autowired
    private RelationEdgeProcessor relationProcessor;

    @Autowired
    private ResourceEdgeProcessor resourceProcessor;

    @Autowired
    private RuleChainEdgeProcessor ruleChainProcessor;

    @Autowired
    private TelemetryEdgeProcessor telemetryProcessor;

    @Autowired
    private TenantEdgeProcessor tenantProcessor;

    @Autowired
    private TenantProfileEdgeProcessor tenantProfileProcessor;

    @Autowired
    private UserEdgeProcessor userProcessor;

    @Autowired
    private WidgetBundleEdgeProcessor widgetBundleProcessor;

    @Autowired
    private WidgetTypeEdgeProcessor widgetTypeProcessor;

    // msg constructors
    @Autowired
    private EdgeMsgConstructor edgeMsgConstructor;

    // factories
    @Autowired
    private AlarmEdgeProcessorFactory alarmEdgeProcessorFactory;

    @Autowired
    private AssetEdgeProcessorFactory assetEdgeProcessorFactory;

    @Autowired
    private AssetMsgConstructorFactory assetMsgConstructorFactory;

    @Autowired
    private AssetProfileEdgeProcessorFactory assetProfileEdgeProcessorFactory;

    @Autowired
    private DashboardEdgeProcessorFactory dashboardEdgeProcessorFactory;

    @Autowired
    private DeviceEdgeProcessorFactory deviceEdgeProcessorFactory;

    @Autowired
    private DeviceMsgConstructorFactory deviceMsgConstructorFactory;

    @Autowired
    private DeviceProfileEdgeProcessorFactory deviceProfileEdgeProcessorFactory;

    @Autowired
    private EntityViewProcessorFactory entityViewProcessorFactory;

    @Autowired
    private RelationEdgeProcessorFactory relationEdgeProcessorFactory;

    @Autowired
    private ResourceEdgeProcessorFactory resourceEdgeProcessorFactory;

    // config
    @Autowired
    private EdgeEventStorageSettings edgeEventStorageSettings;

    // callback
    @Autowired
    private GrpcCallbackExecutorService grpcCallbackExecutorService;

}
