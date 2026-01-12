/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.actors.device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.LinkedHashMapRemoveEldest;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.shared.AbstractContextAwareMsgProcessor;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.edge.EdgeHighPriorityMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponseActorMsg;
import org.thingsboard.server.common.msg.rpc.RemoveRpcActorMsg;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceCredentialsUpdateNotificationMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceEdgeUpdateMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.common.msg.timeout.DeviceActorServerSideRpcTimeoutMsg;
import org.thingsboard.server.common.util.KvProtoUtil;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ClaimDeviceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceSessionsCacheEntry;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionCloseNotificationProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionCloseReason;
import org.thingsboard.server.gen.transport.TransportProtos.SessionEvent;
import org.thingsboard.server.gen.transport.TransportProtos.SessionEventMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionSubscriptionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionType;
import org.thingsboard.server.gen.transport.TransportProtos.SubscribeToAttributeUpdatesMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SubscribeToRPCMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SubscriptionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcResponseStatusMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportUpdateCredentialsProto;
import org.thingsboard.server.gen.transport.TransportProtos.TransportToDeviceActorMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvProto;
import org.thingsboard.server.gen.transport.TransportProtos.UplinkNotificationMsg;
import org.thingsboard.server.service.rpc.RpcSubmitStrategy;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class DeviceActorMessageProcessor extends AbstractContextAwareMsgProcessor {

    final TenantId tenantId;
    final DeviceId deviceId;
    final LinkedHashMapRemoveEldest<UUID, SessionInfoMetaData> sessions;
    final Map<UUID, SessionInfo> attributeSubscriptions;
    final Map<UUID, SessionInfo> rpcSubscriptions;
    private final Map<Integer, ToDeviceRpcRequestMetadata> toDeviceRpcPendingMap;
    private final boolean rpcSequential;
    private final RpcSubmitStrategy rpcSubmitStrategy;
    private final ScheduledExecutorService scheduler;
    private final boolean closeTransportSessionOnRpcDeliveryTimeout;

    private int rpcSeq = 0;
    private String deviceName;
    private String deviceType;
    private TbMsgMetaData defaultMetaData;
    private EdgeId edgeId;
    private ScheduledFuture<?> awaitRpcResponseFuture;

    DeviceActorMessageProcessor(ActorSystemContext systemContext, TenantId tenantId, DeviceId deviceId) {
        super(systemContext);
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.rpcSubmitStrategy = RpcSubmitStrategy.parse(systemContext.getRpcSubmitStrategy());
        this.closeTransportSessionOnRpcDeliveryTimeout = systemContext.isCloseTransportSessionOnRpcDeliveryTimeout();
        this.rpcSequential = !rpcSubmitStrategy.equals(RpcSubmitStrategy.BURST);
        this.attributeSubscriptions = new HashMap<>();
        this.rpcSubscriptions = new HashMap<>();
        this.toDeviceRpcPendingMap = new LinkedHashMap<>();
        this.sessions = new LinkedHashMapRemoveEldest<>(systemContext.getMaxConcurrentSessionsPerDevice(), this::notifyTransportAboutClosedSessionMaxSessionsLimit);
        this.scheduler = systemContext.getScheduler();
        if (initAttributes()) {
            restoreSessions();
        }
    }

    boolean initAttributes() {
        Device device = systemContext.getDeviceService().findDeviceById(tenantId, deviceId);
        if (device != null) {
            this.deviceName = device.getName();
            this.deviceType = device.getType();
            this.defaultMetaData = new TbMsgMetaData();
            this.defaultMetaData.putValue("deviceName", deviceName);
            this.defaultMetaData.putValue("deviceType", deviceType);
            if (systemContext.isEdgesEnabled()) {
                this.edgeId = findRelatedEdgeId();
            }
            return true;
        } else {
            return false;
        }
    }

    private EdgeId findRelatedEdgeId() {
        List<EntityRelation> result =
                systemContext.getRelationService().findByToAndType(tenantId, deviceId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
        if (result != null && !result.isEmpty()) {
            EntityRelation relationToEdge = result.get(0);
            if (relationToEdge.getFrom() != null && relationToEdge.getFrom().getId() != null) {
                log.trace("[{}][{}] found edge [{}] for device", tenantId, deviceId, relationToEdge.getFrom().getId());
                return new EdgeId(relationToEdge.getFrom().getId());
            } else {
                log.trace("[{}][{}] edge relation is empty {}", tenantId, deviceId, relationToEdge);
            }
        } else {
            log.trace("[{}][{}] device doesn't have any related edge", tenantId, deviceId);
        }
        return null;
    }

    void processRpcRequest(TbActorCtx context, ToDeviceRpcRequestActorMsg msg) {
        ToDeviceRpcRequest request = msg.getMsg();
        UUID rpcId = request.getId();
        log.debug("[{}][{}] Received RPC request to process ...", deviceId, rpcId);
        ToDeviceRpcRequestMsg rpcRequest = createToDeviceRpcRequestMsg(request);

        long timeout = request.getExpirationTime() - System.currentTimeMillis();
        boolean persisted = request.isPersisted();

        if (timeout <= 0) {
            log.debug("[{}][{}] Ignoring message due to exp time reached, {}", deviceId, rpcId, request.getExpirationTime());
            if (persisted) {
                createRpc(request, RpcStatus.EXPIRED);
            }
            return;
        } else if (persisted) {
            createRpc(request, RpcStatus.QUEUED);
        }

        boolean sent = false;
        int requestId = rpcRequest.getRequestId();
        if (systemContext.isEdgesEnabled() && edgeId != null) {
            log.debug("[{}][{}] device is related to edge: [{}]. Saving RPC request: [{}][{}] to edge queue", tenantId, deviceId, edgeId.getId(), rpcId, requestId);
            try {
                if (systemContext.getEdgeService().isEdgeActiveAsync(tenantId, edgeId, DefaultDeviceStateService.ACTIVITY_STATE).get()) {
                    saveRpcRequestToEdgeQueue(request, requestId);
                } else {
                    log.error("[{}][{}][{}] Failed to save RPC request to edge queue {}. The Edge is currently offline or unreachable", tenantId, deviceId, edgeId.getId(), request);
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("[{}][{}][{}] Failed to save RPC request to edge queue {}", tenantId, deviceId, edgeId.getId(), request, e);
            }
        } else if (isSendNewRpcAvailable()) {
            sent = !rpcSubscriptions.isEmpty();
            Set<UUID> syncSessionSet = new HashSet<>();
            rpcSubscriptions.forEach((sessionId, sessionInfo) -> {
                log.debug("[{}][{}][{}][{}] send RPC request to transport ...", deviceId, sessionId, rpcId, requestId);
                sendToTransport(rpcRequest, sessionId, sessionInfo.getNodeId());
                if (SessionType.SYNC == sessionInfo.getType()) {
                    syncSessionSet.add(sessionId);
                }
            });
            log.trace("Rpc syncSessionSet [{}] subscription after sent [{}]", syncSessionSet, rpcSubscriptions);
            syncSessionSet.forEach(rpcSubscriptions::remove);
        }

        if (persisted) {
            ObjectNode response = JacksonUtil.newObjectNode();
            response.put("rpcId", rpcId.toString());
            systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(new FromDeviceRpcResponse(rpcId, JacksonUtil.toString(response), null));
        }

        if (!persisted && request.isOneway() && sent) {
            log.debug("[{}] RPC command response sent [{}][{}]!", deviceId, rpcId, requestId);
            systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(new FromDeviceRpcResponse(rpcId, null, null));
        } else {
            registerPendingRpcRequest(context, msg, sent, rpcRequest, timeout);
        }
        String rpcSent = sent ? "sent!" : "NOT sent!";
        log.debug("[{}][{}][{}] RPC request is {}", deviceId, rpcId, requestId, rpcSent);
    }

    private boolean isSendNewRpcAvailable() {
        return switch (rpcSubmitStrategy) {
            case SEQUENTIAL_ON_ACK_FROM_DEVICE -> toDeviceRpcPendingMap.values().stream().filter(md -> !md.isDelivered()).findAny().isEmpty();
            case SEQUENTIAL_ON_RESPONSE_FROM_DEVICE -> toDeviceRpcPendingMap.isEmpty();
            default -> true;
        };
    }

    private void createRpc(ToDeviceRpcRequest request, RpcStatus status) {
        Rpc rpc = new Rpc(new RpcId(request.getId()));
        rpc.setCreatedTime(System.currentTimeMillis());
        rpc.setTenantId(tenantId);
        rpc.setDeviceId(deviceId);
        rpc.setExpirationTime(request.getExpirationTime());
        rpc.setRequest(JacksonUtil.valueToTree(request));
        rpc.setStatus(status);
        rpc.setAdditionalInfo(getAdditionalInfo(request));
        systemContext.getTbRpcService().save(tenantId, rpc);
    }

    private JsonNode getAdditionalInfo(ToDeviceRpcRequest request) {
        try {
            return JacksonUtil.toJsonNode(request.getAdditionalInfo());
        } catch (IllegalArgumentException e) {
            log.debug("Failed to parse additional info [{}]", request.getAdditionalInfo());
            return JacksonUtil.valueToTree(request.getAdditionalInfo());
        }
    }

    private ToDeviceRpcRequestMsg createToDeviceRpcRequestMsg(ToDeviceRpcRequest request) {
        ToDeviceRpcRequestBody body = request.getBody();
        return ToDeviceRpcRequestMsg.newBuilder()
                .setRequestId(rpcSeq++)
                .setMethodName(body.getMethod())
                .setParams(body.getParams())
                .setExpirationTime(request.getExpirationTime())
                .setRequestIdMSB(request.getId().getMostSignificantBits())
                .setRequestIdLSB(request.getId().getLeastSignificantBits())
                .setOneway(request.isOneway())
                .setPersisted(request.isPersisted())
                .build();
    }

    void processRpcResponsesFromEdge(FromDeviceRpcResponseActorMsg responseMsg) {
        log.debug("[{}] Processing RPC command response from edge session", deviceId);
        ToDeviceRpcRequestMetadata requestMd = toDeviceRpcPendingMap.remove(responseMsg.getRequestId());
        boolean success = requestMd != null;
        if (success) {
            systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(responseMsg.getMsg());
        } else {
            log.debug("[{}] RPC command response [{}] is stale!", deviceId, responseMsg.getRequestId());
        }
    }

    void processRemoveRpc(RemoveRpcActorMsg msg) {
        UUID rpcId = msg.getRequestId();
        log.debug("[{}][{}] Received remove RPC request ...", deviceId, rpcId);
        Map.Entry<Integer, ToDeviceRpcRequestMetadata> entry = null;
        for (Map.Entry<Integer, ToDeviceRpcRequestMetadata> e : toDeviceRpcPendingMap.entrySet()) {
            if (e.getValue().getMsg().getMsg().getId().equals(rpcId)) {
                entry = e;
                break;
            }
        }

        if (entry != null) {
            Integer requestId = entry.getKey();
            if (entry.getValue().isDelivered()) {
                toDeviceRpcPendingMap.remove(requestId);
                if (rpcSubmitStrategy.equals(RpcSubmitStrategy.SEQUENTIAL_ON_RESPONSE_FROM_DEVICE)) {
                    clearAwaitRpcResponseScheduler();
                    sendNextPendingRequest(rpcId, requestId, "Removed pending RPC!");
                }
            } else {
                Optional<Map.Entry<Integer, ToDeviceRpcRequestMetadata>> firstRpc = getFirstRpc();
                if (firstRpc.isPresent() && requestId.equals(firstRpc.get().getKey())) {
                    toDeviceRpcPendingMap.remove(requestId);
                    sendNextPendingRequest(rpcId, requestId, "Removed pending RPC!");
                } else {
                    toDeviceRpcPendingMap.remove(requestId);
                }
            }
        }
    }

    private void registerPendingRpcRequest(TbActorCtx context, ToDeviceRpcRequestActorMsg msg, boolean sent, ToDeviceRpcRequestMsg rpcRequest, long timeout) {
        int requestId = rpcRequest.getRequestId();
        UUID rpcId = new UUID(rpcRequest.getRequestIdMSB(), rpcRequest.getRequestIdLSB());
        log.debug("[{}][{}][{}] Registering pending RPC request...", deviceId, rpcId, requestId);
        toDeviceRpcPendingMap.put(requestId, new ToDeviceRpcRequestMetadata(msg, sent));
        DeviceActorServerSideRpcTimeoutMsg timeoutMsg = new DeviceActorServerSideRpcTimeoutMsg(requestId, timeout);
        scheduleMsgWithDelay(context, timeoutMsg, timeoutMsg.getTimeout());
    }

    void processServerSideRpcTimeout(DeviceActorServerSideRpcTimeoutMsg msg) {
        Integer requestId = msg.getId();
        var requestMd = toDeviceRpcPendingMap.remove(requestId);
        if (requestMd != null) {
            var toDeviceRpcRequest = requestMd.getMsg().getMsg();
            UUID rpcId = toDeviceRpcRequest.getId();
            log.debug("[{}][{}][{}] RPC request timeout detected!", deviceId, rpcId, requestId);
            if (toDeviceRpcRequest.isPersisted()) {
                systemContext.getTbRpcService().save(tenantId, new RpcId(rpcId), RpcStatus.EXPIRED, null);
            }
            systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(new FromDeviceRpcResponse(rpcId,
                    null, requestMd.isSent() ? RpcError.TIMEOUT : RpcError.NO_ACTIVE_CONNECTION));
            if (!requestMd.isDelivered()) {
                sendNextPendingRequest(rpcId, requestId, "Pending RPC timeout detected!");
                return;
            }
            if (rpcSubmitStrategy.equals(RpcSubmitStrategy.SEQUENTIAL_ON_RESPONSE_FROM_DEVICE)) {
                clearAwaitRpcResponseScheduler();
                sendNextPendingRequest(rpcId, requestId, "Pending RPC timeout detected!");
            }
        }
    }

    private void sendPendingRequests(UUID sessionId, String nodeId) {
        SessionType sessionType = getSessionType(sessionId);
        if (!toDeviceRpcPendingMap.isEmpty()) {
            log.debug("[{}] Pushing {} pending RPC messages to session: [{}]", deviceId, sessionId, toDeviceRpcPendingMap.size());
            if (sessionType == SessionType.SYNC) {
                log.debug("[{}] Cleanup sync RPC session [{}]", deviceId, sessionId);
                rpcSubscriptions.remove(sessionId);
            }
        } else {
            log.debug("[{}] No pending RPC messages for session: [{}]", deviceId, sessionId);
        }
        Set<Integer> sentOneWayIds = new HashSet<>();

        if (rpcSequential) {
            getFirstRpc().ifPresent(processPendingRpc(sessionId, nodeId, sentOneWayIds));
        } else if (sessionType == SessionType.ASYNC) {
            toDeviceRpcPendingMap.entrySet().forEach(processPendingRpc(sessionId, nodeId, sentOneWayIds));
        } else {
            toDeviceRpcPendingMap.entrySet().stream().findFirst().ifPresent(processPendingRpc(sessionId, nodeId, sentOneWayIds));
        }

        sentOneWayIds.stream().filter(id -> !toDeviceRpcPendingMap.get(id).getMsg().getMsg().isPersisted()).forEach(toDeviceRpcPendingMap::remove);
    }

    private Optional<Map.Entry<Integer, ToDeviceRpcRequestMetadata>> getFirstRpc() {
        if (rpcSubmitStrategy.equals(RpcSubmitStrategy.SEQUENTIAL_ON_RESPONSE_FROM_DEVICE)) {
            return toDeviceRpcPendingMap.entrySet().stream()
                    .findFirst().filter(entry -> {
                        var md = entry.getValue();
                        if (md.isDelivered()) {
                            if (awaitRpcResponseFuture == null || awaitRpcResponseFuture.isCancelled()) {
                                var toDeviceRpcRequest = md.getMsg().getMsg();
                                awaitRpcResponseFuture = scheduleAwaitRpcResponseFuture(toDeviceRpcRequest.getId(), entry.getKey());
                            }
                            return false;
                        }
                        return true;
                    });
        }
        return toDeviceRpcPendingMap.entrySet().stream().filter(e -> !e.getValue().isDelivered()).findFirst();
    }

    private void sendNextPendingRequest(UUID rpcId, int requestId, String logMessage) {
        log.debug("[{}][{}][{}] {} Going to send next pending request ...", deviceId, rpcId, requestId, logMessage);
        if (rpcSequential) {
            rpcSubscriptions.forEach((id, s) -> sendPendingRequests(id, s.getNodeId()));
        }
    }

    private Consumer<Map.Entry<Integer, ToDeviceRpcRequestMetadata>> processPendingRpc(UUID sessionId, String nodeId, Set<Integer> sentOneWayIds) {
        return entry -> {
            ToDeviceRpcRequest request = entry.getValue().getMsg().getMsg();
            ToDeviceRpcRequestBody body = request.getBody();
            Integer requestId = entry.getKey();
            UUID rpcId = request.getId();
            if (request.isOneway() && !rpcSequential) {
                sentOneWayIds.add(requestId);
                systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(new FromDeviceRpcResponse(rpcId, null, null));
            }
            ToDeviceRpcRequestMsg rpcRequest = ToDeviceRpcRequestMsg.newBuilder()
                    .setRequestId(requestId)
                    .setMethodName(body.getMethod())
                    .setParams(body.getParams())
                    .setExpirationTime(request.getExpirationTime())
                    .setRequestIdMSB(rpcId.getMostSignificantBits())
                    .setRequestIdLSB(rpcId.getLeastSignificantBits())
                    .setOneway(request.isOneway())
                    .setPersisted(request.isPersisted())
                    .build();
            log.debug("[{}][{}][{}][{}] Send pending RPC request to transport ...", deviceId, sessionId, rpcId, requestId);
            sendToTransport(rpcRequest, sessionId, nodeId);
        };
    }

    void process(TransportToDeviceActorMsgWrapper wrapper) {
        TransportToDeviceActorMsg msg = wrapper.getMsg();
        TbCallback callback = wrapper.getCallback();
        var sessionInfo = msg.getSessionInfo();

        if (msg.hasSessionEvent()) {
            processSessionStateMsgs(sessionInfo, msg.getSessionEvent());
        }
        if (msg.hasSubscribeToAttributes()) {
            processSubscriptionCommands(sessionInfo, msg.getSubscribeToAttributes());
        }
        if (msg.hasSubscribeToRPC()) {
            processSubscriptionCommands(sessionInfo, msg.getSubscribeToRPC());
        }
        if (msg.hasSendPendingRPC()) {
            sendPendingRequests(getSessionId(sessionInfo), sessionInfo.getNodeId());
        }
        if (msg.hasGetAttributes()) {
            handleGetAttributesRequest(sessionInfo, msg.getGetAttributes());
        }
        if (msg.hasToDeviceRPCCallResponse()) {
            processRpcResponses(sessionInfo, msg.getToDeviceRPCCallResponse());
        }
        if (msg.hasSubscriptionInfo()) {
            handleSessionActivity(sessionInfo, msg.getSubscriptionInfo());
        }
        if (msg.hasClaimDevice()) {
            handleClaimDeviceMsg(sessionInfo, msg.getClaimDevice());
        }
        if (msg.hasRpcResponseStatusMsg()) {
            processRpcResponseStatus(sessionInfo, msg.getRpcResponseStatusMsg());
        }
        if (msg.hasUplinkNotificationMsg()) {
            processUplinkNotificationMsg(sessionInfo, msg.getUplinkNotificationMsg());
        }
        callback.onSuccess();
    }

    private void processUplinkNotificationMsg(SessionInfoProto sessionInfo, UplinkNotificationMsg uplinkNotificationMsg) {
        String nodeId = sessionInfo.getNodeId();
        sessions.entrySet().stream()
                .filter(kv -> kv.getValue().getSessionInfo().getNodeId().equals(nodeId) && (kv.getValue().isSubscribedToAttributes() || kv.getValue().isSubscribedToRPC()))
                .forEach(kv -> {
                    ToTransportMsg msg = ToTransportMsg.newBuilder()
                            .setSessionIdMSB(kv.getKey().getMostSignificantBits())
                            .setSessionIdLSB(kv.getKey().getLeastSignificantBits())
                            .setUplinkNotificationMsg(uplinkNotificationMsg)
                            .build();
                    systemContext.getTbCoreToTransportService().process(kv.getValue().getSessionInfo().getNodeId(), msg);
                });
    }

    private void handleClaimDeviceMsg(SessionInfoProto sessionInfo, ClaimDeviceMsg msg) {
        UUID sessionId = getSessionId(sessionInfo);
        DeviceId deviceId = new DeviceId(new UUID(msg.getDeviceIdMSB(), msg.getDeviceIdLSB()));
        ListenableFuture<Void> registrationFuture = systemContext.getClaimDevicesService()
                .registerClaimingInfo(tenantId, deviceId, msg.getSecretKey(), msg.getDurationMs());
        Futures.addCallback(registrationFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(Void result) {
                log.debug("[{}][{}] Successfully processed register claiming info request!", sessionId, deviceId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}][{}] Failed to process register claiming info request due to: ", sessionId, deviceId, t);
            }
        }, MoreExecutors.directExecutor());
    }

    private void reportSessionOpen() {
        systemContext.getDeviceStateService().onDeviceConnect(tenantId, deviceId);
    }

    private void reportSessionClose() {
        systemContext.getDeviceStateService().onDeviceDisconnect(tenantId, deviceId);
    }

    private void handleGetAttributesRequest(SessionInfoProto sessionInfo, GetAttributeRequestMsg request) {
        int requestId = request.getRequestId();
        if (request.getOnlyShared()) {
            Futures.addCallback(findAllAttributesByScope(AttributeScope.SHARED_SCOPE), new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable List<AttributeKvEntry> result) {
                    GetAttributeResponseMsg responseMsg = GetAttributeResponseMsg.newBuilder()
                            .setRequestId(requestId)
                            .setSharedStateMsg(true)
                            .addAllSharedAttributeList(KvProtoUtil.attrToTsKvProtos(result))
                            .setIsMultipleAttributesRequest(request.getSharedAttributeNamesCount() > 1)
                            .build();
                    sendToTransport(responseMsg, sessionInfo);
                }

                @Override
                public void onFailure(Throwable t) {
                    GetAttributeResponseMsg responseMsg = GetAttributeResponseMsg.newBuilder()
                            .setError(t.getMessage())
                            .setSharedStateMsg(true)
                            .build();
                    sendToTransport(responseMsg, sessionInfo);
                }
            }, MoreExecutors.directExecutor());
        } else {
            Futures.addCallback(getAttributesKvEntries(request), new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable List<List<AttributeKvEntry>> result) {
                    GetAttributeResponseMsg responseMsg = GetAttributeResponseMsg.newBuilder()
                            .setRequestId(requestId)
                            .addAllClientAttributeList(KvProtoUtil.attrToTsKvProtos(result.get(0)))
                            .addAllSharedAttributeList(KvProtoUtil.attrToTsKvProtos(result.get(1)))
                            .setIsMultipleAttributesRequest(
                                    request.getSharedAttributeNamesCount() + request.getClientAttributeNamesCount() > 1)
                            .build();
                    sendToTransport(responseMsg, sessionInfo);
                }

                @Override
                public void onFailure(Throwable t) {
                    GetAttributeResponseMsg responseMsg = GetAttributeResponseMsg.newBuilder()
                            .setError(t.getMessage())
                            .build();
                    sendToTransport(responseMsg, sessionInfo);
                }
            }, MoreExecutors.directExecutor());
        }
    }

    private ListenableFuture<List<List<AttributeKvEntry>>> getAttributesKvEntries(GetAttributeRequestMsg request) {
        ListenableFuture<List<AttributeKvEntry>> clientAttributesFuture;
        ListenableFuture<List<AttributeKvEntry>> sharedAttributesFuture;
        if (CollectionUtils.isEmpty(request.getClientAttributeNamesList()) && CollectionUtils.isEmpty(request.getSharedAttributeNamesList())) {
            clientAttributesFuture = findAllAttributesByScope(AttributeScope.CLIENT_SCOPE);
            sharedAttributesFuture = findAllAttributesByScope(AttributeScope.SHARED_SCOPE);
        } else if (!CollectionUtils.isEmpty(request.getClientAttributeNamesList()) && !CollectionUtils.isEmpty(request.getSharedAttributeNamesList())) {
            clientAttributesFuture = findAttributesByScope(toSet(request.getClientAttributeNamesList()), AttributeScope.CLIENT_SCOPE);
            sharedAttributesFuture = findAttributesByScope(toSet(request.getSharedAttributeNamesList()), AttributeScope.SHARED_SCOPE);
        } else if (CollectionUtils.isEmpty(request.getClientAttributeNamesList()) && !CollectionUtils.isEmpty(request.getSharedAttributeNamesList())) {
            clientAttributesFuture = Futures.immediateFuture(Collections.emptyList());
            sharedAttributesFuture = findAttributesByScope(toSet(request.getSharedAttributeNamesList()), AttributeScope.SHARED_SCOPE);
        } else {
            sharedAttributesFuture = Futures.immediateFuture(Collections.emptyList());
            clientAttributesFuture = findAttributesByScope(toSet(request.getClientAttributeNamesList()), AttributeScope.CLIENT_SCOPE);
        }
        return Futures.allAsList(Arrays.asList(clientAttributesFuture, sharedAttributesFuture));
    }

    private ListenableFuture<List<AttributeKvEntry>> findAllAttributesByScope(AttributeScope scope) {
        return systemContext.getAttributesService().findAll(tenantId, deviceId, scope);
    }

    private ListenableFuture<List<AttributeKvEntry>> findAttributesByScope(Set<String> attributesSet, AttributeScope scope) {
        return systemContext.getAttributesService().find(tenantId, deviceId, scope, attributesSet);
    }

    private Set<String> toSet(List<String> strings) {
        return new HashSet<>(strings);
    }

    private SessionType getSessionType(UUID sessionId) {
        return sessions.containsKey(sessionId) ? SessionType.ASYNC : SessionType.SYNC;
    }

    void processAttributesUpdate(DeviceAttributesEventNotificationMsg msg) {
        if (!attributeSubscriptions.isEmpty()) {
            boolean hasNotificationData = false;
            AttributeUpdateNotificationMsg.Builder notification = AttributeUpdateNotificationMsg.newBuilder();
            if (msg.isDeleted()) {
                List<String> sharedKeys = msg.getDeletedKeys().stream()
                        .filter(key -> DataConstants.SHARED_SCOPE.equals(key.getScope()))
                        .map(AttributeKey::getAttributeKey)
                        .collect(Collectors.toList());
                if (!sharedKeys.isEmpty()) {
                    notification.addAllSharedDeleted(sharedKeys);
                    hasNotificationData = true;
                }
            } else {
                if (DataConstants.SHARED_SCOPE.equals(msg.getScope())) {
                    List<AttributeKvEntry> attributes = new ArrayList<>(msg.getValues());
                    if (!attributes.isEmpty()) {
                        List<TsKvProto> sharedUpdated = msg.getValues().stream().map(t -> KvProtoUtil.toTsKvProto(t.getLastUpdateTs(), t))
                                .collect(Collectors.toList());
                        if (!sharedUpdated.isEmpty()) {
                            notification.addAllSharedUpdated(sharedUpdated);
                            hasNotificationData = true;
                        }
                    } else {
                        log.debug("[{}] No public shared side attributes changed!", deviceId);
                    }
                }
            }
            if (hasNotificationData) {
                AttributeUpdateNotificationMsg finalNotification = notification.build();
                attributeSubscriptions.forEach((key, value) -> sendToTransport(finalNotification, key, value.getNodeId()));
            }
        } else {
            log.debug("[{}] No registered attributes subscriptions to process!", deviceId);
        }
    }

    private void processRpcResponses(SessionInfoProto sessionInfo, ToDeviceRpcResponseMsg responseMsg) {
        UUID sessionId = getSessionId(sessionInfo);
        log.debug("[{}][{}] Processing RPC command response: {}", deviceId, sessionId, responseMsg);
        int requestId = responseMsg.getRequestId();
        ToDeviceRpcRequestMetadata requestMd = toDeviceRpcPendingMap.remove(requestId);
        boolean success = requestMd != null;
        if (success) {
            ToDeviceRpcRequest toDeviceRequestMsg = requestMd.getMsg().getMsg();
            UUID rpcId = toDeviceRequestMsg.getId();
            boolean delivered = requestMd.isDelivered();
            boolean hasError = StringUtils.isNotEmpty(responseMsg.getError());
            try {
                String payload = hasError ? responseMsg.getError() : responseMsg.getPayload();
                systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(
                        new FromDeviceRpcResponse(rpcId, payload, null));
                if (toDeviceRequestMsg.isPersisted()) {
                    RpcStatus status = hasError ? RpcStatus.FAILED : RpcStatus.SUCCESSFUL;
                    JsonNode response;
                    try {
                        response = JacksonUtil.toJsonNode(payload);
                    } catch (IllegalArgumentException e) {
                        response = JacksonUtil.newObjectNode().put("error", payload);
                    }
                    systemContext.getTbRpcService().save(tenantId, new RpcId(rpcId), status, response);
                }
            } finally {
                if (rpcSubmitStrategy.equals(RpcSubmitStrategy.SEQUENTIAL_ON_RESPONSE_FROM_DEVICE)) {
                    clearAwaitRpcResponseScheduler();
                    String errorResponse = hasError ? "error response" : "response";
                    String rpcState = delivered ? "" : "undelivered ";
                    sendNextPendingRequest(rpcId, requestId, String.format("Received %s for %sRPC!", errorResponse, rpcState));
                } else if (!delivered) {
                    String errorResponse = hasError ? "error response" : "response";
                    sendNextPendingRequest(rpcId, requestId, String.format("Received %s for undelivered RPC!", errorResponse));
                }
            }
        } else {
            log.debug("[{}][{}][{}] RPC command response is stale!", deviceId, sessionId, requestId);
        }
    }

    private void processRpcResponseStatus(SessionInfoProto sessionInfo, ToDeviceRpcResponseStatusMsg responseMsg) {
        UUID rpcId = new UUID(responseMsg.getRequestIdMSB(), responseMsg.getRequestIdLSB());
        RpcStatus status = RpcStatus.valueOf(responseMsg.getStatus());
        UUID sessionId = getSessionId(sessionInfo);
        int requestId = responseMsg.getRequestId();
        log.debug("[{}][{}][{}][{}] Processing RPC command response status: [{}]", deviceId, sessionId, rpcId, requestId, status);
        ToDeviceRpcRequestMetadata md = toDeviceRpcPendingMap.get(requestId);
        if (md != null) {
            var toDeviceRpcRequest = md.getMsg().getMsg();
            boolean persisted = toDeviceRpcRequest.isPersisted();
            boolean oneWayRpc = toDeviceRpcRequest.isOneway();
            JsonNode response = null;
            if (status.equals(RpcStatus.DELIVERED)) {
                if (oneWayRpc) {
                    toDeviceRpcPendingMap.remove(requestId);
                    if (rpcSequential) {
                        var fromDeviceRpcResponse = new FromDeviceRpcResponse(rpcId, null, null);
                        systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(fromDeviceRpcResponse);
                    }
                } else {
                    md.setDelivered(true);
                    if (rpcSubmitStrategy.equals(RpcSubmitStrategy.SEQUENTIAL_ON_RESPONSE_FROM_DEVICE)) {
                        awaitRpcResponseFuture = scheduleAwaitRpcResponseFuture(rpcId, requestId);
                    }
                }
            } else if (status.equals(RpcStatus.TIMEOUT)) {
                Integer maxRpcRetries = toDeviceRpcRequest.getRetries();
                maxRpcRetries = maxRpcRetries == null ?
                        systemContext.getMaxRpcRetries() : Math.min(maxRpcRetries, systemContext.getMaxRpcRetries());
                if (maxRpcRetries <= md.getRetries()) {
                    if (closeTransportSessionOnRpcDeliveryTimeout) {
                        md.setRetries(0);
                        status = RpcStatus.QUEUED;
                        notifyTransportAboutSessionsCloseAndDumpSessions(TransportSessionCloseReason.RPC_DELIVERY_TIMEOUT);
                    } else {
                        toDeviceRpcPendingMap.remove(requestId);
                        status = RpcStatus.FAILED;
                        response = JacksonUtil.newObjectNode().put("error", "There was a Timeout and all retry " +
                                "attempts have been exhausted. Retry attempts set: " + maxRpcRetries);
                    }
                } else {
                    md.setRetries(md.getRetries() + 1);
                }
            }

            if (persisted) {
                systemContext.getTbRpcService().save(tenantId, new RpcId(rpcId), status, response);
            }
            if (rpcSubmitStrategy.equals(RpcSubmitStrategy.SEQUENTIAL_ON_RESPONSE_FROM_DEVICE)
                    && status.equals(RpcStatus.DELIVERED) && !oneWayRpc) {
                return;
            }
            if (!status.equals(RpcStatus.SENT)) {
                sendNextPendingRequest(rpcId, requestId, String.format("RPC was %s!", status.name().toLowerCase()));
            }
        } else {
            log.warn("[{}][{}][{}][{}] RPC has already been removed from pending map.", deviceId, sessionId, rpcId, requestId);
        }
    }

    private void processSubscriptionCommands(SessionInfoProto sessionInfo, SubscribeToAttributeUpdatesMsg subscribeCmd) {
        UUID sessionId = getSessionId(sessionInfo);
        if (subscribeCmd.getUnsubscribe()) {
            log.debug("[{}] Canceling attributes subscription for session: [{}]", deviceId, sessionId);
            attributeSubscriptions.remove(sessionId);
        } else {
            SessionInfoMetaData sessionMD = sessions.get(sessionId);
            if (sessionMD == null) {
                sessionMD = new SessionInfoMetaData(new SessionInfo(subscribeCmd.getSessionType(), sessionInfo.getNodeId()));
            }
            sessionMD.setSubscribedToAttributes(true);
            log.debug("[{}] Registering attributes subscription for session: [{}]", deviceId, sessionId);
            attributeSubscriptions.put(sessionId, sessionMD.getSessionInfo());
            dumpSessions();
        }
    }

    private UUID getSessionId(SessionInfoProto sessionInfo) {
        return new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
    }

    private void processSubscriptionCommands(SessionInfoProto sessionInfo, SubscribeToRPCMsg subscribeCmd) {
        UUID sessionId = getSessionId(sessionInfo);
        if (subscribeCmd.getUnsubscribe()) {
            log.debug("[{}] Canceling RPC subscription for session: [{}]", deviceId, sessionId);
            rpcSubscriptions.remove(sessionId);
            clearAwaitRpcResponseScheduler();
        } else {
            SessionInfoMetaData sessionMD = sessions.get(sessionId);
            if (sessionMD == null) {
                sessionMD = new SessionInfoMetaData(new SessionInfo(subscribeCmd.getSessionType(), sessionInfo.getNodeId()));
            }
            sessionMD.setSubscribedToRPC(true);
            rpcSubscriptions.put(sessionId, sessionMD.getSessionInfo());
            log.debug("[{}] Registered RPC subscription for session: [{}] Going to check for pending requests ...", deviceId, sessionId);
            sendPendingRequests(sessionId, sessionInfo.getNodeId());
            dumpSessions();
        }
    }

    private void processSessionStateMsgs(SessionInfoProto sessionInfo, SessionEventMsg msg) {
        UUID sessionId = getSessionId(sessionInfo);
        Objects.requireNonNull(sessionId);
        if (msg.getEvent() == SessionEvent.OPEN) {
            if (sessions.containsKey(sessionId)) {
                log.debug("[{}][{}] Received duplicate session open event.", deviceId, sessionId);
                return;
            }
            log.debug("[{}] Processing new session: [{}] Current sessions size: {}", deviceId, sessionId, sessions.size());

            sessions.put(sessionId, new SessionInfoMetaData(new SessionInfo(SessionType.ASYNC, sessionInfo.getNodeId())));
            if (sessions.size() == 1) {
                reportSessionOpen();
            }
            systemContext.getDeviceStateService().onDeviceActivity(tenantId, deviceId, System.currentTimeMillis());
            dumpSessions();
        } else if (msg.getEvent() == SessionEvent.CLOSED) {
            log.debug("[{}][{}] Canceling subscriptions for closed session.", deviceId, sessionId);
            sessions.remove(sessionId);
            attributeSubscriptions.remove(sessionId);
            rpcSubscriptions.remove(sessionId);
            clearAwaitRpcResponseScheduler();
            if (sessions.isEmpty()) {
                reportSessionClose();
            }
            dumpSessions();
        }
    }

    private ScheduledFuture<?> scheduleAwaitRpcResponseFuture(UUID rpcId, int requestId) {
        return scheduler.schedule(() -> {
            var md = toDeviceRpcPendingMap.remove(requestId);
            if (md == null) {
                return;
            }
            sendNextPendingRequest(rpcId, requestId, "RPC was removed from pending map due to await timeout on response from device!");
            var toDeviceRpcRequest = md.getMsg().getMsg();
            if (toDeviceRpcRequest.isPersisted()) {
                var responseAwaitTimeout = JacksonUtil.newObjectNode().put("error", "There was a timeout awaiting for RPC response from device.");
                systemContext.getTbRpcService().save(tenantId, new RpcId(rpcId), RpcStatus.FAILED, responseAwaitTimeout);
            }
        }, systemContext.getRpcResponseTimeout(), TimeUnit.MILLISECONDS);
    }

    private void clearAwaitRpcResponseScheduler() {
        if (rpcSubmitStrategy.equals(RpcSubmitStrategy.SEQUENTIAL_ON_RESPONSE_FROM_DEVICE) && awaitRpcResponseFuture != null) {
            awaitRpcResponseFuture.cancel(true);
        }
    }

    private void handleSessionActivity(SessionInfoProto sessionInfoProto, SubscriptionInfoProto subscriptionInfo) {
        UUID sessionId = getSessionId(sessionInfoProto);
        Objects.requireNonNull(sessionId);

        SessionInfoMetaData sessionMD = sessions.get(sessionId);
        if (sessionMD != null) {
            sessionMD.setLastActivityTime(subscriptionInfo.getLastActivityTime());
            sessionMD.setSubscribedToAttributes(subscriptionInfo.getAttributeSubscription());
            sessionMD.setSubscribedToRPC(subscriptionInfo.getRpcSubscription());
            if (subscriptionInfo.getAttributeSubscription()) {
                attributeSubscriptions.putIfAbsent(sessionId, sessionMD.getSessionInfo());
            }
            if (subscriptionInfo.getRpcSubscription()) {
                rpcSubscriptions.putIfAbsent(sessionId, sessionMD.getSessionInfo());
            }
        }
        systemContext.getDeviceStateService().onDeviceActivity(tenantId, deviceId, subscriptionInfo.getLastActivityTime());
        if (sessionMD != null) {
            dumpSessions();
        }
    }

    void processCredentialsUpdate(TbActorMsg msg) {
        if (((DeviceCredentialsUpdateNotificationMsg) msg).getDeviceCredentials().getCredentialsType() == DeviceCredentialsType.LWM2M_CREDENTIALS) {
            sessions.forEach((k, v) ->
                    notifyTransportAboutDeviceCredentialsUpdate(k, v, ((DeviceCredentialsUpdateNotificationMsg) msg).getDeviceCredentials()));
        } else {
            notifyTransportAboutSessionsCloseAndDumpSessions(TransportSessionCloseReason.CREDENTIALS_UPDATED);
        }
    }

    private void notifyTransportAboutSessionsCloseAndDumpSessions(TransportSessionCloseReason transportSessionCloseReason) {
        sessions.forEach((sessionId, sessionMd) -> notifyTransportAboutClosedSession(sessionId, sessionMd, transportSessionCloseReason));
        attributeSubscriptions.clear();
        rpcSubscriptions.clear();
        dumpSessions();
    }

    private void notifyTransportAboutClosedSessionMaxSessionsLimit(UUID sessionId, SessionInfoMetaData sessionMd) {
        attributeSubscriptions.remove(sessionId);
        rpcSubscriptions.remove(sessionId);
        notifyTransportAboutClosedSession(sessionId, sessionMd, TransportSessionCloseReason.MAX_CONCURRENT_SESSIONS_LIMIT_REACHED);
    }

    private void notifyTransportAboutClosedSession(UUID sessionId, SessionInfoMetaData sessionMd, TransportSessionCloseReason transportSessionCloseReason) {
        log.debug("{} sessionId: [{}] sessionMd: [{}]", transportSessionCloseReason.getLogMessage(), sessionId, sessionMd);
        SessionCloseNotificationProto sessionCloseNotificationProto = SessionCloseNotificationProto
                .newBuilder()
                .setMessage(transportSessionCloseReason.getNotificationMessage())
                .setReason(SessionCloseReason.forNumber(transportSessionCloseReason.getProtoNumber()))
                .build();
        ToTransportMsg msg = ToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setSessionCloseNotification(sessionCloseNotificationProto)
                .build();
        systemContext.getTbCoreToTransportService().process(sessionMd.getSessionInfo().getNodeId(), msg);
    }

    void notifyTransportAboutDeviceCredentialsUpdate(UUID sessionId, SessionInfoMetaData sessionMd, DeviceCredentials deviceCredentials) {
        ToTransportUpdateCredentialsProto.Builder notification = ToTransportUpdateCredentialsProto.newBuilder();
        notification.addCredentialsId(deviceCredentials.getCredentialsId());
        notification.addCredentialsValue(deviceCredentials.getCredentialsValue());
        ToTransportMsg msg = ToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setToTransportUpdateCredentialsNotification(notification).build();
        systemContext.getTbCoreToTransportService().process(sessionMd.getSessionInfo().getNodeId(), msg);
    }

    void processNameOrTypeUpdate(DeviceNameOrTypeUpdateMsg msg) {
        this.deviceName = msg.getDeviceName();
        this.deviceType = msg.getDeviceType();
        this.defaultMetaData = new TbMsgMetaData();
        this.defaultMetaData.putValue("deviceName", deviceName);
        this.defaultMetaData.putValue("deviceType", deviceType);
    }

    void processEdgeUpdate(DeviceEdgeUpdateMsg msg) {
        log.trace("[{}] Processing edge update {}", deviceId, msg);
        this.edgeId = msg.getEdgeId();
    }

    private void sendToTransport(GetAttributeResponseMsg responseMsg, SessionInfoProto sessionInfo) {
        ToTransportMsg msg = ToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionInfo.getSessionIdMSB())
                .setSessionIdLSB(sessionInfo.getSessionIdLSB())
                .setGetAttributesResponse(responseMsg).build();
        systemContext.getTbCoreToTransportService().process(sessionInfo.getNodeId(), msg);
    }

    private void sendToTransport(AttributeUpdateNotificationMsg notificationMsg, UUID sessionId, String nodeId) {
        ToTransportMsg msg = ToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setAttributeUpdateNotification(notificationMsg).build();
        systemContext.getTbCoreToTransportService().process(nodeId, msg);
    }

    private void sendToTransport(ToDeviceRpcRequestMsg rpcMsg, UUID sessionId, String nodeId) {
        ToTransportMsg msg = ToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setToDeviceRequest(rpcMsg).build();
        systemContext.getTbCoreToTransportService().process(nodeId, msg);
    }

    private void saveRpcRequestToEdgeQueue(ToDeviceRpcRequest msg, Integer requestId) {
        ObjectNode body = JacksonUtil.newObjectNode();
        body.put("requestId", requestId);
        body.put("requestUUID", msg.getId().toString());
        body.put("oneway", msg.isOneway());
        body.put("expirationTime", msg.getExpirationTime());
        body.put("method", msg.getBody().getMethod());
        body.put("params", msg.getBody().getParams());
        body.put("persisted", msg.isPersisted());
        body.put("retries", msg.getRetries());
        body.put("additionalInfo", msg.getAdditionalInfo());

        EdgeEvent edgeEvent = EdgeUtils.constructEdgeEvent(tenantId, edgeId, EdgeEventType.DEVICE, EdgeEventActionType.RPC_CALL, deviceId, body);

        systemContext.getClusterService().onEdgeHighPriorityMsg(new EdgeHighPriorityMsg(tenantId, edgeEvent));
    }

    void restoreSessions() {
        if (systemContext.isLocalCacheType()) {
            return;
        }
        log.debug("[{}] Restoring sessions from cache", deviceId);
        DeviceSessionsCacheEntry sessionsDump;
        try {
            sessionsDump = systemContext.getDeviceSessionCacheService().get(deviceId);
        } catch (Exception e) {
            log.warn("[{}] Failed to decode device sessions from cache", deviceId);
            return;
        }
        if (sessionsDump.getSessionsCount() == 0) {
            log.debug("[{}] No session information found", deviceId);
            return;
        }
        // TODO: Take latest max allowed sessions size from cache
        for (SessionSubscriptionInfoProto sessionSubscriptionInfoProto : sessionsDump.getSessionsList()) {
            SessionInfoProto sessionInfoProto = sessionSubscriptionInfoProto.getSessionInfo();
            UUID sessionId = getSessionId(sessionInfoProto);
            SessionInfo sessionInfo = new SessionInfo(SessionType.ASYNC, sessionInfoProto.getNodeId());
            SubscriptionInfoProto subInfo = sessionSubscriptionInfoProto.getSubscriptionInfo();
            SessionInfoMetaData sessionMD = new SessionInfoMetaData(sessionInfo, subInfo.getLastActivityTime());
            sessions.put(sessionId, sessionMD);
            if (subInfo.getAttributeSubscription()) {
                attributeSubscriptions.put(sessionId, sessionInfo);
                sessionMD.setSubscribedToAttributes(true);
            }
            if (subInfo.getRpcSubscription()) {
                rpcSubscriptions.put(sessionId, sessionInfo);
                sessionMD.setSubscribedToRPC(true);
            }
            log.debug("[{}] Restored session: {}", deviceId, sessionMD);
        }
        log.debug("[{}] Restored sessions: {}, RPC subscriptions: {}, attribute subscriptions: {}", deviceId, sessions.size(), rpcSubscriptions.size(), attributeSubscriptions.size());
    }

    private void dumpSessions() {
        if (systemContext.isLocalCacheType()) {
            return;
        }
        log.debug("[{}] Dumping sessions: {}, RPC subscriptions: {}, attribute subscriptions: {} to cache", deviceId, sessions.size(), rpcSubscriptions.size(), attributeSubscriptions.size());
        List<SessionSubscriptionInfoProto> sessionsList = new ArrayList<>(sessions.size());
        sessions.forEach((uuid, sessionMD) -> {
            if (sessionMD.getSessionInfo().getType() == SessionType.SYNC) {
                return;
            }
            SessionInfo sessionInfo = sessionMD.getSessionInfo();
            SubscriptionInfoProto subscriptionInfoProto = SubscriptionInfoProto.newBuilder()
                    .setLastActivityTime(sessionMD.getLastActivityTime())
                    .setAttributeSubscription(sessionMD.isSubscribedToAttributes())
                    .setRpcSubscription(sessionMD.isSubscribedToRPC()).build();
            SessionInfoProto sessionInfoProto = SessionInfoProto.newBuilder()
                    .setSessionIdMSB(uuid.getMostSignificantBits())
                    .setSessionIdLSB(uuid.getLeastSignificantBits())
                    .setNodeId(sessionInfo.getNodeId()).build();
            sessionsList.add(SessionSubscriptionInfoProto.newBuilder()
                    .setSessionInfo(sessionInfoProto)
                    .setSubscriptionInfo(subscriptionInfoProto).build());
            log.debug("[{}] Dumping session: {}", deviceId, sessionMD);
        });
        systemContext.getDeviceSessionCacheService()
                .put(deviceId, DeviceSessionsCacheEntry.newBuilder()
                        .addAllSessions(sessionsList).build());
    }

    void init(TbActorCtx ctx) {
        PageLink pageLink = new PageLink(1024, 0, null, new SortOrder("createdTime"));
        PageData<Rpc> pageData;
        do {
            pageData = systemContext.getTbRpcService().findAllByDeviceIdAndStatus(tenantId, deviceId, RpcStatus.QUEUED, pageLink);
            pageData.getData().forEach(rpc -> {
                ToDeviceRpcRequest msg = JacksonUtil.convertValue(rpc.getRequest(), ToDeviceRpcRequest.class);
                long timeout = rpc.getExpirationTime() - System.currentTimeMillis();
                if (timeout <= 0) {
                    rpc.setStatus(RpcStatus.EXPIRED);
                    systemContext.getTbRpcService().save(tenantId, rpc);
                } else {
                    registerPendingRpcRequest(ctx, new ToDeviceRpcRequestActorMsg(systemContext.getServiceId(), msg), false, createToDeviceRpcRequestMsg(msg), timeout);
                }
            });
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
    }

    void checkSessionsTimeout() {
        final long expTime = System.currentTimeMillis() - systemContext.getSessionInactivityTimeout();
        List<UUID> expiredIds = null;

        for (Map.Entry<UUID, SessionInfoMetaData> kv : sessions.entrySet()) { //entry set are cached for stable sessions
            if (kv.getValue().getLastActivityTime() < expTime) {
                final UUID id = kv.getKey();
                if (expiredIds == null) {
                    expiredIds = new ArrayList<>(1); //most of the expired sessions is a single event
                }
                expiredIds.add(id);
            }
        }

        if (expiredIds != null) {
            int removed = 0;
            for (UUID id : expiredIds) {
                final SessionInfoMetaData session = sessions.remove(id);
                rpcSubscriptions.remove(id);
                attributeSubscriptions.remove(id);
                if (session != null) {
                    removed++;
                    notifyTransportAboutClosedSession(id, session, TransportSessionCloseReason.SESSION_TIMEOUT);
                }
            }
            if (removed != 0) {
                dumpSessions();
            }
        }

    }

}
