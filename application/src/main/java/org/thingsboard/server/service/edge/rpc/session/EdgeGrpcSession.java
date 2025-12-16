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
package org.thingsboard.server.service.edge.rpc.session;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.notification.rule.trigger.EdgeCommunicationFailureTrigger;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.edge.EdgeEventUpdateMsg;
import org.thingsboard.server.dao.edge.stats.EdgeStatsKey;
import org.thingsboard.server.gen.edge.v1.ConnectRequestMsg;
import org.thingsboard.server.gen.edge.v1.ConnectResponseCode;
import org.thingsboard.server.gen.edge.v1.ConnectResponseMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.RequestMsg;
import org.thingsboard.server.gen.edge.v1.RequestMsgType;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.gen.edge.v1.SyncCompletedMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.DownlinkMessageMapper;
import org.thingsboard.server.service.edge.rpc.EdgeSessionState;
import org.thingsboard.server.service.edge.rpc.EdgeSyncCursor;
import org.thingsboard.server.service.edge.rpc.EdgeUplinkMessageDispatcher;
import org.thingsboard.server.service.edge.rpc.fetch.EdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.GeneralEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.session.manager.EdgeGrpcSessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
public class EdgeGrpcSession implements EdgeSession {

    private static final int MAX_DOWNLINK_ATTEMPTS = 3;
    private static final String RATE_LIMIT_REACHED = "Rate limit reached";

    private final EdgeGrpcSessionManager parentManagerRef;
    private final EdgeContextComponent ctx;
    private final StreamObserver<ResponseMsg> outputStream;
    private final DownlinkMessageMapper downlinkMessageMapper;
    private final EdgeUplinkMessageDispatcher uplinkMessageDispatcher;
    private final BiConsumer<EdgeId, EdgeGrpcSessionManager> sessionOpenListener;
    private final BiConsumer<Edge, UUID> sessionCloseListener;
    private final ScheduledExecutorService sendDownlinkExecutorService;
    private final int maxInboundMessageSize;
    private final int maxHighPriorityQueueSizePerSession;

    private final EdgeSessionState state = new EdgeSessionState();
    private final Lock downlinkMsgLock = new ReentrantLock();
    private final ConcurrentLinkedQueue<EdgeEvent> highPriorityQueue = new ConcurrentLinkedQueue<>();

    private int clientMaxInboundMessageSize;

    @Override
    public EdgeSessionState getState() {
        return state;
    }

    @Override
    public StreamObserver<RequestMsg> initInputStream() {
        return new StreamObserver<>() {
            @Override
            public void onNext(RequestMsg requestMsg) {
                if (!state.isConnected() && requestMsg.getMsgType().equals(RequestMsgType.CONNECT_RPC_MESSAGE)) {
                    ConnectResponseMsg responseMsg = processConnect(requestMsg.getConnectRequestMsg());
                    outputStream.onNext(ResponseMsg.newBuilder()
                            .setConnectResponseMsg(responseMsg)
                            .build());
                    if (ConnectResponseCode.ACCEPTED != responseMsg.getResponseCode()) {
                        outputStream.onError(new RuntimeException(responseMsg.getErrorMsg()));
                    } else {
                        if (requestMsg.getConnectRequestMsg().hasMaxInboundMessageSize()) {
                            log.debug("[{}][{}] Client max inbound message size: {}",
                                    getTenantId(), getSessionId(), requestMsg.getConnectRequestMsg().getMaxInboundMessageSize());
                            clientMaxInboundMessageSize = requestMsg.getConnectRequestMsg().getMaxInboundMessageSize();
                        }
                        state.setConnected(true);
                    }
                }
                if (state.isConnected()) {
                    if (requestMsg.getMsgType().equals(RequestMsgType.SYNC_REQUEST_RPC_MESSAGE)) {
                        if (requestMsg.hasSyncRequestMsg()) {
                            boolean fullSync = false;
                            if (requestMsg.getSyncRequestMsg().hasFullSync()) {
                                fullSync = requestMsg.getSyncRequestMsg().getFullSync();
                            }
                            startSyncProcess(fullSync);
                        } else {
                            state.finishSync();
                        }
                    }
                    if (requestMsg.getMsgType().equals(RequestMsgType.UPLINK_RPC_MESSAGE)) {
                        if (requestMsg.hasUplinkMsg()) {
                            onUplinkMsg(requestMsg.getUplinkMsg());
                        }
                        if (requestMsg.hasDownlinkResponseMsg()) {
                            onDownlinkResponse(requestMsg.getDownlinkResponseMsg());
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                log.trace("[{}][{}] Stream was terminated due to error:", getTenantId(), getSessionId(), t);
                closeSession();
            }

            @Override
            public void onCompleted() {
                log.info("[{}][{}] Stream was closed and completed successfully!", getTenantId(), getSessionId());
                closeSession();
            }

            private void closeSession() {
                state.setConnected(false);
                if (state.getEdge() != null) {
                    try {
                        sessionCloseListener.accept(state.getEdge(), getSessionId());
                    } catch (Exception ignored) {
                    }
                }
                try {
                    outputStream.onCompleted();
                } catch (Exception ignored) {
                }
            }
        };
    }

    @Override
    public void startSyncProcess(boolean fullSync) {
        if (state.tryStartSync()) {
            log.info("[{}][{}][{}] Staring edge sync process", getTenantId(), getEdgeId(), getSessionId());
            interruptGeneralProcessingOnSync();
            doSync(new EdgeSyncCursor(ctx, state.getEdge(), fullSync));
        } else {
            log.info("[{}][{}][{}] Sync is already started, skipping starting it now", getTenantId(), getEdgeId(), getSessionId());
        }
    }

    @Override
    public void sendDownlinkMsg(ResponseMsg responseMsg) {
        if (state.isConnected()) {
            String responseMsgStr = StringUtils.truncate(responseMsg.toString(), 10000);
            log.trace("[{}][{}] Sending downlink msg [{}]", getTenantId(), getEdgeId(), responseMsgStr);
            downlinkMsgLock.lock();
            String downlinkMsgStr = responseMsg.hasDownlinkMsg() ? String.valueOf(responseMsg.getDownlinkMsg().getDownlinkMsgId()) : responseMsgStr;
            try {
                outputStream.onNext(responseMsg);
            } catch (Exception e) {
                log.trace("[{}][{}] Failed to send downlink message [{}]", getTenantId(), getEdgeId(), downlinkMsgStr, e);
                state.setConnected(false);
                sessionCloseListener.accept(state.getEdge(), getSessionId());
            } finally {
                downlinkMsgLock.unlock();
            }
            log.trace("[{}][{}] downlink msg successfully sent [{}]", getTenantId(), getEdgeId(), downlinkMsgStr);
        }
    }

    @Override
    public void addHighPriorityEvent(EdgeEvent edgeEvent) {
        while (highPriorityQueue.size() > maxHighPriorityQueueSizePerSession) {
            EdgeEvent oldestHighPriority = highPriorityQueue.poll();
            if (oldestHighPriority != null) {
                log.warn("[{}][{}][{}] High priority queue is full. Removing oldest high priority event from queue {}",
                        state.getTenantId(), state.getEdgeId(), getSessionId(), oldestHighPriority);
            }
        }
        highPriorityQueue.add(edgeEvent);
        ctx.getStatsCounterService().ifPresent(statsCounterService ->
                statsCounterService.recordEvent(EdgeStatsKey.DOWNLINK_MSGS_ADDED, state.getEdge().getTenantId(), edgeEvent.getEdgeId(), 1));
    }

    @Override
    public void processHighPriorityEvents() {
        try {
            if (state.isConnected() && !state.isSyncInProgress()) {
                if (highPriorityQueue.isEmpty()) {
                    return;
                }
                List<EdgeEvent> highPriorityEvents = new ArrayList<>();
                EdgeEvent event;
                while ((event = highPriorityQueue.poll()) != null) {
                    highPriorityEvents.add(event);
                }
                log.trace("[{}][{}] Sending high priority events {}", getTenantId(), getEdgeId(), highPriorityEvents.size());
                List<DownlinkMsg> downlinkMsgsPack = downlinkMessageMapper.convertToDownlinkMsgsPack(state, highPriorityEvents);
                sendDownlinkMsgsPack(downlinkMsgsPack).get();
            }
        } catch (Exception e) {
            log.error("[{}] Failed to process high priority events", getEdgeId(), e);
        }
    }

    @Override
    public boolean hasHighPriorityEvents() {
        return !highPriorityQueue.isEmpty();
    }

    @Override
    public ListenableFuture<Pair<Long, Long>> fetchAndSendEdgeEvents(EdgeEventFetcher fetcher) {
        SettableFuture<Pair<Long, Long>> result = SettableFuture.create();
        PageLink pageLink = fetcher.getPageLink(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount());
        fetchAndSendEdgeEvents(fetcher, pageLink, result);
        return result;
    }

    @Override
    public ListenableFuture<Boolean> sendDownlinkMsgsPack(List<DownlinkMsg> downlinkMsgsPack) {
        interruptPreviousSendDownlinkMsgsTask();

        state.setSendDownlinkMsgsFuture(SettableFuture.create());
        state.getPendingMsgsMap().clear();

        downlinkMsgsPack.forEach(msg -> state.getPendingMsgsMap().put(msg.getDownlinkMsgId(), msg));
        scheduleDownlinkMsgsPackSend(1);

        return state.getSendDownlinkMsgsFuture();
    }

    @Override
    public void close() {
        log.debug("[{}][{}] Closing session", getTenantId(), getSessionId());
        state.setConnected(false);
        try {
            outputStream.onCompleted();
        } catch (Exception e) {
            log.debug("[{}][{}] Failed to close output stream: {}", getTenantId(), getSessionId(), e.getMessage());
        }
    }

    private void onUplinkMsg(UplinkMsg uplinkMsg) {
        if (isRateLimitViolated(uplinkMsg)) {
            return;
        }
        Futures.addCallback(uplinkMessageDispatcher.processUplinkMsg(state, uplinkMsg), new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                sendResponseMessage(uplinkMsg.getUplinkMsgId(), true, null);
            }

            @Override
            public void onFailure(Throwable t) {
                String errorMsg = EdgeUtils.createErrorMsgFromRootCauseAndStackTrace(t);
                sendResponseMessage(uplinkMsg.getUplinkMsgId(), false, errorMsg);
            }
        }, ctx.getGrpcCallbackExecutorService());
    }

    private boolean isRateLimitViolated(UplinkMsg uplinkMsg) {
        if (!ctx.getRateLimitService().checkRateLimit(LimitedApi.EDGE_UPLINK_MESSAGES, getTenantId()) ||
                !ctx.getRateLimitService().checkRateLimit(LimitedApi.EDGE_UPLINK_MESSAGES_PER_EDGE, getTenantId(), getEdgeId())) {
            String errorMsg = String.format("Failed to process uplink message. %s", RATE_LIMIT_REACHED);
            sendResponseMessage(uplinkMsg.getUplinkMsgId(), false, errorMsg);
            return true;
        }
        return false;
    }

    private void sendResponseMessage(int uplinkMsgId, boolean success, String errorMsg) {
        UplinkResponseMsg.Builder responseBuilder = UplinkResponseMsg.newBuilder()
                .setUplinkMsgId(uplinkMsgId)
                .setSuccess(success);
        if (errorMsg != null) {
            responseBuilder.setErrorMsg(errorMsg);
        }
        sendDownlinkMsg(ResponseMsg.newBuilder()
                .setUplinkResponseMsg(responseBuilder.build())
                .build());
    }

    private void onDownlinkResponse(DownlinkResponseMsg msg) {
        try {
            if (msg.getSuccess()) {
                state.getPendingMsgsMap().remove(msg.getDownlinkMsgId());
                ctx.getStatsCounterService().ifPresent(statsCounterService -> statsCounterService.recordEvent(EdgeStatsKey.DOWNLINK_MSGS_PUSHED, getTenantId(), getEdgeId(), 1));
                log.debug("[{}][{}][{}] Msg has been processed successfully! Msg Id: [{}], Msg: {}", getTenantId(), getEdgeId(), getSessionId(), msg.getDownlinkMsgId(), msg);
            } else {
                log.debug("[{}][{}][{}] Msg processing failed! Msg Id: [{}], Error msg: {}", getTenantId(), getEdgeId(), getSessionId(), msg.getDownlinkMsgId(), msg.getErrorMsg());
                DownlinkMsg downlinkMsg = state.getPendingMsgsMap().get(msg.getDownlinkMsgId());
                // if NOT timeseries or attributes failures - ack failed downlink
                if (downlinkMsg.getEntityDataCount() == 0) {
                    state.getPendingMsgsMap().remove(msg.getDownlinkMsgId());
                }
            }
            if (state.getPendingMsgsMap().isEmpty()) {
                log.debug("[{}][{}][{}] Pending msgs map is empty. Stopping current iteration", getTenantId(), getEdgeId(), getSessionId());
                stopCurrentSendDownlinkMsgsTask(false);
            }
        } catch (Exception e) {
            log.error("[{}][{}] Can't process downlink response message [{}]", getTenantId(), getEdgeId(), msg, e);
        }
    }

    private void interruptGeneralProcessingOnSync() {
        log.debug("[{}][{}][{}] Sync process started. General processing interrupted!", getTenantId(), getEdgeId(), getSessionId());
        stopCurrentSendDownlinkMsgsTask(true);
    }

    private void doSync(EdgeSyncCursor cursor) {
        if (cursor.hasNext()) {
            EdgeEventFetcher next = cursor.getNext();
            log.debug("[{}][{}] starting sync process, cursor current idx = {}, class = {}",
                    getTenantId(), getEdgeId(), cursor.getCurrentIdx(), next.getClass().getSimpleName());
            ListenableFuture<Pair<Long, Long>> future = fetchAndSendEdgeEvents(next);
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Pair<Long, Long> result) {
                    doSync(cursor);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Exception during sync process", getTenantId(), getEdgeId(), t);
                }
            }, ctx.getGrpcCallbackExecutorService());
        } else {
            log.info("[{}][{}] sync process completed", getTenantId(), getEdgeId());
            DownlinkMsg syncCompleteDownlinkMsg = DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .setSyncCompletedMsg(SyncCompletedMsg.newBuilder().build())
                    .build();
            Futures.addCallback(sendDownlinkMsgsPack(Collections.singletonList(syncCompleteDownlinkMsg)), new FutureCallback<>() {
                @Override
                public void onSuccess(Boolean isInterrupted) {
                    markSyncCompletedSendEdgeEventUpdate();
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Exception during sending sync complete", getTenantId(), getEdgeId(), t);
                    markSyncCompletedSendEdgeEventUpdate();
                }
            }, ctx.getGrpcCallbackExecutorService());
        }
    }

    private void markSyncCompletedSendEdgeEventUpdate() {
        state.finishSync();
        ctx.getClusterService().onEdgeEventUpdate(new EdgeEventUpdateMsg(getTenantId(), getEdgeId()));
    }

    private void fetchAndSendEdgeEvents(EdgeEventFetcher fetcher, PageLink pageLink, SettableFuture<Pair<Long, Long>> result) {
        Edge edge = state.getEdge();
        TenantId tenantId = getTenantId();
        try {
            log.trace("[{}] Start processing edge events, fetcher = {}, pageLink = {}", edge.getId(), fetcher.getClass().getSimpleName(), pageLink);
            processHighPriorityEvents();
            PageData<EdgeEvent> pageData = fetcher.fetchEdgeEvents(edge.getTenantId(), edge, pageLink);
            if (state.isConnected() && !pageData.getData().isEmpty()) {
                if (fetcher instanceof GeneralEdgeEventFetcher) {
                    long queueSize = pageData.getTotalElements() - ((long) pageLink.getPageSize() * pageLink.getPage());
                    ctx.getStatsCounterService().ifPresent(statsCounterService -> statsCounterService.setDownlinkMsgsLag(edge.getTenantId(), edge.getId(), queueSize));
                }
                log.trace("[{}][{}][{}] event(s) are going to be processed.", tenantId, edge.getId(), pageData.getData().size());
                List<DownlinkMsg> downlinkMsgsPack = downlinkMessageMapper.convertToDownlinkMsgsPack(state, pageData.getData());
                Futures.addCallback(sendDownlinkMsgsPack(downlinkMsgsPack), new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable Boolean isInterrupted) {
                        if (Boolean.TRUE.equals(isInterrupted)) {
                            log.debug("[{}][{}][{}] Send downlink messages task was interrupted", tenantId, edge.getId(), getSessionId());
                            result.set(null);
                        } else {
                            if (state.isConnected() && pageData.hasNext()) {
                                fetchAndSendEdgeEvents(fetcher, pageLink.nextPageLink(), result);
                            } else {
                                EdgeEvent latestEdgeEvent = pageData.getData().get(pageData.getData().size() - 1);
                                UUID idOffset = latestEdgeEvent.getUuidId();
                                if (idOffset != null) {
                                    Long newStartTs = Uuids.unixTimestamp(idOffset);
                                    long newStartSeqId = latestEdgeEvent.getSeqId();
                                    result.set(Pair.of(newStartTs, newStartSeqId));
                                } else {
                                    result.set(null);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("[{}] Failed to send downlink msgs pack", edge.getId(), t);
                        result.setException(t);
                    }
                }, ctx.getGrpcCallbackExecutorService());
            } else {
                log.trace("[{}] no event(s) found. Stop processing edge events, fetcher = {}, pageLink = {}", edge.getId(), fetcher.getClass().getSimpleName(), pageLink);
                result.set(null);
            }
        } catch (Exception e) {
            log.error("[{}] Failed to fetch edge events", edge.getId(), e);
            result.setException(e);
        }
    }

    private void scheduleDownlinkMsgsPackSend(int attempt) {
        Runnable sendDownlinkMsgsTask = () -> {
            try {
                if (!state.isConnected()) {
                    stopCurrentSendDownlinkMsgsTask(true);
                    return;
                }
                if (!state.getPendingMsgsMap().values().isEmpty()) {
                    Edge edge = state.getEdge();
                    List<DownlinkMsg> copy = new ArrayList<>(state.getPendingMsgsMap().values());
                    if (attempt > 1) {
                        String error = "Failed to deliver the batch";
                        String failureMsg = String.format("{%s} (size: {%s})", error, copy.size());
                        if (attempt == 2) {
                            // Send a failure notification only on the second attempt.
                            // This ensures that failure alerts are sent just once to avoid redundant notifications.
                            ctx.getRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(getTenantId())
                                    .edgeId(getEdgeId()).customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(failureMsg).error(error).build());
                        }
                        ctx.getStatsCounterService().ifPresent(statsCounterService ->
                                statsCounterService.recordEvent(EdgeStatsKey.DOWNLINK_MSGS_TMP_FAILED, edge.getTenantId(), getEdgeId(), 1));
                        log.warn("[{}][{}] {} on attempt {}", getTenantId(), getEdgeId(), failureMsg, attempt);
                        log.debug("[{}][{}] entities in failed batch: {}", getTenantId(), getEdgeId(), copy);
                    }
                    log.trace("[{}][{}][{}] downlink msg(s) are going to be send.", getTenantId(), getEdgeId(), copy.size());
                    for (DownlinkMsg downlinkMsg : copy) {
                        if (clientMaxInboundMessageSize != 0 && downlinkMsg.getSerializedSize() > clientMaxInboundMessageSize) {
                            String error = String.format("Client max inbound message size %s is exceeded. Please increase value of CLOUD_RPC_MAX_INBOUND_MESSAGE_SIZE " +
                                    "env variable on the edge and restart it.", clientMaxInboundMessageSize);
                            String message = String.format("Downlink msg size %s exceeds client max inbound message size %s. " +
                                    "Please increase value of CLOUD_RPC_MAX_INBOUND_MESSAGE_SIZE env variable on the edge and restart it.", downlinkMsg.getSerializedSize(), clientMaxInboundMessageSize);
                            log.error("[{}][{}][{}] {} Message {}", getTenantId(), getEdgeId(), getSessionId(), message, downlinkMsg);
                            ctx.getRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(getTenantId())
                                    .edgeId(getEdgeId()).customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(message).error(error).build());
                            ctx.getStatsCounterService().ifPresent(statsCounterService ->
                                    statsCounterService.recordEvent(EdgeStatsKey.DOWNLINK_MSGS_PERMANENTLY_FAILED, edge.getTenantId(), getEdgeId(), 1));
                            state.getPendingMsgsMap().remove(downlinkMsg.getDownlinkMsgId());
                        } else {
                            sendDownlinkMsg(ResponseMsg.newBuilder()
                                    .setDownlinkMsg(downlinkMsg)
                                    .build());
                        }
                    }
                    if (attempt < MAX_DOWNLINK_ATTEMPTS) {
                        scheduleDownlinkMsgsPackSend(attempt + 1);
                    } else {
                        String failureMsg = String.format("Failed to deliver messages: %s", copy);
                        log.warn("[{}][{}] Failed to deliver the batch after {} attempts. Next messages are going to be discarded {}",
                                getTenantId(), getEdgeId(), MAX_DOWNLINK_ATTEMPTS, copy);
                        ctx.getRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(getTenantId()).edgeId(getEdgeId())
                                .customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(failureMsg)
                                .error("Failed to deliver messages after " + MAX_DOWNLINK_ATTEMPTS + " attempts").build());
                        ctx.getStatsCounterService().ifPresent(statsCounterService -> statsCounterService.recordEvent(EdgeStatsKey.DOWNLINK_MSGS_PERMANENTLY_FAILED, edge.getTenantId(), getEdgeId(), copy.size()));
                        stopCurrentSendDownlinkMsgsTask(false);
                    }
                } else {
                    stopCurrentSendDownlinkMsgsTask(false);
                }
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to send downlink msgs. Error msg {}", getTenantId(), getEdgeId(), e.getMessage(), e);
                stopCurrentSendDownlinkMsgsTask(true);
            }
        };

        if (attempt == 1) {
            sendDownlinkExecutorService.submit(sendDownlinkMsgsTask);
        } else {
            state.setScheduledSendDownlinkTask(
                    sendDownlinkExecutorService.schedule(
                            sendDownlinkMsgsTask,
                            ctx.getEdgeEventStorageSettings().getSleepIntervalBetweenBatches(),
                            TimeUnit.MILLISECONDS)
            );
        }
    }

    private void interruptPreviousSendDownlinkMsgsTask() {
        if (state.getSendDownlinkMsgsFuture() != null && !state.getSendDownlinkMsgsFuture().isDone()
                || state.getScheduledSendDownlinkTask() != null && !state.getScheduledSendDownlinkTask().isCancelled()) {
            log.debug("[{}][{}][{}] Previous send downlink future was not properly completed, stopping it now!",
                    getTenantId(), getEdgeId(), getSessionId());
            stopCurrentSendDownlinkMsgsTask(true);
        } else {
            log.trace("[{}][{}][{}] Previous send downlink future is not active",
                    getTenantId(), getEdgeId(), getSessionId());
        }
    }

    private void stopCurrentSendDownlinkMsgsTask(Boolean isInterrupted) {
        if (state.getSendDownlinkMsgsFuture() != null && !state.getSendDownlinkMsgsFuture().isDone()) {
            state.getSendDownlinkMsgsFuture().set(isInterrupted);
        }
        if (state.getScheduledSendDownlinkTask() != null) {
            state.getScheduledSendDownlinkTask().cancel(true);
        }
    }

    private ConnectResponseMsg processConnect(ConnectRequestMsg request) {
        log.trace("[{}] processConnect [{}]", getSessionId(), request);
        Optional<Edge> optional = ctx.getEdgeService().findEdgeByRoutingKey(TenantId.SYS_TENANT_ID, request.getEdgeRoutingKey());
        if (optional.isPresent()) {
            Edge edge = optional.get();
            TenantId tenantId = edge.getTenantId();
            state.setEdge(edge);
            try {
                if (edge.getSecret().equals(request.getEdgeSecret())) {
                    sessionOpenListener.accept(edge.getId(), parentManagerRef);
                    state.setEdgeVersion(request.getEdgeVersion());
                    processSaveEdgeVersionAsAttribute(request.getEdgeVersion().name());
                    return ConnectResponseMsg.newBuilder()
                            .setResponseCode(ConnectResponseCode.ACCEPTED)
                            .setErrorMsg("")
                            .setConfiguration(EdgeMsgConstructorUtils.constructEdgeConfiguration(edge))
                            .setMaxInboundMessageSize(maxInboundMessageSize)
                            .build();
                }
                String error = "Failed to validate the edge!";
                String failureMsg = String.format("%s Provided request secret: %s", error, request.getEdgeSecret());
                ctx.getRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(tenantId).edgeId(edge.getId())
                        .customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(failureMsg).error(error).build());
                return ConnectResponseMsg.newBuilder()
                        .setResponseCode(ConnectResponseCode.BAD_CREDENTIALS)
                        .setErrorMsg(failureMsg)
                        .setConfiguration(EdgeConfiguration.getDefaultInstance()).build();
            } catch (Exception e) {
                String failureMsg = "Failed to process edge connection!";
                ctx.getRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(tenantId).edgeId(edge.getId())
                        .customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(failureMsg).error(e.getMessage()).build());
                log.error(failureMsg, e);
                return ConnectResponseMsg.newBuilder()
                        .setResponseCode(ConnectResponseCode.SERVER_UNAVAILABLE)
                        .setErrorMsg(failureMsg)
                        .setConfiguration(EdgeConfiguration.getDefaultInstance()).build();
            }
        }
        return ConnectResponseMsg.newBuilder()
                .setResponseCode(ConnectResponseCode.BAD_CREDENTIALS)
                .setErrorMsg("Failed to find the edge! Routing key: " + request.getEdgeRoutingKey())
                .setConfiguration(EdgeConfiguration.getDefaultInstance()).build();
    }

    private void processSaveEdgeVersionAsAttribute(String edgeVersion) {
        AttributeKvEntry attributeKvEntry = new BaseAttributeKvEntry(new StringDataEntry(DataConstants.EDGE_VERSION_ATTR_KEY, edgeVersion), System.currentTimeMillis());
        ctx.getAttributesService().save(getTenantId(), getEdgeId(), AttributeScope.SERVER_SCOPE, attributeKvEntry);
    }

    private TenantId getTenantId() {
        return state.getTenantId();
    }

    private EdgeId getEdgeId() {
        return state.getEdgeId();
    }

    private UUID getSessionId() {
        return state.getSessionId();
    }
}
