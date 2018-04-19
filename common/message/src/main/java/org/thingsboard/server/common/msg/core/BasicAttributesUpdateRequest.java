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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.msg.session.SessionMsgType;

public class BasicAttributesUpdateRequest extends BasicRequest implements AttributesUpdateRequest {

    private static final long serialVersionUID = 1L;

    private final Set<AttributeKvEntry> data;

    public BasicAttributesUpdateRequest() {
        this(DEFAULT_REQUEST_ID);
    }

    public BasicAttributesUpdateRequest(Integer requestId) {
        super(requestId);
        this.data = new LinkedHashSet<>();
    }

    public void add(AttributeKvEntry entry) {
        this.data.add(entry);
    }

    public void add(Collection<AttributeKvEntry> entries) {
        this.data.addAll(entries);
    }

    @Override
    public SessionMsgType getMsgType() {
        return SessionMsgType.POST_ATTRIBUTES_REQUEST;
    }

    @Override
    public Set<AttributeKvEntry> getAttributes() {
        return data;
    }

    @Override
    public String toString() {
        return "BasicAttributesUpdateRequest [data=" + data + "]";
    }

}
