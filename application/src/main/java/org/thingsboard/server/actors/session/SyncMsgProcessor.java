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
import org.thingsboard.server.common.msg.device.DeviceToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.BasicSessionActorToAdaptorMsg;
import org.thingsboard.server.common.msg.session.SessionContext;
import org.thingsboard.server.common.msg.session.SessionType;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.common.msg.session.TransportToDeviceSessionActorMsg;
import org.thingsboard.server.common.msg.session.ctrl.SessionCloseMsg;
import org.thingsboard.server.common.msg.session.ex.SessionException;

import java.util.Optional;

class SyncMsgProcessor extends AbstractSessionActorMsgProcessor {
    private DeviceToDeviceActorMsg pendingMsg;
    private Optional<ServerAddress> currentTargetServer;
    private boolean pendingResponse;

    public SyncMsgProcessor(ActorSystemContext ctx, LoggingAdapter logger, SessionId sessionId) {
        super(ctx, logger, sessionId);
    }

    @Override
    protected void processToDeviceActorMsg(ActorContext ctx, TransportToDeviceSessionActorMsg msg) {
        updateSessionCtx(msg, SessionType.SYNC);
        pendingMsg = toDeviceMsg(msg);
        pendingResponse = true;
        currentTargetServer = forwardToAppActor(ctx, pendingMsg);
        scheduleMsgWithDelay(ctx, new SessionTimeoutMsg(sessionId), getTimeout(systemContext, msg.getSessionMsg().getSessionContext()), ctx.parent());
    }

    public void processTimeoutMsg(ActorContext context, SessionTimeoutMsg msg) {
        if (pendingResponse) {
            try {
                sessionCtx.onMsg(SessionCloseMsg.onTimeout(sessionId));
            } catch (SessionException e) {
                logger.warning("Failed to push session close msg", e);
            }
            terminateSession(context, this.sessionId);
        }
    }

    public void processToDeviceMsg(ActorContext context, ToDeviceMsg msg) {
        try {
            sessionCtx.onMsg(new BasicSessionActorToAdaptorMsg(this.sessionCtx, msg));
            pendingResponse = false;
        } catch (SessionException e) {
            logger.warning("Failed to push session response msg", e);
        }
        terminateSession(context, this.sessionId);
    }

    @Override
    public void processClusterEvent(ActorContext context, ClusterEventMsg msg) {
        if (pendingResponse) {
            Optional<ServerAddress> newTargetServer = forwardToAppActorIfAddressChanged(context, pendingMsg, currentTargetServer);
            if (logger.isDebugEnabled()) {
                if (!newTargetServer.equals(currentTargetServer)) {
                    if (newTargetServer.isPresent()) {
                        logger.debug("[{}] Forwarded msg to new server: {}", sessionId, newTargetServer.get());
                    } else {
                        logger.debug("[{}] Forwarded msg to local server.", sessionId);
                    }
                }
            }
            currentTargetServer = newTargetServer;
        }
    }

    private long getTimeout(ActorSystemContext ctx, SessionContext sessionCtx) {
        return sessionCtx.getTimeout() > 0 ? sessionCtx.getTimeout() : ctx.getSyncSessionTimeout();
    }
}
