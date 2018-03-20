package org.thingsboard.server.actors.ruleChain;

import akka.actor.ActorRef;
import lombok.Data;
import org.thingsboard.server.common.data.id.RuleNodeId;

/**
 * Created by ashvayka on 19.03.18.
 */
@Data
final class RuleNodeCtx {
    private final ActorRef chainActor;
    private final ActorRef self;
    private final RuleNodeId selfId;
}
