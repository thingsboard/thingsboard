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
package org.thingsboard.server.transport.mqtt.session;

import io.netty.handler.codec.mqtt.MqttMessage;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.msg.core.ResponseMsg;
import org.thingsboard.server.common.msg.session.*;
import org.thingsboard.server.common.msg.session.ex.SessionException;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;
import org.thingsboard.server.transport.mqtt.MqttTransportHandler;

import java.util.Optional;

/**
 * Created by ashvayka on 19.01.17.
 */
public class GatewayDeviceSessionCtx extends DeviceAwareSessionContext {

    private GatewaySessionCtx parent;
    private final MqttSessionId sessionId;
    private volatile boolean closed;

    public GatewayDeviceSessionCtx(GatewaySessionCtx parent, Device device) {
        super(parent.getProcessor(), parent.getAuthService(), device);
        this.parent = parent;
        this.sessionId = new MqttSessionId();
    }

    @Override
    public SessionId getSessionId() {
        return sessionId;
    }

    @Override
    public SessionType getSessionType() {
        return SessionType.ASYNC;
    }

    @Override
    public void onMsg(SessionActorToAdaptorMsg sessionMsg) throws SessionException {
        Optional<MqttMessage> message = getToDeviceMsg(sessionMsg);
        message.ifPresent(parent::writeAndFlush);
    }

    private Optional<MqttMessage> getToDeviceMsg(SessionActorToAdaptorMsg sessionMsg) {
        ToDeviceMsg msg = sessionMsg.getMsg();
        switch (msg.getMsgType()) {
            case STATUS_CODE_RESPONSE:
                ResponseMsg<?> responseMsg = (ResponseMsg) msg;
                if (responseMsg.isSuccess()) {
                    MsgType requestMsgType = responseMsg.getRequestMsgType();
                    Integer requestId = responseMsg.getRequestId();
                    if (requestMsgType == MsgType.POST_ATTRIBUTES_REQUEST || requestMsgType == MsgType.POST_TELEMETRY_REQUEST) {
                        return Optional.of(MqttTransportHandler.createMqttPubAckMsg(requestId));
                    }
                }
                break;
        }
        return Optional.empty();
    }

    @Override
    public void onMsg(SessionCtrlMsg msg) throws SessionException {

    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    @Override
    public long getTimeout() {
        return 0;
    }
}
