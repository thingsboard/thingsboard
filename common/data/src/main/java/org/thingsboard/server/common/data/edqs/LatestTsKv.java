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
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LatestTsKv implements EdqsObject {

    private EntityId entityId;
    private String key;
    private Long version;

    private DataPoint dataPoint; // optional (on deletion)

    private Long ts; // only for serialization
    private KvEntry value; // only for serialization

    public LatestTsKv(EntityId entityId, TsKvEntry tsKvEntry, Long version) {
        this.entityId = entityId;
        this.key = tsKvEntry.getKey();
        this.ts = tsKvEntry.getTs();
        this.version = version != null ? version : 0L;
        this.value = tsKvEntry;
    }

    public LatestTsKv(EntityId entityId, String key, Long version) {
        this.entityId = entityId;
        this.key = key;
        this.version = version != null ? version : 0L;
    }

    @Override
    public String stringKey() {
        return "l_" + entityId + "_" + key;
    }

    @Override
    public Long version() {
        return version;
    }

    @Override
    public ObjectType type() {
        return ObjectType.LATEST_TS_KV;
    }

    public record Key(UUID entityId, int key) implements EdqsObjectKey {}

}
