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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.grpc.stub.StreamObserver;
import lombok.Data;
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
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.notification.rule.trigger.EdgeCommunicationFailureTrigger;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.msg.edge.EdgeEventUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.v1.ConnectRequestMsg;
import org.thingsboard.server.gen.edge.v1.ConnectResponseCode;
import org.thingsboard.server.gen.edge.v1.ConnectResponseMsg;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.EdgeUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityViewsRequestMsg;
import org.thingsboard.server.gen.edge.v1.RelationRequestMsg;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RequestMsg;
import org.thingsboard.server.gen.edge.v1.RequestMsgType;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.v1.SyncCompletedMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.WidgetBundleTypesRequestMsg;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.fetch.EdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.GeneralEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.profile.AssetProfileProcessor;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.DashboardProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.profile.DeviceProfileProcessor;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewProcessor;
import org.thingsboard.server.service.edge.rpc.processor.relation.RelationProcessor;
import org.thingsboard.server.service.edge.rpc.processor.resource.ResourceProcessor;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

@Slf4j
@Data
public abstract class EdgeGrpcSession implements Closeable {

    private static final String QUEUE_START_TS_ATTR_KEY = "queueStartTs";
    private static final String QUEUE_START_SEQ_ID_ATTR_KEY = "queueStartSeqId";

    private static final int MAX_DOWNLINK_ATTEMPTS = 3;
    private static final String RATE_LIMIT_REACHED = "Rate limit reached";

    protected static final ConcurrentLinkedQueue<EdgeEvent> highPriorityQueue = new ConcurrentLinkedQueue<>();

    protected UUID sessionId;
    private BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener;
    private BiConsumer<Edge, UUID> sessionCloseListener;

    private final EdgeSessionState sessionState = new EdgeSessionState();
    private final ReentrantLock downlinkMsgLock = new ReentrantLock();

    protected EdgeContextComponent ctx;
    protected Edge edge;
    protected TenantId tenantId;

    private Long newStartTs;
    private Long previousStartTs;
    private Long newStartSeqId;
    private Long previousStartSeqId;

    private StreamObserver<RequestMsg> inputStream;
    private StreamObserver<ResponseMsg> outputStream;

    private volatile boolean connected;
    private volatile boolean syncInProgress;

    private EdgeVersion edgeVersion;
    private int maxInboundMessageSize;
    private int clientMaxInboundMessageSize;
    private int maxHighPriorityQueueSizePerSession;

    private ScheduledExecutorService sendDownlinkExecutorService;

    public EdgeGrpcSession(EdgeContextComponent ctx, StreamObserver<ResponseMsg> outputStream,
                           BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener,
                           BiConsumer<Edge, UUID> sessionCloseListener,
                           ScheduledExecutorService sendDownlinkExecutorService,
                           int maxInboundMessageSize, int maxHighPriorityQueueSizePerSession) {
        this.sessionId = UUID.randomUUID();
        this.ctx = ctx;
        this.outputStream = outputStream;
        this.sessionOpenListener = sessionOpenListener;
        this.sessionCloseListener = sessionCloseListener;
        this.sendDownlinkExecutorService = sendDownlinkExecutorService;
        this.maxInboundMessageSize = maxInboundMessageSize;
        this.maxHighPriorityQueueSizePerSession = maxHighPriorityQueueSizePerSession;
        initInputStream();
    }

    protected abstract ListenableFuture<Boolean> migrateEdgeEvents() throws Exception;

    public void initInputStream() {
        inputStream = new StreamObserver<>() {
            @Override
            public void onNext(RequestMsg requestMsg) {
                if (!connected && requestMsg.getMsgType().equals(RequestMsgType.CONNECT_RPC_MESSAGE)) {
                    ConnectResponseMsg responseMsg = processConnect(requestMsg.getConnectRequestMsg());
                    outputStream.onNext(ResponseMsg.newBuilder()
                            .setConnectResponseMsg(responseMsg)
                            .build());
                    if (ConnectResponseCode.ACCEPTED != responseMsg.getResponseCode()) {
                        outputStream.onError(new RuntimeException(responseMsg.getErrorMsg()));
                    } else {
                        if (requestMsg.getConnectRequestMsg().hasMaxInboundMessageSize()) {
                            log.debug("[{}][{}] Client max inbound message size: {}", tenantId, sessionId, requestMsg.getConnectRequestMsg().getMaxInboundMessageSize());
                            clientMaxInboundMessageSize = requestMsg.getConnectRequestMsg().getMaxInboundMessageSize();
                        }
                        connected = true;
                    }
                }
                if (connected) {
                    if (requestMsg.getMsgType().equals(RequestMsgType.SYNC_REQUEST_RPC_MESSAGE)) {
                        if (requestMsg.hasSyncRequestMsg()) {
                            boolean fullSync = false;
                            if (requestMsg.getSyncRequestMsg().hasFullSync()) {
                                fullSync = requestMsg.getSyncRequestMsg().getFullSync();
                            }
                            startSyncProcess(fullSync);
                        } else {
                            syncInProgress = false;
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
                log.error("[{}][{}] Stream was terminated due to error:", tenantId, sessionId, t);
                closeSession();
            }

            @Override
            public void onCompleted() {
                log.info("[{}][{}] Stream was closed and completed successfully!", tenantId, sessionId);
                closeSession();
            }

            private void closeSession() {
                connected = false;
                if (edge != null) {
                    try {
                        sessionCloseListener.accept(edge, sessionId);
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

    public void onConfigurationUpdate(Edge edge) {
        log.debug("[{}] onConfigurationUpdate [{}]", sessionId, edge);
        this.tenantId = edge.getTenantId();
        this.edge = edge;
        EdgeUpdateMsg edgeConfig = EdgeUpdateMsg.newBuilder()
                .setConfiguration(ctx.getEdgeMsgConstructor().constructEdgeConfiguration(edge)).build();
        ResponseMsg edgeConfigMsg = ResponseMsg.newBuilder()
                .setEdgeUpdateMsg(edgeConfig)
                .build();
        sendDownlinkMsg(edgeConfigMsg);
    }

    public void startSyncProcess(boolean fullSync) {
        if (!syncInProgress) {
            log.info("[{}][{}][{}] Staring edge sync process", tenantId, edge.getId(), sessionId);
            syncInProgress = true;
            interruptGeneralProcessingOnSync();
            doSync(new EdgeSyncCursor(ctx, edge, fullSync));
        } else {
            log.info("[{}][{}][{}] Sync is already started, skipping starting it now", tenantId, edge.getId(), sessionId);
        }
    }

    private void doSync(EdgeSyncCursor cursor) {
        if (cursor.hasNext()) {
            EdgeEventFetcher next = cursor.getNext();
            log.info("[{}][{}] starting sync process, cursor current idx = {}, class = {}",
                    tenantId, edge.getId(), cursor.getCurrentIdx(), next.getClass().getSimpleName());
            ListenableFuture<Pair<Long, Long>> future = startProcessingEdgeEvents(next);
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Pair<Long, Long> result) {
                    doSync(cursor);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Exception during sync process", tenantId, edge.getId(), t);
                }
            }, ctx.getGrpcCallbackExecutorService());
        } else {
            log.info("[{}][{}] sync process completed", tenantId, edge.getId());
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
                    log.error("[{}][{}] Exception during sending sync complete", tenantId, edge.getId(), t);
                    markSyncCompletedSendEdgeEventUpdate();
                }
            }, ctx.getGrpcCallbackExecutorService());
        }
    }

    protected void processEdgeEvents(EdgeEventFetcher fetcher, PageLink pageLink, SettableFuture<Pair<Long, Long>> result) {
        try {
            log.trace("[{}] Start processing edge events, fetcher = {}, pageLink = {}", sessionId, fetcher.getClass().getSimpleName(), pageLink);
            processHighPriorityEvents();
            PageData<EdgeEvent> pageData = fetcher.fetchEdgeEvents(edge.getTenantId(), edge, pageLink);
            if (isConnected() && !pageData.getData().isEmpty()) {
                log.trace("[{}][{}][{}] event(s) are going to be processed.", tenantId, sessionId, pageData.getData().size());
                List<DownlinkMsg> downlinkMsgsPack = convertToDownlinkMsgsPack(pageData.getData());
                Futures.addCallback(sendDownlinkMsgsPack(downlinkMsgsPack), new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable Boolean isInterrupted) {
                        if (Boolean.TRUE.equals(isInterrupted)) {
                            log.debug("[{}][{}][{}] Send downlink messages task was interrupted", tenantId, edge.getId(), sessionId);
                            result.set(null);
                        } else {
                            if (isConnected() && pageData.hasNext()) {
                                processEdgeEvents(fetcher, pageLink.nextPageLink(), result);
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
                        log.error("[{}] Failed to send downlink msgs pack", sessionId, t);
                        result.setException(t);
                    }
                }, ctx.getGrpcCallbackExecutorService());
            } else {
                log.trace("[{}] no event(s) found. Stop processing edge events, fetcher = {}, pageLink = {}", sessionId, fetcher.getClass().getSimpleName(), pageLink);
                result.set(null);
            }
        } catch (Exception e) {
            log.error("[{}] Failed to fetch edge events", sessionId, e);
            result.setException(e);
        }
    }

    private ConnectResponseMsg processConnect(ConnectRequestMsg request) {
        log.trace("[{}] processConnect [{}]", sessionId, request);
        Optional<Edge> optional = ctx.getEdgeService().findEdgeByRoutingKey(TenantId.SYS_TENANT_ID, request.getEdgeRoutingKey());
        if (optional.isPresent()) {
            edge = optional.get();
            tenantId = edge.getTenantId();
            try {
                if (edge.getSecret().equals(request.getEdgeSecret())) {
                    sessionOpenListener.accept(edge.getId(), this);
                    edgeVersion = request.getEdgeVersion();
                    processSaveEdgeVersionAsAttribute(request.getEdgeVersion().name());
                    return ConnectResponseMsg.newBuilder()
                            .setResponseCode(ConnectResponseCode.ACCEPTED)
                            .setErrorMsg("")
                            .setConfiguration(ctx.getEdgeMsgConstructor().constructEdgeConfiguration(edge))
                            .setMaxInboundMessageSize(maxInboundMessageSize)
                            .build();
                }
                String error = "Failed to validate the edge!";
                String failureMsg = String.format("%s Provided request secret: %s", error, request.getEdgeSecret());
                ctx.getNotificationRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(tenantId).edgeId(edge.getId())
                        .customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(failureMsg).error(error).build());
                return ConnectResponseMsg.newBuilder()
                        .setResponseCode(ConnectResponseCode.BAD_CREDENTIALS)
                        .setErrorMsg(failureMsg)
                        .setConfiguration(EdgeConfiguration.getDefaultInstance()).build();
            } catch (Exception e) {
                String failureMsg = "Failed to process edge connection!";
                ctx.getNotificationRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(tenantId).edgeId(edge.getId())
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
        ctx.getAttributesService().save(tenantId, edge.getId(), AttributeScope.SERVER_SCOPE, attributeKvEntry);
    }

    private void interruptGeneralProcessingOnSync() {
        log.debug("[{}][{}][{}] Sync process started. General processing interrupted!", tenantId, edge.getId(), sessionId);
        stopCurrentSendDownlinkMsgsTask(true);
    }

    protected ListenableFuture<Boolean> sendDownlinkMsgsPack(List<DownlinkMsg> downlinkMsgsPack) {
        interruptPreviousSendDownlinkMsgsTask();

        sessionState.setSendDownlinkMsgsFuture(SettableFuture.create());
        sessionState.getPendingMsgsMap().clear();

        downlinkMsgsPack.forEach(msg -> sessionState.getPendingMsgsMap().put(msg.getDownlinkMsgId(), msg));
        scheduleDownlinkMsgsPackSend(1);

        return sessionState.getSendDownlinkMsgsFuture();
    }

    private void interruptPreviousSendDownlinkMsgsTask() {
        if (sessionState.getSendDownlinkMsgsFuture() != null && !sessionState.getSendDownlinkMsgsFuture().isDone()
                || sessionState.getScheduledSendDownlinkTask() != null && !sessionState.getScheduledSendDownlinkTask().isCancelled()) {
            log.debug("[{}][{}][{}] Previous send downlink future was not properly completed, stopping it now!", tenantId, edge.getId(), sessionId);
            stopCurrentSendDownlinkMsgsTask(true);
        } else {
            log.trace("[{}][{}][{}] Previous send downlink future is not active", tenantId, edge.getId(), sessionId);
        }
    }

    private void onUplinkMsg(UplinkMsg uplinkMsg) {
        if (isRateLimitViolated(uplinkMsg)) {
            return;
        }
        ListenableFuture<List<Void>> future = processUplinkMsg(uplinkMsg);
        Futures.addCallback(future, new FutureCallback<>() {
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
        if (!ctx.getRateLimitService().checkRateLimit(LimitedApi.EDGE_UPLINK_MESSAGES, tenantId) ||
                !ctx.getRateLimitService().checkRateLimit(LimitedApi.EDGE_UPLINK_MESSAGES_PER_EDGE, tenantId, edge.getId())) {
            String errorMsg = String.format("Failed to process uplink message. %s", RATE_LIMIT_REACHED);
            sendResponseMessage(uplinkMsg.getUplinkMsgId(), false, errorMsg);
            return true;
        }
        return false;
    }

    private void scheduleDownlinkMsgsPackSend(int attempt) {
        Runnable sendDownlinkMsgsTask = () -> {
            try {
                if (isConnected() && !sessionState.getPendingMsgsMap().values().isEmpty()) {
                    List<DownlinkMsg> copy = new ArrayList<>(sessionState.getPendingMsgsMap().values());
                    if (attempt > 1) {
                        String error = "Failed to deliver the batch";
                        String failureMsg = String.format("{%s}: {%s}", error, copy);
                        if (attempt == 2) {
                            // Send a failure notification only on the second attempt.
                            // This ensures that failure alerts are sent just once to avoid redundant notifications.
                            ctx.getNotificationRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(tenantId)
                                    .edgeId(edge.getId()).customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(failureMsg).error(error).build());
                        }
                        log.warn("[{}][{}] {}, attempt: {}", tenantId, sessionId, failureMsg, attempt);
                    }
                    log.trace("[{}][{}][{}] downlink msg(s) are going to be send.", tenantId, sessionId, copy.size());
                    for (DownlinkMsg downlinkMsg : copy) {
                        if (clientMaxInboundMessageSize != 0 && downlinkMsg.getSerializedSize() > clientMaxInboundMessageSize) {
                            String error = String.format("Client max inbound message size %s is exceeded. Please increase value of CLOUD_RPC_MAX_INBOUND_MESSAGE_SIZE " +
                                    "env variable on the edge and restart it.", clientMaxInboundMessageSize);
                            String message = String.format("Downlink msg size %s exceeds client max inbound message size %s. " +
                                    "Please increase value of CLOUD_RPC_MAX_INBOUND_MESSAGE_SIZE env variable on the edge and restart it.", downlinkMsg.getSerializedSize(), clientMaxInboundMessageSize);
                            log.error("[{}][{}][{}] {} Message {}", tenantId, edge.getId(), sessionId, message, downlinkMsg);
                            ctx.getNotificationRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(tenantId)
                                    .edgeId(edge.getId()).customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(message).error(error).build());
                            sessionState.getPendingMsgsMap().remove(downlinkMsg.getDownlinkMsgId());
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
                                tenantId, sessionId, MAX_DOWNLINK_ATTEMPTS, copy);
                        ctx.getNotificationRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(tenantId).edgeId(edge.getId())
                                .customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(failureMsg)
                                .error("Failed to deliver messages after " + MAX_DOWNLINK_ATTEMPTS + " attempts").build());
                        stopCurrentSendDownlinkMsgsTask(false);
                    }
                } else {
                    stopCurrentSendDownlinkMsgsTask(false);
                }
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to send downlink msgs. Error msg {}", tenantId, sessionId, e.getMessage(), e);
                stopCurrentSendDownlinkMsgsTask(true);
            }
        };

        if (attempt == 1) {
            sendDownlinkExecutorService.submit(sendDownlinkMsgsTask);
        } else {
            sessionState.setScheduledSendDownlinkTask(
                    sendDownlinkExecutorService.schedule(
                            sendDownlinkMsgsTask,
                            ctx.getEdgeEventStorageSettings().getSleepIntervalBetweenBatches(),
                            TimeUnit.MILLISECONDS)
            );
        }
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
                sessionState.getPendingMsgsMap().remove(msg.getDownlinkMsgId());
                log.debug("[{}][{}][{}] Msg has been processed successfully! Msg Id: [{}], Msg: {}", tenantId, edge.getId(), sessionId, msg.getDownlinkMsgId(), msg);
            } else {
                log.error("[{}][{}][{}] Msg processing failed! Msg Id: [{}], Error msg: {}", tenantId, edge.getId(), sessionId, msg.getDownlinkMsgId(), msg.getErrorMsg());
            }
            if (sessionState.getPendingMsgsMap().isEmpty()) {
                log.debug("[{}][{}][{}] Pending msgs map is empty. Stopping current iteration", tenantId, edge.getId(), sessionId);
                stopCurrentSendDownlinkMsgsTask(false);
            }
        } catch (Exception e) {
            log.error("[{}][{}] Can't process downlink response message [{}]", tenantId, sessionId, msg, e);
        }
    }

    public void processHighPriorityEvents() {
        try {
            if (isConnected() && !isSyncInProgress()) {
                if (highPriorityQueue.isEmpty()) {
                    return;
                }
                List<EdgeEvent> highPriorityEvents = new ArrayList<>();
                EdgeEvent event;
                while ((event = highPriorityQueue.poll()) != null) {
                    highPriorityEvents.add(event);
                }
                log.trace("[{}][{}] Sending high priority events {}", tenantId, sessionId, highPriorityEvents.size());
                List<DownlinkMsg> downlinkMsgsPack = convertToDownlinkMsgsPack(highPriorityEvents);
                sendDownlinkMsgsPack(downlinkMsgsPack).get();
            }
        } catch (Exception e) {
            log.error("[{}] Failed to process high priority events", sessionId, e);
        }
    }

    public ListenableFuture<Boolean> processEdgeEvents() throws Exception {
        SettableFuture<Boolean> result = SettableFuture.create();
        if (isConnected() && !isSyncInProgress()) {
            Pair<Long, Long> startTsAndSeqId = getQueueStartTsAndSeqId().get();
            previousStartTs = startTsAndSeqId.getFirst();
            previousStartSeqId = startTsAndSeqId.getSecond();
            GeneralEdgeEventFetcher fetcher = new GeneralEdgeEventFetcher(
                    previousStartTs,
                    previousStartSeqId,
                    false,
                    Integer.toUnsignedLong(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount()),
                    ctx.getEdgeEventService());
            log.trace("[{}][{}] starting processing edge events, previousStartTs = {}, previousStartSeqId = {}",
                    tenantId, sessionId, previousStartTs, previousStartSeqId);
            Futures.addCallback(startProcessingEdgeEvents(fetcher), new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Pair<Long, Long> newStartTsAndSeqId) {
                    if (newStartTsAndSeqId != null) {
                        ListenableFuture<List<Long>> updateFuture = updateQueueStartTsAndSeqId(newStartTsAndSeqId);
                        Futures.addCallback(updateFuture, new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable List<Long> list) {
                                log.debug("[{}][{}] queue offset was updated [{}]", tenantId, sessionId, newStartTsAndSeqId);
                                boolean newEventsAvailable;
                                if (fetcher.isSeqIdNewCycleStarted()) {
                                    newEventsAvailable = isNewEdgeEventsAvailable();
                                } else {
                                    newEventsAvailable = isSeqIdStartedNewCycle();
                                    if (!newEventsAvailable) {
                                        newEventsAvailable = isNewEdgeEventsAvailable();
                                    }
                                }
                                result.set(newEventsAvailable);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.error("[{}][{}] Failed to update queue offset [{}]", tenantId, sessionId, newStartTsAndSeqId, t);
                                result.setException(t);
                            }
                        }, ctx.getGrpcCallbackExecutorService());
                    } else {
                        log.trace("[{}][{}] newStartTsAndSeqId is null. Skipping iteration without db update", tenantId, sessionId);
                        result.set(Boolean.FALSE);
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Failed to process events", tenantId, sessionId, t);
                    result.setException(t);
                }
            }, ctx.getGrpcCallbackExecutorService());
        } else {
            if (isSyncInProgress()) {
                log.trace("[{}][{}] edge sync is not completed yet. Skipping iteration", tenantId, sessionId);
                result.set(Boolean.TRUE);
            } else {
                log.trace("[{}][{}] edge is not connected. Skipping iteration", tenantId, sessionId);
                result.set(null);
            }
        }
        return result;
    }

    protected List<DownlinkMsg> convertToDownlinkMsgsPack(List<EdgeEvent> edgeEvents) {
        List<DownlinkMsg> result = new ArrayList<>();
        for (EdgeEvent edgeEvent : edgeEvents) {
            log.trace("[{}][{}] converting edge event to downlink msg [{}]", tenantId, sessionId, edgeEvent);
            DownlinkMsg downlinkMsg = null;
            try {
                switch (edgeEvent.getAction()) {
                    case UPDATED, ADDED, DELETED, ASSIGNED_TO_EDGE, UNASSIGNED_FROM_EDGE, ALARM_ACK, ALARM_CLEAR,
                         ALARM_DELETE, CREDENTIALS_UPDATED, RELATION_ADD_OR_UPDATE, RELATION_DELETED, RPC_CALL,
                         ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER, ADDED_COMMENT, UPDATED_COMMENT, DELETED_COMMENT -> {
                        downlinkMsg = convertEntityEventToDownlink(edgeEvent);
                        if (downlinkMsg != null && downlinkMsg.getWidgetTypeUpdateMsgCount() > 0) {
                            log.trace("[{}][{}] widgetTypeUpdateMsg message processed, downlinkMsgId = {}", tenantId, sessionId, downlinkMsg.getDownlinkMsgId());
                        } else {
                            log.trace("[{}][{}] entity message processed [{}]", tenantId, sessionId, downlinkMsg);
                        }
                    }
                    case ATTRIBUTES_UPDATED, POST_ATTRIBUTES, ATTRIBUTES_DELETED, TIMESERIES_UPDATED ->
                            downlinkMsg = ctx.getTelemetryProcessor().convertTelemetryEventToDownlink(edge, edgeEvent);
                    default -> log.warn("[{}][{}] Unsupported action type [{}]", tenantId, sessionId, edgeEvent.getAction());
                }
            } catch (Exception e) {
                log.error("[{}][{}] Exception during converting edge event to downlink msg", tenantId, sessionId, e);
            }
            if (downlinkMsg != null) {
                result.add(downlinkMsg);
            }
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
            log.trace("[{}][{}][{}] Checking if seq id started new cycle", tenantId, edge.getId(), sessionId);
            TimePageLink pageLink = new TimePageLink(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount(), 0, null, null, newStartTs, System.currentTimeMillis());
            PageData<EdgeEvent> edgeEvents = ctx.getEdgeEventService().findEdgeEvents(edge.getTenantId(), edge.getId(), 0L, previousStartSeqId == 0 ? null : previousStartSeqId - 1, pageLink);
            boolean result = !edgeEvents.getData().isEmpty();
            log.trace("[{}][{}][{}] Result of check if seq id started new cycle, result = {}", tenantId, edge.getId(), sessionId, result);
            return result;
        } catch (Exception e) {
            log.error("[{}][{}][{}] Failed to execute isSeqIdStartedNewCycle", tenantId, edge.getId(), sessionId, e);
        }
        return false;
    }

    private boolean isNewEdgeEventsAvailable() {
        try {
            log.trace("[{}][{}][{}] Checking if new edge events available", tenantId, edge.getId(), sessionId);
            TimePageLink pageLink = new TimePageLink(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount(), 0, null, null, newStartTs, System.currentTimeMillis());
            PageData<EdgeEvent> edgeEvents = ctx.getEdgeEventService().findEdgeEvents(edge.getTenantId(), edge.getId(), newStartSeqId, null, pageLink);
            boolean result = !edgeEvents.getData().isEmpty() || !highPriorityQueue.isEmpty();
            log.trace("[{}][{}][{}] Result of check if new edge events available, result = {}", tenantId, edge.getId(), sessionId, result);
            return result;
        } catch (Exception e) {
            log.error("[{}][{}][{}] Failed to execute isNewEdgeEventsAvailable", tenantId, edge.getId(), sessionId, e);
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
            log.error("[{}][{}][{}] Failed to execute findStartSeqIdFromOldestEventIfAny", tenantId, edge.getId(), sessionId, e);
        }
        return startSeqId;
    }

    private ListenableFuture<List<Long>> updateQueueStartTsAndSeqId(Pair<Long, Long> pair) {
        newStartTs = pair.getFirst();
        newStartSeqId = pair.getSecond();
        log.trace("[{}] updateQueueStartTsAndSeqId [{}][{}][{}]", sessionId, edge.getId(), newStartTs, newStartSeqId);
        List<AttributeKvEntry> attributes = Arrays.asList(
                new BaseAttributeKvEntry(new LongDataEntry(QUEUE_START_TS_ATTR_KEY, newStartTs), System.currentTimeMillis()),
                new BaseAttributeKvEntry(new LongDataEntry(QUEUE_START_SEQ_ID_ATTR_KEY, newStartSeqId), System.currentTimeMillis()));
        return ctx.getAttributesService().save(edge.getTenantId(), edge.getId(), AttributeScope.SERVER_SCOPE, attributes);
    }

    protected ListenableFuture<Pair<Long, Long>> startProcessingEdgeEvents(EdgeEventFetcher fetcher) {
        SettableFuture<Pair<Long, Long>> result = SettableFuture.create();
        PageLink pageLink = fetcher.getPageLink(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount());
        processEdgeEvents(fetcher, pageLink, result);
        return result;
    }

    private void markSyncCompletedSendEdgeEventUpdate() {
        syncInProgress = false;
        ctx.getClusterService().onEdgeEventUpdate(new EdgeEventUpdateMsg(edge.getTenantId(), edge.getId()));
    }

    private void stopCurrentSendDownlinkMsgsTask(Boolean isInterrupted) {
        if (sessionState.getSendDownlinkMsgsFuture() != null && !sessionState.getSendDownlinkMsgsFuture().isDone()) {
            sessionState.getSendDownlinkMsgsFuture().set(isInterrupted);
        }
        if (sessionState.getScheduledSendDownlinkTask() != null) {
            sessionState.getScheduledSendDownlinkTask().cancel(true);
        }
    }

    private void sendDownlinkMsg(ResponseMsg responseMsg) {
        if (isConnected()) {
            String responseMsgStr = StringUtils.truncate(responseMsg.toString(), 10000);
            log.trace("[{}][{}] Sending downlink msg [{}]", tenantId, sessionId, responseMsgStr);
            downlinkMsgLock.lock();
            String downlinkMsgStr = responseMsg.hasDownlinkMsg() ? String.valueOf(responseMsg.getDownlinkMsg().getDownlinkMsgId()) : responseMsgStr;
            try {
                outputStream.onNext(responseMsg);
            } catch (Exception e) {
                log.error("[{}][{}] Failed to send downlink message [{}]", tenantId, sessionId, downlinkMsgStr, e);
                connected = false;
                sessionCloseListener.accept(edge, sessionId);
            } finally {
                downlinkMsgLock.unlock();
            }
            log.trace("[{}][{}] downlink msg successfully sent [{}]", tenantId, sessionId, downlinkMsgStr);
        }
    }

    private DownlinkMsg convertEntityEventToDownlink(EdgeEvent edgeEvent) {
        log.trace("[{}][{}] Executing convertEntityEventToDownlink, edgeEvent [{}], action [{}]", tenantId, sessionId, edgeEvent, edgeEvent.getAction());
        return switch (edgeEvent.getType()) {
            case EDGE -> ctx.getEdgeProcessor().convertEdgeEventToDownlink(edgeEvent);
            case DEVICE -> ctx.getDeviceProcessor().convertDeviceEventToDownlink(edgeEvent, edgeVersion);
            case DEVICE_PROFILE -> ctx.getDeviceProfileProcessor().convertDeviceProfileEventToDownlink(edgeEvent, edgeVersion);
            case ASSET_PROFILE -> ctx.getAssetProfileProcessor().convertAssetProfileEventToDownlink(edgeEvent, edgeVersion);
            case ASSET -> ctx.getAssetProcessor().convertAssetEventToDownlink(edgeEvent, edgeVersion);
            case ENTITY_VIEW -> ctx.getEntityViewProcessor().convertEntityViewEventToDownlink(edgeEvent, edgeVersion);
            case DASHBOARD -> ctx.getDashboardProcessor().convertDashboardEventToDownlink(edgeEvent, edgeVersion);
            case CUSTOMER -> ctx.getCustomerProcessor().convertCustomerEventToDownlink(edgeEvent, edgeVersion);
            case RULE_CHAIN -> ctx.getRuleChainProcessor().convertRuleChainEventToDownlink(edgeEvent, edgeVersion);
            case RULE_CHAIN_METADATA -> ctx.getRuleChainProcessor().convertRuleChainMetadataEventToDownlink(edgeEvent, edgeVersion);
            case ALARM -> ctx.getAlarmProcessor().convertAlarmEventToDownlink(edgeEvent, edgeVersion);
            case ALARM_COMMENT -> ctx.getAlarmProcessor().convertAlarmCommentEventToDownlink(edgeEvent, edgeVersion);
            case USER -> ctx.getUserProcessor().convertUserEventToDownlink(edgeEvent, edgeVersion);
            case RELATION -> ctx.getRelationProcessor().convertRelationEventToDownlink(edgeEvent, edgeVersion);
            case WIDGETS_BUNDLE -> ctx.getWidgetBundleProcessor().convertWidgetsBundleEventToDownlink(edgeEvent, edgeVersion);
            case WIDGET_TYPE -> ctx.getWidgetTypeProcessor().convertWidgetTypeEventToDownlink(edgeEvent, edgeVersion);
            case ADMIN_SETTINGS -> ctx.getAdminSettingsProcessor().convertAdminSettingsEventToDownlink(edgeEvent, edgeVersion);
            case OTA_PACKAGE -> ctx.getOtaPackageProcessor().convertOtaPackageEventToDownlink(edgeEvent, edgeVersion);
            case TB_RESOURCE -> ctx.getResourceProcessor().convertResourceEventToDownlink(edgeEvent, edgeVersion);
            case QUEUE -> ctx.getQueueProcessor().convertQueueEventToDownlink(edgeEvent, edgeVersion);
            case TENANT -> ctx.getTenantProcessor().convertTenantEventToDownlink(edgeEvent, edgeVersion);
            case TENANT_PROFILE -> ctx.getTenantProfileProcessor().convertTenantProfileEventToDownlink(edgeEvent, edgeVersion);
            case NOTIFICATION_RULE -> ctx.getNotificationEdgeProcessor().convertNotificationRuleToDownlink(edgeEvent);
            case NOTIFICATION_TARGET -> ctx.getNotificationEdgeProcessor().convertNotificationTargetToDownlink(edgeEvent);
            case NOTIFICATION_TEMPLATE -> ctx.getNotificationEdgeProcessor().convertNotificationTemplateToDownlink(edgeEvent);
            case OAUTH2_CLIENT -> ctx.getOAuth2EdgeProcessor().convertOAuth2ClientEventToDownlink(edgeEvent, edgeVersion);
            case DOMAIN -> ctx.getOAuth2EdgeProcessor().convertOAuth2DomainEventToDownlink(edgeEvent, edgeVersion);
            default -> {
                log.warn("[{}][{}] Unsupported edge event type [{}]", tenantId, sessionId, edgeEvent);
                yield null;
            }
        };
    }

    public void addEventToHighPriorityQueue(EdgeEvent edgeEvent) {
        while (highPriorityQueue.size() > maxHighPriorityQueueSizePerSession) {
            EdgeEvent oldestHighPriority = highPriorityQueue.poll();
            if (oldestHighPriority != null) {
                log.warn("[{}][{}][{}] High priority queue is full. Removing oldest high priority event from queue {}",
                        tenantId, edge.getId(), sessionId, oldestHighPriority);
            }
        }
        highPriorityQueue.add(edgeEvent);
    }

    protected ListenableFuture<List<Void>> processUplinkMsg(UplinkMsg uplinkMsg) {
        List<ListenableFuture<Void>> result = new ArrayList<>();
        try {
            if (uplinkMsg.getDeviceProfileUpdateMsgCount() > 0) {
                for (DeviceProfileUpdateMsg deviceProfileUpdateMsg : uplinkMsg.getDeviceProfileUpdateMsgList()) {
                    result.add(((DeviceProfileProcessor) ctx.getDeviceProfileEdgeProcessorFactory().getProcessorByEdgeVersion(edgeVersion))
                            .processDeviceProfileMsgFromEdge(edge.getTenantId(), edge, deviceProfileUpdateMsg));
                }
            }
            if (uplinkMsg.getDeviceUpdateMsgCount() > 0) {
                for (DeviceUpdateMsg deviceUpdateMsg : uplinkMsg.getDeviceUpdateMsgList()) {
                    result.add(((DeviceProcessor) ctx.getDeviceEdgeProcessorFactory().getProcessorByEdgeVersion(edgeVersion))
                            .processDeviceMsgFromEdge(edge.getTenantId(), edge, deviceUpdateMsg));
                }
            }
            if (uplinkMsg.getDeviceCredentialsUpdateMsgCount() > 0) {
                for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg : uplinkMsg.getDeviceCredentialsUpdateMsgList()) {
                    result.add(((DeviceProcessor) ctx.getDeviceEdgeProcessorFactory().getProcessorByEdgeVersion(edgeVersion))
                            .processDeviceCredentialsMsgFromEdge(edge.getTenantId(), edge.getId(), deviceCredentialsUpdateMsg));
                }
            }
            if (uplinkMsg.getAssetProfileUpdateMsgCount() > 0) {
                for (AssetProfileUpdateMsg assetProfileUpdateMsg : uplinkMsg.getAssetProfileUpdateMsgList()) {
                    result.add(((AssetProfileProcessor) ctx.getAssetProfileEdgeProcessorFactory().getProcessorByEdgeVersion(edgeVersion))
                            .processAssetProfileMsgFromEdge(edge.getTenantId(), edge, assetProfileUpdateMsg));
                }
            }
            if (uplinkMsg.getAssetUpdateMsgCount() > 0) {
                for (AssetUpdateMsg assetUpdateMsg : uplinkMsg.getAssetUpdateMsgList()) {
                    result.add(((AssetProcessor) ctx.getAssetEdgeProcessorFactory().getProcessorByEdgeVersion(edgeVersion))
                            .processAssetMsgFromEdge(edge.getTenantId(), edge, assetUpdateMsg));
                }
            }
            if (uplinkMsg.getEntityViewUpdateMsgCount() > 0) {
                for (EntityViewUpdateMsg entityViewUpdateMsg : uplinkMsg.getEntityViewUpdateMsgList()) {
                    result.add(((EntityViewProcessor) ctx.getEntityViewProcessorFactory().getProcessorByEdgeVersion(edgeVersion))
                            .processEntityViewMsgFromEdge(edge.getTenantId(), edge, entityViewUpdateMsg));
                }
            }
            if (uplinkMsg.getEntityDataCount() > 0) {
                for (EntityDataProto entityData : uplinkMsg.getEntityDataList()) {
                    result.addAll(ctx.getTelemetryProcessor().processTelemetryMsg(edge.getTenantId(), entityData));
                }
            }
            if (uplinkMsg.getAlarmUpdateMsgCount() > 0) {
                for (AlarmUpdateMsg alarmUpdateMsg : uplinkMsg.getAlarmUpdateMsgList()) {
                    result.add(((AlarmProcessor) ctx.getAlarmEdgeProcessorFactory().getProcessorByEdgeVersion(edgeVersion))
                            .processAlarmMsgFromEdge(edge.getTenantId(), edge.getId(), alarmUpdateMsg));
                }
            }
            if (uplinkMsg.getAlarmCommentUpdateMsgCount() > 0) {
                for (AlarmCommentUpdateMsg alarmCommentUpdateMsg : uplinkMsg.getAlarmCommentUpdateMsgList()) {
                    result.add(((AlarmProcessor) ctx.getAlarmEdgeProcessorFactory().getProcessorByEdgeVersion(edgeVersion))
                            .processAlarmCommentMsgFromEdge(edge.getTenantId(), edge.getId(), alarmCommentUpdateMsg));
                }
            }
            if (uplinkMsg.getRelationUpdateMsgCount() > 0) {
                for (RelationUpdateMsg relationUpdateMsg : uplinkMsg.getRelationUpdateMsgList()) {
                    result.add(((RelationProcessor) ctx.getRelationEdgeProcessorFactory().getProcessorByEdgeVersion(edgeVersion))
                            .processRelationMsgFromEdge(edge.getTenantId(), edge, relationUpdateMsg));
                }
            }
            if (uplinkMsg.getDashboardUpdateMsgCount() > 0) {
                for (DashboardUpdateMsg dashboardUpdateMsg : uplinkMsg.getDashboardUpdateMsgList()) {
                    result.add(((DashboardProcessor) ctx.getDashboardEdgeProcessorFactory().getProcessorByEdgeVersion(edgeVersion))
                            .processDashboardMsgFromEdge(edge.getTenantId(), edge, dashboardUpdateMsg));
                }
            }
            if (uplinkMsg.getResourceUpdateMsgCount() > 0) {
                for (ResourceUpdateMsg resourceUpdateMsg : uplinkMsg.getResourceUpdateMsgList()) {
                    result.add(((ResourceProcessor) ctx.getResourceEdgeProcessorFactory().getProcessorByEdgeVersion(edgeVersion))
                            .processResourceMsgFromEdge(edge.getTenantId(), edge, resourceUpdateMsg));
                }
            }
            if (uplinkMsg.getRuleChainMetadataRequestMsgCount() > 0) {
                for (RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg : uplinkMsg.getRuleChainMetadataRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processRuleChainMetadataRequestMsg(edge.getTenantId(), edge, ruleChainMetadataRequestMsg));
                }
            }
            if (uplinkMsg.getAttributesRequestMsgCount() > 0) {
                for (AttributesRequestMsg attributesRequestMsg : uplinkMsg.getAttributesRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processAttributesRequestMsg(edge.getTenantId(), edge, attributesRequestMsg));
                }
            }
            if (uplinkMsg.getRelationRequestMsgCount() > 0) {
                for (RelationRequestMsg relationRequestMsg : uplinkMsg.getRelationRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processRelationRequestMsg(edge.getTenantId(), edge, relationRequestMsg));
                }
            }
            if (uplinkMsg.getUserCredentialsRequestMsgCount() > 0) {
                for (UserCredentialsRequestMsg userCredentialsRequestMsg : uplinkMsg.getUserCredentialsRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processUserCredentialsRequestMsg(edge.getTenantId(), edge, userCredentialsRequestMsg));
                }
            }
            if (uplinkMsg.getDeviceCredentialsRequestMsgCount() > 0) {
                for (DeviceCredentialsRequestMsg deviceCredentialsRequestMsg : uplinkMsg.getDeviceCredentialsRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processDeviceCredentialsRequestMsg(edge.getTenantId(), edge, deviceCredentialsRequestMsg));
                }
            }
            if (uplinkMsg.getDeviceRpcCallMsgCount() > 0) {
                for (DeviceRpcCallMsg deviceRpcCallMsg : uplinkMsg.getDeviceRpcCallMsgList()) {
                    result.add(((DeviceProcessor) ctx.getDeviceEdgeProcessorFactory().getProcessorByEdgeVersion(edgeVersion))
                            .processDeviceRpcCallFromEdge(edge.getTenantId(), edge, deviceRpcCallMsg));
                }
            }
            if (uplinkMsg.getWidgetBundleTypesRequestMsgCount() > 0) {
                for (WidgetBundleTypesRequestMsg widgetBundleTypesRequestMsg : uplinkMsg.getWidgetBundleTypesRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processWidgetBundleTypesRequestMsg(edge.getTenantId(), edge, widgetBundleTypesRequestMsg));
                }
            }
            if (uplinkMsg.getEntityViewsRequestMsgCount() > 0) {
                for (EntityViewsRequestMsg entityViewRequestMsg : uplinkMsg.getEntityViewsRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processEntityViewsRequestMsg(edge.getTenantId(), edge, entityViewRequestMsg));
                }
            }
        } catch (Exception e) {
            String failureMsg = String.format("Can't process uplink msg [%s] from edge", uplinkMsg);
            log.error("[{}][{}] Can't process uplink msg [{}]", tenantId, sessionId, uplinkMsg, e);
            ctx.getNotificationRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(tenantId).edgeId(edge.getId())
                    .customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(failureMsg).error(e.getMessage()).build());
            return Futures.immediateFailedFuture(e);
        }
        return Futures.allAsList(result);
    }

    protected void destroy() {}

    protected void cleanUp() {}

    @Override
    public void close() {
        log.debug("[{}][{}] Closing session", tenantId, sessionId);
        connected = false;
        try {
            outputStream.onCompleted();
        } catch (Exception e) {
            log.debug("[{}][{}] Failed to close output stream: {}", tenantId, sessionId, e.getMessage());
        }
    }

}
