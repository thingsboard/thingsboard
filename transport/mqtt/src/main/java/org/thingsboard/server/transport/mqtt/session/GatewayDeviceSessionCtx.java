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

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.msg.session.SessionActorToAdaptorMsg;
import org.thingsboard.server.common.msg.session.SessionCtrlMsg;
import org.thingsboard.server.common.msg.session.SessionType;
import org.thingsboard.server.common.msg.session.ex.SessionException;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;

/**
 * Created by ashvayka on 19.01.17.
 */
public class GatewayDeviceSessionCtx extends DeviceAwareSessionContext {

    private final MqttSessionId sessionId;
    private volatile boolean closed;

    public GatewayDeviceSessionCtx(SessionMsgProcessor processor, DeviceAuthService authService, Device device) {
        super(processor, authService, device);
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
    public void onMsg(SessionActorToAdaptorMsg msg) throws SessionException {

    }

    @Override
    public void onMsg(SessionCtrlMsg msg) throws SessionException {

    }

    @Override
    public void onError(SessionException e) {

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
