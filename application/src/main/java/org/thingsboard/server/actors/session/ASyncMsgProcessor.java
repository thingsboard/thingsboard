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
package org.thingsboard.server.actors.session;

import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.shared.SessionTimeoutMsg;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.core.*;
import org.thingsboard.server.common.msg.core.SessionCloseMsg;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.*;

import akka.actor.ActorContext;
import akka.event.LoggingAdapter;
import org.thingsboard.server.common.msg.session.ctrl.*;
import org.thingsboard.server.common.msg.session.ex.SessionException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class ASyncMsgProcessor extends AbstractSessionActorMsgProcessor {

    private boolean firstMsg = true;
    private Map<Integer, ToDeviceActorMsg> pendingMap = new HashMap<>();
    private Optional<ServerAddress> currentTargetServer;
    private boolean subscribedToAttributeUpdates;
    private boolean subscribedToRpcCommands;

    public ASyncMsgProcessor(ActorSystemContext ctx, LoggingAdapter logger, SessionId sessionId) {
        super(ctx, logger, sessionId);
    }

    @Override
    protected void processToDeviceActorMsg(ActorContext ctx, ToDeviceActorSessionMsg msg) {
        updateSessionCtx(msg, SessionType.ASYNC);
        if (firstMsg) {
            toDeviceMsg(new SessionOpenMsg()).ifPresent(m -> forwardToAppActor(ctx, m));
            firstMsg = false;
        }
        ToDeviceActorMsg pendingMsg = toDeviceMsg(msg);
        FromDeviceMsg fromDeviceMsg = pendingMsg.getPayload();
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
        }
        currentTargetServer = forwardToAppActor(ctx, pendingMsg);
    }

    @Override
    public void processToDeviceMsg(ActorContext context, ToDeviceMsg msg) {
        try {
            if (msg.getMsgType() != MsgType.SESSION_CLOSE) {
                switch (msg.getMsgType()) {
                    case STATUS_CODE_RESPONSE:
                    case GET_ATTRIBUTES_RESPONSE:
                        ResponseMsg responseMsg = (ResponseMsg) msg;
                        if (responseMsg.getRequestId() >= 0) {
                            logger.debug("[{}] Pending request processed: {}", responseMsg.getRequestId(), responseMsg);
                            pendingMap.remove(responseMsg.getRequestId());
                        }
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
