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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.profile.AssetProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.customer.CustomerEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.DashboardEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.profile.DeviceProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.edge.EdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.oauth2.OAuth2EdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.ota.OtaPackageEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.queue.QueueEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.relation.RelationEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.resource.ResourceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.rule.RuleChainEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.tenant.TenantEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.tenant.TenantProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.user.UserEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.widget.WidgetBundleEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.widget.WidgetTypeEdgeProcessor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@TbCoreComponent
@Slf4j
public class DefaultEdgeNotificationService implements EdgeNotificationService {

    public static final String EDGE_IS_ROOT_BODY_KEY = "isRoot";

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private EdgeProcessor edgeProcessor;

    @Autowired
    private AssetEdgeProcessor assetProcessor;

    @Autowired
    private AssetProfileEdgeProcessor assetProfileEdgeProcessor;

    @Autowired
    private DeviceEdgeProcessor deviceProcessor;

    @Autowired
    private DeviceProfileEdgeProcessor deviceProfileEdgeProcessor;

    @Autowired
    private EntityViewEdgeProcessor entityViewProcessor;

    @Autowired
    private DashboardEdgeProcessor dashboardProcessor;

    @Autowired
    private RuleChainEdgeProcessor ruleChainProcessor;

    @Autowired
    private UserEdgeProcessor userProcessor;

    @Autowired
    private CustomerEdgeProcessor customerProcessor;

    @Autowired
    private OtaPackageEdgeProcessor otaPackageProcessor;

    @Autowired
    private WidgetBundleEdgeProcessor widgetBundleProcessor;

    @Autowired
    private WidgetTypeEdgeProcessor widgetTypeProcessor;

    @Autowired
    private QueueEdgeProcessor queueProcessor;

    @Autowired
    private TenantEdgeProcessor tenantEdgeProcessor;

    @Autowired
    private TenantProfileEdgeProcessor tenantProfileEdgeProcessor;

    @Autowired
    private AlarmEdgeProcessor alarmProcessor;

    @Autowired
    private RelationEdgeProcessor relationProcessor;

    @Autowired
    private ResourceEdgeProcessor resourceEdgeProcessor;

    @Autowired
    private OAuth2EdgeProcessor oAuth2EdgeProcessor;

    @Autowired
    protected ApplicationEventPublisher eventPublisher;

    @Value("${actors.system.edge_dispatcher_pool_size:4}")
    private int edgeDispatcherSize;

    private ExecutorService executor;

    @PostConstruct
    public void initExecutor() {
        executor = ThingsBoardExecutors.newWorkStealingPool(edgeDispatcherSize, "edge-notifications");
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public Edge setEdgeRootRuleChain(TenantId tenantId, Edge edge, RuleChainId ruleChainId) {
        edge.setRootRuleChainId(ruleChainId);
        Edge savedEdge = edgeService.saveEdge(edge);
        ObjectNode isRootBody = JacksonUtil.newObjectNode();
        isRootBody.put(EDGE_IS_ROOT_BODY_KEY, Boolean.TRUE);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edge.getId()).entityId(ruleChainId)
                .body(JacksonUtil.toString(isRootBody)).actionType(ActionType.UPDATED).build());
        return savedEdge;
    }

    @Override
    public void pushNotificationToEdge(TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback) {
        TenantId tenantId = TenantId.fromUUID(new UUID(edgeNotificationMsg.getTenantIdMSB(), edgeNotificationMsg.getTenantIdLSB()));
        log.debug("[{}] Pushing notification to edge {}", tenantId, edgeNotificationMsg);
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        try {
            executor.submit(() -> {
                try {
                    if (deadline < System.nanoTime()) {
                        log.warn("[{}] Skipping notification message because deadline reached {}", tenantId, edgeNotificationMsg);
                        return;
                    }
                    EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
                    switch (type) {
                        case EDGE:
                            edgeProcessor.processEdgeNotification(tenantId, edgeNotificationMsg);
                            break;
                        case ASSET:
                            assetProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case ASSET_PROFILE:
                            assetProfileEdgeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case DEVICE:
                            deviceProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case DEVICE_PROFILE:
                            deviceProfileEdgeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case ENTITY_VIEW:
                            entityViewProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case DASHBOARD:
                            dashboardProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case RULE_CHAIN:
                            ruleChainProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case USER:
                            userProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case CUSTOMER:
                            customerProcessor.processCustomerNotification(tenantId, edgeNotificationMsg);
                            break;
                        case OTA_PACKAGE:
                            otaPackageProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case WIDGETS_BUNDLE:
                            widgetBundleProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case WIDGET_TYPE:
                            widgetTypeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case QUEUE:
                            queueProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case ALARM:
                            alarmProcessor.processAlarmNotification(tenantId, edgeNotificationMsg);
                            break;
                        case ALARM_COMMENT:
                            alarmProcessor.processAlarmCommentNotification(tenantId, edgeNotificationMsg);
                            break;
                        case RELATION:
                            relationProcessor.processRelationNotification(tenantId, edgeNotificationMsg);
                            break;
                        case TENANT:
                            tenantEdgeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case TENANT_PROFILE:
                            tenantProfileEdgeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case TB_RESOURCE:
                            resourceEdgeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                            break;
                        case OAUTH2:
                            oAuth2EdgeProcessor.processOAuth2Notification(tenantId, edgeNotificationMsg);
                            break;
                        default:
                            log.warn("[{}] Edge event type [{}] is not designed to be pushed to edge", tenantId, type);
                    }
                } catch (Exception e) {
                    callBackFailure(tenantId, edgeNotificationMsg, callback, e);
                }
            });
            callback.onSuccess();
        } catch (Exception e) {
            callBackFailure(tenantId, edgeNotificationMsg, callback, e);
        }
    }

    private void callBackFailure(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback, Throwable throwable) {
        log.error("[{}] Can't push to edge updates, edgeNotificationMsg [{}]", tenantId, edgeNotificationMsg, throwable);
        callback.onFailure(throwable);
    }
}
