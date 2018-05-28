/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "create alarm", relationTypes = {"Created", "Updated", "False"},
        configClazz = TbCreateAlarmNodeConfiguration.class,
        nodeDescription = "Create or Update Alarm",
        nodeDetails =
                "Details - JS function that creates JSON object based on incoming message. This object will be added into Alarm.details field.\n" +
                "Node output:\n" +
                "If alarm was not created, original message is returned. Otherwise new Message returned with type 'ALARM', Alarm object in 'msg' property and 'matadata' will contains one of those properties 'isNewAlarm/isExistingAlarm'. " +
                "Message payload can be accessed via <code>msg</code> property. For example <code>'temperature = ' + msg.temperature ;</code>. " +
                "Message metadata can be accessed via <code>metadata</code> property. For example <code>'name = ' + metadata.customerName;</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeCreateAlarmConfig",
        icon = "notifications_active"
)
public class TbCreateAlarmNode extends TbAbstractAlarmNode<TbCreateAlarmNodeConfiguration> {

    @Override
    protected TbCreateAlarmNodeConfiguration loadAlarmNodeConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbCreateAlarmNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<AlarmResult> processAlarm(TbContext ctx, TbMsg msg) {
        ListenableFuture<Alarm> latest = ctx.getAlarmService().findLatestByOriginatorAndType(ctx.getTenantId(), msg.getOriginator(), config.getAlarmType());
        return Futures.transformAsync(latest, a -> {
            if (a == null || a.getStatus().isCleared()) {
                return createNewAlarm(ctx, msg);
            } else {
                return updateAlarm(ctx, msg, a);
            }
        }, ctx.getDbCallbackExecutor());

    }

    private ListenableFuture<AlarmResult> createNewAlarm(TbContext ctx, TbMsg msg) {
        ListenableFuture<Alarm> asyncAlarm = Futures.transform(buildAlarmDetails(ctx, msg, null),
                details -> buildAlarm(msg, details, ctx.getTenantId()));
        ListenableFuture<Alarm> asyncCreated = Futures.transform(asyncAlarm,
                alarm -> ctx.getAlarmService().createOrUpdateAlarm(alarm), ctx.getDbCallbackExecutor());
        return Futures.transform(asyncCreated, alarm -> new AlarmResult(true, false, false, alarm));
    }

    private ListenableFuture<AlarmResult> updateAlarm(TbContext ctx, TbMsg msg, Alarm alarm) {
        ListenableFuture<Alarm> asyncUpdated = Futures.transform(buildAlarmDetails(ctx, msg, alarm.getDetails()), (Function<JsonNode, Alarm>) details -> {
            alarm.setSeverity(config.getSeverity());
            alarm.setPropagate(config.isPropagate());
            alarm.setDetails(details);
            alarm.setEndTs(System.currentTimeMillis());
            return ctx.getAlarmService().createOrUpdateAlarm(alarm);
        }, ctx.getDbCallbackExecutor());

        return Futures.transform(asyncUpdated, a -> new AlarmResult(false, true, false, a));
    }

    private Alarm buildAlarm(TbMsg msg, JsonNode details, TenantId tenantId) {
        return Alarm.builder()
                .tenantId(tenantId)
                .originator(msg.getOriginator())
                .status(AlarmStatus.ACTIVE_UNACK)
                .severity(config.getSeverity())
                .propagate(config.isPropagate())
                .type(config.getAlarmType())
                //todo-vp: alarm date should be taken from Message or current Time should be used?
//                .startTs(System.currentTimeMillis())
//                .endTs(System.currentTimeMillis())
                .details(details)
                .build();
    }

}
