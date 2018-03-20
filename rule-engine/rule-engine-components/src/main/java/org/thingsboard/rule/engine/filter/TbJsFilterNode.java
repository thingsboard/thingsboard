package org.thingsboard.rule.engine.filter;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.js.NashornJsEngine;
import org.thingsboard.server.common.msg.TbMsg;

import javax.script.Bindings;

import static org.thingsboard.rule.engine.DonAsynchron.withCallback;

@Slf4j
public class TbJsFilterNode implements TbNode {

    private TbJsFilterNodeConfiguration config;
    private NashornJsEngine jsEngine;

    @Override
    public void init(TbNodeConfiguration configuration, TbNodeState state) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbJsFilterNodeConfiguration.class);
        this.jsEngine = new NashornJsEngine(config.getJsScript());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ListeningExecutor jsExecutor = ctx.getJsExecutor();
        withCallback(jsExecutor.executeAsync(() -> jsEngine.executeFilter(toBindings(msg))),
                result -> processFilter(ctx, msg, result),
                t -> ctx.tellError(msg, t));
    }

    private void processFilter(TbContext ctx, TbMsg msg, Boolean filterResult) {
        if (filterResult) {
            ctx.tellNext(msg);
        } else {
            log.debug("Msg filtered out {}", msg.getId());
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
