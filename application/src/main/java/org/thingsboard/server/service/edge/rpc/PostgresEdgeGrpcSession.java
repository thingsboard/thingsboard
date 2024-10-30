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
package org.thingsboard.server.service.edge.rpc;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.fetch.GeneralEdgeEventFetcher;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;

@Slf4j
public class PostgresEdgeGrpcSession extends AbstractEdgeGrpcSession<PostgresEdgeGrpcSession> {

    private static final String QUEUE_START_TS_ATTR_KEY = "queueStartTs";
    private static final String QUEUE_START_SEQ_ID_ATTR_KEY = "queueStartSeqId";

    private Long newStartTs;
    private Long previousStartTs;
    private Long newStartSeqId;
    private Long previousStartSeqId;
    private Long seqIdEnd;

    PostgresEdgeGrpcSession(EdgeContextComponent ctx, StreamObserver<ResponseMsg> outputStream,
                            BiConsumer<EdgeId, PostgresEdgeGrpcSession> sessionOpenListener,
                            BiConsumer<Edge, UUID> sessionCloseListener, ScheduledExecutorService sendDownlinkExecutorService,
                            int maxInboundMessageSize, int maxHighPriorityQueueSizePerSession) {
        super(ctx, outputStream, sessionOpenListener, sessionCloseListener, sendDownlinkExecutorService, maxInboundMessageSize, maxHighPriorityQueueSizePerSession);
        initInputStream();
    }

    protected ListenableFuture<Boolean> processEdgeEvents() throws Exception {
        SettableFuture<Boolean> result = SettableFuture.create();
        log.trace("[{}][{}] starting processing edge events", this.tenantId, this.sessionId);
        if (isConnected() && isSyncCompleted()) {
            Pair<Long, Long> startTsAndSeqId = getQueueStartTsAndSeqId().get();
            this.previousStartTs = startTsAndSeqId.getFirst();
            this.previousStartSeqId = startTsAndSeqId.getSecond();
            GeneralEdgeEventFetcher fetcher = new GeneralEdgeEventFetcher(
                    this.previousStartTs,
                    this.previousStartSeqId,
                    this.seqIdEnd,
                    false,
                    Integer.toUnsignedLong(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount()),
                    ctx.getEdgeEventService());
            Futures.addCallback(startProcessingEdgeEvents(fetcher), new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Pair<Long, Long> newStartTsAndSeqId) {
                    if (newStartTsAndSeqId != null) {
                        ListenableFuture<List<Long>> updateFuture = updateQueueStartTsAndSeqId(newStartTsAndSeqId);
                        Futures.addCallback(updateFuture, new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable List<Long> list) {
                                log.debug("[{}][{}] queue offset was updated [{}]", tenantId, sessionId, newStartTsAndSeqId);
                                if (fetcher.isSeqIdNewCycleStarted()) {
                                    seqIdEnd = fetcher.getSeqIdEnd();
                                    boolean newEventsAvailable = isNewEdgeEventsAvailable();
                                    result.set(newEventsAvailable);
                                } else {
                                    seqIdEnd = null;
                                    boolean newEventsAvailable = isSeqIdStartedNewCycle();
                                    if (!newEventsAvailable) {
                                        newEventsAvailable = isNewEdgeEventsAvailable();
                                    }
                                    result.set(newEventsAvailable);
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.error("[{}][{}] Failed to update queue offset [{}]", tenantId, sessionId, newStartTsAndSeqId, t);
                                result.setException(t);
                            }
                        }, ctx.getGrpcCallbackExecutorService());
                    } else {
                        log.trace("[{}][{}] newStartTsAndSeqId is null. Skipping iteration without db update", tenantId, sessionId);
                        result.set(null);
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Failed to process events", tenantId, sessionId, t);
                    result.setException(t);
                }
            }, ctx.getGrpcCallbackExecutorService());
        } else {
            log.trace("[{}][{}] edge is not connected or sync is not completed. Skipping iteration", tenantId, sessionId);
            result.set(null);
        }
        return result;
    }

    private ListenableFuture<Pair<Long, Long>> getQueueStartTsAndSeqId() {
        ListenableFuture<List<AttributeKvEntry>> future =
                ctx.getAttributesService().find(edge.getTenantId(), edge.getId(), AttributeScope.SERVER_SCOPE, Arrays.asList(QUEUE_START_TS_ATTR_KEY, QUEUE_START_SEQ_ID_ATTR_KEY));
        return Futures.transform(future, attributeKvEntries -> {
            long startTs = 0L;
            long startSeqId = 0L;
            for (AttributeKvEntry attributeKvEntry : attributeKvEntries) {
                if (QUEUE_START_TS_ATTR_KEY.equals(attributeKvEntry.getKey())) {
                    startTs = attributeKvEntry.getLongValue().isPresent() ? attributeKvEntry.getLongValue().get() : 0L;
                }
                if (QUEUE_START_SEQ_ID_ATTR_KEY.equals(attributeKvEntry.getKey())) {
                    startSeqId = attributeKvEntry.getLongValue().isPresent() ? attributeKvEntry.getLongValue().get() : 0L;
                }
            }
            if (startSeqId == 0L) {
                startSeqId = findStartSeqIdFromOldestEventIfAny();
            }
            return Pair.of(startTs, startSeqId);
        }, ctx.getGrpcCallbackExecutorService());
    }

    private boolean isSeqIdStartedNewCycle() {
        try {
            TimePageLink pageLink = new TimePageLink(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount(), 0, null, null, this.newStartTs, System.currentTimeMillis());
            PageData<EdgeEvent> edgeEvents = ctx.getEdgeEventService().findEdgeEvents(edge.getTenantId(), edge.getId(), 0L, this.previousStartSeqId == 0 ? null : this.previousStartSeqId - 1, pageLink);
            return !edgeEvents.getData().isEmpty();
        } catch (Exception e) {
            log.error("[{}][{}][{}] Failed to execute isSeqIdStartedNewCycle", this.tenantId, edge.getId(), sessionId, e);
        }
        return false;
    }

    private boolean isNewEdgeEventsAvailable() {
        try {
            TimePageLink pageLink = new TimePageLink(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount(), 0, null, null, this.newStartTs, System.currentTimeMillis());
            PageData<EdgeEvent> edgeEvents = ctx.getEdgeEventService().findEdgeEvents(edge.getTenantId(), edge.getId(), this.newStartSeqId, null, pageLink);
            return !edgeEvents.getData().isEmpty() || !highPriorityQueue.isEmpty();
        } catch (Exception e) {
            log.error("[{}][{}][{}] Failed to execute isNewEdgeEventsAvailable", this.tenantId, edge.getId(), sessionId, e);
        }
        return false;
    }

    private long findStartSeqIdFromOldestEventIfAny() {
        long startSeqId = 0L;
        try {
            TimePageLink pageLink = new TimePageLink(1, 0, null, new SortOrder("createdTime"), null, null);
            PageData<EdgeEvent> edgeEvents = ctx.getEdgeEventService().findEdgeEvents(edge.getTenantId(), edge.getId(), null, null, pageLink);
            if (!edgeEvents.getData().isEmpty()) {
                startSeqId = edgeEvents.getData().get(0).getSeqId() - 1;
            }
        } catch (Exception e) {
            log.error("[{}][{}][{}] Failed to execute findStartSeqIdFromOldestEventIfAny", this.tenantId, edge.getId(), sessionId, e);
        }
        return startSeqId;
    }

    private ListenableFuture<List<Long>> updateQueueStartTsAndSeqId(Pair<Long, Long> pair) {
        this.newStartTs = pair.getFirst();
        this.newStartSeqId = pair.getSecond();
        log.trace("[{}] updateQueueStartTsAndSeqId [{}][{}][{}]", this.sessionId, edge.getId(), this.newStartTs, this.newStartSeqId);
        List<AttributeKvEntry> attributes = Arrays.asList(
                new BaseAttributeKvEntry(new LongDataEntry(QUEUE_START_TS_ATTR_KEY, this.newStartTs), System.currentTimeMillis()),
                new BaseAttributeKvEntry(new LongDataEntry(QUEUE_START_SEQ_ID_ATTR_KEY, this.newStartSeqId), System.currentTimeMillis()));
        return ctx.getAttributesService().save(edge.getTenantId(), edge.getId(), AttributeScope.SERVER_SCOPE, attributes);
    }

}
