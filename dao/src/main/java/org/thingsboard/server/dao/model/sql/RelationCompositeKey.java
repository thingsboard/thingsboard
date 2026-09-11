// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.relation.EntityRelation;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RelationCompositeKey implements Serializable {

    @Transient
    private static final long serialVersionUID = -4089175869616037592L;

    private UUID fromId;
    private String fromType;
    private UUID toId;
    private String toType;
    private String relationType;
    private String relationTypeGroup;

    public RelationCompositeKey(EntityRelation relation) {
        this.fromId = relation.getFrom().getId();
        this.fromType = relation.getFrom().getEntityType().name();
        this.toId = relation.getTo().getId();
        this.toType = relation.getTo().getEntityType().name();
        this.relationType = relation.getType();
        this.relationTypeGroup = relation.getTypeGroup().name();
    }
}
