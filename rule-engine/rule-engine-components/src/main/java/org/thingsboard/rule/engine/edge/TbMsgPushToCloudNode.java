// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.edge;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.UUID;

@RuleNode(
        type = ComponentType.ACTION,
        name = "push to cloud",
        configClazz = TbMsgPushToCloudNodeConfiguration.class,
        nodeDescription = "Pushes messages from edge to cloud",
        nodeDetails = "Push messages from edge to cloud. " +
                "This node used only on edge to push messages from edge to cloud. " +
                "Once message arrived into this node it’s going to be converted into cloud event and saved to the local database. " +
                "Node doesn't push messages directly to cloud, but stores event(s) in the cloud queue. " +
                "Supports next message types:" +
                "<br><code>POST_TELEMETRY_REQUEST</code>" +
                "<br><code>POST_ATTRIBUTES_REQUEST</code>" +
                "<br><code>ATTRIBUTES_UPDATED</code>" +
                "<br><code>ATTRIBUTES_DELETED</code>" +
                "<br><code>ALARM</code><br><br>" +
                "Message will be routed via <b>Failure</b> route if node was not able to save cloud event to database or unsupported originator type/message type arrived. " +
                "In case successful storage cloud event to database message will be routed via <b>Success</b> route.",
        configDirective = "tbActionNodePushToCloudConfig",
        icon = "cloud_upload",
        ruleChainTypes = RuleChainType.EDGE,
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/push-to-cloud/"
)
public class TbMsgPushToCloudNode extends AbstractTbMsgPushNode<TbMsgPushToCloudNodeConfiguration, Object, Object> {

    // Implementation of this node is done on the Edge

    @Override
    Object buildEvent(TenantId tenantId, EdgeEventActionType eventAction, UUID entityId, Object eventType, JsonNode entityBody) {
        return null;
    }

    @Override
    Object getEventTypeByEntityType(EntityType entityType) {
        return null;
    }

    @Override
    Object getAlarmEventType() {
        return null;
    }

    @Override
    String getIgnoredMessageSource() {
        return null;
    }

    @Override
    protected Class<TbMsgPushToCloudNodeConfiguration> getConfigClazz() {
        return TbMsgPushToCloudNodeConfiguration.class;
    }

    @Override
    void processMsg(TbContext ctx, TbMsg msg) {}

}
