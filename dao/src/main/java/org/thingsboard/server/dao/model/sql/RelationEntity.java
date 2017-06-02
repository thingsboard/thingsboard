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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.*;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Data
@Entity
@Table(name = RELATION_COLUMN_FAMILY_NAME)
@IdClass(RelationCompositeKey.class)
public final class RelationEntity implements ToData<EntityRelation> {

    @Transient
    private static final long serialVersionUID = -4089175869616037592L;

    @Id
    @Column(name = RELATION_FROM_ID_PROPERTY)
    private UUID fromId;

    @Id
    @Column(name = RELATION_FROM_TYPE_PROPERTY)
    private String fromType;

    @Id
    @Column(name = RELATION_TO_ID_PROPERTY)
    private UUID toId;

    @Id
    @Column(name = RELATION_TO_TYPE_PROPERTY)
    private String toType;

    @Id
    @Column(name = RELATION_TYPE_GROUP_PROPERTY)
    private String relationTypeGroup;

    @Id
    @Column(name = RELATION_TYPE_PROPERTY)
    private String relationType;

    @Type(type = "jsonb")
    @Column(name = ADDITIONAL_INFO_PROPERTY, columnDefinition = "jsonb")
    private JsonNode additionalInfo;

    public RelationEntity() {
        super();
    }

    public RelationEntity(EntityRelation relation) {
        if (relation.getTo() != null) {
            this.toId = relation.getTo().getId();
            this.toType = relation.getTo().getEntityType().name();
        }
        if (relation.getFrom() != null) {
            this.fromId = relation.getFrom().getId();
            this.fromType = relation.getFrom().getEntityType().name();
        }
        this.relationType = relation.getType();
        this.relationTypeGroup = relation.getTypeGroup().name();
        this.additionalInfo = relation.getAdditionalInfo();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((additionalInfo == null) ? 0 : additionalInfo.hashCode());
        result = prime * result + ((toId == null) ? 0 : toId.hashCode());
        result = prime * result + ((toType == null) ? 0 : toType.hashCode());
        result = prime * result + ((fromId == null) ? 0 : fromId.hashCode());
        result = prime * result + ((fromType == null) ? 0 : fromType.hashCode());
        result = prime * result + ((relationType == null) ? 0 : relationType.hashCode());
        result = prime * result + ((relationTypeGroup == null) ? 0 : relationTypeGroup.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RelationEntity other = (RelationEntity) obj;
        if (additionalInfo == null) {
            if (other.additionalInfo != null)
                return false;
        } else if (!additionalInfo.equals(other.additionalInfo))
            return false;
        if (toId == null) {
            if (other.toId != null)
                return false;
        } else if (!toId.equals(other.toId))
            return false;
        if (fromId == null) {
            if (other.fromId != null)
                return false;
        } else if (!fromId.equals(other.fromId))
            return false;
        if (toType == null) {
            if (other.toType != null)
                return false;
        } else if (!toType.equals(other.toType))
            return false;
        if (fromType == null) {
            if (other.fromType != null)
                return false;
        } else if (!fromType.equals(other.fromType))
            return false;
        if (relationType == null) {
            if (other.relationType != null)
                return false;
        } else if (!relationType.equals(other.relationType))
            return false;
        if (relationTypeGroup == null) {
            if (other.relationTypeGroup != null)
                return false;
        } else if (!relationTypeGroup.equals(other.relationTypeGroup))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AssetEntity [toId=");
        builder.append(toId);
        builder.append(", toType=");
        builder.append(toType);
        builder.append(", fromId=");
        builder.append(fromId);
        builder.append(", fromType=");
        builder.append(fromType);
        builder.append(", relationType=");
        builder.append(relationType);
        builder.append(", relationTypeGroup=");
        builder.append(relationTypeGroup);
        builder.append(", additionalInfo=");
        builder.append(additionalInfo);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public EntityRelation toData() {
        EntityRelation relation = new EntityRelation();
        if (toId != null && toType != null) {
            relation.setTo(EntityIdFactory.getByTypeAndUuid(toType, toId));
        }
        if (fromId != null && fromType != null) {
            relation.setFrom(EntityIdFactory.getByTypeAndUuid(fromType, fromId));
        }
        relation.setType(relationType);
        relation.setTypeGroup(RelationTypeGroup.valueOf(relationTypeGroup));
        relation.setAdditionalInfo(additionalInfo);
        return relation;
    }

}