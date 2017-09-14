package org.thingsboard.server.extensions.core.action.rpc;

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.extensions.api.plugins.msg.AbstractRuleToPluginMsg;

/**
 * Created by ashvayka on 14.09.17.
 */
public class ServerSideRpcCallRuleToPluginActionMsg extends AbstractRuleToPluginMsg<ServerSideRpcCallActionMsg> {

    public ServerSideRpcCallRuleToPluginActionMsg(TenantId tenantId, CustomerId customerId, DeviceId deviceId,
                                         ServerSideRpcCallActionMsg payload) {
        super(tenantId, customerId, deviceId, payload);
    }
}
