/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
import akka.actor.ActorRef;
import akka.event.LoggingAdapter;
import com.datastax.driver.core.utils.UUIDs;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.shared.AbstractContextAwareMsgProcessor;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.core.AttributesUpdateNotification;
import org.thingsboard.server.common.msg.core.BasicCommandAckResponse;
import org.thingsboard.server.common.msg.core.BasicStatusCodeResponse;
import org.thingsboard.server.common.msg.core.BasicToDeviceSessionActorMsg;
import org.thingsboard.server.common.msg.core.RuleEngineError;
import org.thingsboard.server.common.msg.core.RuleEngineErrorMsg;
import org.thingsboard.server.common.msg.core.SessionCloseMsg;
import org.thingsboard.server.common.msg.core.SessionCloseNotification;
import org.thingsboard.server.common.msg.core.SessionOpenMsg;
import org.thingsboard.server.common.msg.core.TelemetryUploadRequest;
import org.thingsboard.server.common.msg.core.ToDeviceRpcRequestMsg;
import org.thingsboard.server.common.msg.core.ToDeviceRpcResponseMsg;
import org.thingsboard.server.common.msg.core.ToDeviceSessionActorMsg;
import org.thingsboard.server.common.msg.device.DeviceToDeviceActorMsg;
import org.thingsboard.server.common.msg.kv.BasicAttributeKVMsg;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.common.msg.session.FromDeviceMsg;
import org.thingsboard.server.common.msg.session.FromDeviceRequestMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.msg.session.SessionType;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.common.msg.timeout.DeviceActorQueueTimeoutMsg;
import org.thingsboard.server.common.msg.timeout.DeviceActorRpcTimeoutMsg;
import org.thingsboard.server.extensions.api.device.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.extensions.api.device.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.extensions.api.plugins.msg.FromDeviceRpcResponse;
import org.thingsboard.server.extensions.api.plugins.msg.RpcError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
public class DeviceActorMessageProcessor extends AbstractContextAwareMsgProcessor {

    private final TenantId tenantId;
    private final DeviceId deviceId;
    private final Map<SessionId, SessionInfo> sessions;
    private final Map<SessionId, SessionInfo> attributeSubscriptions;
    private final Map<SessionId, SessionInfo> rpcSubscriptions;
    private final Map<Integer, ToDeviceRpcRequestMetadata> rpcPendingMap;
    private final Map<UUID, PendingSessionMsgData> pendingMsgs;

    private final Gson gson = new Gson();

    private int rpcSeq = 0;
    private String deviceName;
    private String deviceType;
    private TbMsgMetaData defaultMetaData;

    public DeviceActorMessageProcessor(ActorSystemContext systemContext, LoggingAdapter logger, TenantId tenantId, DeviceId deviceId) {
        super(systemContext, logger);
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.sessions = new HashMap<>();
        this.attributeSubscriptions = new HashMap<>();
        this.rpcSubscriptions = new HashMap<>();
        this.rpcPendingMap = new HashMap<>();
        this.pendingMsgs = new HashMap<>();
        initAttributes();
    }

    private void initAttributes() {
        //TODO: add invalidation of deviceType cache.
        Device device = systemContext.getDeviceService().findDeviceById(deviceId);
        this.deviceName = device.getName();
        this.deviceType = device.getType();
        this.defaultMetaData = new TbMsgMetaData();
        this.defaultMetaData.putValue("deviceName", deviceName);
        this.defaultMetaData.putValue("deviceType", deviceType);
    }

    void processRpcRequest(ActorContext context, org.thingsboard.server.service.rpc.ToDeviceRpcRequestMsg msg) {
        ToDeviceRpcRequest request = msg.getMsg();
        ToDeviceRpcRequestBody body = request.getBody();
        ToDeviceRpcRequestMsg rpcRequest = new ToDeviceRpcRequestMsg(
                rpcSeq++,
                body.getMethod(),
                body.getParams()
        );

        long timeout = request.getExpirationTime() - System.currentTimeMillis();
        if (timeout <= 0) {
            logger.debug("[{}][{}] Ignoring message due to exp time reached", deviceId, request.getId(), request.getExpirationTime());
            return;
        }

        boolean sent = rpcSubscriptions.size() > 0;
        Set<SessionId> syncSessionSet = new HashSet<>();
        rpcSubscriptions.entrySet().forEach(sub -> {
            ToDeviceSessionActorMsg response = new BasicToDeviceSessionActorMsg(rpcRequest, sub.getKey());
            sendMsgToSessionActor(response, sub.getValue().getServer());
            if (SessionType.SYNC == sub.getValue().getType()) {
                syncSessionSet.add(sub.getKey());
            }
        });
        syncSessionSet.forEach(rpcSubscriptions::remove);

        if (request.isOneway() && sent) {
            logger.debug("[{}] Rpc command response sent [{}]!", deviceId, request.getId());
            systemContext.getDeviceRpcService().process(new FromDeviceRpcResponse(msg.getMsg().getId(), null, null));
        } else {
            registerPendingRpcRequest(context, msg, sent, rpcRequest, timeout);
        }
        if (sent) {
            logger.debug("[{}] RPC request {} is sent!", deviceId, request.getId());
        } else {
            logger.debug("[{}] RPC request {} is NOT sent!", deviceId, request.getId());
        }

    }

    private void registerPendingRpcRequest(ActorContext context, org.thingsboard.server.service.rpc.ToDeviceRpcRequestMsg msg, boolean sent, ToDeviceRpcRequestMsg rpcRequest, long timeout) {
        rpcPendingMap.put(rpcRequest.getRequestId(), new ToDeviceRpcRequestMetadata(msg, sent));
        DeviceActorRpcTimeoutMsg timeoutMsg = new DeviceActorRpcTimeoutMsg(rpcRequest.getRequestId(), timeout);
        scheduleMsgWithDelay(context, timeoutMsg, timeoutMsg.getTimeout());
    }

    void processRpcTimeout(ActorContext context, DeviceActorRpcTimeoutMsg msg) {
        ToDeviceRpcRequestMetadata requestMd = rpcPendingMap.remove(msg.getId());
        if (requestMd != null) {
            logger.debug("[{}] RPC request [{}] timeout detected!", deviceId, msg.getId());
            systemContext.getDeviceRpcService().process(new FromDeviceRpcResponse(requestMd.getMsg().getMsg().getId(),
                    null, requestMd.isSent() ? RpcError.TIMEOUT : RpcError.NO_ACTIVE_CONNECTION));
        }
    }

    void processQueueTimeout(ActorContext context, DeviceActorQueueTimeoutMsg msg) {
        PendingSessionMsgData data = pendingMsgs.remove(msg.getId());
        if (data != null) {
            logger.debug("[{}] Queue put [{}] timeout detected!", deviceId, msg.getId());
            ToDeviceMsg toDeviceMsg = new RuleEngineErrorMsg(data.getSessionMsgType(), RuleEngineError.QUEUE_PUT_TIMEOUT);
            sendMsgToSessionActor(new BasicToDeviceSessionActorMsg(toDeviceMsg, data.getSessionId()), data.getServerAddress());
        }
    }

    void processQueueAck(ActorContext context, RuleEngineQueuePutAckMsg msg) {
        PendingSessionMsgData data = pendingMsgs.remove(msg.getId());
        if (data != null) {
            logger.debug("[{}] Queue put [{}] ack detected!", deviceId, msg.getId());
            ToDeviceMsg toDeviceMsg = BasicStatusCodeResponse.onSuccess(data.getSessionMsgType(), data.getRequestId());
            sendMsgToSessionActor(new BasicToDeviceSessionActorMsg(toDeviceMsg, data.getSessionId()), data.getServerAddress());
        }
    }

    private void sendPendingRequests(ActorContext context, SessionId sessionId, SessionType type, Optional<ServerAddress> server) {
        if (!rpcPendingMap.isEmpty()) {
            logger.debug("[{}] Pushing {} pending RPC messages to new async session [{}]", deviceId, rpcPendingMap.size(), sessionId);
            if (type == SessionType.SYNC) {
                logger.debug("[{}] Cleanup sync rpc session [{}]", deviceId, sessionId);
                rpcSubscriptions.remove(sessionId);
            }
        } else {
            logger.debug("[{}] No pending RPC messages for new async session [{}]", deviceId, sessionId);
        }
        Set<Integer> sentOneWayIds = new HashSet<>();
        if (type == SessionType.ASYNC) {
            rpcPendingMap.entrySet().forEach(processPendingRpc(context, sessionId, server, sentOneWayIds));
        } else {
            rpcPendingMap.entrySet().stream().findFirst().ifPresent(processPendingRpc(context, sessionId, server, sentOneWayIds));
        }

        sentOneWayIds.forEach(rpcPendingMap::remove);
    }

    private Consumer<Map.Entry<Integer, ToDeviceRpcRequestMetadata>> processPendingRpc(ActorContext context, SessionId sessionId, Optional<ServerAddress> server, Set<Integer> sentOneWayIds) {
        return entry -> {
            ToDeviceRpcRequest request = entry.getValue().getMsg().getMsg();
            ToDeviceRpcRequestBody body = request.getBody();
            if (request.isOneway()) {
                sentOneWayIds.add(entry.getKey());
                systemContext.getDeviceRpcService().process(new FromDeviceRpcResponse(request.getId(), null, null));
            }
            ToDeviceRpcRequestMsg rpcRequest = new ToDeviceRpcRequestMsg(
                    entry.getKey(),
                    body.getMethod(),
                    body.getParams()
            );
            ToDeviceSessionActorMsg response = new BasicToDeviceSessionActorMsg(rpcRequest, sessionId);
            sendMsgToSessionActor(response, server);
        };
    }

    void process(ActorContext context, DeviceToDeviceActorMsg msg) {
        processSubscriptionCommands(context, msg);
        processRpcResponses(context, msg);
        processSessionStateMsgs(msg);
        SessionMsgType sessionMsgType = msg.getPayload().getMsgType();
        if (sessionMsgType.requiresRulesProcessing()) {
            switch (sessionMsgType) {
                case GET_ATTRIBUTES_REQUEST:
                    handleGetAttributesRequest(msg);
                    break;
                case POST_ATTRIBUTES_REQUEST:
                    break;
                case POST_TELEMETRY_REQUEST:
                    handlePostTelemetryRequest(context, msg);
                    break;
                case TO_SERVER_RPC_REQUEST:
                    break;
                //TODO: push to queue and start processing!
            }
        }
    }

    private void handleGetAttributesRequest(DeviceToDeviceActorMsg msg) {

    }

    private void handlePostTelemetryRequest(ActorContext context, DeviceToDeviceActorMsg src) {
        TelemetryUploadRequest telemetry = (TelemetryUploadRequest) src.getPayload();

        Map<Long, List<KvEntry>> tsData = telemetry.getData();

        JsonArray json = new JsonArray();
        for (Map.Entry<Long, List<KvEntry>> entry : tsData.entrySet()) {
            JsonObject ts = new JsonObject();
            ts.addProperty("ts", entry.getKey());
            JsonObject values = new JsonObject();
            for (KvEntry kv : entry.getValue()) {
                kv.getBooleanValue().ifPresent(v -> values.addProperty(kv.getKey(), v));
                kv.getLongValue().ifPresent(v -> values.addProperty(kv.getKey(), v));
                kv.getDoubleValue().ifPresent(v -> values.addProperty(kv.getKey(), v));
                kv.getStrValue().ifPresent(v -> values.addProperty(kv.getKey(), v));
            }
            ts.add("values", values);
            json.add(ts);
        }

        TbMsg tbMsg = new TbMsg(UUIDs.timeBased(), SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, defaultMetaData, TbMsgDataType.JSON, gson.toJson(json));
        pushToRuleEngineWithTimeout(context, tbMsg, src, telemetry);
    }

    private void pushToRuleEngineWithTimeout(ActorContext context, TbMsg tbMsg, DeviceToDeviceActorMsg src, FromDeviceRequestMsg fromDeviceRequestMsg) {
        SessionMsgType sessionMsgType = fromDeviceRequestMsg.getMsgType();
        int requestId = fromDeviceRequestMsg.getRequestId();
        if (systemContext.isQueuePersistenceEnabled()) {
            pendingMsgs.put(tbMsg.getId(), new PendingSessionMsgData(src.getSessionId(), src.getServerAddress(), sessionMsgType, requestId));
            scheduleMsgWithDelay(context, new DeviceActorQueueTimeoutMsg(tbMsg.getId(), systemContext.getQueuePersistenceTimeout()), systemContext.getQueuePersistenceTimeout());
        } else {
            ToDeviceSessionActorMsg response = new BasicToDeviceSessionActorMsg(BasicStatusCodeResponse.onSuccess(sessionMsgType, requestId), src.getSessionId());
            sendMsgToSessionActor(response, src.getServerAddress());
        }
        context.parent().tell(new DeviceActorToRuleEngineMsg(context.self(), tbMsg), context.self());
    }

    void processAttributesUpdate(ActorContext context, DeviceAttributesEventNotificationMsg msg) {
        if (attributeSubscriptions.size() > 0) {
            ToDeviceMsg notification = null;
            if (msg.isDeleted()) {
                List<AttributeKey> sharedKeys = msg.getDeletedKeys().stream()
                        .filter(key -> DataConstants.SHARED_SCOPE.equals(key.getScope()))
                        .collect(Collectors.toList());
                notification = new AttributesUpdateNotification(BasicAttributeKVMsg.fromDeleted(sharedKeys));
            } else {
                if (DataConstants.SHARED_SCOPE.equals(msg.getScope())) {
                    List<AttributeKvEntry> attributes = new ArrayList<>(msg.getValues());
                    if (attributes.size() > 0) {
                        notification = new AttributesUpdateNotification(BasicAttributeKVMsg.fromShared(attributes));
                    } else {
                        logger.debug("[{}] No public server side attributes changed!", deviceId);
                    }
                }
            }
            if (notification != null) {
                ToDeviceMsg finalNotification = notification;
                attributeSubscriptions.entrySet().forEach(sub -> {
                    ToDeviceSessionActorMsg response = new BasicToDeviceSessionActorMsg(finalNotification, sub.getKey());
                    sendMsgToSessionActor(response, sub.getValue().getServer());
                });
            }
        } else {
            logger.debug("[{}] No registered attributes subscriptions to process!", deviceId);
        }
    }

    private void processRpcResponses(ActorContext context, DeviceToDeviceActorMsg msg) {
        SessionId sessionId = msg.getSessionId();
        FromDeviceMsg inMsg = msg.getPayload();
        if (inMsg.getMsgType() == SessionMsgType.TO_DEVICE_RPC_RESPONSE) {
            logger.debug("[{}] Processing rpc command response [{}]", deviceId, sessionId);
            ToDeviceRpcResponseMsg responseMsg = (ToDeviceRpcResponseMsg) inMsg;
            ToDeviceRpcRequestMetadata requestMd = rpcPendingMap.remove(responseMsg.getRequestId());
            boolean success = requestMd != null;
            if (success) {
                systemContext.getDeviceRpcService().process(new FromDeviceRpcResponse(requestMd.getMsg().getMsg().getId(), responseMsg.getData(), null));
            } else {
                logger.debug("[{}] Rpc command response [{}] is stale!", deviceId, responseMsg.getRequestId());
            }
            if (msg.getSessionType() == SessionType.SYNC) {
                BasicCommandAckResponse response = success
                        ? BasicCommandAckResponse.onSuccess(SessionMsgType.TO_DEVICE_RPC_REQUEST, responseMsg.getRequestId())
                        : BasicCommandAckResponse.onError(SessionMsgType.TO_DEVICE_RPC_REQUEST, responseMsg.getRequestId(), new TimeoutException());
                sendMsgToSessionActor(new BasicToDeviceSessionActorMsg(response, msg.getSessionId()), msg.getServerAddress());
            }
        }
    }

    void processClusterEventMsg(ClusterEventMsg msg) {
        if (!msg.isAdded()) {
            logger.debug("[{}] Clearing attributes/rpc subscription for server [{}]", deviceId, msg.getServerAddress());
            Predicate<Map.Entry<SessionId, SessionInfo>> filter = e -> e.getValue().getServer()
                    .map(serverAddress -> serverAddress.equals(msg.getServerAddress())).orElse(false);
            attributeSubscriptions.entrySet().removeIf(filter);
            rpcSubscriptions.entrySet().removeIf(filter);
        }
    }

    private void processSubscriptionCommands(ActorContext context, DeviceToDeviceActorMsg msg) {
        SessionId sessionId = msg.getSessionId();
        SessionType sessionType = msg.getSessionType();
        FromDeviceMsg inMsg = msg.getPayload();
        if (inMsg.getMsgType() == SessionMsgType.SUBSCRIBE_ATTRIBUTES_REQUEST) {
            logger.debug("[{}] Registering attributes subscription for session [{}]", deviceId, sessionId);
            attributeSubscriptions.put(sessionId, new SessionInfo(sessionType, msg.getServerAddress()));
        } else if (inMsg.getMsgType() == SessionMsgType.UNSUBSCRIBE_ATTRIBUTES_REQUEST) {
            logger.debug("[{}] Canceling attributes subscription for session [{}]", deviceId, sessionId);
            attributeSubscriptions.remove(sessionId);
        } else if (inMsg.getMsgType() == SessionMsgType.SUBSCRIBE_RPC_COMMANDS_REQUEST) {
            logger.debug("[{}] Registering rpc subscription for session [{}][{}]", deviceId, sessionId, sessionType);
            rpcSubscriptions.put(sessionId, new SessionInfo(sessionType, msg.getServerAddress()));
            sendPendingRequests(context, sessionId, sessionType, msg.getServerAddress());
        } else if (inMsg.getMsgType() == SessionMsgType.UNSUBSCRIBE_RPC_COMMANDS_REQUEST) {
            logger.debug("[{}] Canceling rpc subscription for session [{}][{}]", deviceId, sessionId, sessionType);
            rpcSubscriptions.remove(sessionId);
        }
    }

    private void processSessionStateMsgs(DeviceToDeviceActorMsg msg) {
        SessionId sessionId = msg.getSessionId();
        FromDeviceMsg inMsg = msg.getPayload();
        if (inMsg instanceof SessionOpenMsg) {
            logger.debug("[{}] Processing new session [{}]", deviceId, sessionId);
            sessions.put(sessionId, new SessionInfo(SessionType.ASYNC, msg.getServerAddress()));
        } else if (inMsg instanceof SessionCloseMsg) {
            logger.debug("[{}] Canceling subscriptions for closed session [{}]", deviceId, sessionId);
            sessions.remove(sessionId);
            attributeSubscriptions.remove(sessionId);
            rpcSubscriptions.remove(sessionId);
        }
    }

    private void sendMsgToSessionActor(ToDeviceSessionActorMsg response, Optional<ServerAddress> sessionAddress) {
        if (sessionAddress.isPresent()) {
            ServerAddress address = sessionAddress.get();
            logger.debug("{} Forwarding msg: {}", address, response);
            systemContext.getRpcService().tell(sessionAddress.get(), response);
        } else {
            systemContext.getSessionManagerActor().tell(response, ActorRef.noSender());
        }
    }

    private List<AttributeKvEntry> fetchAttributes(String scope) {
        try {
            //TODO: replace this with async operation. Happens only during actor creation, but is still criticla for performance,
            return systemContext.getAttributesService().findAll(this.deviceId, scope).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.warning("[{}] Failed to fetch attributes for scope: {}", deviceId, scope);
            throw new RuntimeException(e);
        }
    }

    void processCredentialsUpdate() {
        sessions.forEach((k, v) -> {
            sendMsgToSessionActor(new BasicToDeviceSessionActorMsg(new SessionCloseNotification(), k), v.getServer());
        });
        attributeSubscriptions.clear();
        rpcSubscriptions.clear();
    }

    void processNameOrTypeUpdate(DeviceNameOrTypeUpdateMsg msg) {
        this.deviceName = msg.getDeviceName();
        this.deviceType = msg.getDeviceType();
        this.defaultMetaData = new TbMsgMetaData();
        this.defaultMetaData.putValue("deviceName", deviceName);
        this.defaultMetaData.putValue("deviceType", deviceType);
    }

}
