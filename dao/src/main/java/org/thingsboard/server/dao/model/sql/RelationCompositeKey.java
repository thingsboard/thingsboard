/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.relation.EntityRelation;

import javax.persistence.Transient;
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RelationCompositeKey implements Serializable {

    @Transient
    private static final long serialVersionUID = -4089175869616037592L;

    private String fromId;
    private String fromType;
    private String toId;
    private String toType;
    private String relationType;
    private String relationTypeGroup;

    public RelationCompositeKey(EntityRelation relation) {
        this.fromId = UUIDConverter.fromTimeUUID(relation.getFrom().getId());
        this.fromType = relation.getFrom().getEntityType().name();
        this.toId = UUIDConverter.fromTimeUUID(relation.getTo().getId());
        this.toType = relation.getTo().getEntityType().name();
        this.relationType = relation.getType();
        this.relationTypeGroup = relation.getTypeGroup().name();
    }
}
