/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.common.data.kv;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BaseEntityAttributeKvEntry extends BaseAttributeKvEntry implements EntityAttributeKvEntry {

    private final EntityType entityType;
    private final String entityId;

    public BaseEntityAttributeKvEntry(EntityType entityType, String entityId, long lastUpdateTs, KvEntry kv) {
        super(kv, lastUpdateTs);
        this.entityType = entityType;
        this.entityId = entityId;
    }

    @Override
    public EntityType getEntityType() {
        return entityType;
    }

    @Override
    public String getEntityId() {
        return entityId;
    }
}
