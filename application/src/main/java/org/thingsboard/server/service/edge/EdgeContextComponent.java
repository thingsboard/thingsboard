// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.edge;

import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.dao.ai.AiModelService;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.service.edge.rpc.processor.apikey.ApiKeyProcessor;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.edge.stats.EdgeStatsCounterService;
import org.thingsboard.server.dao.encryptionkey.EncryptionService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.menu.CustomMenuService;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.CustomersHierarchyEdgeService;
import org.thingsboard.server.service.edge.rpc.EdgeEventStorageSettings;
import org.thingsboard.server.service.edge.rpc.EdgeRpcService;
import org.thingsboard.server.service.edge.rpc.processor.EdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.ai.AiModelProcessor;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmProcessor;
import org.thingsboard.server.service.edge.rpc.processor.alarm.comment.AlarmCommentProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.profile.AssetProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.cf.CalculatedFieldProcessor;
import org.thingsboard.server.service.edge.rpc.processor.converter.ConverterEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.DashboardEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.ota.DeviceOtaPackageEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.profile.DeviceProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.edge.EdgeEntityProcessor;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.group.EntityGroupEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.group.GroupPermissionsEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.integration.IntegrationEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.menu.CustomMenuEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.relation.RelationEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.report.ReportTemplateProcessor;
import org.thingsboard.server.service.edge.rpc.processor.resource.ResourceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.role.RoleEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.rule.RuleChainEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.rule.RuleChainMetadataEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.scheduler.SchedulerEventEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.telemetry.TelemetryEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.tenant.TenantEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.tenant.TenantProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.translation.CustomTranslationEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.user.UserProcessor;
import org.thingsboard.server.service.edge.rpc.processor.widget.WidgetBundleEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.widget.WidgetTypeEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.wl.WhiteLabelingEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.sync.EdgeRequestsService;
import org.thingsboard.server.service.executors.GrpcCallbackExecutorService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

@Lazy
@Data
@Slf4j
@Component
@TbCoreComponent
public class EdgeContextComponent {

    private final Map<EdgeEventType, EdgeProcessor> processorMap = new EnumMap<>(EdgeEventType.class);

    private ScheduledExecutorService edgeEventProcessingExecutorService;

    @Autowired
    public EdgeContextComponent(List<EdgeProcessor> processors,
                                @Value("${edges.scheduler_pool_size}") int schedulerPoolSize) {
        processors.forEach(processor -> {
            EdgeEventType eventType = processor.getEdgeEventType();
            if (eventType != null) {
                processorMap.put(eventType, processor);
            }
            if (EdgeEventType.WHITE_LABELING.equals(eventType)) {
                processorMap.put(EdgeEventType.LOGIN_WHITE_LABELING, processor);
                processorMap.put(EdgeEventType.MAIL_TEMPLATES, processor);
            }
        });
        this.edgeEventProcessingExecutorService = ThingsBoardExecutors.newScheduledThreadPool(schedulerPoolSize, "edge-event-check-scheduler");
    }

    @PreDestroy
    private void destroy() {
        if (edgeEventProcessingExecutorService != null && !edgeEventProcessingExecutorService.isShutdown()) {
            edgeEventProcessingExecutorService.shutdownNow();
        }
    }

    // services
    @Autowired
    private ActorService actorService;

    @Autowired
    private TelemetrySubscriptionService tsSubService;

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

    @Autowired
    private Optional<EdgeStatsCounterService> statsCounterService;

    // PE services
    @Autowired
    private CustomersHierarchyEdgeService customersHierarchyEdgeService;

    @Autowired
    private CustomMenuService customMenuService;

    @Autowired
    private CustomTranslationService customTranslationService;

    @Autowired
    private ConverterService converterService;

    @Autowired
    private DeviceGroupOtaPackageService deviceGroupOtaPackageService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private GroupPermissionService groupPermissionService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private SchedulerEventService schedulerEventService;

    @Autowired
    private ReportTemplateService reportTemplateService;

    // processors
    @Autowired
    private AlarmProcessor alarmProcessor;

    @Autowired
    private AlarmCommentProcessor alarmCommentProcessor;

    @Autowired
    private AssetEdgeProcessor assetProcessor;

    @Autowired
    private AssetProfileEdgeProcessor assetProfileProcessor;

    @Autowired
    private DashboardEdgeProcessor dashboardProcessor;

    @Autowired
    private DeviceEdgeProcessor deviceProcessor;

    @Autowired
    private DeviceProfileEdgeProcessor deviceProfileProcessor;

    @Autowired
    private DeviceOtaPackageEdgeProcessor deviceOtaPackageProcessor;

    @Autowired
    private EdgeEntityProcessor edgeEntityProcessor;

    @Autowired
    private EntityViewEdgeProcessor entityViewProcessor;

    @Autowired
    private NotificationRuleProcessor ruleProcessor;

    @Autowired
    private RelationEdgeProcessor relationProcessor;

    @Autowired
    private ResourceEdgeProcessor resourceProcessor;

    @Autowired
    private RuleChainEdgeProcessor ruleChainProcessor;

    @Autowired
    private RuleChainMetadataEdgeProcessor ruleChainMetadataProcessor;

    @Autowired
    private TelemetryEdgeProcessor telemetryProcessor;

    @Autowired
    private TenantEdgeProcessor tenantProcessor;

    @Autowired
    private TenantProfileEdgeProcessor tenantProfileProcessor;

    @Autowired
    private WidgetBundleEdgeProcessor widgetBundleProcessor;

    @Autowired
    private WidgetTypeEdgeProcessor widgetTypeProcessor;

    // PE processors
    @Autowired
    private ConverterEdgeProcessor converterProcessor;

    @Autowired
    private CustomMenuEdgeProcessor customMenuProcessor;

    @Autowired
    private CustomTranslationEdgeProcessor customTranslationProcessor;

    @Autowired
    private EntityGroupEdgeProcessor entityGroupProcessor;

    @Autowired
    private GroupPermissionsEdgeProcessor groupPermissionsProcessor;

    @Autowired
    private IntegrationEdgeProcessor integrationProcessor;

    @Autowired
    private RoleEdgeProcessor roleProcessor;

    @Autowired
    private SchedulerEventEdgeProcessor schedulerProcessor;

    @Autowired
    private WhiteLabelingEdgeProcessor whiteLabelingProcessor;

    @Autowired
    private ReportTemplateProcessor reportTemplateProcessor;

    @Autowired
    private SchedulerEventEdgeProcessor schedulerEventProcessor;

    @Autowired
    @Lazy
    private SubscriptionService subscriptionService;

    // config
    @Autowired
    private EdgeEventStorageSettings edgeEventStorageSettings;

    // callback
    @Autowired
    private GrpcCallbackExecutorService grpcCallbackExecutorService;

    @Autowired
    private CalculatedFieldService calculatedFieldService;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private SecretService secretService;

    @Autowired
    private CalculatedFieldProcessor calculatedFieldProcessor;

    @Autowired
    private AiModelService aiModelService;

    @Autowired
    private AiModelProcessor aiModelProcessor;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ApiKeyProcessor apiKeyProcessor;

    @Autowired
    private UserProcessor userProcessor;

    public EdgeProcessor getProcessor(EdgeEventType edgeEventType) {
        EdgeProcessor processor = processorMap.get(edgeEventType);
        if (processor == null) {
            throw new UnsupportedOperationException("No processor found for EdgeEventType: " + edgeEventType);
        }
        return processor;
    }

}
