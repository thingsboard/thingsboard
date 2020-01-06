/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.actors.app;

import akka.actor.ActorRef;
import akka.actor.LocalActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.ruleChain.RuleChainManagerActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.rulechain.SystemRuleChainManager;
import org.thingsboard.server.actors.tenant.TenantActor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.aware.TenantAwareMsg;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.tenant.TenantService;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AppActor extends RuleChainManagerActor {

    private static final TenantId SYSTEM_TENANT = new TenantId(ModelConstants.NULL_UUID);
    private final TenantService tenantService;
    private final BiMap<TenantId, ActorRef> tenantActors;
    private boolean ruleChainsInitialized;

    private AppActor(ActorSystemContext systemContext) {
        super(systemContext, new SystemRuleChainManager(systemContext));
        this.tenantService = systemContext.getTenantService();
        this.tenantActors = HashBiMap.create();
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    @Override
    public void preStart() {
    }

    @Override
    protected boolean process(TbActorMsg msg) {
        if (!ruleChainsInitialized) {
            initRuleChainsAndTenantActors();
            ruleChainsInitialized = true;
            if (msg.getMsgType() != MsgType.APP_INIT_MSG) {
                log.warn("Rule Chains initialized by unexpected message: {}", msg);
            }
        }
        switch (msg.getMsgType()) {
            case APP_INIT_MSG:
                break;
            case SEND_TO_CLUSTER_MSG:
                onPossibleClusterMsg((SendToClusterMsg) msg);
                break;
            case CLUSTER_EVENT_MSG:
                broadcast(msg);
                break;
            case COMPONENT_LIFE_CYCLE_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case SERVICE_TO_RULE_ENGINE_MSG:
                onServiceToRuleEngineMsg((ServiceToRuleEngineMsg) msg);
                break;
            case TRANSPORT_TO_DEVICE_ACTOR_MSG:
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG:
            case SERVER_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
            case REMOTE_TO_RULE_CHAIN_TELL_NEXT_MSG:
                onToDeviceActorMsg((TenantAwareMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    private void initRuleChainsAndTenantActors() {
        log.info("Starting main system actor.");
        try {
            initRuleChains();
            if (systemContext.isTenantComponentsInitEnabled()) {
                PageDataIterable<Tenant> tenantIterator = new PageDataIterable<>(tenantService::findTenants, ENTITY_PACK_LIMIT);
                for (Tenant tenant : tenantIterator) {
                    log.debug("[{}] Creating tenant actor", tenant.getId());
                    getOrCreateTenantActor(tenant.getId());
                    log.debug("Tenant actor created.");
                }
            }
            log.info("Main system actor started.");
        } catch (Exception e) {
            log.warn("Unknown failure", e);
        }
    }

    private void onPossibleClusterMsg(SendToClusterMsg msg) {
        Optional<ServerAddress> address = systemContext.getRoutingService().resolveById(msg.getEntityId());
        if (address.isPresent()) {
            systemContext.getRpcService().tell(
                    systemContext.getEncodingService().convertToProtoDataMessage(address.get(), msg.getMsg()));
        } else {
            self().tell(msg.getMsg(), ActorRef.noSender());
        }
    }

    private void onServiceToRuleEngineMsg(ServiceToRuleEngineMsg msg) {
        if (SYSTEM_TENANT.equals(msg.getTenantId())) {
//            this may be a notification about system entities created.
//            log.warn("[{}] Invalid service to rule engine msg called. System messages are not supported yet: {}", SYSTEM_TENANT, msg);
        } else {
            getOrCreateTenantActor(msg.getTenantId()).tell(msg, self());
        }
    }

    @Override
    protected void broadcast(Object msg) {
        super.broadcast(msg);
        tenantActors.values().forEach(actorRef -> actorRef.tell(msg, ActorRef.noSender()));
    }

    private void onComponentLifecycleMsg(ComponentLifecycleMsg msg) {
        ActorRef target = null;
        if (SYSTEM_TENANT.equals(msg.getTenantId())) {
            target = getEntityActorRef(msg.getEntityId());
        } else {
            if (msg.getEntityId().getEntityType() == EntityType.TENANT
                    && msg.getEvent() == ComponentLifecycleEvent.DELETED) {
                log.debug("[{}] Handling tenant deleted notification: {}", msg.getTenantId(), msg);
                ActorRef tenantActor = tenantActors.remove(new TenantId(msg.getEntityId().getId()));
                if (tenantActor != null) {
                    log.debug("[{}] Deleting tenant actor: {}", msg.getTenantId(), tenantActor);
                    context().stop(tenantActor);
                }
            } else {
                target = getOrCreateTenantActor(msg.getTenantId());
            }
        }
        if (target != null) {
            target.tell(msg, ActorRef.noSender());
        } else {
            log.debug("[{}] Invalid component lifecycle msg: {}", msg.getTenantId(), msg);
        }
    }

    private void onToDeviceActorMsg(TenantAwareMsg msg) {
        getOrCreateTenantActor(msg.getTenantId()).tell(msg, ActorRef.noSender());
    }

    private ActorRef getOrCreateTenantActor(TenantId tenantId) {
        return tenantActors.computeIfAbsent(tenantId, k -> {
            log.debug("[{}] Creating tenant actor.", tenantId);
            ActorRef tenantActor = context().actorOf(Props.create(new TenantActor.ActorCreator(systemContext, tenantId))
                    .withDispatcher(DefaultActorService.CORE_DISPATCHER_NAME), tenantId.toString());
            context().watch(tenantActor);
            log.debug("[{}] Created tenant actor: {}.", tenantId, tenantActor);
            return tenantActor;
        });
    }

    @Override
    protected void processTermination(Terminated message) {
        ActorRef terminated = message.actor();
        if (terminated instanceof LocalActorRef) {
            boolean removed = tenantActors.inverse().remove(terminated) != null;
            if (removed) {
                log.debug("[{}] Removed actor:", terminated);
            }
        } else {
            throw new IllegalStateException("Remote actors are not supported!");
        }
    }

    public static class ActorCreator extends ContextBasedCreator<AppActor> {
        private static final long serialVersionUID = 1L;

        public ActorCreator(ActorSystemContext context) {
            super(context);
        }

        @Override
        public AppActor create() {
            return new AppActor(context);
        }
    }

    private final SupervisorStrategy strategy = new OneForOneStrategy(3, Duration.create("1 minute"), t -> {
        log.warn("Unknown failure", t);
        if (t instanceof RuntimeException) {
            return SupervisorStrategy.restart();
        } else {
            return SupervisorStrategy.stop();
        }
    });
}
