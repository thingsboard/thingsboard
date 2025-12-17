/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.AttributesSaveResult;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.EdgeSessionState;
import org.thingsboard.server.service.edge.rpc.fetch.GeneralEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.session.EdgeSession;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
@RequiredArgsConstructor
public class PostgresGeneralEdgeEventsDispatcher implements EdgeEventsDispatcher {

    private static final String QUEUE_START_TS_ATTR_KEY = "queueStartTs";
    private static final String QUEUE_START_SEQ_ID_ATTR_KEY = "queueStartSeqId";

    private final EdgeSession session;
    private final EdgeContextComponent ctx;

    private Long newStartTs;
    private Long previousStartTs;
    private Long newStartSeqId;
    private Long previousStartSeqId;

    @Override
    public ListenableFuture<Boolean> processNewEvents() throws Exception {
        EdgeSessionState state = session.getState();
        TenantId tenantId = state.getTenantId();
        EdgeId edgeId = state.getEdgeId();
        UUID sessionId = state.getSessionId();

        SettableFuture<Boolean> result = SettableFuture.create();
        if (state.isConnected() && !state.isSyncInProgress()) {
            setLocalPrevStartTsAndSeqId(tenantId, edgeId, sessionId);
            GeneralEdgeEventFetcher fetcher = initGeneralEdgeEventFetcher();
            log.trace("[{}][{}] starting processing edge events, previousStartTs = {}, previousStartSeqId = {}",
                    tenantId, edgeId, previousStartTs, previousStartSeqId);

            Futures.addCallback(session.fetchAndSendEdgeEvents(fetcher), new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Pair<Long, Long> newStartTsAndSeqId) {
                    if (newStartTsAndSeqId != null) {
                        Futures.addCallback(updateQueueStartTsAndSeqId(tenantId, edgeId, newStartTsAndSeqId), new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable AttributesSaveResult saveResult) {
                                log.debug("[{}][{}] queue offset was updated [{}]", tenantId, edgeId, newStartTsAndSeqId);
                                boolean newEventsAvailable;
                                if (fetcher.isSeqIdNewCycleStarted()) {
                                    newEventsAvailable = isNewEdgeEventsAvailable(tenantId, edgeId, sessionId);
                                } else {
                                    newEventsAvailable = isSeqIdStartedNewCycle(tenantId, edgeId, sessionId);
                                    if (!newEventsAvailable) {
                                        newEventsAvailable = isNewEdgeEventsAvailable(tenantId, edgeId, sessionId);
                                    }
                                }
                                result.set(newEventsAvailable);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.error("[{}][{}] Failed to update queue offset [{}]", tenantId, edgeId, newStartTsAndSeqId, t);
                                result.setException(t);
                            }
                        }, ctx.getGrpcCallbackExecutorService());
                    } else {
                        log.trace("[{}][{}] newStartTsAndSeqId is null. Skipping iteration without db update", tenantId, edgeId);
                        result.set(Boolean.FALSE);
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Failed to process events", tenantId, edgeId, t);
                    result.setException(t);
                }
            }, ctx.getGrpcCallbackExecutorService());
        } else {
            if (state.isSyncInProgress()) {
                log.trace("[{}][{}] edge sync is not completed yet. Skipping iteration", tenantId, edgeId);
                result.set(Boolean.TRUE);
            } else {
                log.trace("[{}][{}] edge is not connected. Skipping iteration", tenantId, edgeId);
                result.set(null);
            }
        }
        return result;
    }

    private boolean isSeqIdStartedNewCycle(TenantId tenantId, EdgeId edgeId, UUID sessionId) {
        try {
            log.trace("[{}][{}][{}] Checking if seq id started new cycle", tenantId, edgeId, sessionId);
            TimePageLink pageLink = new TimePageLink(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount(), 0, null, null, newStartTs, System.currentTimeMillis());
            PageData<EdgeEvent> edgeEvents = ctx.getEdgeEventService()
                    .findEdgeEvents(tenantId, edgeId, 0L, previousStartSeqId == 0 ? null : previousStartSeqId - 1, pageLink);

            boolean result = !edgeEvents.getData().isEmpty();
            log.trace("[{}][{}][{}] Result of check if seq id started new cycle, result = {}", tenantId, edgeId, sessionId, result);
            return result;
        } catch (Exception e) {
            log.error("[{}][{}][{}] Failed to execute isSeqIdStartedNewCycle", tenantId, edgeId, sessionId, e);
        }
        return false;
    }

    private boolean isNewEdgeEventsAvailable(TenantId tenantId, EdgeId edgeId, UUID sessionId) {
        try {
            log.trace("[{}][{}][{}] Checking if new edge events available", tenantId, edgeId, sessionId);
            TimePageLink pageLink = new TimePageLink(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount(), 0, null, null, newStartTs, System.currentTimeMillis());
            PageData<EdgeEvent> edgeEvents = ctx.getEdgeEventService().findEdgeEvents(tenantId, edgeId, newStartSeqId, null, pageLink);
            boolean available = !edgeEvents.getData().isEmpty() || session.hasHighPriorityEvents();
            log.trace("[{}][{}][{}] Result of check if new edge events available, result = {}", tenantId, edgeId, sessionId, available);
            return available;
        } catch (Exception e) {
            log.error("[{}][{}][{}] Failed to execute isNewEdgeEventsAvailable", tenantId, edgeId, sessionId, e);
        }
        return false;
    }

    private ListenableFuture<AttributesSaveResult> updateQueueStartTsAndSeqId(TenantId tenantId, EdgeId edgeId, Pair<Long, Long> pair) {
        newStartTs = pair.getFirst();
        newStartSeqId = pair.getSecond();
        log.trace("[{}] updateQueueStartTsAndSeqId [{}][{}][{}]", session.getState().getSessionId(), edgeId, newStartTs, newStartSeqId);
        List<AttributeKvEntry> attributes = List.of(
                new BaseAttributeKvEntry(new LongDataEntry(QUEUE_START_TS_ATTR_KEY, newStartTs), System.currentTimeMillis()),
                new BaseAttributeKvEntry(new LongDataEntry(QUEUE_START_SEQ_ID_ATTR_KEY, newStartSeqId), System.currentTimeMillis())
        );
        return ctx.getAttributesService().save(tenantId, edgeId, AttributeScope.SERVER_SCOPE, attributes);
    }

    private GeneralEdgeEventFetcher initGeneralEdgeEventFetcher() {
        return new GeneralEdgeEventFetcher(
                previousStartTs,
                previousStartSeqId,
                false,
                Integer.toUnsignedLong(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount()),
                ctx.getEdgeEventService(),
                ctx.getEdgeEventStorageSettings().getMisorderingCompensationMillis());
    }

    private void setLocalPrevStartTsAndSeqId(TenantId tenantId, EdgeId edgeId, UUID sessionId) throws InterruptedException, ExecutionException {
        Pair<Long, Long> startTsAndSeqId = getQueueStartTsAndSeqId(tenantId, edgeId, sessionId).get();
        previousStartTs = startTsAndSeqId.getFirst();
        previousStartSeqId = startTsAndSeqId.getSecond();
    }

    private ListenableFuture<Pair<Long, Long>> getQueueStartTsAndSeqId(TenantId tenantId, EdgeId edgeId, UUID sessionId) {
        ListenableFuture<List<AttributeKvEntry>> future = ctx.getAttributesService()
                .find(tenantId, edgeId, AttributeScope.SERVER_SCOPE, Arrays.asList(QUEUE_START_TS_ATTR_KEY, QUEUE_START_SEQ_ID_ATTR_KEY));

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
                startSeqId = findStartSeqIdFromOldestEventIfAny(tenantId, edgeId, sessionId);
            }
            return Pair.of(startTs, startSeqId);
        }, ctx.getGrpcCallbackExecutorService());
    }

    private long findStartSeqIdFromOldestEventIfAny(TenantId tenantId, EdgeId edgeId, UUID sessionId) {
        long startSeqId = 0L;
        try {
            TimePageLink pageLink = new TimePageLink(1, 0, null, null, null, null);
            PageData<EdgeEvent> edgeEvents = ctx.getEdgeEventService()
                    .findEdgeEvents(tenantId, edgeId, null, null, pageLink);
            if (!edgeEvents.getData().isEmpty()) {
                startSeqId = edgeEvents.getData().get(0).getSeqId() - 1;
            }
        } catch (Exception e) {
            log.error("[{}][{}][{}] Failed to execute findStartSeqIdFromOldestEventIfAny", tenantId, edgeId, sessionId, e);
        }
        return startSeqId;
    }
}
