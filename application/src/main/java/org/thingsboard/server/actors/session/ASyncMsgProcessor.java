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
package org.thingsboard.server.actors.session;

import akka.actor.ActorContext;
import akka.event.LoggingAdapter;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.shared.SessionTimeoutMsg;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.core.AttributesSubscribeMsg;
import org.thingsboard.server.common.msg.core.ResponseMsg;
import org.thingsboard.server.common.msg.core.RpcSubscribeMsg;
import org.thingsboard.server.common.msg.core.SessionCloseMsg;
import org.thingsboard.server.common.msg.core.SessionOpenMsg;
import org.thingsboard.server.common.msg.device.DeviceToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.BasicSessionActorToAdaptorMsg;
import org.thingsboard.server.common.msg.session.FromDeviceMsg;
import org.thingsboard.server.common.msg.session.FromDeviceRequestMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.msg.session.SessionType;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.common.msg.session.TransportToDeviceSessionActorMsg;
import org.thingsboard.server.common.msg.session.ex.SessionException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class ASyncMsgProcessor extends AbstractSessionActorMsgProcessor {

    private boolean firstMsg = true;
    private Map<Integer, DeviceToDeviceActorMsg> pendingMap = new HashMap<>();
    private Optional<ServerAddress> currentTargetServer;
    private boolean subscribedToAttributeUpdates;
    private boolean subscribedToRpcCommands;

    public ASyncMsgProcessor(ActorSystemContext ctx, LoggingAdapter logger, SessionId sessionId) {
        super(ctx, logger, sessionId);
    }

    @Override
    protected void processToDeviceActorMsg(ActorContext ctx, TransportToDeviceSessionActorMsg msg) {
        updateSessionCtx(msg, SessionType.ASYNC);
        DeviceToDeviceActorMsg pendingMsg = toDeviceMsg(msg);
        FromDeviceMsg fromDeviceMsg = pendingMsg.getPayload();
        if (firstMsg) {
            if (fromDeviceMsg.getMsgType() != SessionMsgType.SESSION_OPEN) {
                toDeviceMsg(new SessionOpenMsg()).ifPresent(m -> forwardToAppActor(ctx, m));
            }
            firstMsg = false;
        }
        switch (fromDeviceMsg.getMsgType()) {
            case POST_TELEMETRY_REQUEST:
            case POST_ATTRIBUTES_REQUEST:
                FromDeviceRequestMsg requestMsg = (FromDeviceRequestMsg) fromDeviceMsg;
                if (requestMsg.getRequestId() >= 0) {
                    logger.debug("[{}] Pending request {} registered", requestMsg.getRequestId(), requestMsg.getMsgType());
                    //TODO: handle duplicates.
                    pendingMap.put(requestMsg.getRequestId(), pendingMsg);
                }
                break;
            case SUBSCRIBE_ATTRIBUTES_REQUEST:
                subscribedToAttributeUpdates = true;
                break;
            case UNSUBSCRIBE_ATTRIBUTES_REQUEST:
                subscribedToAttributeUpdates = false;
                break;
            case SUBSCRIBE_RPC_COMMANDS_REQUEST:
                subscribedToRpcCommands = true;
                break;
            case UNSUBSCRIBE_RPC_COMMANDS_REQUEST:
                subscribedToRpcCommands = false;
                break;
            default:
                break;
        }
        currentTargetServer = forwardToAppActor(ctx, pendingMsg);
    }

    @Override
    public void processToDeviceMsg(ActorContext context, ToDeviceMsg msg) {
        try {
            if (msg.getSessionMsgType() != SessionMsgType.SESSION_CLOSE) {
                switch (msg.getSessionMsgType()) {
                    case STATUS_CODE_RESPONSE:
                    case GET_ATTRIBUTES_RESPONSE:
                        ResponseMsg responseMsg = (ResponseMsg) msg;
                        if (responseMsg.getRequestId() >= 0) {
                            logger.debug("[{}] Pending request processed: {}", responseMsg.getRequestId(), responseMsg);
                            pendingMap.remove(responseMsg.getRequestId());
                        }
                        break;
                    default:
                        break;
                }
                sessionCtx.onMsg(new BasicSessionActorToAdaptorMsg(this.sessionCtx, msg));
            } else {
                sessionCtx.onMsg(org.thingsboard.server.common.msg.session.ctrl.SessionCloseMsg.onCredentialsRevoked(sessionCtx.getSessionId()));
            }
        } catch (SessionException e) {
            logger.warning("Failed to push session response msg", e);
        }
    }

    @Override
    public void processTimeoutMsg(ActorContext context, SessionTimeoutMsg msg) {
        // TODO Auto-generated method stub        
    }

    @Override
    protected void cleanupSession(ActorContext ctx) {
        toDeviceMsg(new SessionCloseMsg()).ifPresent(m -> forwardToAppActor(ctx, m));
    }

    @Override
    public void processClusterEvent(ActorContext context, ClusterEventMsg msg) {
        if (pendingMap.size() > 0 || subscribedToAttributeUpdates || subscribedToRpcCommands) {
            Optional<ServerAddress> newTargetServer = systemContext.getRoutingService().resolveById(getDeviceId());
            if (!newTargetServer.equals(currentTargetServer)) {
                firstMsg = true;
                currentTargetServer = newTargetServer;
                pendingMap.values().forEach(v -> {
                    forwardToAppActor(context, v, currentTargetServer);
                    if (currentTargetServer.isPresent()) {
                        logger.debug("[{}] Forwarded msg to new server: {}", sessionId, currentTargetServer.get());
                    } else {
                        logger.debug("[{}] Forwarded msg to local server.", sessionId);
                    }
                });
                if (subscribedToAttributeUpdates) {
                    toDeviceMsg(new AttributesSubscribeMsg()).ifPresent(m -> forwardToAppActor(context, m, currentTargetServer));
                    logger.debug("[{}] Forwarded attributes subscription.", sessionId);
                }
                if (subscribedToRpcCommands) {
                    toDeviceMsg(new RpcSubscribeMsg()).ifPresent(m -> forwardToAppActor(context, m, currentTargetServer));
                    logger.debug("[{}] Forwarded rpc commands subscription.", sessionId);
                }
            }
        }
    }
}
