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
package org.thingsboard.rule.engine.profile;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.common.data.DataConstants.ACTIVITY_EVENT;
import static org.thingsboard.server.common.data.DataConstants.ALARM_ACK;
import static org.thingsboard.server.common.data.DataConstants.ALARM_CLEAR;
import static org.thingsboard.server.common.data.DataConstants.ALARM_DELETE;
import static org.thingsboard.server.common.data.DataConstants.ATTRIBUTES_DELETED;
import static org.thingsboard.server.common.data.DataConstants.ATTRIBUTES_UPDATED;
import static org.thingsboard.server.common.data.DataConstants.ENTITY_ASSIGNED;
import static org.thingsboard.server.common.data.DataConstants.ENTITY_DELETED;
import static org.thingsboard.server.common.data.DataConstants.ENTITY_UNASSIGNED;
import static org.thingsboard.server.common.data.DataConstants.ENTITY_UPDATED;
import static org.thingsboard.server.common.data.DataConstants.INACTIVITY_EVENT;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "device profile",
        customRelations = true,
        relationTypes = {"Alarm Created", "Alarm Updated", "Alarm Severity Updated", "Alarm Cleared", "Success", "Failure"},
        configClazz = TbDeviceProfileNodeConfiguration.class,
        nodeDescription = "Process device messages based on device profile settings",
        nodeDetails = "Create and clear alarms based on alarm rules defined in device profile. The output relation type is either " +
                "'Alarm Created', 'Alarm Updated', 'Alarm Severity Updated' and 'Alarm Cleared' or simply 'Success' if no alarms were affected.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbDeviceProfileConfig"
)
public class TbDeviceProfileNode implements TbNode {

    private TbDeviceProfileNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbDeviceProfileNodeConfiguration.class);

    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        try {
            switch (msg.getType()) {
                case "POST_TELEMETRY_REQUEST":
                case "POST_ATTRIBUTES_REQUEST":
                case ACTIVITY_EVENT:
                case INACTIVITY_EVENT:
                case ENTITY_ASSIGNED:
                case ENTITY_UNASSIGNED:
                    ctx.getAlarmRuleStateService().process(ctx, msg);
                    break;
            }
            ctx.tellSuccess(msg);
        } catch (Exception e) {
            ctx.tellFailure(msg, e);
        }
    }
}
