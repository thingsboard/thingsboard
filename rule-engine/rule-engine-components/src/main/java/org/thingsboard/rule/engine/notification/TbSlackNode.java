/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.rule.engine.notification;

import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.concurrent.ExecutionException;

@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "send to slack",
        configClazz = TbSlackNodeConfiguration.class,
        nodeDescription = "Send message via Slack",
        nodeDetails = "Sends message to a Slack channel or user",
        configDirective = "tbExternalNodeSlackConfig",
        iconUrl = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTYsMTVBMiwyIDAgMCwxIDQsMTdBMiwyIDAgMCwxIDIsMTVBMiwyIDAgMCwxIDQsMTNINlYxNU03LDE1QTIsMiAwIDAsMSA5LDEzQTIsMiAwIDAsMSAxMSwxNVYyMEEyLDIgMCAwLDEgOSwyMkEyLDIgMCAwLDEgNywyMFYxNU05LDdBMiwyIDAgMCwxIDcsNUEyLDIgMCAwLDEgOSwzQTIsMiAwIDAsMSAxMSw1VjdIOU05LDhBMiwyIDAgMCwxIDExLDEwQTIsMiAwIDAsMSA5LDEySDRBMiwyIDAgMCwxIDIsMTBBMiwyIDAgMCwxIDQsOEg5TTE3LDEwQTIsMiAwIDAsMSAxOSw4QTIsMiAwIDAsMSAyMSwxMEEyLDIgMCAwLDEgMTksMTJIMTdWMTBNMTYsMTBBMiwyIDAgMCwxIDE0LDEyQTIsMiAwIDAsMSAxMiwxMFY1QTIsMiAwIDAsMSAxNCwzQTIsMiAwIDAsMSAxNiw1VjEwTTE0LDE4QTIsMiAwIDAsMSAxNiwyMEEyLDIgMCAwLDEgMTQsMjJBMiwyIDAgMCwxIDEyLDIwVjE4SDE0TTE0LDE3QTIsMiAwIDAsMSAxMiwxNUEyLDIgMCAwLDEgMTQsMTNIMTlBMiwyIDAgMCwxIDIxLDE1QTIsMiAwIDAsMSAxOSwxN0gxNFoiIC8+PC9zdmc+",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/external/send-to-slack/"
)
public class TbSlackNode extends TbAbstractExternalNode {

    private TbSlackNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        config = TbNodeUtils.convert(configuration, TbSlackNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        String token;
        if (config.isUseSystemSettings()) {
            token = ctx.getSlackService().getToken(ctx.getTenantId());
        } else {
            token = config.getBotToken();
        }
        if (token == null) {
            throw new IllegalArgumentException("Slack token is missing");
        }

        String message = TbNodeUtils.processPattern(config.getMessageTemplate(), msg);
        var tbMsg = ackIfNeeded(ctx, msg);
        DonAsynchron.withCallback(ctx.getExternalCallExecutor().executeAsync(() -> {
                    ctx.getSlackService().sendMessage(ctx.getTenantId(), token, config.getConversation().getId(), message);
                }),
                r -> tellSuccess(ctx, tbMsg),
                e -> tellFailure(ctx, tbMsg, e));
    }

}
