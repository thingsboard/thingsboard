// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edqs.query;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;

import java.util.Collections;
import java.util.Map;

@Data
@RequiredArgsConstructor
public class QueryResult {

    private final EntityId entityId;
    private final Map<EntityKeyType, Map<String, TsValue>> latest;

    public EntityData toOldEntityData() {
        return new EntityData(entityId, latest, Collections.emptyMap(), Collections.emptyMap());
    }

}
