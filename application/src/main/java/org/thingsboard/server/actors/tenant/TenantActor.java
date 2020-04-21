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

import akka.actor.ActorInitializationException;
import akka.actor.ActorRef;
import akka.actor.LocalActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.device.DeviceActorCreator;
import org.thingsboard.server.actors.ruleChain.RuleChainManagerActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
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
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TenantActor extends RuleChainManagerActor {

    private final BiMap<DeviceId, ActorRef> deviceActors;
    private boolean isRuleEngineForCurrentTenant;
    private boolean isCore;

    private TenantActor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext, tenantId);
        this.deviceActors = HashBiMap.create();
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    boolean cantFindTenant = false;

    @Override
    public void preStart() {
        log.info("[{}] Starting tenant actor.", tenantId);
        try {
            Tenant tenant = systemContext.getTenantService().findTenantById(tenantId);
            if (tenant == null) {
                cantFindTenant = true;
                log.info("[{}] Started tenant actor for missing tenant.", tenantId);
            } else {
                // This Service may be started for specific tenant only.
                Optional<TenantId> isolatedTenantId = systemContext.getServiceInfoProvider().getIsolatedTenant();

                isRuleEngineForCurrentTenant = systemContext.getServiceInfoProvider().isService(ServiceType.TB_RULE_ENGINE);
                isCore = systemContext.getServiceInfoProvider().isService(ServiceType.TB_CORE);

                if (isRuleEngineForCurrentTenant) {
                    try {
                        if (isolatedTenantId.map(id -> id.equals(tenantId)).orElseGet(() -> !tenant.isIsolatedTbRuleEngine())) {
                            log.info("[{}] Going to init rule chains", tenantId);
                            initRuleChains();
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
    public void postStop() {
        log.info("[{}] Stopping tenant actor.", tenantId);
    }

    @Override
    protected boolean process(TbActorMsg msg) {
        if (cantFindTenant) {
            log.info("[{}] Processing missing Tenant msg: {}", tenantId, msg);
            if (msg.getMsgType().equals(MsgType.QUEUE_TO_RULE_ENGINE_MSG)) {
                QueueToRuleEngineMsg queueMsg = (QueueToRuleEngineMsg) msg;
                queueMsg.getTbMsg().getCallback().onSuccess();
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
                    //To Device Actors
                    List<DeviceId> repartitionedDevices =
                            deviceActors.keySet().stream().filter(deviceId -> !isMyPartition(deviceId)).collect(Collectors.toList());
                    for (DeviceId deviceId : repartitionedDevices) {
                        ActorRef deviceActor = deviceActors.remove(deviceId);
                        context().stop(deviceActor);
                    }
                }
                break;
            case COMPONENT_LIFE_CYCLE_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case QUEUE_TO_RULE_ENGINE_MSG:
                onQueueToRuleEngineMsg((QueueToRuleEngineMsg) msg);
                break;
            case TRANSPORT_TO_DEVICE_ACTOR_MSG:
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG:
            case SERVER_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((DeviceAwareMsg) msg);
                break;
            case RULE_CHAIN_TO_RULE_CHAIN_MSG:
                onRuleChainMsg((RuleChainAwareMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    private boolean isMyPartition(DeviceId deviceId) {
        return systemContext.resolve(ServiceType.TB_CORE, tenantId, deviceId).isMyPartition();
    }

    private void onQueueToRuleEngineMsg(QueueToRuleEngineMsg msg) {
        if (!isRuleEngineForCurrentTenant) {
            log.warn("RECEIVED INVALID MESSAGE: {}", msg);
            return;
        }
        TbMsg tbMsg = msg.getTbMsg();
        if (tbMsg.getRuleChainId() == null) {
            if (getRootChainActor() != null) {
                getRootChainActor().tell(msg, self());
            } else {
                tbMsg.getCallback().onFailure(new RuleEngineException("No Root Rule Chain available!"));
                log.info("[{}] No Root Chain: {}", tenantId, msg);
            }
        } else {
            ActorRef ruleChainActor = get(tbMsg.getRuleChainId());
            if (ruleChainActor != null) {
                ruleChainActor.tell(msg, self());
            } else {
                log.trace("Received message for non-existing rule chain: [{}]", tbMsg.getRuleChainId());
                //TODO: 3.1 Log it to dead letters queue;
                tbMsg.getCallback().onSuccess();
            }
        }
    }

    private void onRuleChainMsg(RuleChainAwareMsg msg) {
        getOrCreateActor(context(), msg.getRuleChainId()).tell(msg, self());
    }

    private void onToDeviceActorMsg(DeviceAwareMsg msg) {
        if (!isCore) {
            log.warn("RECEIVED INVALID MESSAGE: {}", msg);
        }
        getOrCreateDeviceActor(msg.getDeviceId()).tell(msg, ActorRef.noSender());
    }

    private void onComponentLifecycleMsg(ComponentLifecycleMsg msg) {
        if (isRuleEngineForCurrentTenant) {
            ActorRef target = getEntityActorRef(msg.getEntityId());
            if (target != null) {
                if (msg.getEntityId().getEntityType() == EntityType.RULE_CHAIN) {
                    RuleChain ruleChain = systemContext.getRuleChainService().
                            findRuleChainById(tenantId, new RuleChainId(msg.getEntityId().getId()));
                    visit(ruleChain, target);
                }
                target.tell(msg, ActorRef.noSender());
            } else {
                log.debug("[{}] Invalid component lifecycle msg: {}", tenantId, msg);
            }
        }
    }

    private ActorRef getOrCreateDeviceActor(DeviceId deviceId) {
        return deviceActors.computeIfAbsent(deviceId, k -> {
            log.debug("[{}][{}] Creating device actor.", tenantId, deviceId);
            ActorRef deviceActor = context().actorOf(Props.create(new DeviceActorCreator(systemContext, tenantId, deviceId))
                            .withDispatcher(DefaultActorService.CORE_DISPATCHER_NAME)
                    , deviceId.toString());
            context().watch(deviceActor);
            log.debug("[{}][{}] Created device actor: {}.", tenantId, deviceId, deviceActor);
            return deviceActor;
        });
    }

    @Override
    protected void processTermination(Terminated message) {
        ActorRef terminated = message.actor();
        if (terminated instanceof LocalActorRef) {
            boolean removed = deviceActors.inverse().remove(terminated) != null;
            if (removed) {
                log.debug("[{}] Removed actor:", terminated);
            } else {
                log.debug("Removed actor was not found in the device map!");
            }
        } else {
            throw new IllegalStateException("Remote actors are not supported!");
        }
    }

    public static class ActorCreator extends ContextBasedCreator<TenantActor> {
        private static final long serialVersionUID = 1L;

        private final TenantId tenantId;

        public ActorCreator(ActorSystemContext context, TenantId tenantId) {
            super(context);
            this.tenantId = tenantId;
        }

        @Override
        public TenantActor create() {
            return new TenantActor(context, tenantId);
        }
    }

    private final SupervisorStrategy strategy = new OneForOneStrategy(3, Duration.create("1 minute"), t -> {
        log.warn("[{}] Unknown failure", tenantId, t);
        if (t instanceof ActorInitializationException) {
            return SupervisorStrategy.stop();
        } else {
            return SupervisorStrategy.resume();
        }
    });

}
