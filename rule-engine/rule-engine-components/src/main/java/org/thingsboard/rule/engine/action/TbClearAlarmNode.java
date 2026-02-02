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
package org.thingsboard.rule.engine.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@RuleNode(
        type = ComponentType.ACTION,
        name = "clear alarm", relationTypes = {"Cleared", "False"},
        configClazz = TbClearAlarmNodeConfiguration.class,
        nodeDescription = "Clear Alarm",
        nodeDetails =
                "Details - JS function that creates JSON object based on incoming message. This object will be added into Alarm.details field.\n" +
                        "Node output:\n" +
                        "If alarm was not cleared, original message is returned. Otherwise new Message returned with type 'ALARM', Alarm object in 'msg' property and 'metadata' will contains 'isClearedAlarm' property. " +
                        "Message payload can be accessed via <code>msg</code> property. For example <code>'temperature = ' + msg.temperature ;</code>. " +
                        "Message metadata can be accessed via <code>metadata</code> property. For example <code>'name = ' + metadata.customerName;</code>.",
        configDirective = "tbActionNodeClearAlarmConfig",
        icon = "notifications_off",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/clear-alarm/"
)
public class TbClearAlarmNode extends TbAbstractAlarmNode<TbClearAlarmNodeConfiguration> {

    @Override
    protected TbClearAlarmNodeConfiguration loadAlarmNodeConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbClearAlarmNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<TbAlarmResult> processAlarm(TbContext ctx, TbMsg msg) {
        String alarmType = TbNodeUtils.processPattern(this.config.getAlarmType(), msg);
        Alarm alarm;
        if (msg.getOriginator().getEntityType().equals(EntityType.ALARM)) {
            alarm = ctx.getAlarmService().findAlarmById(ctx.getTenantId(), new AlarmId(msg.getOriginator().getId()));
        } else {
            alarm = ctx.getAlarmService().findLatestActiveByOriginatorAndType(ctx.getTenantId(), msg.getOriginator(), alarmType);
        }
        if (alarm != null && !alarm.getStatus().isCleared()) {
            return clearAlarm(ctx, msg, alarm);
        }
        return Futures.immediateFuture(new TbAlarmResult(false, false, false, null));
    }

    private ListenableFuture<TbAlarmResult> clearAlarm(TbContext ctx, TbMsg msg, Alarm alarm) {
        ListenableFuture<JsonNode> asyncDetails = buildAlarmDetails(msg, alarm.getDetails());
        return Futures.transform(asyncDetails, details -> {
            AlarmApiCallResult result = ctx.getAlarmService().clearAlarm(ctx.getTenantId(), alarm.getId(), System.currentTimeMillis(), details);
            if (result.isSuccessful()) {
                return new TbAlarmResult(false, false, result.isCleared(), result.getAlarm());
            } else {
                return new TbAlarmResult(false, false, false, alarm);
            }
        }, ctx.getDbCallbackExecutor());
    }

}
