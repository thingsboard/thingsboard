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
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@ToString
public class BasicGetAttributesRequest extends BasicRequest implements GetAttributesRequest {

    private static final long serialVersionUID = 1L;

    private final Set<String> clientKeys;
    private final Set<String> sharedKeys;

    public BasicGetAttributesRequest(Integer requestId) {
        this(requestId, Collections.emptySet(), Collections.emptySet());
    }

    public BasicGetAttributesRequest(Integer requestId, Set<String> clientKeys, Set<String> sharedKeys) {
        super(requestId);
        this.clientKeys = clientKeys;
        this.sharedKeys = sharedKeys;
    }

    @Override
    public SessionMsgType getMsgType() {
        return SessionMsgType.GET_ATTRIBUTES_REQUEST;
    }

    @Override
    public Optional<Set<String>> getClientAttributeNames() {
        return Optional.ofNullable(clientKeys);
    }

    @Override
    public Optional<Set<String>> getSharedAttributeNames() {
        return Optional.ofNullable(sharedKeys);
    }

}
