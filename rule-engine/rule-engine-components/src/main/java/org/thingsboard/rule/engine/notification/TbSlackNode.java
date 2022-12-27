/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.notification.template.SlackConversation;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.concurrent.ExecutionException;

@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "send to Slack",
        configClazz = TbSlackNodeConfiguration.class,
        nodeDescription = "Send message to a Slack channel or user",
        nodeDetails = "",
        uiResources = {"static/rulenode/rulenode-core-config.js"}
)
public class TbSlackNode implements TbNode {

    private TbSlackNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbSlackNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        String token;
        if (config.isUseDefaultNotificationSettings()) {
            token = ctx.getSlackService().getToken(ctx.getTenantId());
        } else {
            token = config.getBotToken();
        }
        if (token == null) {
            throw new IllegalArgumentException("Slack token is missing");
        }
        String message = TbNodeUtils.processPattern(config.getMessageTemplate(), msg);

        ListenableFuture<?> result;
        if (StringUtils.isNotEmpty(config.getConversationId())) {
            result = ctx.getExternalCallExecutor().executeAsync(() -> {
                ctx.getSlackService().sendMessage(ctx.getTenantId(), token, config.getConversationId(), message);
            });
        } else {
            result = ctx.getExternalCallExecutor().executeAsync(() -> {
                SlackConversation conversation = ctx.getSlackService().findConversation(ctx.getTenantId(), token, config.getConversationType(), config.getConversationNamePattern());
                if (conversation == null) {
                    throw new IllegalArgumentException("Couldn't find conversation by name pattern");
                }
                ctx.getSlackService().sendMessage(ctx.getTenantId(), token, conversation.getId(), message);
            });
        }

        DonAsynchron.withCallback(result, r -> {
                    ctx.tellSuccess(msg);
                },
                e -> {
                    ctx.tellFailure(msg, e);
                });
    }

}
