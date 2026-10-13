// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
    private final boolean readAttrs;
    private final boolean readTs;
    private final Map<EntityKeyType, Map<String, TsValue>> latest;

    public EntityData toOldEntityData() {
        return new EntityData(entityId, readAttrs, readTs, latest, Collections.emptyMap(), Collections.emptyMap());
    }

}
