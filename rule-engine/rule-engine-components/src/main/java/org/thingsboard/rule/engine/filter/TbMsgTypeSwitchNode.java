/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.rule.engine.filter;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;

@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "message type switch",
        configClazz = EmptyNodeConfiguration.class,
        relationTypes = {"Post attributes", "Post telemetry", "RPC Request from Device", "RPC Request to Device", "RPC Queued", "RPC Sent", "RPC Delivered", "RPC Successful", "RPC Timeout", "RPC Expired", "RPC Failed", "RPC Deleted",
                "Activity Event", "Inactivity Event", "Connect Event", "Disconnect Event", "Entity Created", "Entity Updated", "Entity Deleted", "Entity Assigned",
                "Entity Unassigned", "Attributes Updated", "Attributes Deleted", "Alarm Acknowledged", "Alarm Cleared", "Alarm Assigned", "Alarm Unassigned", "Comment Created", "Comment Updated", "Other", "Entity Assigned From Tenant", "Entity Assigned To Tenant",
                "Relation Added or Updated", "Relation Deleted", "All Relations Deleted", "Timeseries Updated", "Timeseries Deleted"},
        nodeDescription = "Route incoming messages by Message Type",
        nodeDetails = "Sends messages with message types <b>\"Post attributes\", \"Post telemetry\", \"RPC Request\"</b> etc. via corresponding chain, otherwise <b>Other</b> chain is used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig")
public class TbMsgTypeSwitchNode implements TbNode {

    EmptyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        String relationType;
        if (msg.getType().equals(SessionMsgType.POST_ATTRIBUTES_REQUEST.name())) {
            relationType = "Post attributes";
        } else if (msg.getType().equals(SessionMsgType.POST_TELEMETRY_REQUEST.name())) {
            relationType = "Post telemetry";
        } else if (msg.getType().equals(SessionMsgType.TO_SERVER_RPC_REQUEST.name())) {
            relationType = "RPC Request from Device";
        } else if (msg.getType().equals(DataConstants.ACTIVITY_EVENT)) {
            relationType = "Activity Event";
        } else if (msg.getType().equals(DataConstants.INACTIVITY_EVENT)) {
            relationType = "Inactivity Event";
        } else if (msg.getType().equals(DataConstants.CONNECT_EVENT)) {
            relationType = "Connect Event";
        } else if (msg.getType().equals(DataConstants.DISCONNECT_EVENT)) {
            relationType = "Disconnect Event";
        } else if (msg.getType().equals(DataConstants.ENTITY_CREATED)) {
            relationType = "Entity Created";
        } else if (msg.getType().equals(DataConstants.ENTITY_UPDATED)) {
            relationType = "Entity Updated";
        } else if (msg.getType().equals(DataConstants.ENTITY_DELETED)) {
            relationType = "Entity Deleted";
        } else if (msg.getType().equals(DataConstants.ENTITY_ASSIGNED)) {
            relationType = "Entity Assigned";
        } else if (msg.getType().equals(DataConstants.ENTITY_UNASSIGNED)) {
            relationType = "Entity Unassigned";
        } else if (msg.getType().equals(DataConstants.ATTRIBUTES_UPDATED)) {
            relationType = "Attributes Updated";
        } else if (msg.getType().equals(DataConstants.ATTRIBUTES_DELETED)) {
            relationType = "Attributes Deleted";
        } else if (msg.getType().equals(DataConstants.ALARM_ACK)) {
            relationType = "Alarm Acknowledged";
        } else if (msg.getType().equals(DataConstants.ALARM_CLEAR)) {
            relationType = "Alarm Cleared";
        } else if (msg.getType().equals(DataConstants.ALARM_ASSIGN)) {
            relationType = "Alarm Assigned";
        } else if (msg.getType().equals(DataConstants.ALARM_UNASSIGN)) {
            relationType = "Alarm Unassigned";
        } else if (msg.getType().equals(DataConstants.COMMENT_CREATED)) {
            relationType = "Comment Created";
        } else if (msg.getType().equals(DataConstants.COMMENT_UPDATED)) {
            relationType = "Comment Updated";
        } else if (msg.getType().equals(DataConstants.RPC_CALL_FROM_SERVER_TO_DEVICE)) {
            relationType = "RPC Request to Device";
        } else if (msg.getType().equals(DataConstants.ENTITY_ASSIGNED_FROM_TENANT)) {
            relationType = "Entity Assigned From Tenant";
        } else if (msg.getType().equals(DataConstants.ENTITY_ASSIGNED_TO_TENANT)) {
            relationType = "Entity Assigned To Tenant";
        } else if (msg.getType().equals(DataConstants.TIMESERIES_UPDATED)) {
            relationType = "Timeseries Updated";
        } else if (msg.getType().equals(DataConstants.TIMESERIES_DELETED)) {
            relationType = "Timeseries Deleted";
        } else if (msg.getType().equals(DataConstants.RPC_QUEUED)) {
            relationType = "RPC Queued";
        } else if (msg.getType().equals(DataConstants.RPC_SENT)) {
            relationType = "RPC Sent";
        } else if (msg.getType().equals(DataConstants.RPC_DELIVERED)) {
            relationType = "RPC Delivered";
        } else if (msg.getType().equals(DataConstants.RPC_SUCCESSFUL)) {
            relationType = "RPC Successful";
        } else if (msg.getType().equals(DataConstants.RPC_TIMEOUT)) {
            relationType = "RPC Timeout";
        } else if (msg.getType().equals(DataConstants.RPC_EXPIRED)) {
            relationType = "RPC Expired";
        } else if (msg.getType().equals(DataConstants.RPC_FAILED)) {
            relationType = "RPC Failed";
        } else if (msg.getType().equals(DataConstants.RPC_DELETED)) {
            relationType = "RPC Deleted";
        } else if (msg.getType().equals(DataConstants.RELATION_ADD_OR_UPDATE)) {
            relationType = "Relation Added or Updated";
        } else if (msg.getType().equals(DataConstants.RELATION_DELETED)) {
            relationType = "Relation Deleted";
        } else if (msg.getType().equals(DataConstants.RELATIONS_DELETED)) {
            relationType = "All Relations Deleted";
        } else {
            relationType = "Other";
        }
        ctx.tellNext(msg, relationType);
    }

}
