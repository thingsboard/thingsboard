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
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.customer.CustomerEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.DashboardEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.edge.EdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.ota.OtaPackageEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.queue.QueueEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.relation.RelationEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.rule.RuleChainEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.user.UserEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.widget.WidgetBundleEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.widget.WidgetTypeEdgeProcessor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@TbCoreComponent
@Slf4j
public class DefaultEdgeNotificationService implements EdgeNotificationService {

    public static final String EDGE_IS_ROOT_BODY_KEY = "isRoot";

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private EdgeEventService edgeEventService;

    @Autowired
    private TbClusterService clusterService;

    @Autowired
    private EdgeProcessor edgeProcessor;

    @Autowired
    private AssetEdgeProcessor assetProcessor;

    @Autowired
    private DeviceEdgeProcessor deviceProcessor;

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
    private DeviceProfileEdgeProcessor deviceProfileProcessor;

    @Autowired
    private AssetProfileEdgeProcessor assetProfileProcessor;

    @Autowired
    private OtaPackageEdgeProcessor otaPackageProcessor;

    @Autowired
    private WidgetBundleEdgeProcessor widgetBundleProcessor;

    @Autowired
    private WidgetTypeEdgeProcessor widgetTypeProcessor;

    @Autowired
    private QueueEdgeProcessor queueProcessor;

    @Autowired
    private AlarmEdgeProcessor alarmProcessor;

    @Autowired
    private RelationEdgeProcessor relationProcessor;

    private ExecutorService dbCallBackExecutor;

    @PostConstruct
    public void initExecutor() {
        dbCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("edge-notifications"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (dbCallBackExecutor != null) {
            dbCallBackExecutor.shutdownNow();
        }
    }

    @Override
    public Edge setEdgeRootRuleChain(TenantId tenantId, Edge edge, RuleChainId ruleChainId) throws Exception {
        edge.setRootRuleChainId(ruleChainId);
        Edge savedEdge = edgeService.saveEdge(edge);
        ObjectNode isRootBody = JacksonUtil.newObjectNode();
        isRootBody.put(EDGE_IS_ROOT_BODY_KEY, Boolean.TRUE);
        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.RULE_CHAIN, EdgeEventActionType.UPDATED, ruleChainId, isRootBody).get();
        return savedEdge;
    }

    private ListenableFuture<Void> saveEdgeEvent(TenantId tenantId,
                               EdgeId edgeId,
                               EdgeEventType type,
                               EdgeEventActionType action,
                               EntityId entityId,
                               JsonNode body) {
        log.debug("Pushing edge event to edge queue. tenantId [{}], edgeId [{}], type [{}], action[{}], entityId [{}], body [{}]",
                tenantId, edgeId, type, action, entityId, body);

        EdgeEvent edgeEvent = EdgeUtils.constructEdgeEvent(tenantId, edgeId, type, action, entityId, body);

        return Futures.transform(edgeEventService.saveAsync(edgeEvent), unused -> {
            clusterService.onEdgeEventUpdate(tenantId, edgeId);
            return null;
        }, dbCallBackExecutor);
    }

    @Override
    public void pushNotificationToEdge(TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback) {
        log.debug("Pushing notification to edge {}", edgeNotificationMsg);
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(edgeNotificationMsg.getTenantIdMSB(), edgeNotificationMsg.getTenantIdLSB()));
            EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
            ListenableFuture<Void> future;
            switch (type) {
                case EDGE:
                    future = edgeProcessor.processEdgeNotification(tenantId, edgeNotificationMsg);
                    break;
                case ASSET:
                    future = assetProcessor.processAssetNotification(tenantId, edgeNotificationMsg);
                    break;
                case DEVICE:
                    future = deviceProcessor.processDeviceNotification(tenantId, edgeNotificationMsg);
                    break;
                case ENTITY_VIEW:
                    future = entityViewProcessor.processEntityViewNotification(tenantId, edgeNotificationMsg);
                    break;
                case DASHBOARD:
                    future = dashboardProcessor.processDashboardNotification(tenantId, edgeNotificationMsg);
                    break;
                case RULE_CHAIN:
                    future = ruleChainProcessor.processRuleChainNotification(tenantId, edgeNotificationMsg);
                    break;
                case USER:
                    future = userProcessor.processUserNotification(tenantId, edgeNotificationMsg);
                    break;
                case CUSTOMER:
                    future = customerProcessor.processCustomerNotification(tenantId, edgeNotificationMsg);
                    break;
                case DEVICE_PROFILE:
                    future = deviceProfileProcessor.processDeviceProfileNotification(tenantId, edgeNotificationMsg);
                    break;
                case ASSET_PROFILE:
                    future = assetProfileProcessor.processAssetProfileNotification(tenantId, edgeNotificationMsg);
                    break;
                case OTA_PACKAGE:
                    future = otaPackageProcessor.processOtaPackageNotification(tenantId, edgeNotificationMsg);
                    break;
                case WIDGETS_BUNDLE:
                    future = widgetBundleProcessor.processWidgetsBundleNotification(tenantId, edgeNotificationMsg);
                    break;
                case WIDGET_TYPE:
                    future = widgetTypeProcessor.processWidgetTypeNotification(tenantId, edgeNotificationMsg);
                    break;
                case QUEUE:
                    future = queueProcessor.processQueueNotification(tenantId, edgeNotificationMsg);
                    break;
                case ALARM:
                    future = alarmProcessor.processAlarmNotification(tenantId, edgeNotificationMsg);
                    break;
                case RELATION:
                    future = relationProcessor.processRelationNotification(tenantId, edgeNotificationMsg);
                    break;
                default:
                    log.warn("Edge event type [{}] is not designed to be pushed to edge", type);
                    future = Futures.immediateFuture(null);
            }
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Void unused) {
                    callback.onSuccess();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    callBackFailure(edgeNotificationMsg, callback, throwable);
                }
            }, dbCallBackExecutor);
        } catch (Exception e) {
            callBackFailure(edgeNotificationMsg, callback, e);
        }
    }

    private void callBackFailure(TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback, Throwable throwable) {
        log.error("Can't push to edge updates, edgeNotificationMsg [{}]", edgeNotificationMsg, throwable);
        callback.onFailure(throwable);
    }

}


