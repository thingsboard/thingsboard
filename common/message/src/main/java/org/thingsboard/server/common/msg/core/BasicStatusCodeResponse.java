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

import lombok.ToString;
import org.thingsboard.server.common.msg.session.MsgType;

@ToString
public class BasicStatusCodeResponse extends BasicResponseMsg<Integer> implements StatusCodeResponse {

    private static final long serialVersionUID = 1L;

    public static BasicStatusCodeResponse onSuccess(MsgType requestMsgType, Integer requestId) {
        return BasicStatusCodeResponse.onSuccess(requestMsgType, requestId, 0);
    }

    public static BasicStatusCodeResponse onSuccess(MsgType requestMsgType, Integer requestId, Integer code) {
        return new BasicStatusCodeResponse(requestMsgType, requestId, true, null, code);
    }

    public static BasicStatusCodeResponse onError(MsgType requestMsgType, Integer requestId, Exception error) {
        return new BasicStatusCodeResponse(requestMsgType, requestId, false, error, null);
    }

    private BasicStatusCodeResponse(MsgType requestMsgType, Integer requestId, boolean success, Exception error, Integer code) {
        super(requestMsgType, requestId, MsgType.STATUS_CODE_RESPONSE, success, error, code);
    }
}
