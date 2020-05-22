/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.rule.engine.edge;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "push to edge",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Pushes messages to edge",
        nodeDetails = "Pushes messages to edge, if Message Originator assigned to particular edge or is EDGE entity. This node is used only on Cloud instances to push messages from Cloud to Edge. Supports only DEVICE, ENTITY_VIEW, ASSET and EDGE Message Originator(s).",
        uiResources = {"static/rulenode/rulenode-core-config.js", "static/rulenode/rulenode-core-config.css"},
        configDirective = "tbNodeEmptyConfig",
        icon = "cloud_download"
)
public class TbMsgPushToEdgeNode implements TbNode {

    private static final String CLOUD_MSG_SOURCE = "cloud";
    private static final String EDGE_MSG_SOURCE = "edge";
    private static final String MSG_SOURCE_KEY = "source";
    private static final String TS_METADATA_KEY = "ts";

    private EmptyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (EDGE_MSG_SOURCE.equalsIgnoreCase(msg.getMetaData().getValue(MSG_SOURCE_KEY))) {
            ctx.tellSuccess(msg);
            return;
        }
        if (msg.getType().equals(SessionMsgType.POST_TELEMETRY_REQUEST.name())) {
            msg.getMetaData().putValue(TS_METADATA_KEY, Long.toString(System.currentTimeMillis()));
        }
        msg.getMetaData().putValue(MSG_SOURCE_KEY, CLOUD_MSG_SOURCE);
        ctx.getEdgeService().pushEventToEdge(ctx.getTenantId(), msg, new PushToEdgeNodeCallback(ctx, msg));
    }

    @Override
    public void destroy() {
    }

}
