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
package org.thingsboard.server.common.msg.core;

import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;

public class BasicActorSystemToDeviceSessionActorMsg implements ActorSystemToDeviceSessionActorMsg {

    private final ToDeviceMsg msg;
    private final SessionId sessionId;

    public BasicActorSystemToDeviceSessionActorMsg(ToDeviceMsg msg, SessionId sessionId) {
        super();
        this.msg = msg;
        this.sessionId = sessionId;
    }

    @Override
    public SessionId getSessionId() {
        return sessionId;
    }

    @Override
    public ToDeviceMsg getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "BasicActorSystemToDeviceSessionActorMsg [msg=" + msg + ", sessionId=" + sessionId + "]";
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.ACTOR_SYSTEM_TO_DEVICE_SESSION_ACTOR_MSG;
    }
}
