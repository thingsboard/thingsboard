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
package org.thingsboard.server.common.msg.core;

import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;

public class BasicToDeviceSessionActorMsg implements ToDeviceSessionActorMsg {

    private final ToDeviceMsg msg;
    private final SessionId sessionId;

    public BasicToDeviceSessionActorMsg(ToDeviceMsg msg, SessionId sessionId) {
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
        return "BasicToSessionResponseMsg [msg=" + msg + ", sessionId=" + sessionId + "]";
    }

}
