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
package org.thingsboard.server.dao.relation;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode
@Getter
@RequiredArgsConstructor
@Builder
public class RelationCacheKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 3911151843961657570L;

    private final EntityId from;
    private final EntityId to;
    private final String type;
    private final RelationTypeGroup typeGroup;
    private final EntitySearchDirection direction;

    public RelationCacheKey(EntityId from, EntityId to, String type, RelationTypeGroup typeGroup) {
        this(from, to, type, typeGroup, null);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = add(sb, true, from);
        first = add(sb, first, to);
        first = add(sb, first, type);
        first = add(sb, first, typeGroup);
        add(sb, first, direction);
        return sb.toString();
    }

    private boolean add(StringBuilder sb, boolean first, Object param) {
        if (param != null) {
            if (!first) {
                sb.append("_");
            }
            first = false;
            sb.append(param);
        }
        return first;
    }

}
