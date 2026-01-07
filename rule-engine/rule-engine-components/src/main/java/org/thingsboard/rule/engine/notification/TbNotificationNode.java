/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import com.google.common.util.concurrent.FutureCallback;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.info.RuleEngineOriginatedNotificationInfo;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.concurrent.ExecutionException;

@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "send notification",
        configClazz = TbNotificationNodeConfiguration.class,
        nodeDescription = "Sends notification to targets using the template",
        nodeDetails = "Will send notification to the specified targets using the template",
        configDirective = "tbExternalNodeNotificationConfig",
        icon = "notifications",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/external/send-notification/"
)
public class TbNotificationNode extends TbAbstractExternalNode {

    private TbNotificationNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        config = TbNodeUtils.convert(configuration, TbNotificationNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        RuleEngineOriginatedNotificationInfo notificationInfo = RuleEngineOriginatedNotificationInfo.builder()
                .msgOriginator(msg.getOriginator())
                .msgCustomerId(msg.getOriginator().getEntityType() == EntityType.CUSTOMER
                        && msg.getOriginator().equals(msg.getCustomerId()) ? null : msg.getCustomerId())
                .msgMetadata(msg.getMetaData().getData())
                .msgData(JacksonUtil.toFlatMap(JacksonUtil.toJsonNode(msg.getData())))
                .msgType(msg.getType())
                .build();

        NotificationRequest notificationRequest = NotificationRequest.builder()
                .tenantId(ctx.getTenantId())
                .targets(config.getTargets())
                .templateId(config.getTemplateId())
                .info(notificationInfo)
                .additionalConfig(new NotificationRequestConfig())
                .originatorEntityId(ctx.getSelf().getRuleChainId())
                .build();

        var tbMsg = ackIfNeeded(ctx, msg);

        var callback = new FutureCallback<NotificationRequestStats>() {
            @Override
            public void onSuccess(NotificationRequestStats stats) {
                TbMsgMetaData metaData = tbMsg.getMetaData().copy();
                metaData.putValue("notificationRequestResult", JacksonUtil.toString(stats));
                tellSuccess(ctx, tbMsg.transform()
                        .metaData(metaData)
                        .build());
            }

            @Override
            public void onFailure(Throwable e) {
                tellFailure(ctx, tbMsg, e);
            }
        };

        var future = ctx.getNotificationExecutor().executeAsync(() ->
                ctx.getNotificationCenter().processNotificationRequest(ctx.getTenantId(), notificationRequest, callback));
        DonAsynchron.withCallback(future, r -> {}, callback::onFailure);
    }

}
