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
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.io.Serializable;
import java.util.UUID;

@JsonDeserialize(using = EntityIdDeserializer.class)
@JsonSerialize(using = EntityIdSerializer.class)
@Schema
public interface EntityId extends HasUUID, Serializable { //NOSONAR, the constant is closely related to EntityId

    UUID NULL_UUID = UUID.fromString("13814000-1dd2-11b2-8080-808080808080");

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "ID of the entity, time-based UUID v1", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    UUID getId();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "DEVICE")
    EntityType getEntityType();

    @JsonIgnore
    default boolean isNullUid() {
        return NULL_UUID.equals(getId());
    }

}
