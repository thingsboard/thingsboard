// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
