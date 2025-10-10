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
package org.thingsboard.server.actors.tenant;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.ProcessFailureStrategy;
import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorException;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.actors.TbActorNotRegisteredException;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.actors.TbEntityTypeActorIdPredicate;
import org.thingsboard.server.actors.TbStringActorId;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldManagerActorCreator;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldStateRestoreMsg;
import org.thingsboard.server.actors.device.DeviceActorCreator;
import org.thingsboard.server.actors.ruleChain.RuleChainManagerActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbActorStopReason;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.ToCalculatedFieldSystemMsg;
import org.thingsboard.server.common.msg.aware.DeviceAwareMsg;
import org.thingsboard.server.common.msg.aware.RuleChainAwareMsg;
import org.thingsboard.server.common.msg.aware.TenantAwareMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldCacheInitMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldEntityLifecycleMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.rule.engine.DeviceDeleteMsg;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class TenantActor extends RuleChainManagerActor {

    private boolean isRuleEngine;
    private boolean isCore;
    private ApiUsageState apiUsageState;
    private Set<DeviceId> deletedDevices;
    private TbActorRef cfActor;

    private TenantActor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext, tenantId);
        this.deletedDevices = new HashSet<>();
    }

    boolean cantFindTenant = false;

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        log.debug("[{}] Starting tenant actor.", tenantId);
        try {
            Tenant tenant = systemContext.getTenantService().findTenantById(tenantId);
            if (tenant == null) {
                cantFindTenant = true;
                log.info("[{}] Started tenant actor for missing tenant.", tenantId);
            } else {
                isCore = systemContext.getServiceInfoProvider().isService(ServiceType.TB_CORE);
                isRuleEngine = systemContext.getServiceInfoProvider().isService(ServiceType.TB_RULE_ENGINE);
                if (isRuleEngine) {
                    if (systemContext.getPartitionService().isManagedByCurrentService(tenantId)) {
                        try {
                            //TODO: IM - extend API usage to have CF Exec Enabled? Not in 4.0;
                            cfActor = ctx.getOrCreateChildActor(new TbStringActorId("CFM|" + tenantId),
                                    () -> DefaultActorService.CF_MANAGER_DISPATCHER_NAME,
                                    () -> new CalculatedFieldManagerActorCreator(systemContext, tenantId),
                                    () -> true);
                            cfActor.tellWithHighPriority(new CalculatedFieldCacheInitMsg(tenantId));
                        } catch (Exception e) {
                            log.info("[{}] Failed to init CF Actor.", tenantId, e);
                        }
                        try {
                            if (getApiUsageState().isReExecEnabled()) {
                                log.debug("[{}] Going to init rule chains", tenantId);
                                initRuleChains();
                            } else {
                                log.info("[{}] Skip init of the rule chains due to API limits", tenantId);
                            }
                        } catch (Exception e) {
                            log.info("Failed to check ApiUsage \"ReExecEnabled\"!!!", e);
                            cantFindTenant = true;
                        }
                    } else {
                        log.info("Tenant {} is not managed by current service, skipping rule chains and cf actor init", tenantId);
                    }
                }
                log.debug("[{}] Tenant actor started.", tenantId);
            }
        } catch (Exception e) {
            log.warn("[{}] Unknown failure", tenantId, e);
        }
    }

    @Override
    public void destroy(TbActorStopReason stopReason, Throwable cause) {
        log.info("[{}] Stopping tenant actor.", tenantId);
        if (cfActor != null) {
            ctx.stop(cfActor.getActorId());
            cfActor = null;
        }
    }

    @Override
    protected boolean doProcess(TbActorMsg msg) {
        if (cantFindTenant) {
            log.info("[{}] Processing missing Tenant msg: {}", tenantId, msg);
            if (msg.getMsgType().equals(MsgType.QUEUE_TO_RULE_ENGINE_MSG)) {
                QueueToRuleEngineMsg queueMsg = (QueueToRuleEngineMsg) msg;
                queueMsg.getMsg().getCallback().onSuccess();
            } else if (msg.getMsgType().equals(MsgType.TRANSPORT_TO_DEVICE_ACTOR_MSG)) {
                TransportToDeviceActorMsgWrapper transportMsg = (TransportToDeviceActorMsgWrapper) msg;
                transportMsg.getCallback().onSuccess();
            }
            return true;
        }
        switch (msg.getMsgType()) {
            case PARTITION_CHANGE_MSG:
                onPartitionChangeMsg((PartitionChangeMsg) msg);
                break;
            case COMPONENT_LIFE_CYCLE_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case CF_ENTITY_ACTION_EVENT_MSG:
                forwardToCfActor((TenantAwareMsg) msg, true);
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
            case REMOVE_RPC_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((DeviceAwareMsg) msg, true);
                break;
            case SESSION_TIMEOUT_MSG:
                ctx.broadcastToChildrenByType(msg, EntityType.DEVICE);
                break;
            case RULE_CHAIN_INPUT_MSG:
            case RULE_CHAIN_OUTPUT_MSG:
            case RULE_CHAIN_TO_RULE_CHAIN_MSG:
                onRuleChainMsg((RuleChainAwareMsg) msg);
                break;
            case CF_CACHE_INIT_MSG:
            case CF_STATE_RESTORE_MSG:
            case CF_PARTITIONS_CHANGE_MSG:
            case CF_STATE_PARTITION_RESTORE_MSG:
                forwardToCfActor((ToCalculatedFieldSystemMsg) msg, true);
                break;
            case CF_TELEMETRY_MSG:
            case CF_LINKED_TELEMETRY_MSG:
                forwardToCfActor((ToCalculatedFieldSystemMsg) msg, false);
                break;
            default:
                return false;
        }
        return true;
    }

    private void forwardToCfActor(TenantAwareMsg msg, boolean priority) {
        if (cfActor == null) {
            if (msg instanceof CalculatedFieldStateRestoreMsg) {
                log.warn("[{}] CF Actor is not initialized. ToCalculatedFieldSystemMsg: [{}]", tenantId, msg);
            } else {
                log.debug("[{}] CF Actor is not initialized. ToCalculatedFieldSystemMsg: [{}]", tenantId, msg);
            }
            msg.getCallback().onSuccess();
            return;
        }
        if (priority) {
            cfActor.tellWithHighPriority(msg);
        } else {
            cfActor.tell(msg);
        }
    }

    private boolean isMyPartition(EntityId entityId) {
        return systemContext.resolve(ServiceType.TB_CORE, tenantId, entityId).isMyPartition();
    }

    private void onQueueToRuleEngineMsg(QueueToRuleEngineMsg msg) {
        if (!isRuleEngine) {
            log.warn("RECEIVED INVALID MESSAGE: {}", msg);
            return;
        }
        TbMsg tbMsg = msg.getMsg();
        if (getApiUsageState().isReExecEnabled()) {
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
        if (getApiUsageState().isReExecEnabled()) {
            getOrCreateActor(msg.getRuleChainId()).tell(msg);
        }
    }

    private void onToDeviceActorMsg(DeviceAwareMsg msg, boolean priority) {
        if (!isCore) {
            log.warn("RECEIVED INVALID MESSAGE: {}", msg);
        }
        if (deletedDevices.contains(msg.getDeviceId())) {
            log.debug("RECEIVED MESSAGE FOR DELETED DEVICE: {}", msg);
            return;
        }
        TbActorRef deviceActor = getOrCreateDeviceActor(msg.getDeviceId());
        if (priority) {
            deviceActor.tellWithHighPriority(msg);
        } else {
            deviceActor.tell(msg);
        }
    }

    private void onPartitionChangeMsg(PartitionChangeMsg msg) {
        ServiceType serviceType = msg.getServiceType();
        if (ServiceType.TB_RULE_ENGINE.equals(serviceType)) {
            if (systemContext.getPartitionService().isManagedByCurrentService(tenantId)) {
                if (cfActor == null) {
                    try {
                        //TODO: IM - extend API usage to have CF Exec Enabled? Not in 4.0;
                        cfActor = ctx.getOrCreateChildActor(new TbStringActorId("CFM|" + tenantId),
                                () -> DefaultActorService.CF_MANAGER_DISPATCHER_NAME,
                                () -> new CalculatedFieldManagerActorCreator(systemContext, tenantId),
                                () -> true);
                        cfActor.tellWithHighPriority(new CalculatedFieldCacheInitMsg(tenantId));
                    } catch (Exception e) {
                        log.info("[{}] Failed to init CF Actor.", tenantId, e);
                    }
                }
                if (!ruleChainsInitialized) {
                    log.info("Tenant {} is now managed by this service, initializing rule chains", tenantId);
                    initRuleChains();
                }
            } else {
                if (cfActor != null) {
                    ctx.stop(cfActor.getActorId());
                    cfActor = null;
                }
                if (ruleChainsInitialized) {
                    log.info("Tenant {} is no longer managed by this service, stopping rule chains", tenantId);
                    destroyRuleChains();
                }
                return;
            }

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
    }

    private void onComponentLifecycleMsg(ComponentLifecycleMsg msg) {
        if (msg.getEntityId().getEntityType().equals(EntityType.API_USAGE_STATE)) {
            ApiUsageState old = getApiUsageState();
            apiUsageState = new ApiUsageState(systemContext.getApiUsageStateService().getApiUsageState(tenantId));
            if (old.isReExecEnabled() && !apiUsageState.isReExecEnabled()) {
                log.info("[{}] Received API state update. Going to DISABLE Rule Engine execution.", tenantId);
                destroyRuleChains();
            } else if (!old.isReExecEnabled() && apiUsageState.isReExecEnabled()) {
                log.info("[{}] Received API state update. Going to ENABLE Rule Engine execution.", tenantId);
                initRuleChains();
            }
        }
        if (msg.getEntityId().getEntityType() == EntityType.DEVICE && ComponentLifecycleEvent.DELETED == msg.getEvent() && isMyPartition(msg.getEntityId())) {
            DeviceId deviceId = (DeviceId) msg.getEntityId();
            onToDeviceActorMsg(new DeviceDeleteMsg(tenantId, deviceId), true);
            deletedDevices.add(deviceId);
        }
        if (isRuleEngine) {
            if (ruleChainsInitialized) {
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
            if (cfActor != null) {
                if (msg.getEntityId().getEntityType().isOneOf(EntityType.CALCULATED_FIELD, EntityType.DEVICE, EntityType.ASSET, EntityType.CUSTOMER)) {
                    cfActor.tellWithHighPriority(new CalculatedFieldEntityLifecycleMsg(tenantId, msg));
                }
            }
        }
    }

    private TbActorRef getOrCreateDeviceActor(DeviceId deviceId) {
        return ctx.getOrCreateChildActor(new TbEntityActorId(deviceId),
                () -> DefaultActorService.DEVICE_DISPATCHER_NAME,
                () -> new DeviceActorCreator(systemContext, tenantId, deviceId),
                () -> true);
    }

    private ApiUsageState getApiUsageState() {
        if (apiUsageState == null) {
            apiUsageState = new ApiUsageState(systemContext.getApiUsageStateService().getApiUsageState(tenantId));
        }
        return apiUsageState;
    }

    @Override
    public ProcessFailureStrategy onProcessFailure(TbActorMsg msg, Throwable t) {
        log.error("[{}] Failed to process msg: {}", tenantId, msg, t);
        return doProcessFailure(t);
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
