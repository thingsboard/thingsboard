/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.rule.*;
import org.thingsboard.server.actors.shared.AbstractContextAwareMsgProcessor;
import org.thingsboard.server.actors.tenant.RuleChainDeviceMsg;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.core.*;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.kv.BasicAttributeKVMsg;
import org.thingsboard.server.common.msg.session.FromDeviceMsg;
import org.thingsboard.server.common.msg.session.MsgType;
import org.thingsboard.server.common.msg.session.SessionType;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.extensions.api.device.*;
import org.thingsboard.server.extensions.api.plugins.msg.FromDeviceRpcResponse;
import org.thingsboard.server.extensions.api.plugins.msg.RpcError;
import org.thingsboard.server.extensions.api.plugins.msg.TimeoutIntMsg;
import org.thingsboard.server.extensions.api.plugins.msg.TimeoutMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ToDeviceRpcRequest;
import org.thingsboard.server.extensions.api.plugins.msg.ToDeviceRpcRequestBody;
import org.thingsboard.server.extensions.api.plugins.msg.ToDeviceRpcRequestPluginMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ToPluginRpcResponseDeviceMsg;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
public class DeviceActorMessageProcessor extends AbstractContextAwareMsgProcessor {

    private final DeviceId deviceId;
    private final Map<SessionId, SessionInfo> sessions;
    private final Map<SessionId, SessionInfo> attributeSubscriptions;
    private final Map<SessionId, SessionInfo> rpcSubscriptions;

    private final Map<Integer, ToDeviceRpcRequestMetadata> rpcPendingMap;

    private int rpcSeq = 0;
    private String deviceName;
    private String deviceType;
    private DeviceAttributes deviceAttributes;

    public DeviceActorMessageProcessor(ActorSystemContext systemContext, LoggingAdapter logger, DeviceId deviceId) {
        super(systemContext, logger);
        this.deviceId = deviceId;
        this.sessions = new HashMap<>();
        this.attributeSubscriptions = new HashMap<>();
        this.rpcSubscriptions = new HashMap<>();
        this.rpcPendingMap = new HashMap<>();
        initAttributes();
    }

    private void initAttributes() {
        //TODO: add invalidation of deviceType cache.
        Device device = systemContext.getDeviceService().findDeviceById(deviceId);
        this.deviceName = device.getName();
        this.deviceType = device.getType();
        this.deviceAttributes = new DeviceAttributes(fetchAttributes(DataConstants.CLIENT_SCOPE),
                fetchAttributes(DataConstants.SERVER_SCOPE), fetchAttributes(DataConstants.SHARED_SCOPE));
    }

    private void refreshAttributes(DeviceAttributesEventNotificationMsg msg) {
        if (msg.isDeleted()) {
            msg.getDeletedKeys().forEach(key -> deviceAttributes.remove(key));
        } else {
            deviceAttributes.update(msg.getScope(), msg.getValues());
        }
    }

    void processRpcRequest(ActorContext context, ToDeviceRpcRequestPluginMsg msg) {
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
            ToPluginRpcResponseDeviceMsg responsePluginMsg = toPluginRpcResponseMsg(msg, (String) null);
            context.parent().tell(responsePluginMsg, ActorRef.noSender());
            logger.debug("[{}] Rpc command response sent [{}]!", deviceId, request.getId());
        } else {
            registerPendingRpcRequest(context, msg, sent, rpcRequest, timeout);
        }
        if (sent) {
            logger.debug("[{}] RPC request {} is sent!", deviceId, request.getId());
        } else {
            logger.debug("[{}] RPC request {} is NOT sent!", deviceId, request.getId());
        }

    }

    private void registerPendingRpcRequest(ActorContext context, ToDeviceRpcRequestPluginMsg msg, boolean sent, ToDeviceRpcRequestMsg rpcRequest, long timeout) {
        rpcPendingMap.put(rpcRequest.getRequestId(), new ToDeviceRpcRequestMetadata(msg, sent));
        TimeoutIntMsg timeoutMsg = new TimeoutIntMsg(rpcRequest.getRequestId(), timeout);
        scheduleMsgWithDelay(context, timeoutMsg, timeoutMsg.getTimeout());
    }

    public void processTimeout(ActorContext context, TimeoutMsg msg) {
        ToDeviceRpcRequestMetadata requestMd = rpcPendingMap.remove(msg.getId());
        if (requestMd != null) {
            logger.debug("[{}] RPC request [{}] timeout detected!", deviceId, msg.getId());
            ToPluginRpcResponseDeviceMsg responsePluginMsg = toPluginRpcResponseMsg(requestMd.getMsg(), requestMd.isSent() ? RpcError.TIMEOUT : RpcError.NO_ACTIVE_CONNECTION);
            context.parent().tell(responsePluginMsg, ActorRef.noSender());
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
        Set<UUID> sentOneWayIds = new HashSet<>();
        if (type == SessionType.ASYNC) {
            rpcPendingMap.entrySet().forEach(processPendingRpc(context, sessionId, server, sentOneWayIds));
        } else {
            rpcPendingMap.entrySet().stream().findFirst().ifPresent(processPendingRpc(context, sessionId, server, sentOneWayIds));
        }

        sentOneWayIds.forEach(rpcPendingMap::remove);
    }

    private Consumer<Map.Entry<Integer, ToDeviceRpcRequestMetadata>> processPendingRpc(ActorContext context, SessionId sessionId, Optional<ServerAddress> server, Set<UUID> sentOneWayIds) {
        return entry -> {
            ToDeviceRpcRequest request = entry.getValue().getMsg().getMsg();
            ToDeviceRpcRequestBody body = request.getBody();
            if (request.isOneway()) {
                sentOneWayIds.add(request.getId());
                ToPluginRpcResponseDeviceMsg responsePluginMsg = toPluginRpcResponseMsg(entry.getValue().getMsg(), (String) null);
                context.parent().tell(responsePluginMsg, ActorRef.noSender());
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

    void process(ActorContext context, ToDeviceActorMsg msg) {
        processSubscriptionCommands(context, msg);
        processRpcResponses(context, msg);
        processSessionStateMsgs(msg);
    }

    void processAttributesUpdate(ActorContext context, DeviceAttributesEventNotificationMsg msg) {
        refreshAttributes(msg);
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

    void process(ActorContext context, RuleChainDeviceMsg srcMsg) {
        ChainProcessingMetaData md = new ChainProcessingMetaData(srcMsg.getRuleChain(),
                srcMsg.getToDeviceActorMsg(), new DeviceMetaData(deviceId, deviceName, deviceType, deviceAttributes), context.self());
        ChainProcessingContext ctx = new ChainProcessingContext(md);
        if (ctx.getChainLength() > 0) {
            RuleProcessingMsg msg = new RuleProcessingMsg(ctx);
            ActorRef ruleActorRef = ctx.getCurrentActor();
            ruleActorRef.tell(msg, ActorRef.noSender());
        } else {
            context.self().tell(new RulesProcessedMsg(ctx), context.self());
        }
    }

    void processRpcResponses(ActorContext context, ToDeviceActorMsg msg) {
        SessionId sessionId = msg.getSessionId();
        FromDeviceMsg inMsg = msg.getPayload();
        if (inMsg.getMsgType() == MsgType.TO_DEVICE_RPC_RESPONSE) {
            logger.debug("[{}] Processing rpc command response [{}]", deviceId, sessionId);
            ToDeviceRpcResponseMsg responseMsg = (ToDeviceRpcResponseMsg) inMsg;
            ToDeviceRpcRequestMetadata requestMd = rpcPendingMap.remove(responseMsg.getRequestId());
            boolean success = requestMd != null;
            if (success) {
                ToPluginRpcResponseDeviceMsg responsePluginMsg = toPluginRpcResponseMsg(requestMd.getMsg(), responseMsg.getData());
                Optional<ServerAddress> pluginServerAddress = requestMd.getMsg().getServerAddress();
                if (pluginServerAddress.isPresent()) {
                    systemContext.getRpcService().tell(pluginServerAddress.get(), responsePluginMsg);
                    logger.debug("[{}] Rpc command response sent to remote plugin actor [{}]!", deviceId, requestMd.getMsg().getMsg().getId());
                } else {
                    context.parent().tell(responsePluginMsg, ActorRef.noSender());
                    logger.debug("[{}] Rpc command response sent to local plugin actor [{}]!", deviceId, requestMd.getMsg().getMsg().getId());
                }
            } else {
                logger.debug("[{}] Rpc command response [{}] is stale!", deviceId, responseMsg.getRequestId());
            }
            if (msg.getSessionType() == SessionType.SYNC) {
                BasicCommandAckResponse response = success
                        ? BasicCommandAckResponse.onSuccess(MsgType.TO_DEVICE_RPC_REQUEST, responseMsg.getRequestId())
                        : BasicCommandAckResponse.onError(MsgType.TO_DEVICE_RPC_REQUEST, responseMsg.getRequestId(), new TimeoutException());
                sendMsgToSessionActor(new BasicToDeviceSessionActorMsg(response, msg.getSessionId()), msg.getServerAddress());
            }
        }
    }

    public void processClusterEventMsg(ClusterEventMsg msg) {
        if (!msg.isAdded()) {
            logger.debug("[{}] Clearing attributes/rpc subscription for server [{}]", deviceId, msg.getServerAddress());
            Predicate<Map.Entry<SessionId, SessionInfo>> filter = e -> e.getValue().getServer()
                    .map(serverAddress -> serverAddress.equals(msg.getServerAddress())).orElse(false);
            attributeSubscriptions.entrySet().removeIf(filter);
            rpcSubscriptions.entrySet().removeIf(filter);
        }
    }

    private ToPluginRpcResponseDeviceMsg toPluginRpcResponseMsg(ToDeviceRpcRequestPluginMsg requestMsg, String data) {
        return toPluginRpcResponseMsg(requestMsg, data, null);
    }

    private ToPluginRpcResponseDeviceMsg toPluginRpcResponseMsg(ToDeviceRpcRequestPluginMsg requestMsg, RpcError error) {
        return toPluginRpcResponseMsg(requestMsg, null, error);
    }

    private ToPluginRpcResponseDeviceMsg toPluginRpcResponseMsg(ToDeviceRpcRequestPluginMsg requestMsg, String data, RpcError error) {
        return new ToPluginRpcResponseDeviceMsg(
                requestMsg.getPluginId(),
                requestMsg.getPluginTenantId(),
                new FromDeviceRpcResponse(requestMsg.getMsg().getId(),
                        data,
                        error
                )
        );
    }

    void onRulesProcessedMsg(ActorContext context, RulesProcessedMsg msg) {
        ChainProcessingContext ctx = msg.getCtx();
        ToDeviceActorMsg inMsg = ctx.getInMsg();
        SessionId sid = inMsg.getSessionId();
        ToDeviceSessionActorMsg response;
        if (ctx.getResponse() != null) {
            response = new BasicToDeviceSessionActorMsg(ctx.getResponse(), sid);
        } else {
            response = new BasicToDeviceSessionActorMsg(ctx.getError(), sid);
        }
        sendMsgToSessionActor(response, inMsg.getServerAddress());
    }

    private void processSubscriptionCommands(ActorContext context, ToDeviceActorMsg msg) {
        SessionId sessionId = msg.getSessionId();
        SessionType sessionType = msg.getSessionType();
        FromDeviceMsg inMsg = msg.getPayload();
        if (inMsg.getMsgType() == MsgType.SUBSCRIBE_ATTRIBUTES_REQUEST) {
            logger.debug("[{}] Registering attributes subscription for session [{}]", deviceId, sessionId);
            attributeSubscriptions.put(sessionId, new SessionInfo(sessionType, msg.getServerAddress()));
        } else if (inMsg.getMsgType() == MsgType.UNSUBSCRIBE_ATTRIBUTES_REQUEST) {
            logger.debug("[{}] Canceling attributes subscription for session [{}]", deviceId, sessionId);
            attributeSubscriptions.remove(sessionId);
        } else if (inMsg.getMsgType() == MsgType.SUBSCRIBE_RPC_COMMANDS_REQUEST) {
            logger.debug("[{}] Registering rpc subscription for session [{}][{}]", deviceId, sessionId, sessionType);
            rpcSubscriptions.put(sessionId, new SessionInfo(sessionType, msg.getServerAddress()));
            sendPendingRequests(context, sessionId, sessionType, msg.getServerAddress());
        } else if (inMsg.getMsgType() == MsgType.UNSUBSCRIBE_RPC_COMMANDS_REQUEST) {
            logger.debug("[{}] Canceling rpc subscription for session [{}][{}]", deviceId, sessionId, sessionType);
            rpcSubscriptions.remove(sessionId);
        }
    }

    private void processSessionStateMsgs(ToDeviceActorMsg msg) {
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

    public void processCredentialsUpdate() {
        sessions.forEach((k, v) -> {
            sendMsgToSessionActor(new BasicToDeviceSessionActorMsg(new SessionCloseNotification(), k), v.getServer());
        });
        attributeSubscriptions.clear();
        rpcSubscriptions.clear();
    }

    public void processNameOrTypeUpdate(DeviceNameOrTypeUpdateMsg msg) {
        this.deviceName = msg.getDeviceName();
        this.deviceType = msg.getDeviceType();
    }
}
