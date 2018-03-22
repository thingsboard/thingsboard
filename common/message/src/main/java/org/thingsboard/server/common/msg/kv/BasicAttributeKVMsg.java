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
package org.thingsboard.server.common.msg.kv;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.util.Collections;
import java.util.List;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class BasicAttributeKVMsg implements AttributesKVMsg {

    private static final long serialVersionUID = 1L;

    private final List<AttributeKvEntry> clientAttributes;
    private final List<AttributeKvEntry> sharedAttributes;
    private final List<AttributeKey> deletedAttributes;

    public static BasicAttributeKVMsg fromClient(List<AttributeKvEntry> attributes) {
        return new BasicAttributeKVMsg(attributes, Collections.emptyList(), Collections.emptyList());
    }

    public static BasicAttributeKVMsg fromShared(List<AttributeKvEntry> attributes) {
        return new BasicAttributeKVMsg(Collections.emptyList(), attributes, Collections.emptyList());
    }

    public static BasicAttributeKVMsg from(List<AttributeKvEntry> client, List<AttributeKvEntry> shared) {
        return new BasicAttributeKVMsg(client, shared, Collections.emptyList());
    }

    public static AttributesKVMsg fromDeleted(List<AttributeKey> shared) {
        return new BasicAttributeKVMsg(Collections.emptyList(), Collections.emptyList(), shared);
    }
}
