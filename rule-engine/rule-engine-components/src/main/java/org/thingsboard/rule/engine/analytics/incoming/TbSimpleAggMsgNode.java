// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.incoming;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.analytics.incoming.state.StatePersistPolicy;
import org.thingsboard.rule.engine.analytics.incoming.state.TbIntervalState;
import org.thingsboard.rule.engine.analytics.latest.ParentEntitiesQuery;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.thingsboard.server.common.data.DataConstants.QUEUE_NAME;

@Slf4j
@RuleNode(
        type = ComponentType.ANALYTICS,
        name = "aggregate stream",
        configClazz = TbSimpleAggMsgNodeConfiguration.class,
        version = 2,
        hasQueueName = true,
        nodeDescription = "Aggregates incoming data stream grouped by originator Entity Id",
        nodeDetails = "Calculates MIN/MAX/SUM/AVG/COUNT/UNIQUE based on the incoming data stream. " +
                "Groups incoming data stream based on originator id of the message (i.e. particular device, asset, customer) and <b>\"aggregation interval value\"</b> into Intervals.<br/><br/>" +
                "Intervals are periodically persisted based on <b>\"interval persistence policy\"</b> and <b>\"interval check value\"</b>.<br/><br/>" +
                "Intervals are cached in memory based on <b>\"Interval TTL value\"</b>.<br/><br/>" +
                "State of the Intervals are persisted as timeseries entities based on <b>\"state persistence policy\"</b> and <b>\"state persistence value\"</b>.<br/><br/>" +
                "In case there is no data for certain entity, it might be useful to generate default values for those entities. " +
                "To lookup those entities one may select <b>\"Create intervals automatically\"</b> checkbox and configure <b>\"Interval entities\"</b>.<br/><br/>" +
                "Generates outgoing messages with the results of the aggregation for particular interval. By default, an outgoing message generates with 'POST_TELEMETRY_REQUEST' type. " +
                "The type of the outgoing messages controls under \"<b>Output message type</b>\" configuration parameter.",
        configDirective = "tbAnalyticsNodeAggregateIncomingConfig",
        icon = "functions",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/analytics/aggregate-stream/"
)
public class TbSimpleAggMsgNode implements TbNode {

    private final Gson gson = new Gson();

    private StatePersistPolicy statePersistPolicy;
    private IntervalPersistPolicy intervalPersistPolicy;
    private TbSimpleAggMsgNodeConfiguration config;
    private TbIntervalTable intervals;
    private UUID nextReportTickId;
    private UUID nextPersistTickId;
    private UUID nextEntitiesTickId;
    private long intervalReportCheckPeriod;
    private long statePersistCheckPeriod;
    private long entitiesCheckPeriod;
    private String queueName;
    private String outMsgType;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbSimpleAggMsgNodeConfiguration.class);
        this.queueName = ctx.getQueueName();
        this.statePersistPolicy = StatePersistPolicy.valueOf(config.getStatePersistencePolicy());
        this.intervalPersistPolicy = IntervalPersistPolicy.valueOf(config.getIntervalPersistencePolicy());
        this.intervals = new TbIntervalTable(ctx, config);
        this.intervalReportCheckPeriod = Math.max(TimeUnit.valueOf(config.getIntervalCheckTimeUnit()).toMillis(config.getIntervalCheckValue()), TimeUnit.MINUTES.toMillis(1));
        this.statePersistCheckPeriod = Math.max(TimeUnit.valueOf(config.getStatePersistenceTimeUnit()).toMillis(config.getStatePersistenceValue()), TimeUnit.MINUTES.toMillis(1));
        this.outMsgType = StringUtils.isNotBlank(config.getOutMsgType()) ? config.getOutMsgType() : TbMsgType.POST_TELEMETRY_REQUEST.name();
        scheduleReportTickMsg(ctx, null);
        if (StatePersistPolicy.PERIODICALLY.name().equalsIgnoreCase(config.getStatePersistencePolicy())) {
            scheduleStatePersistTickMsg(ctx, null);
        }
        if (config.isAutoCreateIntervals()) {
            this.entitiesCheckPeriod = Math.max(config.getPeriodTimeUnit().toMillis(config.getPeriodValue()), TimeUnit.MINUTES.toMillis(1));
            initEntities(ctx, null);
            scheduleEntitiesTickMsg(ctx, null);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        switch (msg.getInternalType()) {
            case TB_SIMPLE_AGG_REPORT_SELF_MSG:
                onIntervalTickMsg(ctx, msg);
                break;
            case TB_SIMPLE_AGG_PERSIST_SELF_MSG:
                onPersistTickMsg(ctx, msg);
                break;
            case TB_SIMPLE_AGG_ENTITIES_SELF_MSG:
                onEntitiesTickMsg(ctx, msg);
                break;
            default:
                onDataMsg(ctx, msg);
                break;
        }
    }

    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        log.trace("Cluster change msg received: {}", msg);
        intervals.cleanupEntities(ctx);
    }

    private void onDataMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        EntityId entityId = msg.getOriginator();
        long ts = extractTs(msg);
        JsonElement value = extractValue(msg);
        Pair<Long, TbIntervalState> statePair = intervals.getByEntityIdAndTs(entityId, ts);
        TbIntervalState state = statePair.getSecond();
        state.update(value);

        log.trace("Data Msg received: {}", msg);
        if (state.hasChangesToPersist() && statePersistPolicy == StatePersistPolicy.ON_EACH_CHANGE) {
            log.trace("Persisting state: {}", state);
            DonAsynchron.withCallback(intervals.saveIntervalState(entityId, ts, state),
                    v -> {
                        ctx.getPeContext().ack(msg);
                        state.clearChangesToPersist();
                        log.trace("Cleared state after persising: {}", state);
                    },
                    t -> ctx.tellFailure(msg, t),
                    ctx.getDbCallbackExecutor());
        } else {
            ctx.getPeContext().ack(msg);
        }

        if (state.hasChangesToReport() && intervalPersistPolicy == IntervalPersistPolicy.ON_EACH_MESSAGE) {
            reportInterval(ctx, entityId, statePair.getFirst(), state);
            state.clearChangesToReport();
        }
    }

    private void onIntervalTickMsg(TbContext ctx, TbMsg msg) {
        if (!msg.getId().equals(nextReportTickId)) {
            return;
        }
        scheduleReportTickMsg(ctx, msg);
        log.trace("Reporting intervals!");
        intervals.getStatesToReport(intervalPersistPolicy).forEach((entityId, entityStates) -> entityStates.forEach((ts, interval) -> {
            reportInterval(ctx, entityId, ts, interval);
        }));

        intervals.cleanupStatesUsingTTL();
    }

    private void reportInterval(TbContext ctx, EntityId entityId, Long ts, TbIntervalState interval) {
        log.trace("Reporting interval: [{}][{}]", ts, interval);
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ts", Long.toString(ts));
        metaData.putValue("overwriteValue", "true");
        ctx.enqueueForTellNext(TbMsg.newMsg()
                .queueName(queueName)
                .type(outMsgType)
                .originator(entityId)
                .copyMetaData(metaData)
                .data(interval.toValueJson(gson, config.getOutputValueKey()))
                .build(), TbNodeConnectionType.SUCCESS);
    }

    private void onPersistTickMsg(TbContext ctx, TbMsg msg) {
        if (!msg.getId().equals(nextPersistTickId)) {
            return;
        }
        scheduleStatePersistTickMsg(ctx, msg);
        log.trace("[{}] Persisting states!", ctx.getSelfId());
        intervals.getStatesToPersist().forEach((entityId, entityStates) -> entityStates.forEach((ts, state) -> {
            log.trace("[{}] Persisting state: [{}][{}]", ctx.getSelfId(), ts, state);
            intervals.saveIntervalState(entityId, ts, state);
        }));

        intervals.cleanupStatesUsingTTL();
    }

    private void onEntitiesTickMsg(TbContext ctx, TbMsg msg) {
        if (!msg.getId().equals(nextEntitiesTickId)) {
            return;
        }
        scheduleEntitiesTickMsg(ctx, msg);
        initEntities(ctx, msg);
    }

    private void initEntities(TbContext ctx, TbMsg msg) {
        log.trace("[{}] Lookup entities!", ctx.getSelfId());
        ParentEntitiesQuery query = config.getParentEntitiesQuery();
        if (query.useParentEntitiesOnlyForSimpleAggregation()) {
            addIntervals(ctx, msg, query.getLocalParentEntitiesAsync(ctx));
        } else {
            DonAsynchron.withCallback(query.getLocalParentEntitiesAsync(ctx), parents -> {
                for (EntityId parentId : parents) {
                    addIntervals(ctx, msg, query.getChildEntitiesAsync(ctx, parentId));
                }
            }, getErrorsConsumer(ctx, msg), ctx.getDbCallbackExecutor());
        }
    }

    private void addIntervals(TbContext ctx, TbMsg msg, ListenableFuture<List<EntityId>> entities) {
        DonAsynchron.withCallback(entities,
                tmp -> intervals.addEntities(ctx, msg, tmp), getErrorsConsumer(ctx, msg), ctx.getDbCallbackExecutor());
    }

    private Consumer<Throwable> getErrorsConsumer(TbContext ctx, TbMsg msg) {
        return t -> {
            if (msg != null) {
                ctx.tellFailure(msg, t);
            }
        };
    }

    void scheduleReportTickMsg(TbContext ctx, TbMsg msg) {
        TbMsg tickMsg = ctx.newMsg(queueName, TbMsgType.TB_SIMPLE_AGG_REPORT_SELF_MSG, ctx.getSelfId(),
                getCustomerId(msg), TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
        nextReportTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, intervalReportCheckPeriod);
    }

    private void scheduleStatePersistTickMsg(TbContext ctx, TbMsg msg) {
        TbMsg tickMsg = ctx.newMsg(queueName, TbMsgType.TB_SIMPLE_AGG_PERSIST_SELF_MSG, ctx.getSelfId(),
                getCustomerId(msg), TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
        nextPersistTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, statePersistCheckPeriod);
    }

    private void scheduleEntitiesTickMsg(TbContext ctx, TbMsg msg) {
        TbMsg tickMsg = ctx.newMsg(queueName, TbMsgType.TB_SIMPLE_AGG_ENTITIES_SELF_MSG, ctx.getSelfId(),
                getCustomerId(msg), TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
        nextEntitiesTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, entitiesCheckPeriod);
    }

    private CustomerId getCustomerId(TbMsg msg) {
        return msg != null ? msg.getCustomerId() : null;
    }

    private long extractTs(TbMsg msg) {
        String ts = msg.getMetaData().getValue("ts");
        if (!StringUtils.isEmpty(ts)) {
            return Long.parseLong(ts);
        } else {
            return msg.getTs();
        }
    }

    private JsonElement extractValue(TbMsg msg) {
        JsonElement jsonElement = JsonParser.parseString(msg.getData());
        if (!jsonElement.isJsonObject()) {
            throw new IllegalArgumentException("Incoming message is not a json object!");
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (!jsonObject.has(config.getInputValueKey())) {
            throw new IllegalArgumentException("Incoming message does not contain " + config.getInputValueKey() + "!");
        }
        return checkForNullAndGet(jsonObject);
    }

    JsonElement checkForNullAndGet(JsonObject jsonObject) {
        JsonElement je = jsonObject.get(config.getInputValueKey());
        if (je.isJsonNull()) {
            throw new IllegalArgumentException("Found JSON null for [" + config.getInputValueKey() + "] key!");
        }
        return je;
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
