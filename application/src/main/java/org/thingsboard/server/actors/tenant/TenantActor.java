/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.actors.tenant;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorException;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.actors.TbActorNotRegisteredException;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.actors.TbEntityTypeActorIdPredicate;
import org.thingsboard.server.actors.device.DeviceActorCreator;
import org.thingsboard.server.actors.ruleChain.RuleChainManagerActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.aware.DeviceAwareMsg;
import org.thingsboard.server.common.msg.aware.RuleChainAwareMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.service.edge.rpc.EdgeRpcService;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import java.util.List;
import java.util.Optional;

@Slf4j
public class TenantActor extends RuleChainManagerActor {

    private boolean isRuleEngineForCurrentTenant;
    private boolean isCore;
    private ApiUsageState apiUsageState;

    private TenantActor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext, tenantId);
    }

    boolean cantFindTenant = false;

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        log.info("[{}] Starting tenant actor.", tenantId);
        try {
            Tenant tenant = systemContext.getTenantService().findTenantById(tenantId);
            if (tenant == null) {
                cantFindTenant = true;
                log.info("[{}] Started tenant actor for missing tenant.", tenantId);
            } else {
                apiUsageState = new ApiUsageState(systemContext.getApiUsageStateService().getApiUsageState(tenant.getId()));

                // This Service may be started for specific tenant only.
                Optional<TenantId> isolatedTenantId = systemContext.getServiceInfoProvider().getIsolatedTenant();

                TenantProfile tenantProfile = systemContext.getTenantProfileCache().get(tenant.getTenantProfileId());

                isCore = systemContext.getServiceInfoProvider().isService(ServiceType.TB_CORE);
                isRuleEngineForCurrentTenant = systemContext.getServiceInfoProvider().isService(ServiceType.TB_RULE_ENGINE);
                if (isRuleEngineForCurrentTenant) {
                    try {
                        if (isolatedTenantId.map(id -> id.equals(tenantId)).orElseGet(() -> !tenantProfile.isIsolatedTbRuleEngine())) {
                            if (apiUsageState.isReExecEnabled()) {
                                log.info("[{}] Going to init rule chains", tenantId);
                                initRuleChains();
                            } else {
                                log.info("[{}] Skip init of the rule chains due to API limits", tenantId);
                            }
                        } else {
                            isRuleEngineForCurrentTenant = false;
                        }
                    } catch (Exception e) {
                        cantFindTenant = true;
                    }
                }
                log.info("[{}] Tenant actor started.", tenantId);
            }
        } catch (Exception e) {
            log.warn("[{}] Unknown failure", tenantId, e);
        }
    }

    @Override
    public void destroy() {
        log.info("[{}] Stopping tenant actor.", tenantId);
    }

    @Override
    protected boolean doProcess(TbActorMsg msg) {
        if (cantFindTenant) {
            log.info("[{}] Processing missing Tenant msg: {}", tenantId, msg);
            if (msg.getMsgType().equals(MsgType.QUEUE_TO_RULE_ENGINE_MSG)) {
                QueueToRuleEngineMsg queueMsg = (QueueToRuleEngineMsg) msg;
                queueMsg.getTbMsg().getCallback().onSuccess();
            } else if (msg.getMsgType().equals(MsgType.TRANSPORT_TO_DEVICE_ACTOR_MSG)) {
                TransportToDeviceActorMsgWrapper transportMsg = (TransportToDeviceActorMsgWrapper) msg;
                transportMsg.getCallback().onSuccess();
            }
            return true;
        }
        switch (msg.getMsgType()) {
            case PARTITION_CHANGE_MSG:
                PartitionChangeMsg partitionChangeMsg = (PartitionChangeMsg) msg;
                ServiceType serviceType = partitionChangeMsg.getServiceQueueKey().getServiceType();
                if (ServiceType.TB_RULE_ENGINE.equals(serviceType)) {
                    //To Rule Chain Actors
                    broadcast(msg);
                } else if (ServiceType.TB_CORE.equals(serviceType)) {
                    List<TbActorId> deviceActorIds = ctx.filterChildren(new TbEntityTypeActorIdPredicate(EntityType.DEVICE) {
                        @Override
                        protected boolean testEntityId(EntityId entityId) {
                            return super.testEntityId(entityId) && !isMyPartition(entityId);
                        }
                    });
                    deviceActorIds.forEach(id -> ctx.stop(id));
                }
                break;
            case COMPONENT_LIFE_CYCLE_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case QUEUE_TO_RULE_ENGINE_MSG:
                onQueueToRuleEngineMsg((QueueToRuleEngineMsg) msg);
                break;
            case TRANSPORT_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((DeviceAwareMsg) msg, false);
                break;
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_EDGE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
            case SERVER_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((DeviceAwareMsg) msg, true);
                break;
            case RULE_CHAIN_TO_RULE_CHAIN_MSG:
                onRuleChainMsg((RuleChainAwareMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    private boolean isMyPartition(EntityId entityId) {
        return systemContext.resolve(ServiceType.TB_CORE, tenantId, entityId).isMyPartition();
    }

    private void onQueueToRuleEngineMsg(QueueToRuleEngineMsg msg) {
        if (!isRuleEngineForCurrentTenant) {
            log.warn("RECEIVED INVALID MESSAGE: {}", msg);
            return;
        }
        TbMsg tbMsg = msg.getTbMsg();
        if (apiUsageState.isReExecEnabled()) {
            if (tbMsg.getRuleChainId() == null) {
                if (getRootChainActor() != null) {
                    getRootChainActor().tell(msg);
                } else {
                    tbMsg.getCallback().onFailure(new RuleEngineException("No Root Rule Chain available!"));
                    log.info("[{}] No Root Chain: {}", tenantId, msg);
                }
            } else {
                try {
                    ctx.tell(new TbEntityActorId(tbMsg.getRuleChainId()), msg);
                } catch (TbActorNotRegisteredException ex) {
                    log.trace("Received message for non-existing rule chain: [{}]", tbMsg.getRuleChainId());
                    //TODO: 3.1 Log it to dead letters queue;
                    tbMsg.getCallback().onSuccess();
                }
            }
        } else {
            log.trace("[{}] Ack message because Rule Engine is disabled", tenantId);
            tbMsg.getCallback().onSuccess();
        }
    }

    private void onRuleChainMsg(RuleChainAwareMsg msg) {
        if (apiUsageState.isReExecEnabled()) {
            getOrCreateActor(msg.getRuleChainId()).tell(msg);
        }
    }

    private void onToDeviceActorMsg(DeviceAwareMsg msg, boolean priority) {
        if (!isCore) {
            log.warn("RECEIVED INVALID MESSAGE: {}", msg);
        }
        TbActorRef deviceActor = getOrCreateDeviceActor(msg.getDeviceId());
        if (priority) {
            deviceActor.tellWithHighPriority(msg);
        } else {
            deviceActor.tell(msg);
        }
    }

    private void onComponentLifecycleMsg(ComponentLifecycleMsg msg) {
        if (msg.getEntityId().getEntityType().equals(EntityType.API_USAGE_STATE)) {
            ApiUsageState old = apiUsageState;
            apiUsageState = new ApiUsageState(systemContext.getApiUsageStateService().getApiUsageState(tenantId));
            if (old.isReExecEnabled() && !apiUsageState.isReExecEnabled()) {
                log.info("[{}] Received API state update. Going to DISABLE Rule Engine execution.", tenantId);
                destroyRuleChains();
            } else if (!old.isReExecEnabled() && apiUsageState.isReExecEnabled()) {
                log.info("[{}] Received API state update. Going to ENABLE Rule Engine execution.", tenantId);
                initRuleChains();
            }
        } else if (msg.getEntityId().getEntityType() == EntityType.EDGE) {
            EdgeId edgeId = new EdgeId(msg.getEntityId().getId());
            EdgeRpcService edgeRpcService = systemContext.getEdgeRpcService();
            if (msg.getEvent() == ComponentLifecycleEvent.DELETED) {
                edgeRpcService.deleteEdge(edgeId);
            } else {
                Edge edge = systemContext.getEdgeService().findEdgeById(tenantId, edgeId);
                if (msg.getEvent() == ComponentLifecycleEvent.UPDATED) {
                    edgeRpcService.updateEdge(edge);
                }
            }
        } else if (isRuleEngineForCurrentTenant) {
            TbActorRef target = getEntityActorRef(msg.getEntityId());
            if (target != null) {
                if (msg.getEntityId().getEntityType() == EntityType.RULE_CHAIN) {
                    RuleChain ruleChain = systemContext.getRuleChainService().
                            findRuleChainById(tenantId, new RuleChainId(msg.getEntityId().getId()));
                    if (ruleChain != null && RuleChainType.CORE.equals(ruleChain.getType())) {
                        visit(ruleChain, target);
                    }
                }
                target.tellWithHighPriority(msg);
            } else {
                log.debug("[{}] Invalid component lifecycle msg: {}", tenantId, msg);
            }
        }
    }

    private TbActorRef getOrCreateDeviceActor(DeviceId deviceId) {
        return ctx.getOrCreateChildActor(new TbEntityActorId(deviceId),
                () -> DefaultActorService.DEVICE_DISPATCHER_NAME,
                () -> new DeviceActorCreator(systemContext, tenantId, deviceId));
    }

    public static class ActorCreator extends ContextBasedCreator {

        private final TenantId tenantId;

        public ActorCreator(ActorSystemContext context, TenantId tenantId) {
            super(context);
            this.tenantId = tenantId;
        }

        @Override
        public TbActorId createActorId() {
            return new TbEntityActorId(tenantId);
        }

        @Override
        public TbActor createActor() {
            return new TenantActor(context, tenantId);
        }
    }

}
