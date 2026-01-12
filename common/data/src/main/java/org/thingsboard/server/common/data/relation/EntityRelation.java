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
package org.thingsboard.server.common.data.relation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.EdqsObject;
import org.thingsboard.server.common.data.edqs.EdqsObjectKey;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.validation.Length;

import java.io.Serializable;
import java.util.UUID;

@Slf4j
@Schema
@EqualsAndHashCode(exclude = "additionalInfoBytes")
@ToString(exclude = {"additionalInfoBytes"})
public class EntityRelation implements HasVersion, Serializable, EdqsObject {

    private static final long serialVersionUID = 2807343040519543363L;

    public static final String EDGE_TYPE = "ManagedByEdge";
    public static final String CONTAINS_TYPE = "Contains";
    public static final String MANAGES_TYPE = "Manages";
    public static final String USES_TYPE = "Uses";

    @Setter
    private EntityId from;
    @Setter
    private EntityId to;
    @Setter
    @Length(fieldName = "type")
    private String type;
    @Setter
    private RelationTypeGroup typeGroup;
    @Getter
    @Setter
    private Long version;
    private transient JsonNode additionalInfo;
    @JsonIgnore
    private byte[] additionalInfoBytes;

    public EntityRelation() {
        super();
    }

    public EntityRelation(EntityId from, EntityId to, String type) {
        this(from, to, type, RelationTypeGroup.COMMON);
    }

    public EntityRelation(EntityId from, EntityId to, String type, RelationTypeGroup typeGroup) {
        this(from, to, type, typeGroup, null);
    }

    public EntityRelation(EntityId from, EntityId to, String type, RelationTypeGroup typeGroup, JsonNode additionalInfo) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.typeGroup = typeGroup;
        this.additionalInfo = additionalInfo;
    }

    public EntityRelation(EntityRelation entityRelation) {
        this.from = entityRelation.getFrom();
        this.to = entityRelation.getTo();
        this.type = entityRelation.getType();
        this.typeGroup = entityRelation.getTypeGroup();
        this.additionalInfo = entityRelation.getAdditionalInfo();
        this.version = entityRelation.getVersion();
    }

    @Schema(description = "JSON object with [from] Entity Id.", accessMode = Schema.AccessMode.READ_ONLY)
    public EntityId getFrom() {
        return from;
    }

    @Schema(description = "JSON object with [to] Entity Id.", accessMode = Schema.AccessMode.READ_ONLY)
    public EntityId getTo() {
        return to;
    }

    @Schema(description = "String value of relation type.", example = "Contains")
    public String getType() {
        return type;
    }

    @Schema(description = "Represents the type group of the relation.", example = "COMMON")
    public RelationTypeGroup getTypeGroup() {
        return typeGroup;
    }

    @Schema(description = "Additional parameters of the relation", implementation = com.fasterxml.jackson.databind.JsonNode.class)
    public JsonNode getAdditionalInfo() {
        return BaseDataWithAdditionalInfo.getJson(() -> additionalInfo, () -> additionalInfoBytes);
    }

    public void setAdditionalInfo(JsonNode addInfo) {
        BaseDataWithAdditionalInfo.setJson(addInfo, json -> this.additionalInfo = json, bytes -> this.additionalInfoBytes = bytes);
    }

    @Override
    public String stringKey() {
        return "r_" + from + "_" + to + "_" + typeGroup + "_" + type;
    }

    @Override
    public Long version() {
        return version;
    }

    @Override
    public ObjectType type() {
        return ObjectType.RELATION;
    }

    public record Key(UUID from, UUID to, RelationTypeGroup typeGroup, String type) implements EdqsObjectKey {}

}
