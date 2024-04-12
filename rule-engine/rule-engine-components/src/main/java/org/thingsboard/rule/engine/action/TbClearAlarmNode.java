/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeClearAlarmConfig",
        icon = "notifications_off"
)
public class TbClearAlarmNode extends TbAbstractAlarmNode<TbClearAlarmNodeConfiguration> {

    @Override
    protected TbClearAlarmNodeConfiguration loadAlarmNodeConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbClearAlarmNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<TbAlarmResult> processAlarm(TbContext ctx, TbMsg msg) {
        var originator = msg.getOriginator();
        ListenableFuture<Alarm> alarmFuture;
        if (originator.getEntityType().equals(EntityType.ALARM)) {
            alarmFuture = ctx.getAlarmService().findAlarmByIdAsync(ctx.getTenantId(), new AlarmId(originator.getId()));
        } else {
            String alarmType = TbNodeUtils.processPattern(config.getAlarmType(), msg);
            alarmFuture = ctx.getDbCallbackExecutor().submit(() -> ctx.getAlarmService().findLatestActiveByOriginatorAndType(ctx.getTenantId(), originator, alarmType));
        }
        return Futures.transformAsync(alarmFuture, alarm -> {
            if (alarm != null && !alarm.isCleared()) {
                return clearAlarm(ctx, msg, alarm);
            }
            return Futures.immediateFuture(new TbAlarmResult(false, false, false, null));
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<TbAlarmResult> clearAlarm(TbContext ctx, TbMsg msg, Alarm alarm) {
        ListenableFuture<JsonNode> asyncDetails = buildAlarmDetails(ctx, msg, alarm.getDetails());
        return Futures.transform(asyncDetails, details -> {
            AlarmApiCallResult result = ctx.getAlarmService().clearAlarm(ctx.getTenantId(), alarm.getId(), currentTimeMillis(), details);
            if (result.isSuccessful()) {
                return new TbAlarmResult(false, false, result.isCleared(), result.getAlarm());
            }
            throw new RuntimeException("Failed to clear alarm: API returned unsuccessful result. Probably alarm was already deleted.");
        }, ctx.getDbCallbackExecutor());
    }
}
