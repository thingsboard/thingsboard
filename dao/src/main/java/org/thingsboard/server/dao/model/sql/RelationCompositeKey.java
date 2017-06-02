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

import lombok.AllArgsConstructor;
import org.thingsboard.server.common.data.relation.EntityRelation;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
public class RelationCompositeKey implements Serializable {

    private UUID fromId;
    private String fromType;
    private UUID toId;
    private String toType;
    private String relationTypeGroup;
    private String relationType;

    public RelationCompositeKey(EntityRelation relation) {
        this.fromId = relation.getFrom().getId();
        this.fromType = relation.getFrom().getEntityType().name();
        this.toId = relation.getTo().getId();
        this.toType = relation.getTo().getEntityType().name();
        this.relationType = relation.getType();
        this.relationTypeGroup = relation.getTypeGroup().name();
    }
}
