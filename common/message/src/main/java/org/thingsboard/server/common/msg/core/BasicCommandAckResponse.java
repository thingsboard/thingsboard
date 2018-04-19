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

import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.msg.session.SessionMsgType;

public class BasicCommandAckResponse extends BasicResponseMsg<Integer> implements StatusCodeResponse {

    private static final long serialVersionUID = 1L;

    public static BasicCommandAckResponse onSuccess(SessionMsgType requestMsgType, Integer requestId) {
        return BasicCommandAckResponse.onSuccess(requestMsgType, requestId, 200);
    }

    public static BasicCommandAckResponse onSuccess(SessionMsgType requestMsgType, Integer requestId, Integer code) {
        return new BasicCommandAckResponse(requestMsgType, requestId, true, null, code);
    }

    public static BasicCommandAckResponse onError(SessionMsgType requestMsgType, Integer requestId, Exception error) {
        return new BasicCommandAckResponse(requestMsgType, requestId, false, error, null);
    }

    private BasicCommandAckResponse(SessionMsgType requestMsgType, Integer requestId, boolean success, Exception error, Integer code) {
        super(requestMsgType, requestId, SessionMsgType.TO_DEVICE_RPC_RESPONSE_ACK, success, error, code);
    }

    @Override
    public String toString() {
        return "BasicStatusCodeResponse []";
    }
}
