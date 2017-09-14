package org.thingsboard.server.extensions.core.plugin.rpc.handlers;

import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ToDeviceRpcRequest;
import org.thingsboard.server.extensions.api.rules.RuleException;
import org.thingsboard.server.extensions.core.action.rpc.ServerSideRpcCallActionMsg;

/**
 * Created by ashvayka on 14.09.17.
 */
public class RpcRuleMsgHandler implements RuleMsgHandler {

    @Override
    public void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) throws RuleException {
        if (msg instanceof ServerSideRpcCallActionMsg) {
            handle(ctx, tenantId, ruleId, (ServerSideRpcCallActionMsg) msg);
        } else {
            throw new RuntimeException("Not supported msg: " + msg + "!");
        }
    }

    private void handle(PluginContext ctx, TenantId tenantId, RuleId ruleId, ServerSideRpcCallActionMsg msg) {
//        TODO: implement
//        ToDeviceRpcRequest request = new ToDeviceRpcRequest();
//        ctx.sendRpcRequest(request);
    }
}
