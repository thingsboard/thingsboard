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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.fields.EntityFields;
import org.thingsboard.server.common.data.edqs.fields.EntityIdFields;

import java.util.UUID;

@Data
@NoArgsConstructor
public class Entity implements EdqsObject {

    private EntityType type;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private EntityFields fields;

    public Entity(EntityType type) {
        this.type = type;
    }

    public Entity(EntityType type, EntityFields fields) {
        this.type = type;
        this.fields = fields;
    }

    public Entity(EntityType entityType, UUID id, long version) {
        this.type = entityType;
        this.fields = new EntityIdFields(id, version);
    }

    @Override
    public String stringKey() {
        return "e_" + fields.getId().toString();
    }

    @Override
    public Long version() {
        return fields.getVersion();
    }

    @Override
    public ObjectType type() {
        return ObjectType.fromEntityType(type);
    }

    public record Key(UUID id) implements EdqsObjectKey {}

}
