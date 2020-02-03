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
package org.thingsboard.rule.engine.filter;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;

@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "消息类型转换器",
        configClazz = EmptyNodeConfiguration.class,
        relationTypes = {"Post attributes", "Post telemetry", "RPC Request from Device",
                "RPC Request to Device", "Activity Event", "Inactivity Event",
                "Connect Event", "Disconnect Event", "Entity Created", "Entity Updated", "Entity Deleted", "Entity Assigned",
                "Entity Unassigned", "Attributes Updated", "Attributes Deleted", "Other"},
        nodeDescription = "根据消息类型路由传入的消息",
        nodeDetails = "通过相应的链发送消息类型为<b>\"Post attributes\", \"Post telemetry\", \"RPC Request\"</b>等的消息，否则使用<b>Other</b>链。",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig")
public class TbMsgTypeSwitchNode implements TbNode {

    EmptyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
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
        } else if (msg.getType().equals(DataConstants.RPC_CALL_FROM_SERVER_TO_DEVICE)) {
            relationType = "RPC Request to Device";
        } else {
            relationType = "Other";
        }
        ctx.tellNext(msg, relationType);
    }

    @Override
    public void destroy() {

    }
}
