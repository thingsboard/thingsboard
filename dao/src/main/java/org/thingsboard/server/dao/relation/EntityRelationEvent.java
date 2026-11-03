// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.relation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;

@RequiredArgsConstructor
@ToString
public class EntityRelationEvent {
    @Getter
    private final EntityId from;
    @Getter
    private final EntityId to;
    @Getter
    private final String type;
    @Getter
    private final RelationTypeGroup typeGroup;

    public static EntityRelationEvent from(EntityRelation relation) {
        return new EntityRelationEvent(relation.getFrom(), relation.getTo(), relation.getType(), relation.getTypeGroup());
    }
}
