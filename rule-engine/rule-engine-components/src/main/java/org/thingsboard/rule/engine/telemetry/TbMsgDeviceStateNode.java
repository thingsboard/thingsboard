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
package org.thingsboard.rule.engine.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "device state",
        configClazz = TbMsgDeviceStateNodeConfiguration.class,
        nodeDescription = "Initializing device connectivity events",
        nodeDetails = "Triggering the device connectivity events that are pushed to the <b>Rule Engine</b>. " +
                "If msg originator is not a device, incoming message routes via <code>Failure</code> chain, " +
                "otherwise <code>Success</code> chain is used." +
                "<br/><br/>" +
                "Supported events are:" +
                "<br/>  - <b>Connect event<b>" +
                "<br/>  - <b>Disconnect event<b>" +
                "<br/>  - <b>Activity event<b>" +
                "<br/>  - <b>Inactivity event<b>",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeDeviceStateConfig"
)
public class TbMsgDeviceStateNode implements TbNode {

    private TbMsgDeviceStateNodeConfiguration config;
    private String event;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDeviceStateNodeConfiguration.class);
        this.event = config.getEvent();
        checkConfig();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (!msg.getOriginator().getEntityType().equals(EntityType.DEVICE)) {
            throw new TbNodeException("Unsupported entity type: " + msg.getOriginator().getEntityType());
        }
        if (event.equals(DataConstants.ACTIVITY_EVENT)) {
            ctx.getRuleEngineDeviceStateService().onDeviceActivity(ctx.getTenantId(), (DeviceId) msg.getOriginator(), System.currentTimeMillis());
        } else if (event.equals(DataConstants.CONNECT_EVENT)) {
            ctx.getRuleEngineDeviceStateService().onDeviceConnect(ctx.getTenantId(), (DeviceId) msg.getOriginator());
        } else if (event.equals(DataConstants.INACTIVITY_EVENT)) {
            ctx.getRuleEngineDeviceStateService().onDeviceInactivity(ctx.getTenantId(), (DeviceId) msg.getOriginator());
        } else if (event.equals(DataConstants.DISCONNECT_EVENT)) {
            ctx.getRuleEngineDeviceStateService().onDeviceDisconnect(ctx.getTenantId(), (DeviceId) msg.getOriginator());
        }
        ctx.tellSuccess(msg);
    }

    private void checkConfig() {
        List<String> avaliableEvents = List.of(
                DataConstants.CONNECT_EVENT,
                DataConstants.INACTIVITY_EVENT,
                DataConstants.ACTIVITY_EVENT,
                DataConstants.DISCONNECT_EVENT
        );
        if (!avaliableEvents.contains(this.event)) {
            throw new IllegalArgumentException("Unsupported event: " + this.event);
        }
    }

}
