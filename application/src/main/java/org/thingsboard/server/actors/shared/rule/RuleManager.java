/**
 * Copyright © 2016 The Thingsboard Authors
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
package org.thingsboard.server.actors.shared.rule;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.rule.RuleActor;
import org.thingsboard.server.actors.rule.RuleActorChain;
import org.thingsboard.server.actors.rule.RuleActorMetaData;
import org.thingsboard.server.actors.rule.SimpleRuleActorChain;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageDataIterable.FetchFunction;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.dao.rule.RuleService;

import java.util.*;

@Slf4j
public abstract class RuleManager {

    protected final ActorSystemContext systemContext;
    protected final RuleService ruleService;
    protected final Map<RuleId, ActorRef> ruleActors;
    protected final TenantId tenantId;

    Map<RuleMetaData, RuleActorMetaData> ruleMap = new HashMap<>();
    private RuleActorChain ruleChain;

    public RuleManager(ActorSystemContext systemContext, TenantId tenantId) {
        this.systemContext = systemContext;
        this.ruleService = systemContext.getRuleService();
        this.ruleActors = new HashMap<>();
        this.tenantId = tenantId;
    }

    public void init(ActorContext context) {
        PageDataIterable<RuleMetaData> ruleIterator = new PageDataIterable<>(getFetchRulesFunction(),
                ContextAwareActor.ENTITY_PACK_LIMIT);
        ruleMap = new HashMap<>();

        for (RuleMetaData rule : ruleIterator) {
            log.debug("[{}] Creating rule actor {}", rule.getId(), rule);
            ActorRef ref = getOrCreateRuleActor(context, rule.getId());
            RuleActorMetaData actorMd = RuleActorMetaData.systemRule(rule.getId(), rule.getWeight(), ref);
            ruleMap.put(rule, actorMd);
            log.debug("[{}] Rule actor created.", rule.getId());
        }

        refreshRuleChain();
    }

    public Optional<ActorRef> update(ActorContext context, RuleId ruleId, ComponentLifecycleEvent event) {
        RuleMetaData rule = null;
        if (event != ComponentLifecycleEvent.DELETED) {
            rule = systemContext.getRuleService().findRuleById(ruleId);
        }
        if (rule == null) {
            rule = ruleMap.keySet().stream()
                    .filter(r -> r.getId().equals(ruleId))
                    .peek(r -> r.setState(ComponentLifecycleState.SUSPENDED))
                    .findFirst()
                    .orElse(null);
        }
        if (rule != null) {
            RuleActorMetaData actorMd = ruleMap.get(rule);
            if (actorMd == null) {
                ActorRef ref = getOrCreateRuleActor(context, rule.getId());
                actorMd = RuleActorMetaData.systemRule(rule.getId(), rule.getWeight(), ref);
                ruleMap.put(rule, actorMd);
            }
            refreshRuleChain();
            return Optional.of(actorMd.getActorRef());
        } else {
            log.warn("[{}] Can't process unknown rule!", ruleId);
            return Optional.empty();
        }
    }

    abstract FetchFunction<RuleMetaData> getFetchRulesFunction();

    public ActorRef getOrCreateRuleActor(ActorContext context, RuleId ruleId) {
        return ruleActors.computeIfAbsent(ruleId, rId ->
                context.actorOf(Props.create(new RuleActor.ActorCreator(systemContext, tenantId, rId))
                        .withDispatcher(DefaultActorService.RULE_DISPATCHER_NAME), rId.toString()));
    }

    public RuleActorChain getRuleChain() {
        return ruleChain;
    }


    private void refreshRuleChain() {
        Set<RuleActorMetaData> activeRuleSet = new HashSet<>();
        for (Map.Entry<RuleMetaData, RuleActorMetaData> rule : ruleMap.entrySet()) {
            if (rule.getKey().getState() == ComponentLifecycleState.ACTIVE) {
                activeRuleSet.add(rule.getValue());
            }
        }
        ruleChain = new SimpleRuleActorChain(activeRuleSet);
    }
}
