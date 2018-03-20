package org.thingsboard.rule.engine.filter;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.js.NashornJsEngine;
import org.thingsboard.server.common.msg.TbMsg;

import javax.script.Bindings;

import static org.thingsboard.rule.engine.DonAsynchron.withCallback;

@Slf4j
public class TbJsSwitchNode implements TbNode {

    private TbJsSwitchNodeConfiguration config;
    private NashornJsEngine jsEngine;

    @Override
    public void init(TbNodeConfiguration configuration, TbNodeState state) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbJsSwitchNodeConfiguration.class);
        this.jsEngine = new NashornJsEngine(config.getJsScript());
        if (config.getAllowedRelations().size() < 1) {
            String message = "Switch node should have at least 1 relation";
            log.error(message);
            throw new IllegalStateException(message);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ListeningExecutor jsExecutor = ctx.getJsExecutor();
        withCallback(jsExecutor.executeAsync(() -> jsEngine.executeSwitch(toBindings(msg))),
                result -> processSwitch(ctx, msg, result),
                t -> ctx.tellError(msg, t));
    }

    private void processSwitch(TbContext ctx, TbMsg msg, String nextRelation) {
        if (config.getAllowedRelations().contains(nextRelation)) {
            ctx.tellNext(msg, nextRelation);
        } else {
            ctx.tellError(msg, new IllegalStateException("Unsupported relation for switch " + nextRelation));
        }
    }

    private Bindings toBindings(TbMsg msg) {
        return NashornJsEngine.bindMsg(msg);
    }

    @Override
    public void destroy() {
        if (jsEngine != null) {
            jsEngine.destroy();
        }
    }
}
