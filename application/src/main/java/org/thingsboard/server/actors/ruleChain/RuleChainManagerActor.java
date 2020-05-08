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
package org.thingsboard.server.actors.ruleChain;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.Getter;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.rule.RuleChainService;

import java.util.function.Function;

/**
 * Created by ashvayka on 15.03.18.
 */
public abstract class RuleChainManagerActor extends ContextAwareActor {

    protected final TenantId tenantId;
    private final RuleChainService ruleChainService;
    private final BiMap<RuleChainId, ActorRef> actors;
    @Getter
    protected RuleChain rootChain;
    @Getter
    protected ActorRef rootChainActor;

    public RuleChainManagerActor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext);
        this.tenantId = tenantId;
        this.actors = HashBiMap.create();
        this.ruleChainService = systemContext.getRuleChainService();
    }

    protected void initRuleChains() {
        for (RuleChain ruleChain : new PageDataIterable<>(link -> ruleChainService.findTenantRuleChains(tenantId, link), ContextAwareActor.ENTITY_PACK_LIMIT)) {
            RuleChainId ruleChainId = ruleChain.getId();
            log.debug("[{}|{}] Creating rule chain actor", ruleChainId.getEntityType(), ruleChain.getId());
            //TODO: remove this cast making UUIDBased subclass of EntityId an interface and vice versa.
            ActorRef actorRef = getOrCreateActor(this.context(), ruleChainId, id -> ruleChain);
            visit(ruleChain, actorRef);
            log.debug("[{}|{}] Rule Chain actor created.", ruleChainId.getEntityType(), ruleChainId.getId());
        }
    }

    protected void visit(RuleChain entity, ActorRef actorRef) {
        if (entity != null && entity.isRoot()) {
            rootChain = entity;
            rootChainActor = actorRef;
        }
    }

    public ActorRef getOrCreateActor(akka.actor.ActorContext context, RuleChainId ruleChainId) {
        return getOrCreateActor(context, ruleChainId, eId -> ruleChainService.findRuleChainById(TenantId.SYS_TENANT_ID, eId));
    }

    public ActorRef getOrCreateActor(akka.actor.ActorContext context, RuleChainId ruleChainId, Function<RuleChainId, RuleChain> provider) {
        return actors.computeIfAbsent(ruleChainId, eId -> {
            RuleChain ruleChain = provider.apply(eId);
            return context.actorOf(Props.create(new RuleChainActor.ActorCreator(systemContext, tenantId, ruleChain))
                    .withDispatcher(DefaultActorService.TENANT_RULE_DISPATCHER_NAME), eId.toString());
        });
    }

    protected ActorRef getEntityActorRef(EntityId entityId) {
        ActorRef target = null;
        if (entityId.getEntityType() == EntityType.RULE_CHAIN) {
            target = getOrCreateActor(this.context(), (RuleChainId) entityId);
        }
        return target;
    }

    protected void broadcast(Object msg) {
        actors.values().forEach(actorRef -> actorRef.tell(msg, ActorRef.noSender()));
    }

    public ActorRef get(RuleChainId id) {
        return actors.get(id);
    }

}
