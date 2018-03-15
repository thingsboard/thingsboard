package org.thingsboard.server.actors.ruleChain;

import akka.actor.ActorRef;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.actors.shared.plugin.PluginManager;
import org.thingsboard.server.actors.shared.rulechain.RuleChainManager;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.RuleChainId;

/**
 * Created by ashvayka on 15.03.18.
 */
public abstract class RuleChainManagerActor extends ContextAwareActor {

    protected final RuleChainManager ruleChainManager;
    protected final PluginManager pluginManager;

    public RuleChainManagerActor(ActorSystemContext systemContext, RuleChainManager ruleChainManager, PluginManager pluginManager) {
        super(systemContext);
        this.ruleChainManager = ruleChainManager;
        this.pluginManager = pluginManager;
    }

    protected void initRuleChains() {
        pluginManager.init(this.context());
        ruleChainManager.init(this.context());
    }

    protected ActorRef getEntityActorRef(EntityId entityId) {
        ActorRef target = null;
        switch (entityId.getEntityType()) {
            case PLUGIN:
                target = pluginManager.getOrCreateActor(this.context(), (PluginId) entityId);
                break;
            case RULE_CHAIN:
                target = ruleChainManager.getOrCreateActor(this.context(), (RuleChainId) entityId);
                break;
        }
        return target;
    }
}
