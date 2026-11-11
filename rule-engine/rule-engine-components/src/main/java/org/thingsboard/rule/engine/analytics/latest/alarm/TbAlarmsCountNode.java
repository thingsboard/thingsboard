// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.alarm;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import org.thingsboard.rule.engine.analytics.latest.TbAbstractLatestNode;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.alarm.AlarmFilter;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RuleNode(
        type = ComponentType.ANALYTICS,
        name = "alarms count (deprecated)",
        configClazz = TbAlarmsCountNodeConfiguration.class,
        version = 2,
        hasQueueName = true,
        nodeDescription = "Periodically counts alarms for entities",
        nodeDetails = "Performs count of alarms for parent entities and child entities if specified with configurable period. " +
                "Generates outgoing messages with alarm count values for each found entity. By default, an outgoing message generates with 'POST_TELEMETRY_REQUEST' type. " +
                "The type of the outgoing messages controls under \"<b>Output message type</b>\" configuration parameter.",
        inEnabled = false,
        configDirective = "tbAnalyticsNodeAlarmsCountConfig",
        icon = "functions",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/analytics/alarms-count-deprecated/"
)
public class TbAlarmsCountNode extends TbAbstractLatestNode<TbAlarmsCountNodeConfiguration> {

    @Override
    protected TbAlarmsCountNodeConfiguration loadMapperNodeConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbAlarmsCountNodeConfiguration.class);
    }

    @Override
    protected TbMsgType tickMessageType() {
        return TbMsgType.TB_ALARMS_COUNT_SELF_MSG;
    }

    @Override
    protected Map<EntityId, List<ListenableFuture<Optional<JsonObject>>>> doParentAggregations(TbContext ctx, EntityId parentEntityId) {
        List<EntityId> entityIds = new ArrayList<>();
        entityIds.add(parentEntityId);
        if (this.config.isCountAlarmsForChildEntities()) {
            ListenableFuture<List<EntityId>> childEntityIdsFuture =
                    this.config.getParentEntitiesQuery().getChildEntitiesAsync(ctx, parentEntityId);
            try {
                entityIds.addAll(childEntityIdsFuture.get());
            } catch (Exception e) {
                TbMsg msg = TbMsg.newMsg()
                        .queueName(queueName)
                        .type(outMsgType)
                        .originator(parentEntityId)
                        .copyMetaData(TbMsgMetaData.EMPTY)
                        .data(TbMsg.EMPTY_STRING)
                        .build();
                ctx.enqueueForTellFailure(msg, "Failed to fetch child entities for parent entity [" + parentEntityId + "]");
            }
        }
        Map<EntityId, List<ListenableFuture<Optional<JsonObject>>>> result = new HashMap<>();
        entityIds.forEach(entityId -> {
            List<ListenableFuture<Optional<JsonObject>>> aggregateFutures = new ArrayList<>();
            JsonObject data = countAlarms(ctx, entityId);
            aggregateFutures.add(Futures.immediateFuture(Optional.of(data)));
            result.put(entityId, aggregateFutures);
        });
        return result;
    }

    private JsonObject countAlarms(TbContext ctx, EntityId entityId) {
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
        JsonObject obj = new JsonObject();
        for (int i = 0; i < mappings.size(); i++) {
            obj.addProperty(mappings.get(i).getTarget(), alarmCounts.get(i));
        }
        return obj;
    }

}
