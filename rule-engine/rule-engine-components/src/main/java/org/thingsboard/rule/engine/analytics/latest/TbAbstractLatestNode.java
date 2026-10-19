// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.server.common.data.DataConstants.QUEUE_NAME;
import static org.thingsboard.server.common.data.msg.TbNodeConnectionType.SUCCESS;

public abstract class TbAbstractLatestNode<C extends TbAbstractLatestNodeConfiguration> implements TbNode {

    private final Gson gson = new Gson();

    protected C config;
    private long delay;
    private long lastScheduledTs;
    private UUID nextTickId;
    protected String queueName;
    protected String outMsgType;

    private EntityId parentEntitiesQueryRoot;
    private ParentEntitiesQuery parentEntitiesQuery;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = loadMapperNodeConfig(configuration);
        parentEntitiesQuery = config.getParentEntitiesQuery();
        queueName = ctx.getQueueName();
        delay = config.getPeriodTimeUnit().toMillis(config.getPeriodValue());
        outMsgType = StringUtils.notBlankOrDefault(config.getOutMsgType(), TbMsgType.POST_TELEMETRY_REQUEST.name());

        parentEntitiesQueryRoot = getParentEntitiesQueryRoot(config.getParentEntitiesQuery());
        ctx.checkTenantEntity(parentEntitiesQueryRoot);

        initializeIfLocalEntity(ctx, parentEntitiesQueryRoot);
    }

    private static EntityId getParentEntitiesQueryRoot(ParentEntitiesQuery query) throws TbNodeException {
        EntityId parentEntitiesQueryRoot;
        if (query instanceof ParentEntitiesSingleEntity singleEntity) {
            parentEntitiesQueryRoot = singleEntity.getEntityId();
        } else if (query instanceof ParentEntitiesGroup entitiesGroup) {
            parentEntitiesQueryRoot = entitiesGroup.getEntityGroupId();
        } else if (query instanceof ParentEntitiesRelationsQuery relationsQuery) {
            parentEntitiesQueryRoot = relationsQuery.getRootEntityId();
        } else {
            throw new TbNodeException("Unknown parent entity query type: " + query.getClass().getSimpleName(), true);
        }
        return parentEntitiesQueryRoot;
    }

    private void initializeIfLocalEntity(TbContext ctx, EntityId entityId) {
        if (ctx.isLocalEntity(entityId)) {
            if (initialized.compareAndSet(false, true)) {
                scheduleTickMsg(ctx);
            }
        } else if (initialized.compareAndSet(true, false)) {
            destroy();
        }
    }

    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        initializeIfLocalEntity(ctx, parentEntitiesQueryRoot);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (initialized.get() && msg.isTypeOf(tickMessageType()) && msg.getId().equals(nextTickId)) {
            withCallback(aggregate(ctx),
                    success -> {
                        if (initialized.get()) {
                            scheduleTickMsg(ctx);
                        }
                    },
                    error -> {
                        if (initialized.get()) {
                            ctx.tellFailure(msg, error);
                            scheduleTickMsg(ctx);
                        }
                    });
        }
    }

    private void scheduleTickMsg(TbContext ctx) {
        long curTs = System.currentTimeMillis();
        if (lastScheduledTs == 0L) {
            lastScheduledTs = curTs;
        }
        lastScheduledTs = lastScheduledTs + delay;
        long curDelay = Math.max(0L, (lastScheduledTs - curTs));
        TbMsg tickMsg = ctx.newMsg(queueName, tickMessageType(), ctx.getSelfId(), TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
        nextTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, curDelay);
    }

    private ListenableFuture<List<TbMsg>> aggregate(TbContext ctx) {
        ListenableFuture<List<EntityId>> parentEntityIdsFuture = parentEntitiesQuery.getParentEntitiesAsync(ctx);
        return Futures.transformAsync(parentEntityIdsFuture, parentEntityIds -> {
            List<ListenableFuture<TbMsg>> msgFutures = new ArrayList<>();
            String dataTs = Long.toString(System.currentTimeMillis());
            parentEntityIds.forEach(parentEntityId -> {
                Map<EntityId, List<ListenableFuture<Optional<JsonObject>>>> aggregateFuturesMap = doParentAggregations(ctx, parentEntityId);
                aggregateFuturesMap.forEach((originatorId, aggregateFutures) -> aggregateFutures.forEach(aggregateFuture -> {
                    ListenableFuture<Optional<JsonObject>>
                            aggregateFutureWithFallback = Futures.catching(aggregateFuture, Throwable.class, e -> {
                        TbMsg msg = TbMsg.newMsg()
                                .queueName(queueName)
                                .type(outMsgType)
                                .originator(originatorId)
                                .copyMetaData(TbMsgMetaData.EMPTY)
                                .data(TbMsg.EMPTY_STRING)
                                .build();
                        ctx.enqueueForTellFailure(msg, e.getMessage());
                        return Optional.empty();
                    }, MoreExecutors.directExecutor());
                    ListenableFuture<TbMsg> msgFuture = Futures.transform(aggregateFutureWithFallback, element -> {
                        if (element.isPresent()) {
                            TbMsgMetaData metaData = new TbMsgMetaData();
                            metaData.putValue("ts", dataTs);
                            JsonObject messageData = element.get();
                            TbMsg msg = TbMsg.newMsg()
                                    .queueName(queueName)
                                    .type(outMsgType)
                                    .originator(originatorId)
                                    .copyMetaData(metaData)
                                    .data(gson.toJson(messageData))
                                    .build();
                            ctx.enqueueForTellNext(msg, SUCCESS);
                            return msg;
                        } else {
                            return null;
                        }
                    }, MoreExecutors.directExecutor());
                    msgFutures.add(msgFuture);
                }));
            });
            return Futures.allAsList(msgFutures);
        }, ctx.getDbCallbackExecutor());
    }

    protected abstract C loadMapperNodeConfig(TbNodeConfiguration configuration) throws TbNodeException;

    protected abstract TbMsgType tickMessageType();

    protected abstract Map<EntityId, List<ListenableFuture<Optional<JsonObject>>>> doParentAggregations(TbContext ctx, EntityId parentEntityId);

    @Override
    public void destroy() {
        initialized.set(false);
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
