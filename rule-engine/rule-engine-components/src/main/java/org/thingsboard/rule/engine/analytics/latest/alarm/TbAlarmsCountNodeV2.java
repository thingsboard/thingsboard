// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmFilter;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.thingsboard.server.common.data.DataConstants.QUEUE_NAME;

@Slf4j
@RuleNode(
        type = ComponentType.ANALYTICS,
        name = "alarms count",
        configClazz = TbAlarmsCountNodeV2Configuration.class,
        version = 2,
        hasQueueName = true,
        nodeDescription = "Counts alarms by msg originator",
        nodeDetails = "Performs count of alarms for originator and for propagation entities if specified. " +
                "Generates outgoing messages with alarm count values for each found entity. By default, an outgoing message generates with 'POST_TELEMETRY_REQUEST' type. " +
                "The type of the outgoing messages controls under \"<b>Output message type</b>\" configuration parameter.",
        configDirective = "tbAnalyticsNodeAlarmsCountV2Config",
        icon = "functions",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/analytics/alarms-count/"
)
public class TbAlarmsCountNodeV2 implements TbNode {

    private static final List<String> ALARM_FIELDS = List.of("originator", "severity", "status", "ackTs", "clearTs", "details");

    private TbAlarmsCountNodeV2Configuration config;
    private String queueName;
    private String outMsgType;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbAlarmsCountNodeV2Configuration.class);
        this.queueName = ctx.getQueueName();
        this.outMsgType = StringUtils.isNotBlank(config.getOutMsgType()) ? config.getOutMsgType() : TbMsgType.POST_TELEMETRY_REQUEST.name();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        Alarm alarm = null;
        var processAlarmsCount = false;
        if (msg.isTypeOneOf(TbMsgType.ENTITY_CREATED, TbMsgType.ENTITY_UPDATED)) {
            if (msg.getOriginator().getEntityType().equals(EntityType.ALARM)) {
                alarm = convertMsgDataToAlarm(msg);
                processAlarmsCount = true;
            } else {
                JsonNode jsonData = JacksonUtil.toJsonNode(msg.getData());
                var msgDataHasAlarmFields = ALARM_FIELDS.stream().allMatch(jsonData::has);
                if (msgDataHasAlarmFields) {
                    alarm = JacksonUtil.treeToValue(jsonData, AlarmInfo.class);
                    log.debug("[{}] Msg data was successfully parsed to alarm object {}", ctx.getTenantId(), alarm);
                    processAlarmsCount = true;
                }
            }
        } else if (msg.isTypeOneOf(TbMsgType.ALARM, TbMsgType.ALARM_CREATED, TbMsgType.ALARM_UPDATED,
                TbMsgType.ALARM_SEVERITY_UPDATED, TbMsgType.ALARM_ACK, TbMsgType.ALARM_CLEAR)) {
            alarm = convertMsgDataToAlarm(msg);
            processAlarmsCount = true;
        }

        if (processAlarmsCount) {
            process(ctx, msg, alarm);
        } else {
            ctx.tellSuccess(msg);
        }
    }

    private AlarmInfo convertMsgDataToAlarm(TbMsg msg) {
        return JacksonUtil.fromString(msg.getData(), AlarmInfo.class);
    }

    private void process(TbContext ctx, TbMsg msg, Alarm alarm) {
        if (alarm == null) {
            ctx.tellFailure(msg, new RuntimeException("Failed to process alarms count since the msg data could not be converted to alarm!"));
            return;
        }
        Map<EntityId, ObjectNode> result = new HashMap<>();
        getPropagationEntityIds(ctx, alarm).forEach(entityId -> result.put(entityId, countAlarms(ctx, entityId)));

        String dataTs = Long.toString(System.currentTimeMillis());

        result.forEach((entityId, data) -> {
            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("ts", dataTs);
            TbMsg newMsg = TbMsg.newMsg()
                    .queueName(queueName)
                    .type(outMsgType)
                    .originator(entityId)
                    .copyMetaData(metaData)
                    .data(JacksonUtil.toString(data))
                    .build();
            ctx.enqueueForTellNext(newMsg, TbNodeConnectionType.SUCCESS);
        });
        ctx.ack(msg);
    }

    private Set<EntityId> getPropagationEntityIds(TbContext ctx, Alarm alarm) {
        if (config.isCountAlarmsForPropagationEntities() && (alarm.isPropagate() || alarm.isPropagateToOwner() || alarm.isPropagateToOwnerHierarchy() || alarm.isPropagateToTenant())) {
            Set<EntityId> propagationEntityIds = ctx.getAlarmService().getPropagationEntityIds(alarm, config.getPropagationEntityTypes());
            propagationEntityIds.add(alarm.getOriginator());
            return propagationEntityIds;
        } else {
            return Collections.singleton(alarm.getOriginator());
        }
    }

    private ObjectNode countAlarms(TbContext ctx, EntityId entityId) {
        List<AlarmsCountMapping> mappings = this.config.getAlarmsCountMappings();
        List<AlarmFilter> filters = new ArrayList<>();
        for (AlarmsCountMapping mapping : mappings) {
            filters.add(mapping.createAlarmFilter());
        }
        long interval = 0;
        for (AlarmsCountMapping mapping : mappings) {
            if (mapping.getLatestInterval() == 0) {
                interval = 0;
                break;
            } else {
                interval = Math.max(interval, mapping.getLatestInterval());
            }
        }
        TimePageLink pageLink;
        PageLink alarmSearchPageLink = new PageLink(Integer.MAX_VALUE);
        if (interval > 0) {
            pageLink = new TimePageLink(alarmSearchPageLink, System.currentTimeMillis() - interval, null);
        } else {
            pageLink = new TimePageLink(alarmSearchPageLink, null, null);
        }
        AlarmQuery alarmQuery = new AlarmQuery(entityId, pageLink, null, null, null, false);
        List<Long> alarmCounts = ctx.getAlarmService().findAlarmCounts(ctx.getTenantId(), alarmQuery, filters);
        ObjectNode obj = JacksonUtil.newObjectNode();
        for (int i = 0; i < mappings.size(); i++) {
            obj.put(mappings.get(i).getTarget(), alarmCounts.get(i));
        }
        return obj;
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                if (!oldConfiguration.hasNonNull("outMsgType")) {
                    ((ObjectNode) oldConfiguration).put("outMsgType", TbMsgType.POST_TELEMETRY_REQUEST.name());
                    hasChanges = true;
                }
            case 1:
                if (oldConfiguration.has(QUEUE_NAME)) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).remove(QUEUE_NAME);
                }
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
