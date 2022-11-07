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

import org.apache.commons.lang3.StringUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.notification.NotificationOriginatorType;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.concurrent.ExecutionException;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@RuleNode(
        type = ComponentType.ACTION,
        name = "send notification",
        configClazz = TbNotificationNodeConfiguration.class,
        nodeDescription = "Sends notification to a target",
        nodeDetails = "Will send notification to the specified target",
        uiResources = {"static/rulenode/rulenode-core-config.js"}
)
public class TbNotificationNode implements TbNode {

    private TbNotificationNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbNotificationNodeConfiguration.class);
        validateConfig(config);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .tenantId(ctx.getTenantId())
                .targetId(new NotificationTargetId(config.getTargetId()))
                .notificationReason(config.getNotificationReason())
                .textTemplate(TbNodeUtils.processPattern(config.getNotificationTextTemplate(), msg))
                .notificationSeverity(config.getNotificationSeverity())
                .originatorType(NotificationOriginatorType.RULE_NODE)
                .originatorEntityId(ctx.getTenantId())
                .build();
        withCallback(ctx.getDbCallbackExecutor().executeAsync(() -> {
                    return ctx.getNotificationManager().processNotificationRequest(ctx.getTenantId(), notificationRequest);
                }),
                r -> {
                    TbMsgMetaData msgMetaData = msg.getMetaData().copy();
                    msgMetaData.putValue("notificationRequestId", r.getUuidId().toString());
                    msgMetaData.putValue("notificationTextTemplate", r.getTextTemplate());
                    ctx.tellSuccess(TbMsg.transformMsg(msg, msgMetaData));
                },
                e -> ctx.tellFailure(msg, e));
    }

    private void validateConfig(TbNotificationNodeConfiguration config) throws TbNodeException {
        if (config.getTargetId() == null) {
            throw new TbNodeException("Notification target is not specified");
        }
        if (StringUtils.isBlank(config.getNotificationTextTemplate())) {
            throw new TbNodeException("Notification text template is missing");
        }
    }

}
