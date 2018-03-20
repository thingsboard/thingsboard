package org.thingsboard.server.actors.ruleChain;

import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.dao.attributes.AttributesService;

/**
 * Created by ashvayka on 19.03.18.
 */
class DefaultTbContext implements TbContext {

    private final ActorSystemContext mainCtx;
    private final RuleNodeCtx nodeCtx;

    public DefaultTbContext(ActorSystemContext mainCtx, RuleNodeCtx nodeCtx) {
        this.mainCtx = mainCtx;
        this.nodeCtx = nodeCtx;
    }

    @Override
    public void tellNext(TbMsg msg) {
        tellNext(msg, null);
    }

    @Override
    public void tellNext(TbMsg msg, String relationType) {
        nodeCtx.getChainActor().tell(new RuleNodeToRuleChainTellNextMsg(nodeCtx.getSelfId(), relationType, msg), nodeCtx.getSelf());
    }

    @Override
    public void tellSelf(TbMsg msg, long delayMs) {

    }

    @Override
    public void tellOthers(TbMsg msg) {

    }

    @Override
    public void tellSibling(TbMsg msg, ServerAddress address) {

    }

    @Override
    public void spawn(TbMsg msg) {

    }

    @Override
    public void ack(TbMsg msg) {

    }

    @Override
    public AttributesService getAttributesService() {
        return mainCtx.getAttributesService();
    }
}
