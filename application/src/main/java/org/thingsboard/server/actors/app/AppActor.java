/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.ruleChain.RuleChainManagerActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.rulechain.SystemRuleChainManager;
import org.thingsboard.server.actors.tenant.TenantActor;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.aware.TenantAwareMsg;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.core.BasicActorSystemToDeviceSessionActorMsg;
import org.thingsboard.server.common.msg.device.DeviceToDeviceActorMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.tenant.TenantService;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AppActor extends RuleChainManagerActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    public static final TenantId SYSTEM_TENANT = new TenantId(ModelConstants.NULL_UUID);
    private final TenantService tenantService;
    private final Map<TenantId, ActorRef> tenantActors;

    private AppActor(ActorSystemContext systemContext) {
        super(systemContext, new SystemRuleChainManager(systemContext));
        this.tenantService = systemContext.getTenantService();
        this.tenantActors = new HashMap<>();
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    @Override
    public void preStart() {
        logger.info("Starting main system actor.");
        try {
            initRuleChains();

            if (systemContext.isTenantComponentsInitEnabled()) {
                PageDataIterable<Tenant> tenantIterator = new PageDataIterable<>(tenantService::findTenants, ENTITY_PACK_LIMIT);
                for (Tenant tenant : tenantIterator) {
                    logger.debug("[{}] Creating tenant actor", tenant.getId());
                    getOrCreateTenantActor(tenant.getId());
                    logger.debug("Tenant actor created.");
                }
            }

            logger.info("Main system actor started.");
        } catch (Exception e) {
            logger.error(e, "Unknown failure");
        }
    }

    @Override
    protected boolean process(TbActorMsg msg) {
        switch (msg.getMsgType()) {
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
            case DEVICE_SESSION_TO_DEVICE_ACTOR_MSG:
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG:
            case SERVER_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
            case REMOTE_TO_RULE_CHAIN_TELL_NEXT_MSG:
                onToDeviceActorMsg((TenantAwareMsg) msg);
                break;
            case ACTOR_SYSTEM_TO_DEVICE_SESSION_ACTOR_MSG:
                onToDeviceSessionMsg((BasicActorSystemToDeviceSessionActorMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    private void onToDeviceSessionMsg(BasicActorSystemToDeviceSessionActorMsg msg) {
        systemContext.getSessionManagerActor().tell(msg, self());
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
            //TODO: ashvayka handle this.
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
        ActorRef target;
        if (SYSTEM_TENANT.equals(msg.getTenantId())) {
            target = getEntityActorRef(msg.getEntityId());
        } else {
            target = getOrCreateTenantActor(msg.getTenantId());
        }
        if (target != null) {
            target.tell(msg, ActorRef.noSender());
        } else {
            logger.debug("Invalid component lifecycle msg: {}", msg);
        }
    }

    private void onToDeviceActorMsg(TenantAwareMsg msg) {
        getOrCreateTenantActor(msg.getTenantId()).tell(msg, ActorRef.noSender());
    }

    private void processDeviceMsg(DeviceToDeviceActorMsg deviceToDeviceActorMsg) {
        TenantId tenantId = deviceToDeviceActorMsg.getTenantId();
        ActorRef tenantActor = getOrCreateTenantActor(tenantId);
        if (deviceToDeviceActorMsg.getPayload().getMsgType().requiresRulesProcessing()) {
//            tenantActor.tell(new RuleChainDeviceMsg(deviceToDeviceActorMsg, ruleManager.getRuleChain(this.context())), context().self());
        } else {
            tenantActor.tell(deviceToDeviceActorMsg, context().self());
        }
    }

    private ActorRef getOrCreateTenantActor(TenantId tenantId) {
        return tenantActors.computeIfAbsent(tenantId, k -> context().actorOf(Props.create(new TenantActor.ActorCreator(systemContext, tenantId))
                .withDispatcher(DefaultActorService.CORE_DISPATCHER_NAME), tenantId.toString()));
    }

    private void processTermination(Terminated message) {
        ActorRef terminated = message.actor();
        if (terminated instanceof LocalActorRef) {
            logger.debug("Removed actor: {}", terminated);
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
        public AppActor create() throws Exception {
            return new AppActor(context);
        }
    }

    private final SupervisorStrategy strategy = new OneForOneStrategy(3, Duration.create("1 minute"), new Function<Throwable, Directive>() {
        @Override
        public Directive apply(Throwable t) {
            logger.error(t, "Unknown failure");
            if (t instanceof RuntimeException) {
                return SupervisorStrategy.restart();
            } else {
                return SupervisorStrategy.stop();
            }
        }
    });
}
