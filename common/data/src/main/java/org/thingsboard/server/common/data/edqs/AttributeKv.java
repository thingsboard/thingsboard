/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.edqs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttributeKv implements EdqsObject {

    private EntityId entityId;
    private AttributeScope scope;
    private String key;
    private Long version;

    private DataPoint dataPoint; // optional (on deletion)

    private Long lastUpdateTs; // only for serialization
    private KvEntry value; // only for serialization

    public AttributeKv(EntityId entityId, AttributeScope scope, AttributeKvEntry attributeKvEntry, long version) {
        this.entityId = entityId;
        this.scope = scope;
        this.key = attributeKvEntry.getKey();
        this.version = version;
        this.lastUpdateTs = attributeKvEntry.getLastUpdateTs();
        this.value = attributeKvEntry;
    }

    public AttributeKv(EntityId entityId, AttributeScope scope, String key, long version) {
        this.entityId = entityId;
        this.scope = scope;
        this.key = key;
        this.version = version;
    }

    @Override
    public String stringKey() {
        return "a_" + entityId + "_" + scope + "_" + key;
    }

    @Override
    public Long version() {
        return version;
    }

    @Override
    public ObjectType type() {
        return ObjectType.ATTRIBUTE_KV;
    }

    public record Key(UUID entityId, AttributeScope scope, int key) implements EdqsObjectKey {}

}
