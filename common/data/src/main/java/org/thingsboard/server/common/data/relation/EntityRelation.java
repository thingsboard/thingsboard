/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Objects;

public class EntityRelation {

    private static final long serialVersionUID = 2807343040519543363L;

    public static final String CONTAINS_TYPE = "Contains";
    public static final String MANAGES_TYPE = "Manages";

    private EntityId from;
    private EntityId to;
    private String type;
    private JsonNode additionalInfo;

    public EntityRelation() {
        super();
    }

    public EntityRelation(EntityId from, EntityId to, String type) {
        this(from, to, type, null);
    }

    public EntityRelation(EntityId from, EntityId to, String type, JsonNode additionalInfo) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.additionalInfo = additionalInfo;
    }

    public EntityRelation(EntityRelation device) {
        this.from = device.getFrom();
        this.to = device.getTo();
        this.type = device.getType();
        this.additionalInfo = device.getAdditionalInfo();
    }

    public EntityId getFrom() {
        return from;
    }

    public void setFrom(EntityId from) {
        this.from = from;
    }

    public EntityId getTo() {
        return to;
    }

    public void setTo(EntityId to) {
        this.to = to;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public JsonNode getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityRelation relation = (EntityRelation) o;
        return Objects.equals(from, relation.from) &&
                Objects.equals(to, relation.to) &&
                Objects.equals(type, relation.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, type);
    }
}
