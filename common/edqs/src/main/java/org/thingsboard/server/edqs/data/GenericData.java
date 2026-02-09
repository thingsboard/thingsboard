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
package org.thingsboard.server.edqs.data;

import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.EntityFields;

import java.util.UUID;

@ToString(callSuper = true)
public class GenericData extends BaseEntityData<EntityFields> {

    private final EntityType entityType;

    public GenericData(EntityType entityType, UUID entityId) {
        super(entityId);
        this.entityType = entityType;
    }

    @Override
    public EntityType getEntityType() {
        return entityType;
    }
}
