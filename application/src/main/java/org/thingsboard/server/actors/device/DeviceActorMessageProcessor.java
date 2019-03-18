/**
 * Copyright © 2016-2019 The Thingsboard Authors
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

import akka.actor.ActorContext;
import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.thingsboard.rule.engine.api.RpcError;
import org.thingsboard.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.thingsboard.rule.engine.api.msg.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.shared.AbstractContextAwareMsgProcessor;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.msg.timeout.DeviceActorClientSideRpcTimeoutMsg;
import org.thingsboard.server.common.msg.timeout.DeviceActorServerSideRpcTimeoutMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceActorToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueType;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionCloseNotificationProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionEvent;
import org.thingsboard.server.gen.transport.TransportProtos.SessionEventMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.SubscribeToAttributeUpdatesMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SubscribeToRPCMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportToDeviceActorMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvListProto;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvProto;
import org.thingsboard.server.service.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.service.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.service.rpc.ToServerRpcResponseActorMsg;
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
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.CLIENT_SCOPE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;

/**
 * @author Andrew Shvayka
 */
@Slf4j
class DeviceActorMessageProcessor extends AbstractContextAwareMsgProcessor {

    final TenantId tenantId;
    final DeviceId deviceId;
    private final Map<UUID, SessionInfoMetaData> sessions;
    private final Map<UUID, SessionInfo> attributeSubscriptions;
    private final Map<UUID, SessionInfo> rpcSubscriptions;
    private final Map<Integer, ToDeviceRpcRequestMetadata> toDeviceRpcPendingMap;
    private final Map<Integer, ToServerRpcRequestMetadata> toServerRpcPendingMap;

    private final Gson gson = new Gson();
    private final JsonParser jsonParser = new JsonParser();

    private int rpcSeq = 0;
    private String deviceName;
    private String deviceType;
    private TbMsgMetaData defaultMetaData;

    DeviceActorMessageProcessor(ActorSystemContext systemContext, TenantId tenantId, DeviceId deviceId) {
        super(systemContext);
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.sessions = new LinkedHashMap<>();
        this.attributeSubscriptions = new HashMap<>();
        this.rpcSubscriptions = new HashMap<>();
        this.toDeviceRpcPendingMap = new HashMap<>();
        this.toServerRpcPendingMap = new HashMap<>();
        if (initAttributes()) {
            restoreSessions();
        }
    }

    private boolean initAttributes() {
        Device device = systemContext.getDeviceService().findDeviceById(tenantId, deviceId);
        if (device != null) {
            this.deviceName = device.getName();
            this.deviceType = device.getType();
            this.defaultMetaData = new TbMsgMetaData();
            this.defaultMetaData.putValue("deviceName", deviceName);
            this.defaultMetaData.putValue("deviceType", deviceType);
            return true;
        } else {
            return false;
        }
    }

    void processRpcRequest(ActorContext context, ToDeviceRpcRequestActorMsg msg) {
        ToDeviceRpcRequest request = msg.getMsg();
        ToDeviceRpcRequestBody body = request.getBody();
        ToDeviceRpcRequestMsg rpcRequest = ToDeviceRpcRequestMsg.newBuilder().setRequestId(
                rpcSeq++).setMethodName(body.getMethod()).setParams(body.getParams()).build();

        long timeout = request.getExpirationTime() - System.currentTimeMillis();
        if (timeout <= 0) {
            log.debug("[{}][{}] Ignoring message due to exp time reached, {}", deviceId, request.getId(), request.getExpirationTime());
            return;
        }

        boolean sent = rpcSubscriptions.size() > 0;
        Set<UUID> syncSessionSet = new HashSet<>();
        rpcSubscriptions.forEach((key, value) -> {
            sendToTransport(rpcRequest, key, value.getNodeId());
            if (TransportProtos.SessionType.SYNC == value.getType()) {
                syncSessionSet.add(key);
            }
        });
        syncSessionSet.forEach(rpcSubscriptions::remove);

        if (request.isOneway() && sent) {
            log.debug("[{}] Rpc command response sent [{}]!", deviceId, request.getId());
            systemContext.getDeviceRpcService().processResponseToServerSideRPCRequestFromDeviceActor(new FromDeviceRpcResponse(msg.getMsg().getId(), null, null));
        } else {
            registerPendingRpcRequest(context, msg, sent, rpcRequest, timeout);
        }
        if (sent) {
            log.debug("[{}] RPC request {} is sent!", deviceId, request.getId());
        } else {
            log.debug("[{}] RPC request {} is NOT sent!", deviceId, request.getId());
        }
    }

    private void registerPendingRpcRequest(ActorContext context, ToDeviceRpcRequestActorMsg msg, boolean sent, ToDeviceRpcRequestMsg rpcRequest, long timeout) {
        toDeviceRpcPendingMap.put(rpcRequest.getRequestId(), new ToDeviceRpcRequestMetadata(msg, sent));
        DeviceActorServerSideRpcTimeoutMsg timeoutMsg = new DeviceActorServerSideRpcTimeoutMsg(rpcRequest.getRequestId(), timeout);
        scheduleMsgWithDelay(context, timeoutMsg, timeoutMsg.getTimeout());
    }

    void processServerSideRpcTimeout(ActorContext context, DeviceActorServerSideRpcTimeoutMsg msg) {
        ToDeviceRpcRequestMetadata requestMd = toDeviceRpcPendingMap.remove(msg.getId());
        if (requestMd != null) {
            log.debug("[{}] RPC request [{}] timeout detected!", deviceId, msg.getId());
            systemContext.getDeviceRpcService().processResponseToServerSideRPCRequestFromDeviceActor(new FromDeviceRpcResponse(requestMd.getMsg().getMsg().getId(),
                    null, requestMd.isSent() ? RpcError.TIMEOUT : RpcError.NO_ACTIVE_CONNECTION));
        }
    }

    private void sendPendingRequests(ActorContext context, UUID sessionId, SessionInfoProto sessionInfo) {
        TransportProtos.SessionType sessionType = getSessionType(sessionId);
        if (!toDeviceRpcPendingMap.isEmpty()) {
            log.debug("[{}] Pushing {} pending RPC messages to new async session [{}]", deviceId, toDeviceRpcPendingMap.size(), sessionId);
            if (sessionType == TransportProtos.SessionType.SYNC) {
                log.debug("[{}] Cleanup sync rpc session [{}]", deviceId, sessionId);
                rpcSubscriptions.remove(sessionId);
            }
        } else {
            log.debug("[{}] No pending RPC messages for new async session [{}]", deviceId, sessionId);
        }
        Set<Integer> sentOneWayIds = new HashSet<>();
        if (sessionType == TransportProtos.SessionType.ASYNC) {
            toDeviceRpcPendingMap.entrySet().forEach(processPendingRpc(context, sessionId, sessionInfo.getNodeId(), sentOneWayIds));
        } else {
            toDeviceRpcPendingMap.entrySet().stream().findFirst().ifPresent(processPendingRpc(context, sessionId, sessionInfo.getNodeId(), sentOneWayIds));
        }

        sentOneWayIds.forEach(toDeviceRpcPendingMap::remove);
    }

    private Consumer<Map.Entry<Integer, ToDeviceRpcRequestMetadata>> processPendingRpc(ActorContext context, UUID sessionId, String nodeId, Set<Integer> sentOneWayIds) {
        return entry -> {
            ToDeviceRpcRequest request = entry.getValue().getMsg().getMsg();
            ToDeviceRpcRequestBody body = request.getBody();
            if (request.isOneway()) {
                sentOneWayIds.add(entry.getKey());
                systemContext.getDeviceRpcService().processResponseToServerSideRPCRequestFromDeviceActor(new FromDeviceRpcResponse(request.getId(), null, null));
            }
            ToDeviceRpcRequestMsg rpcRequest = ToDeviceRpcRequestMsg.newBuilder().setRequestId(
                    entry.getKey()).setMethodName(body.getMethod()).setParams(body.getParams()).build();
            sendToTransport(rpcRequest, sessionId, nodeId);
        };
    }

    void process(ActorContext context, TransportToDeviceActorMsgWrapper wrapper) {
        TransportToDeviceActorMsg msg = wrapper.getMsg();
        if (msg.hasSessionEvent()) {
            processSessionStateMsgs(msg.getSessionInfo(), msg.getSessionEvent());
        }
        if (msg.hasSubscribeToAttributes()) {
            processSubscriptionCommands(context, msg.getSessionInfo(), msg.getSubscribeToAttributes());
        }
        if (msg.hasSubscribeToRPC()) {
            processSubscriptionCommands(context, msg.getSessionInfo(), msg.getSubscribeToRPC());
        }
        if (msg.hasPostAttributes()) {
            handlePostAttributesRequest(context, msg.getSessionInfo(), msg.getPostAttributes());
            reportLogicalDeviceActivity();
        }
        if (msg.hasPostTelemetry()) {
            handlePostTelemetryRequest(context, msg.getSessionInfo(), msg.getPostTelemetry());
            reportLogicalDeviceActivity();
        }
        if (msg.hasGetAttributes()) {
            handleGetAttributesRequest(context, msg.getSessionInfo(), msg.getGetAttributes());
        }
        if (msg.hasToDeviceRPCCallResponse()) {
            processRpcResponses(context, msg.getSessionInfo(), msg.getToDeviceRPCCallResponse());
        }
        if (msg.hasToServerRPCCallRequest()) {
            handleClientSideRPCRequest(context, msg.getSessionInfo(), msg.getToServerRPCCallRequest());
            reportLogicalDeviceActivity();
        }
        if (msg.hasSubscriptionInfo()) {
            handleSessionActivity(context, msg.getSessionInfo(), msg.getSubscriptionInfo());
        }
    }

    private void reportLogicalDeviceActivity() {
        systemContext.getDeviceStateService().onDeviceActivity(deviceId);
    }

    private void reportSessionOpen() {
        systemContext.getDeviceStateService().onDeviceConnect(deviceId);
    }

    private void reportSessionClose() {
        systemContext.getDeviceStateService().onDeviceDisconnect(deviceId);
    }

    private void handleGetAttributesRequest(ActorContext context, SessionInfoProto sessionInfo, GetAttributeRequestMsg request) {
        int requestId = request.getRequestId();
        Futures.addCallback(getAttributesKvEntries(request), new FutureCallback<List<List<AttributeKvEntry>>>() {
            @Override
            public void onSuccess(@Nullable List<List<AttributeKvEntry>> result) {
                GetAttributeResponseMsg responseMsg = GetAttributeResponseMsg.newBuilder()
                        .setRequestId(requestId)
                        .addAllClientAttributeList(toTsKvProtos(result.get(0)))
                        .addAllSharedAttributeList(toTsKvProtos(result.get(1)))
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
        });
    }

    private ListenableFuture<List<List<AttributeKvEntry>>> getAttributesKvEntries(GetAttributeRequestMsg request) {
        ListenableFuture<List<AttributeKvEntry>> clientAttributesFuture;
        ListenableFuture<List<AttributeKvEntry>> sharedAttributesFuture;
        if (CollectionUtils.isEmpty(request.getClientAttributeNamesList()) && CollectionUtils.isEmpty(request.getSharedAttributeNamesList())) {
            clientAttributesFuture = findAllAttributesByScope(CLIENT_SCOPE);
            sharedAttributesFuture = findAllAttributesByScope(SHARED_SCOPE);
        } else if (!CollectionUtils.isEmpty(request.getClientAttributeNamesList()) && !CollectionUtils.isEmpty(request.getSharedAttributeNamesList())) {
            clientAttributesFuture = findAttributesByScope(toSet(request.getClientAttributeNamesList()), CLIENT_SCOPE);
            sharedAttributesFuture = findAttributesByScope(toSet(request.getSharedAttributeNamesList()), SHARED_SCOPE);
        } else if (CollectionUtils.isEmpty(request.getClientAttributeNamesList()) && !CollectionUtils.isEmpty(request.getSharedAttributeNamesList())) {
            clientAttributesFuture = Futures.immediateFuture(Collections.emptyList());
            sharedAttributesFuture = findAttributesByScope(toSet(request.getSharedAttributeNamesList()), SHARED_SCOPE);
        } else {
            sharedAttributesFuture = Futures.immediateFuture(Collections.emptyList());
            clientAttributesFuture = findAttributesByScope(toSet(request.getClientAttributeNamesList()), CLIENT_SCOPE);
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

    private void handlePostAttributesRequest(ActorContext context, SessionInfoProto sessionInfo, PostAttributeMsg postAttributes) {
        JsonObject json = getJsonObject(postAttributes.getKvList());
        TbMsg tbMsg = new TbMsg(UUIDs.timeBased(), SessionMsgType.POST_ATTRIBUTES_REQUEST.name(), deviceId, defaultMetaData.copy(),
                TbMsgDataType.JSON, gson.toJson(json), null, null, 0L);
        pushToRuleEngine(context, tbMsg);
    }

    private void handlePostTelemetryRequest(ActorContext context, SessionInfoProto sessionInfo, PostTelemetryMsg postTelemetry) {
        for (TsKvListProto tsKv : postTelemetry.getTsKvListList()) {
            JsonObject json = getJsonObject(tsKv.getKvList());
            TbMsgMetaData metaData = defaultMetaData.copy();
            metaData.putValue("ts", tsKv.getTs() + "");
            TbMsg tbMsg = new TbMsg(UUIDs.timeBased(), SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, metaData, TbMsgDataType.JSON, gson.toJson(json), null, null, 0L);
            pushToRuleEngine(context, tbMsg);
        }
    }

    private void handleClientSideRPCRequest(ActorContext context, SessionInfoProto sessionInfo, TransportProtos.ToServerRpcRequestMsg request) {
        UUID sessionId = getSessionId(sessionInfo);
        JsonObject json = new JsonObject();
        json.addProperty("method", request.getMethodName());
        json.add("params", jsonParser.parse(request.getParams()));

        TbMsgMetaData requestMetaData = defaultMetaData.copy();
        requestMetaData.putValue("requestId", Integer.toString(request.getRequestId()));
        TbMsg tbMsg = new TbMsg(UUIDs.timeBased(), SessionMsgType.TO_SERVER_RPC_REQUEST.name(), deviceId, requestMetaData, TbMsgDataType.JSON, gson.toJson(json), null, null, 0L);
        context.parent().tell(new DeviceActorToRuleEngineMsg(context.self(), tbMsg), context.self());

        scheduleMsgWithDelay(context, new DeviceActorClientSideRpcTimeoutMsg(request.getRequestId(), systemContext.getClientSideRpcTimeout()), systemContext.getClientSideRpcTimeout());
        toServerRpcPendingMap.put(request.getRequestId(), new ToServerRpcRequestMetadata(sessionId, getSessionType(sessionId), sessionInfo.getNodeId()));
    }

    private TransportProtos.SessionType getSessionType(UUID sessionId) {
        return sessions.containsKey(sessionId) ? TransportProtos.SessionType.ASYNC : TransportProtos.SessionType.SYNC;
    }

    void processClientSideRpcTimeout(ActorContext context, DeviceActorClientSideRpcTimeoutMsg msg) {
        ToServerRpcRequestMetadata data = toServerRpcPendingMap.remove(msg.getId());
        if (data != null) {
            log.debug("[{}] Client side RPC request [{}] timeout detected!", deviceId, msg.getId());
            sendToTransport(TransportProtos.ToServerRpcResponseMsg.newBuilder()
                            .setRequestId(msg.getId()).setError("timeout").build()
                    , data.getSessionId(), data.getNodeId());
        }
    }

    void processToServerRPCResponse(ActorContext context, ToServerRpcResponseActorMsg msg) {
        int requestId = msg.getMsg().getRequestId();
        ToServerRpcRequestMetadata data = toServerRpcPendingMap.remove(requestId);
        if (data != null) {
            log.debug("[{}] Pushing reply to [{}][{}]!", deviceId, data.getNodeId(), data.getSessionId());
            sendToTransport(TransportProtos.ToServerRpcResponseMsg.newBuilder()
                            .setRequestId(requestId).setPayload(msg.getMsg().getData()).build()
                    , data.getSessionId(), data.getNodeId());
        } else {
            log.debug("[{}][{}] Pending RPC request to server not found!", deviceId, requestId);
        }
    }

    private void pushToRuleEngine(ActorContext context, TbMsg tbMsg) {
        context.parent().tell(new DeviceActorToRuleEngineMsg(context.self(), tbMsg), context.self());
    }

    void processAttributesUpdate(ActorContext context, DeviceAttributesEventNotificationMsg msg) {
        if (attributeSubscriptions.size() > 0) {
            boolean hasNotificationData = false;
            AttributeUpdateNotificationMsg.Builder notification = AttributeUpdateNotificationMsg.newBuilder();
            if (msg.isDeleted()) {
                List<String> sharedKeys = msg.getDeletedKeys().stream()
                        .filter(key -> SHARED_SCOPE.equals(key.getScope()))
                        .map(AttributeKey::getAttributeKey)
                        .collect(Collectors.toList());
                if (!sharedKeys.isEmpty()) {
                    notification.addAllSharedDeleted(sharedKeys);
                    hasNotificationData = true;
                }
            } else {
                if (SHARED_SCOPE.equals(msg.getScope())) {
                    List<AttributeKvEntry> attributes = new ArrayList<>(msg.getValues());
                    if (attributes.size() > 0) {
                        List<TsKvProto> sharedUpdated = msg.getValues().stream().map(this::toTsKvProto)
                                .collect(Collectors.toList());
                        if (!sharedUpdated.isEmpty()) {
                            notification.addAllSharedUpdated(sharedUpdated);
                            hasNotificationData = true;
                        }
                    } else {
                        log.debug("[{}] No public server side attributes changed!", deviceId);
                    }
                }
            }
            if (hasNotificationData) {
                AttributeUpdateNotificationMsg finalNotification = notification.build();
                attributeSubscriptions.entrySet().forEach(sub -> {
                    sendToTransport(finalNotification, sub.getKey(), sub.getValue().getNodeId());
                });
            }
        } else {
            log.debug("[{}] No registered attributes subscriptions to process!", deviceId);
        }
    }

    private void processRpcResponses(ActorContext context, SessionInfoProto sessionInfo, ToDeviceRpcResponseMsg responseMsg) {
        UUID sessionId = getSessionId(sessionInfo);
        log.debug("[{}] Processing rpc command response [{}]", deviceId, sessionId);
        ToDeviceRpcRequestMetadata requestMd = toDeviceRpcPendingMap.remove(responseMsg.getRequestId());
        boolean success = requestMd != null;
        if (success) {
            systemContext.getDeviceRpcService().processResponseToServerSideRPCRequestFromDeviceActor(new FromDeviceRpcResponse(requestMd.getMsg().getMsg().getId(),
                    responseMsg.getPayload(), null));
        } else {
            log.debug("[{}] Rpc command response [{}] is stale!", deviceId, responseMsg.getRequestId());
        }
    }

    private void processSubscriptionCommands(ActorContext context, SessionInfoProto sessionInfo, SubscribeToAttributeUpdatesMsg subscribeCmd) {
        UUID sessionId = getSessionId(sessionInfo);
        if (subscribeCmd.getUnsubscribe()) {
            log.debug("[{}] Canceling attributes subscription for session [{}]", deviceId, sessionId);
            attributeSubscriptions.remove(sessionId);
        } else {
            SessionInfoMetaData sessionMD = sessions.get(sessionId);
            if (sessionMD == null) {
                sessionMD = new SessionInfoMetaData(new SessionInfo(TransportProtos.SessionType.SYNC, sessionInfo.getNodeId()));
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

    private void processSubscriptionCommands(ActorContext context, SessionInfoProto sessionInfo, SubscribeToRPCMsg subscribeCmd) {
        UUID sessionId = getSessionId(sessionInfo);
        if (subscribeCmd.getUnsubscribe()) {
            log.debug("[{}] Canceling rpc subscription for session [{}]", deviceId, sessionId);
            rpcSubscriptions.remove(sessionId);
        } else {
            SessionInfoMetaData sessionMD = sessions.get(sessionId);
            if (sessionMD == null) {
                sessionMD = new SessionInfoMetaData(new SessionInfo(TransportProtos.SessionType.SYNC, sessionInfo.getNodeId()));
            }
            sessionMD.setSubscribedToRPC(true);
            log.debug("[{}] Registering rpc subscription for session [{}]", deviceId, sessionId);
            rpcSubscriptions.put(sessionId, sessionMD.getSessionInfo());
            sendPendingRequests(context, sessionId, sessionInfo);
            dumpSessions();
        }
    }

    private void processSessionStateMsgs(SessionInfoProto sessionInfo, SessionEventMsg msg) {
        UUID sessionId = getSessionId(sessionInfo);
        if (msg.getEvent() == SessionEvent.OPEN) {
            if (sessions.containsKey(sessionId)) {
                log.debug("[{}] Received duplicate session open event [{}]", deviceId, sessionId);
                return;
            }
            log.debug("[{}] Processing new session [{}]", deviceId, sessionId);
            if (sessions.size() >= systemContext.getMaxConcurrentSessionsPerDevice()) {
                UUID sessionIdToRemove = sessions.keySet().stream().findFirst().orElse(null);
                if (sessionIdToRemove != null) {
                    notifyTransportAboutClosedSession(sessionIdToRemove, sessions.remove(sessionIdToRemove));
                }
            }
            sessions.put(sessionId, new SessionInfoMetaData(new SessionInfo(TransportProtos.SessionType.ASYNC, sessionInfo.getNodeId())));
            if (sessions.size() == 1) {
                reportSessionOpen();
            }
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

    private void handleSessionActivity(ActorContext context, SessionInfoProto sessionInfoProto, TransportProtos.SubscriptionInfoProto subscriptionInfo) {
        UUID sessionId = getSessionId(sessionInfoProto);
        SessionInfoMetaData sessionMD = sessions.computeIfAbsent(sessionId,
                id -> new SessionInfoMetaData(new SessionInfo(TransportProtos.SessionType.ASYNC, sessionInfoProto.getNodeId()), 0L));

        sessionMD.setLastActivityTime(subscriptionInfo.getLastActivityTime());
        sessionMD.setSubscribedToAttributes(subscriptionInfo.getAttributeSubscription());
        sessionMD.setSubscribedToRPC(subscriptionInfo.getRpcSubscription());
        if (subscriptionInfo.getAttributeSubscription()) {
            attributeSubscriptions.putIfAbsent(sessionId, sessionMD.getSessionInfo());
        }
        if (subscriptionInfo.getRpcSubscription()) {
            rpcSubscriptions.putIfAbsent(sessionId, sessionMD.getSessionInfo());
        }
        dumpSessions();
    }

    void processCredentialsUpdate() {
        sessions.forEach(this::notifyTransportAboutClosedSession);
        attributeSubscriptions.clear();
        rpcSubscriptions.clear();
        dumpSessions();
    }

    private void notifyTransportAboutClosedSession(UUID sessionId, SessionInfoMetaData sessionMd) {
        DeviceActorToTransportMsg msg = DeviceActorToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setSessionCloseNotification(SessionCloseNotificationProto.getDefaultInstance()).build();
        systemContext.getRuleEngineTransportService().process(sessionMd.getSessionInfo().getNodeId(), msg);
    }

    void processNameOrTypeUpdate(DeviceNameOrTypeUpdateMsg msg) {
        this.deviceName = msg.getDeviceName();
        this.deviceType = msg.getDeviceType();
        this.defaultMetaData = new TbMsgMetaData();
        this.defaultMetaData.putValue("deviceName", deviceName);
        this.defaultMetaData.putValue("deviceType", deviceType);
    }

    private JsonObject getJsonObject(List<KeyValueProto> tsKv) {
        JsonObject json = new JsonObject();
        for (KeyValueProto kv : tsKv) {
            switch (kv.getType()) {
                case BOOLEAN_V:
                    json.addProperty(kv.getKey(), kv.getBoolV());
                    break;
                case LONG_V:
                    json.addProperty(kv.getKey(), kv.getLongV());
                    break;
                case DOUBLE_V:
                    json.addProperty(kv.getKey(), kv.getDoubleV());
                    break;
                case STRING_V:
                    json.addProperty(kv.getKey(), kv.getStringV());
                    break;
            }
        }
        return json;
    }

    private void sendToTransport(GetAttributeResponseMsg responseMsg, SessionInfoProto sessionInfo) {
        DeviceActorToTransportMsg msg = DeviceActorToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionInfo.getSessionIdMSB())
                .setSessionIdLSB(sessionInfo.getSessionIdLSB())
                .setGetAttributesResponse(responseMsg).build();
        systemContext.getRuleEngineTransportService().process(sessionInfo.getNodeId(), msg);
    }

    private void sendToTransport(AttributeUpdateNotificationMsg notificationMsg, UUID sessionId, String nodeId) {
        DeviceActorToTransportMsg msg = DeviceActorToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setAttributeUpdateNotification(notificationMsg).build();
        systemContext.getRuleEngineTransportService().process(nodeId, msg);
    }

    private void sendToTransport(ToDeviceRpcRequestMsg rpcMsg, UUID sessionId, String nodeId) {
        DeviceActorToTransportMsg msg = DeviceActorToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setToDeviceRequest(rpcMsg).build();
        systemContext.getRuleEngineTransportService().process(nodeId, msg);
    }

    private void sendToTransport(TransportProtos.ToServerRpcResponseMsg rpcMsg, UUID sessionId, String nodeId) {
        DeviceActorToTransportMsg msg = DeviceActorToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setToServerResponse(rpcMsg).build();
        systemContext.getRuleEngineTransportService().process(nodeId, msg);
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
        }
        return builder.build();
    }

    private void restoreSessions() {
        log.debug("[{}] Restoring sessions from cache", deviceId);
        TransportProtos.DeviceSessionsCacheEntry sessionsDump = null;
        try {
            sessionsDump = TransportProtos.DeviceSessionsCacheEntry.parseFrom(systemContext.getDeviceSessionCacheService().get(deviceId));
        } catch (InvalidProtocolBufferException e) {
            log.warn("[{}] Failed to decode device sessions from cache", deviceId);
            return;
        }
        if (sessionsDump.getSessionsCount() == 0) {
            log.debug("[{}] No session information found", deviceId);
            return;
        }
        for (TransportProtos.SessionSubscriptionInfoProto sessionSubscriptionInfoProto : sessionsDump.getSessionsList()) {
            SessionInfoProto sessionInfoProto = sessionSubscriptionInfoProto.getSessionInfo();
            UUID sessionId = getSessionId(sessionInfoProto);
            SessionInfo sessionInfo = new SessionInfo(TransportProtos.SessionType.ASYNC, sessionInfoProto.getNodeId());
            TransportProtos.SubscriptionInfoProto subInfo = sessionSubscriptionInfoProto.getSubscriptionInfo();
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
        log.debug("[{}] Dumping sessions: {}, rpc subscriptions: {}, attribute subscriptions: {} to cache", deviceId, sessions.size(), rpcSubscriptions.size(), attributeSubscriptions.size());
        List<TransportProtos.SessionSubscriptionInfoProto> sessionsList = new ArrayList<>(sessions.size());
        sessions.forEach((uuid, sessionMD) -> {
            if (sessionMD.getSessionInfo().getType() == TransportProtos.SessionType.SYNC) {
                return;
            }
            SessionInfo sessionInfo = sessionMD.getSessionInfo();
            TransportProtos.SubscriptionInfoProto subscriptionInfoProto = TransportProtos.SubscriptionInfoProto.newBuilder()
                    .setLastActivityTime(sessionMD.getLastActivityTime())
                    .setAttributeSubscription(sessionMD.isSubscribedToAttributes())
                    .setRpcSubscription(sessionMD.isSubscribedToRPC()).build();
            TransportProtos.SessionInfoProto sessionInfoProto = TransportProtos.SessionInfoProto.newBuilder()
                    .setSessionIdMSB(uuid.getMostSignificantBits())
                    .setSessionIdLSB(uuid.getLeastSignificantBits())
                    .setNodeId(sessionInfo.getNodeId()).build();
            sessionsList.add(TransportProtos.SessionSubscriptionInfoProto.newBuilder()
                    .setSessionInfo(sessionInfoProto)
                    .setSubscriptionInfo(subscriptionInfoProto).build());
            log.debug("[{}] Dumping session: {}", deviceId, sessionMD);
        });
        systemContext.getDeviceSessionCacheService()
                .put(deviceId, TransportProtos.DeviceSessionsCacheEntry.newBuilder()
                        .addAllSessions(sessionsList).build().toByteArray());
    }

    void initSessionTimeout(ActorContext context) {
        schedulePeriodicMsgWithDelay(context, SessionTimeoutCheckMsg.instance(), systemContext.getSessionInactivityTimeout(), systemContext.getSessionInactivityTimeout());
    }

    void checkSessionsTimeout() {
        long expTime = System.currentTimeMillis() - systemContext.getSessionInactivityTimeout();
        Map<UUID, SessionInfoMetaData> sessionsToRemove = sessions.entrySet().stream().filter(kv -> kv.getValue().getLastActivityTime() < expTime).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        sessionsToRemove.forEach((sessionId, sessionMD) -> {
            sessions.remove(sessionId);
            rpcSubscriptions.remove(sessionId);
            attributeSubscriptions.remove(sessionId);
            notifyTransportAboutClosedSession(sessionId, sessionMD);
        });
        if (!sessionsToRemove.isEmpty()) {
            dumpSessions();
        }
    }
}
