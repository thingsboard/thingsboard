// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.integration;

import com.google.common.util.concurrent.FutureCallback;
import jakarta.annotation.Nullable;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@RuleNode(
        type = ComponentType.ACTION,
        name = "integration downlink",
        configClazz = TbIntegrationDownlinkConfiguration.class,
        nodeDescription = "Pushes downlink message to selected integration",
        nodeDetails = "Will push downlink message to the selected integration queue.",
        configDirective = "tbActionNodeIntegrationDownlinkConfig",
        icon = "input",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/integration-downlink/"
)
public class TbIntegrationDownlinkNode implements TbNode {

    private IntegrationId integrationId;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbIntegrationDownlinkConfiguration.class);
        if (config.getIntegrationId() == null) {
            throw new TbNodeException("Integration id is not set in the rule node configuration!");
        }
        integrationId = new IntegrationId(config.getIntegrationId());
        Integration integration = ctx.getPeContext().getIntegrationService().findIntegrationById(ctx.getTenantId(), integrationId);
        if (integration == null) {
            throw new TbNodeException("Integration with ID [" + integrationId + "] not found!");
        } else if (!integration.getTenantId().equals(ctx.getTenantId())) {
            throw new TbNodeException("Integration with ID [" + integrationId + "] belongs to different tenant!");
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (integrationId != null) {
            ctx.getPeContext().pushToIntegration(integrationId, msg, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Void tmp) {
                    ctx.tellNext(msg, TbNodeConnectionType.SUCCESS);
                }

                @Override
                public void onFailure(Throwable t) {
                    ctx.tellFailure(msg, t);
                }
            });
        } else {
            ctx.tellNext(msg, TbNodeConnectionType.FAILURE);
        }
    }

    @Override
    public void destroy() {
        integrationId = null;
    }

}
