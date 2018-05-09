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

import lombok.ToString;
import org.thingsboard.server.common.msg.kv.AttributesKVMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.msg.session.SessionMsgType;

@ToString
public class BasicGetAttributesResponse extends BasicResponseMsg<AttributesKVMsg> implements GetAttributesResponse {

    private static final long serialVersionUID = 1L;

    public static BasicGetAttributesResponse onSuccess(SessionMsgType requestMsgType, int requestId, AttributesKVMsg code) {
        return new BasicGetAttributesResponse(requestMsgType, requestId, true, null, code);
    }

    public static BasicGetAttributesResponse onError(SessionMsgType requestMsgType, int requestId, Exception error) {
        return new BasicGetAttributesResponse(requestMsgType, requestId, false, error, null);
    }

    private BasicGetAttributesResponse(SessionMsgType requestMsgType, int requestId, boolean success, Exception error, AttributesKVMsg code) {
        super(requestMsgType, requestId, SessionMsgType.GET_ATTRIBUTES_RESPONSE, success, error, code);
    }

}
