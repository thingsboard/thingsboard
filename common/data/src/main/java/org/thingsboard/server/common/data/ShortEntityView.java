// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.group.EntityField;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;

import java.util.HashMap;
import java.util.Map;

@Schema
@NoArgsConstructor
public class ShortEntityView implements HasId<EntityId>, HasName {

    @Schema(description = "Entity Id object", requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY)
    private EntityId id;
    @Schema(description = "Map of entity fields that is configurable in the Entity Group", requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY)
    private Map<String, String> properties = new HashMap<>();

    @JsonIgnore
    private boolean skipEntity = false;

    public ShortEntityView(EntityId id) {
        super();
        this.id = id;
    }

    @Override
    public EntityId getId() {
        return id;
    }

    @JsonAnyGetter
    public Map<String, String> properties() {
        return this.properties;
    }

    @JsonAnySetter
    public void put(String name, String value) {
        this.properties.put(name, value);
    }

    @Schema(description = "Name of the entity", requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return this.properties.get(EntityField.NAME.name().toLowerCase());
    }

    public boolean isSkipEntity() {
        return skipEntity;
    }

    public void setSkipEntity(boolean skipEntity) {
        this.skipEntity = skipEntity;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ShortEntityView that = (ShortEntityView) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }
}
