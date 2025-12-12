/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.dao.ai.AiModelService;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.edge.stats.EdgeStatsCounterService;
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
import org.thingsboard.server.service.edge.rpc.processor.EdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.ai.AiModelProcessor;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmProcessor;
import org.thingsboard.server.service.edge.rpc.processor.alarm.comment.AlarmCommentProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.profile.AssetProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.cf.CalculatedFieldProcessor;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.DashboardEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.profile.DeviceProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.relation.RelationEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.resource.ResourceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.rule.RuleChainEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.rule.RuleChainMetadataEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.telemetry.TelemetryEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.user.UserProcessor;
import org.thingsboard.server.service.edge.rpc.sync.EdgeRequestsService;
import org.thingsboard.server.service.executors.GrpcCallbackExecutorService;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Lazy
@Data
@Slf4j
@Component
@TbCoreComponent
public class EdgeContextComponent {

    private final Map<EdgeEventType, EdgeProcessor> processorMap = new EnumMap<>(EdgeEventType.class);

    @Autowired
    public EdgeContextComponent(List<EdgeProcessor> processors) {
        processors.forEach(processor -> {
            EdgeEventType eventType = processor.getEdgeEventType();
            if (eventType != null) {
                processorMap.put(eventType, processor);
            }
        });
    }

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

    @Autowired
    private Optional<EdgeStatsCounterService> statsCounterService;

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

    // config
    @Autowired
    private EdgeEventStorageSettings edgeEventStorageSettings;

    // callback
    @Autowired
    private GrpcCallbackExecutorService grpcCallbackExecutorService;

    @Autowired
    private CalculatedFieldService calculatedFieldService;

    @Autowired
    private CalculatedFieldProcessor calculatedFieldProcessor;

    @Autowired
    private AiModelService aiModelService;

    @Autowired
    private AiModelProcessor aiModelProcessor;

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
