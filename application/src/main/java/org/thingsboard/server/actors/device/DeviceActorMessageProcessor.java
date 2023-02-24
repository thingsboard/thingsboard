/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.LinkedHashMapRemoveEldest;
import org.thingsboard.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.thingsboard.rule.engine.api.msg.DeviceCredentialsUpdateNotificationMsg;
import org.thingsboard.rule.engine.api.msg.DeviceEdgeUpdateMsg;
import org.thingsboard.rule.engine.api.msg.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.shared.AbstractContextAwareMsgProcessor;
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
import org.thingsboard.server.common.data.kv.KvEntry;
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
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.common.msg.timeout.DeviceActorServerSideRpcTimeoutMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ClaimDeviceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceSessionsCacheEntry;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueType;
import org.thingsboard.server.gen.transport.TransportProtos.SessionCloseNotificationProto;
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
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportUpdateCredentialsProto;
import org.thingsboard.server.gen.transport.TransportProtos.TransportToDeviceActorMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvProto;
import org.thingsboard.server.service.rpc.FromDeviceRpcResponseActorMsg;
import org.thingsboard.server.service.rpc.RemoveRpcActorMsg;
import org.thingsboard.server.service.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import javax.annotation.Nullable;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * @author Andrew Shvayka
 */
@Slf4j
class DeviceActorMessageProcessor extends AbstractContextAwareMsgProcessor {

    static final String SESSION_TIMEOUT_MESSAGE = "session timeout!";
    final TenantId tenantId;
    final DeviceId deviceId;
    final LinkedHashMapRemoveEldest<UUID, SessionInfoMetaData> sessions;
    private final Map<UUID, SessionInfo> attributeSubscriptions;
    private final Map<UUID, SessionInfo> rpcSubscriptions;
    private final Map<Integer, ToDeviceRpcRequestMetadata> toDeviceRpcPendingMap;
    private final boolean rpcSequential;

    private int rpcSeq = 0;
    private String deviceName;
    private String deviceType;
    private TbMsgMetaData defaultMetaData;
    private EdgeId edgeId;

    DeviceActorMessageProcessor(ActorSystemContext systemContext, TenantId tenantId, DeviceId deviceId) {
        super(systemContext);
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.rpcSequential = systemContext.isRpcSequential();
        this.attributeSubscriptions = new HashMap<>();
        this.rpcSubscriptions = new HashMap<>();
        this.toDeviceRpcPendingMap = new LinkedHashMap<>();
        this.sessions = new LinkedHashMapRemoveEldest<>(systemContext.getMaxConcurrentSessionsPerDevice(), this::notifyTransportAboutClosedSessionMaxSessionsLimit);
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
                systemContext.getRelationService().findByToAndType(tenantId, deviceId, EntityRelation.EDGE_TYPE, RelationTypeGroup.COMMON);
        if (result != null && result.size() > 0) {
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
        ToDeviceRpcRequestMsg rpcRequest = creteToDeviceRpcRequestMsg(request);

        long timeout = request.getExpirationTime() - System.currentTimeMillis();
        boolean persisted = request.isPersisted();

        if (timeout <= 0) {
            log.debug("[{}][{}] Ignoring message due to exp time reached, {}", deviceId, request.getId(), request.getExpirationTime());
            if (persisted) {
                createRpc(request, RpcStatus.EXPIRED);
            }
            return;
        } else if (persisted) {
            createRpc(request, RpcStatus.QUEUED);
        }

        boolean sent = false;
        if (systemContext.isEdgesEnabled() && edgeId != null) {
            log.debug("[{}][{}] device is related to edge [{}]. Saving RPC request to edge queue", tenantId, deviceId, edgeId.getId());
            try {
                saveRpcRequestToEdgeQueue(request, rpcRequest.getRequestId()).get();
                sent = true;
            } catch (InterruptedException | ExecutionException e) {
                log.error("[{}][{}][{}] Failed to save rpc request to edge queue {}", tenantId, deviceId, edgeId.getId(), request, e);
            }
        } else if (isSendNewRpcAvailable()) {
            sent = rpcSubscriptions.size() > 0;
            Set<UUID> syncSessionSet = new HashSet<>();
            rpcSubscriptions.forEach((key, value) -> {
                sendToTransport(rpcRequest, key, value.getNodeId());
                if (SessionType.SYNC == value.getType()) {
                    syncSessionSet.add(key);
                }
            });
            log.trace("Rpc syncSessionSet [{}] subscription after sent [{}]", syncSessionSet, rpcSubscriptions);
            syncSessionSet.forEach(rpcSubscriptions::remove);
        }

        if (persisted) {
            ObjectNode response = JacksonUtil.newObjectNode();
            response.put("rpcId", request.getId().toString());
            systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(new FromDeviceRpcResponse(msg.getMsg().getId(), JacksonUtil.toString(response), null));
        }

        if (!persisted && request.isOneway() && sent) {
            log.debug("[{}] Rpc command response sent [{}]!", deviceId, request.getId());
            systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(new FromDeviceRpcResponse(msg.getMsg().getId(), null, null));
        } else {
            registerPendingRpcRequest(context, msg, sent, rpcRequest, timeout);
        }
        if (sent) {
            log.debug("[{}] RPC request {} is sent!", deviceId, request.getId());
        } else {
            log.debug("[{}] RPC request {} is NOT sent!", deviceId, request.getId());
        }
    }

    private boolean isSendNewRpcAvailable() {
        return !rpcSequential || toDeviceRpcPendingMap.values().stream().filter(md -> !md.isDelivered()).findAny().isEmpty();
    }

    private Rpc createRpc(ToDeviceRpcRequest request, RpcStatus status) {
        Rpc rpc = new Rpc(new RpcId(request.getId()));
        rpc.setCreatedTime(System.currentTimeMillis());
        rpc.setTenantId(tenantId);
        rpc.setDeviceId(deviceId);
        rpc.setExpirationTime(request.getExpirationTime());
        rpc.setRequest(JacksonUtil.valueToTree(request));
        rpc.setStatus(status);
        rpc.setAdditionalInfo(JacksonUtil.toJsonNode(request.getAdditionalInfo()));
        return systemContext.getTbRpcService().save(tenantId, rpc);
    }

    private ToDeviceRpcRequestMsg creteToDeviceRpcRequestMsg(ToDeviceRpcRequest request) {
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

    void processRpcResponsesFromEdge(TbActorCtx context, FromDeviceRpcResponseActorMsg responseMsg) {
        log.debug("[{}] Processing rpc command response from edge session", deviceId);
        ToDeviceRpcRequestMetadata requestMd = toDeviceRpcPendingMap.remove(responseMsg.getRequestId());
        boolean success = requestMd != null;
        if (success) {
            systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(responseMsg.getMsg());
        } else {
            log.debug("[{}] Rpc command response [{}] is stale!", deviceId, responseMsg.getRequestId());
        }
    }

    void processRemoveRpc(TbActorCtx context, RemoveRpcActorMsg msg) {
        log.debug("[{}] Processing remove rpc command", msg.getRequestId());
        Map.Entry<Integer, ToDeviceRpcRequestMetadata> entry = null;
        for (Map.Entry<Integer, ToDeviceRpcRequestMetadata> e : toDeviceRpcPendingMap.entrySet()) {
            if (e.getValue().getMsg().getMsg().getId().equals(msg.getRequestId())) {
                entry = e;
                break;
            }
        }

        if (entry != null) {
            if (entry.getValue().isDelivered()) {
                toDeviceRpcPendingMap.remove(entry.getKey());
            } else {
                Optional<Map.Entry<Integer, ToDeviceRpcRequestMetadata>> firstRpc = getFirstRpc();
                if (firstRpc.isPresent() && entry.getKey().equals(firstRpc.get().getKey())) {
                    toDeviceRpcPendingMap.remove(entry.getKey());
                    sendNextPendingRequest(context);
                } else {
                    toDeviceRpcPendingMap.remove(entry.getKey());
                }
            }
        }
    }

    private void registerPendingRpcRequest(TbActorCtx context, ToDeviceRpcRequestActorMsg msg, boolean sent, ToDeviceRpcRequestMsg rpcRequest, long timeout) {
        toDeviceRpcPendingMap.put(rpcRequest.getRequestId(), new ToDeviceRpcRequestMetadata(msg, sent));
        DeviceActorServerSideRpcTimeoutMsg timeoutMsg = new DeviceActorServerSideRpcTimeoutMsg(rpcRequest.getRequestId(), timeout);
        scheduleMsgWithDelay(context, timeoutMsg, timeoutMsg.getTimeout());
    }

    void processServerSideRpcTimeout(TbActorCtx context, DeviceActorServerSideRpcTimeoutMsg msg) {
        ToDeviceRpcRequestMetadata requestMd = toDeviceRpcPendingMap.remove(msg.getId());
        if (requestMd != null) {
            log.debug("[{}] RPC request [{}] timeout detected!", deviceId, msg.getId());
            if (requestMd.getMsg().getMsg().isPersisted()) {
                systemContext.getTbRpcService().save(tenantId, new RpcId(requestMd.getMsg().getMsg().getId()), RpcStatus.EXPIRED, null);
            }
            systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(new FromDeviceRpcResponse(requestMd.getMsg().getMsg().getId(),
                    null, requestMd.isSent() ? RpcError.TIMEOUT : RpcError.NO_ACTIVE_CONNECTION));
            if (!requestMd.isDelivered()) {
                sendNextPendingRequest(context);
            }
        }
    }

    private void sendPendingRequests(TbActorCtx context, UUID sessionId, String nodeId) {
        SessionType sessionType = getSessionType(sessionId);
        if (!toDeviceRpcPendingMap.isEmpty()) {
            log.debug("[{}] Pushing {} pending RPC messages to new async session [{}]", deviceId, toDeviceRpcPendingMap.size(), sessionId);
            if (sessionType == SessionType.SYNC) {
                log.debug("[{}] Cleanup sync rpc session [{}]", deviceId, sessionId);
                rpcSubscriptions.remove(sessionId);
            }
        } else {
            log.debug("[{}] No pending RPC messages for new async session [{}]", deviceId, sessionId);
        }
        Set<Integer> sentOneWayIds = new HashSet<>();

        if (rpcSequential) {
            getFirstRpc().ifPresent(processPendingRpc(context, sessionId, nodeId, sentOneWayIds));
        } else if (sessionType == SessionType.ASYNC) {
            toDeviceRpcPendingMap.entrySet().forEach(processPendingRpc(context, sessionId, nodeId, sentOneWayIds));
        } else {
            toDeviceRpcPendingMap.entrySet().stream().findFirst().ifPresent(processPendingRpc(context, sessionId, nodeId, sentOneWayIds));
        }

        sentOneWayIds.stream().filter(id -> !toDeviceRpcPendingMap.get(id).getMsg().getMsg().isPersisted()).forEach(toDeviceRpcPendingMap::remove);
    }

    private Optional<Map.Entry<Integer, ToDeviceRpcRequestMetadata>> getFirstRpc() {
        return toDeviceRpcPendingMap.entrySet().stream().filter(e -> !e.getValue().isDelivered()).findFirst();
    }

    private void sendNextPendingRequest(TbActorCtx context) {
        if (rpcSequential) {
            rpcSubscriptions.forEach((id, s) -> sendPendingRequests(context, id, s.getNodeId()));
        }
    }

    private Consumer<Map.Entry<Integer, ToDeviceRpcRequestMetadata>> processPendingRpc(TbActorCtx context, UUID sessionId, String nodeId, Set<Integer> sentOneWayIds) {
        return entry -> {
            ToDeviceRpcRequest request = entry.getValue().getMsg().getMsg();
            ToDeviceRpcRequestBody body = request.getBody();
            if (request.isOneway() && !rpcSequential) {
                sentOneWayIds.add(entry.getKey());
                systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(new FromDeviceRpcResponse(request.getId(), null, null));
            }
            ToDeviceRpcRequestMsg rpcRequest = ToDeviceRpcRequestMsg.newBuilder()
                    .setRequestId(entry.getKey())
                    .setMethodName(body.getMethod())
                    .setParams(body.getParams())
                    .setExpirationTime(request.getExpirationTime())
                    .setRequestIdMSB(request.getId().getMostSignificantBits())
                    .setRequestIdLSB(request.getId().getLeastSignificantBits())
                    .setOneway(request.isOneway())
                    .setPersisted(request.isPersisted())
                    .build();
            sendToTransport(rpcRequest, sessionId, nodeId);
        };
    }

    void process(TbActorCtx context, TransportToDeviceActorMsgWrapper wrapper) {
        TransportToDeviceActorMsg msg = wrapper.getMsg();
        TbCallback callback = wrapper.getCallback();
        var sessionInfo = msg.getSessionInfo();

        if (msg.hasSessionEvent()) {
            processSessionStateMsgs(sessionInfo, msg.getSessionEvent());
        }
        if (msg.hasSubscribeToAttributes()) {
            processSubscriptionCommands(context, sessionInfo, msg.getSubscribeToAttributes());
        }
        if (msg.hasSubscribeToRPC()) {
            processSubscriptionCommands(context, sessionInfo, msg.getSubscribeToRPC());
        }
        if (msg.hasSendPendingRPC()) {
            sendPendingRequests(context, getSessionId(sessionInfo), sessionInfo.getNodeId());
        }
        if (msg.hasGetAttributes()) {
            handleGetAttributesRequest(context, sessionInfo, msg.getGetAttributes());
        }
        if (msg.hasToDeviceRPCCallResponse()) {
            processRpcResponses(context, sessionInfo, msg.getToDeviceRPCCallResponse());
        }
        if (msg.hasSubscriptionInfo()) {
            handleSessionActivity(context, sessionInfo, msg.getSubscriptionInfo());
        }
        if (msg.hasClaimDevice()) {
            handleClaimDeviceMsg(context, sessionInfo, msg.getClaimDevice());
        }
        if (msg.hasRpcResponseStatusMsg()) {
            processRpcResponseStatus(context, sessionInfo, msg.getRpcResponseStatusMsg());
        }
        if (msg.hasUplinkNotificationMsg()) {
            processUplinkNotificationMsg(context, sessionInfo, msg.getUplinkNotificationMsg());
        }
        callback.onSuccess();
    }

    private void processUplinkNotificationMsg(TbActorCtx context, SessionInfoProto sessionInfo, TransportProtos.UplinkNotificationMsg uplinkNotificationMsg) {
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

    private void handleClaimDeviceMsg(TbActorCtx context, SessionInfoProto sessionInfo, ClaimDeviceMsg msg) {
        DeviceId deviceId = new DeviceId(new UUID(msg.getDeviceIdMSB(), msg.getDeviceIdLSB()));
        systemContext.getClaimDevicesService().registerClaimingInfo(tenantId, deviceId, msg.getSecretKey(), msg.getDurationMs());
    }

    private void reportSessionOpen() {
        systemContext.getDeviceStateService().onDeviceConnect(tenantId, deviceId);
    }

    private void reportSessionClose() {
        systemContext.getDeviceStateService().onDeviceDisconnect(tenantId, deviceId);
    }

    private void handleGetAttributesRequest(TbActorCtx context, SessionInfoProto sessionInfo, GetAttributeRequestMsg request) {
        int requestId = request.getRequestId();
        if (request.getOnlyShared()) {
            Futures.addCallback(findAllAttributesByScope(DataConstants.SHARED_SCOPE), new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable List<AttributeKvEntry> result) {
                    GetAttributeResponseMsg responseMsg = GetAttributeResponseMsg.newBuilder()
                            .setRequestId(requestId)
                            .setSharedStateMsg(true)
                            .addAllSharedAttributeList(toTsKvProtos(result))
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
                            .addAllClientAttributeList(toTsKvProtos(result.get(0)))
                            .addAllSharedAttributeList(toTsKvProtos(result.get(1)))
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
            clientAttributesFuture = findAllAttributesByScope(DataConstants.CLIENT_SCOPE);
            sharedAttributesFuture = findAllAttributesByScope(DataConstants.SHARED_SCOPE);
        } else if (!CollectionUtils.isEmpty(request.getClientAttributeNamesList()) && !CollectionUtils.isEmpty(request.getSharedAttributeNamesList())) {
            clientAttributesFuture = findAttributesByScope(toSet(request.getClientAttributeNamesList()), DataConstants.CLIENT_SCOPE);
            sharedAttributesFuture = findAttributesByScope(toSet(request.getSharedAttributeNamesList()), DataConstants.SHARED_SCOPE);
        } else if (CollectionUtils.isEmpty(request.getClientAttributeNamesList()) && !CollectionUtils.isEmpty(request.getSharedAttributeNamesList())) {
            clientAttributesFuture = Futures.immediateFuture(Collections.emptyList());
            sharedAttributesFuture = findAttributesByScope(toSet(request.getSharedAttributeNamesList()), DataConstants.SHARED_SCOPE);
        } else {
            sharedAttributesFuture = Futures.immediateFuture(Collections.emptyList());
            clientAttributesFuture = findAttributesByScope(toSet(request.getClientAttributeNamesList()), DataConstants.CLIENT_SCOPE);
        }
        return Futures.allAsList(Arrays.asList(clientAttributesFuture, sharedAttributesFuture));
    }

    private ListenableFuture<List<AttributeKvEntry>> findAllAttributesByScope(String scope) {
        return systemContext.getAttributesService().findAll(tenantId, deviceId, scope);
    }

    private ListenableFuture<List<AttributeKvEntry>> findAttributesByScope(Set<String> attributesSet, String scope) {
        return systemContext.getAttributesService().find(tenantId, deviceId, scope, attributesSet);
    }

    private Set<String> toSet(List<String> strings) {
        return new HashSet<>(strings);
    }

    private SessionType getSessionType(UUID sessionId) {
        return sessions.containsKey(sessionId) ? SessionType.ASYNC : SessionType.SYNC;
    }

    void processAttributesUpdate(TbActorCtx context, DeviceAttributesEventNotificationMsg msg) {
        if (attributeSubscriptions.size() > 0) {
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
                    if (attributes.size() > 0) {
                        List<TsKvProto> sharedUpdated = msg.getValues().stream().map(this::toTsKvProto)
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

    private void processRpcResponses(TbActorCtx context, SessionInfoProto sessionInfo, ToDeviceRpcResponseMsg responseMsg) {
        UUID sessionId = getSessionId(sessionInfo);
        log.debug("[{}] Processing rpc command response [{}]", deviceId, sessionId);
        ToDeviceRpcRequestMetadata requestMd = toDeviceRpcPendingMap.remove(responseMsg.getRequestId());
        boolean success = requestMd != null;
        if (success) {
            boolean hasError = StringUtils.isNotEmpty(responseMsg.getError());
            try {
                String payload = hasError ? responseMsg.getError() : responseMsg.getPayload();
                systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(
                        new FromDeviceRpcResponse(requestMd.getMsg().getMsg().getId(),
                                payload, null));
                if (requestMd.getMsg().getMsg().isPersisted()) {
                    RpcStatus status = hasError ? RpcStatus.FAILED : RpcStatus.SUCCESSFUL;
                    JsonNode response;
                    try {
                        response = JacksonUtil.toJsonNode(payload);
                    } catch (IllegalArgumentException e) {
                        response = JacksonUtil.newObjectNode().put("error", payload);
                    }
                    systemContext.getTbRpcService().save(tenantId, new RpcId(requestMd.getMsg().getMsg().getId()), status, response);
                }
            } finally {
                if (hasError && !requestMd.isDelivered()) {
                    sendNextPendingRequest(context);
                }
            }
        } else {
            log.debug("[{}] Rpc command response [{}] is stale!", deviceId, responseMsg.getRequestId());
        }
    }

    private void processRpcResponseStatus(TbActorCtx context, SessionInfoProto sessionInfo, ToDeviceRpcResponseStatusMsg responseMsg) {
        UUID rpcId = new UUID(responseMsg.getRequestIdMSB(), responseMsg.getRequestIdLSB());
        RpcStatus status = RpcStatus.valueOf(responseMsg.getStatus());
        ToDeviceRpcRequestMetadata md = toDeviceRpcPendingMap.get(responseMsg.getRequestId());

        if (md != null) {
            JsonNode response = null;
            if (status.equals(RpcStatus.DELIVERED)) {
                if (md.getMsg().getMsg().isOneway()) {
                    toDeviceRpcPendingMap.remove(responseMsg.getRequestId());
                    if (rpcSequential) {
                        systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(new FromDeviceRpcResponse(rpcId, null, null));
                    }
                } else {
                    md.setDelivered(true);
                }
            } else if (status.equals(RpcStatus.TIMEOUT)) {
                Integer maxRpcRetries = md.getMsg().getMsg().getRetries();
                maxRpcRetries = maxRpcRetries == null ? systemContext.getMaxRpcRetries() : Math.min(maxRpcRetries, systemContext.getMaxRpcRetries());
                if (maxRpcRetries <= md.getRetries()) {
                    toDeviceRpcPendingMap.remove(responseMsg.getRequestId());
                    status = RpcStatus.FAILED;
                    response = JacksonUtil.newObjectNode().put("error", "There was a Timeout and all retry attempts have been exhausted. Retry attempts set: " + maxRpcRetries);
                } else {
                    md.setRetries(md.getRetries() + 1);
                }
            }

            if (md.getMsg().getMsg().isPersisted()) {
                systemContext.getTbRpcService().save(tenantId, new RpcId(rpcId), status, response);
            }
            if (status != RpcStatus.SENT) {
                sendNextPendingRequest(context);
            }
        } else {
            log.info("[{}][{}] Rpc has already removed from pending map.", deviceId, rpcId);
        }
    }

    private void processSubscriptionCommands(TbActorCtx context, SessionInfoProto sessionInfo, SubscribeToAttributeUpdatesMsg subscribeCmd) {
        UUID sessionId = getSessionId(sessionInfo);
        if (subscribeCmd.getUnsubscribe()) {
            log.debug("[{}] Canceling attributes subscription for session [{}]", deviceId, sessionId);
            attributeSubscriptions.remove(sessionId);
        } else {
            SessionInfoMetaData sessionMD = sessions.get(sessionId);
            if (sessionMD == null) {
                sessionMD = new SessionInfoMetaData(new SessionInfo(subscribeCmd.getSessionType(), sessionInfo.getNodeId()));
            }
            sessionMD.setSubscribedToAttributes(true);
            log.debug("[{}] Registering attributes subscription for session [{}]", deviceId, sessionId);
            attributeSubscriptions.put(sessionId, sessionMD.getSessionInfo());
            dumpSessions();
        }
    }

    private UUID getSessionId(SessionInfoProto sessionInfo) {
        return new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
    }

    private void processSubscriptionCommands(TbActorCtx context, SessionInfoProto sessionInfo, SubscribeToRPCMsg subscribeCmd) {
        UUID sessionId = getSessionId(sessionInfo);
        if (subscribeCmd.getUnsubscribe()) {
            log.debug("[{}] Canceling rpc subscription for session [{}]", deviceId, sessionId);
            rpcSubscriptions.remove(sessionId);
        } else {
            SessionInfoMetaData sessionMD = sessions.get(sessionId);
            if (sessionMD == null) {
                sessionMD = new SessionInfoMetaData(new SessionInfo(subscribeCmd.getSessionType(), sessionInfo.getNodeId()));
            }
            sessionMD.setSubscribedToRPC(true);
            log.debug("[{}] Registering rpc subscription for session [{}]", deviceId, sessionId);
            rpcSubscriptions.put(sessionId, sessionMD.getSessionInfo());
            sendPendingRequests(context, sessionId, sessionInfo.getNodeId());
            dumpSessions();
        }
    }

    private void processSessionStateMsgs(SessionInfoProto sessionInfo, SessionEventMsg msg) {
        UUID sessionId = getSessionId(sessionInfo);
        Objects.requireNonNull(sessionId);
        if (msg.getEvent() == SessionEvent.OPEN) {
            if (sessions.containsKey(sessionId)) {
                log.debug("[{}] Received duplicate session open event [{}]", deviceId, sessionId);
                return;
            }
            log.debug("[{}] Processing new session [{}]. Current sessions size {}", deviceId, sessionId, sessions.size());

            sessions.put(sessionId, new SessionInfoMetaData(new SessionInfo(SessionType.ASYNC, sessionInfo.getNodeId())));
            if (sessions.size() == 1) {
                reportSessionOpen();
            }
            systemContext.getDeviceStateService().onDeviceActivity(tenantId, deviceId, System.currentTimeMillis());
            dumpSessions();
        } else if (msg.getEvent() == SessionEvent.CLOSED) {
            log.debug("[{}] Canceling subscriptions for closed session [{}]", deviceId, sessionId);
            sessions.remove(sessionId);
            attributeSubscriptions.remove(sessionId);
            rpcSubscriptions.remove(sessionId);
            if (sessions.isEmpty()) {
                reportSessionClose();
            }
            dumpSessions();
        }
    }

    private void handleSessionActivity(TbActorCtx context, SessionInfoProto sessionInfoProto, SubscriptionInfoProto subscriptionInfo) {
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
            sessions.forEach((k, v) -> {
                notifyTransportAboutDeviceCredentialsUpdate(k, v, ((DeviceCredentialsUpdateNotificationMsg) msg).getDeviceCredentials());
            });
        } else {
            sessions.forEach((sessionId, sessionMd) -> notifyTransportAboutClosedSession(sessionId, sessionMd, "device credentials updated!"));
            attributeSubscriptions.clear();
            rpcSubscriptions.clear();
            dumpSessions();

        }
    }

    private void notifyTransportAboutClosedSessionMaxSessionsLimit(UUID sessionId, SessionInfoMetaData sessionMd) {
        log.debug("remove eldest session (max concurrent sessions limit reached per device) sessionId [{}] sessionMd [{}]", sessionId, sessionMd);
        notifyTransportAboutClosedSession(sessionId, sessionMd, "max concurrent sessions limit reached per device!");
    }

    private void notifyTransportAboutClosedSession(UUID sessionId, SessionInfoMetaData sessionMd, String message) {
        SessionCloseNotificationProto sessionCloseNotificationProto = SessionCloseNotificationProto
                .newBuilder()
                .setMessage(message).build();
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

    private void sendToTransport(ToServerRpcResponseMsg rpcMsg, UUID sessionId, String nodeId) {
        ToTransportMsg msg = ToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setToServerResponse(rpcMsg).build();
        systemContext.getTbCoreToTransportService().process(nodeId, msg);
    }

    private ListenableFuture<Void> saveRpcRequestToEdgeQueue(ToDeviceRpcRequest msg, Integer requestId) {
        ObjectNode body = mapper.createObjectNode();
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

        return Futures.transform(systemContext.getEdgeEventService().saveAsync(edgeEvent), unused -> {
            systemContext.getClusterService().onEdgeEventUpdate(tenantId, edgeId);
            return null;
        }, systemContext.getDbCallbackExecutor());
    }

    private List<TsKvProto> toTsKvProtos(@Nullable List<AttributeKvEntry> result) {
        List<TsKvProto> clientAttributes;
        if (result == null || result.isEmpty()) {
            clientAttributes = Collections.emptyList();
        } else {
            clientAttributes = new ArrayList<>(result.size());
            for (AttributeKvEntry attrEntry : result) {
                clientAttributes.add(toTsKvProto(attrEntry));
            }
        }
        return clientAttributes;
    }

    private TsKvProto toTsKvProto(AttributeKvEntry attrEntry) {
        return TsKvProto.newBuilder().setTs(attrEntry.getLastUpdateTs())
                .setKv(toKeyValueProto(attrEntry)).build();
    }

    private KeyValueProto toKeyValueProto(KvEntry kvEntry) {
        KeyValueProto.Builder builder = KeyValueProto.newBuilder();
        builder.setKey(kvEntry.getKey());
        switch (kvEntry.getDataType()) {
            case BOOLEAN:
                builder.setType(KeyValueType.BOOLEAN_V);
                builder.setBoolV(kvEntry.getBooleanValue().get());
                break;
            case DOUBLE:
                builder.setType(KeyValueType.DOUBLE_V);
                builder.setDoubleV(kvEntry.getDoubleValue().get());
                break;
            case LONG:
                builder.setType(KeyValueType.LONG_V);
                builder.setLongV(kvEntry.getLongValue().get());
                break;
            case STRING:
                builder.setType(KeyValueType.STRING_V);
                builder.setStringV(kvEntry.getStrValue().get());
                break;
            case JSON:
                builder.setType(KeyValueType.JSON_V);
                builder.setJsonV(kvEntry.getJsonValue().get());
                break;
        }
        return builder.build();
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
        log.debug("[{}] Restored sessions: {}, rpc subscriptions: {}, attribute subscriptions: {}", deviceId, sessions.size(), rpcSubscriptions.size(), attributeSubscriptions.size());
    }

    private void dumpSessions() {
        if (systemContext.isLocalCacheType()) {
            return;
        }
        log.debug("[{}] Dumping sessions: {}, rpc subscriptions: {}, attribute subscriptions: {} to cache", deviceId, sessions.size(), rpcSubscriptions.size(), attributeSubscriptions.size());
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
                    registerPendingRpcRequest(ctx, new ToDeviceRpcRequestActorMsg(systemContext.getServiceId(), msg), false, creteToDeviceRpcRequestMsg(msg), timeout);
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
                    notifyTransportAboutClosedSession(id, session, SESSION_TIMEOUT_MESSAGE);
                }
            }
            if (removed != 0) {
                dumpSessions();
            }
        }

    }

}
