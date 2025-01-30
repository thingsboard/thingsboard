/**
 * Copyright © 2016-2024 ThingsBoard, Inc.
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
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.edqs.data.EntityGroupData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.query.SortableEntityData;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractEntityGroupQueryProcessor<T extends EntityFilter> extends AbstractSingleEntityTypeQueryProcessor<T> {

    public AbstractEntityGroupQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query, T filter) {
        super(repo, ctx, query, filter);
    }

    @Override
    protected List<SortableEntityData> processCustomerGenericReadWithGroups(UUID customerId, boolean readAttrPermissions, boolean readTsPermissions, List<GroupPermissions> groupPermissions) {
        var genericReadResults = processCustomerGenericRead(customerId, readAttrPermissions, readTsPermissions);
        Map<UUID, SortableEntityData> mergedResult = new HashMap<>(genericReadResults.size());
        for (SortableEntityData sd : genericReadResults) {
            mergedResult.put(sd.getId(), sd);
        }
        for (GroupPermissions permissions : groupPermissions) {
            SortableEntityData alreadyAdded = mergedResult.get(permissions.groupId);
            if (alreadyAdded != null) {
                alreadyAdded.setReadAttrs(alreadyAdded.isReadAttrs() || permissions.readAttrs);
                alreadyAdded.setReadTs(alreadyAdded.isReadTs() || permissions.readTs);
            } else {
                EntityGroupData egData = repository.getEntityGroup(permissions.groupId);
                if (matches(egData)) {
                    SortableEntityData sortData = toSortData(egData, permissions);
                    mergedResult.put(egData.getId(), sortData);
                }
            }
        }
        return new ArrayList<>(mergedResult.values());
    }

    @Override
    protected CombinedPermissions getCombinedPermissionsInternal(UUID id, boolean read, boolean readAttrs, boolean readTs, List<GroupPermissions> groupPermissions) {
        for (GroupPermissions eg : groupPermissions) {
            if (read && readAttrs && readTs) {
                break;
            }
            if (eg.groupId.equals(id)) {
                read = true;
                readAttrs = readAttrs || eg.readAttrs;
                readTs = readTs || eg.readTs;
            }
        }
        return new CombinedPermissions(read, readAttrs, readTs);
    }

}
