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

import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.permission.MergedGroupTypePermissionInfo;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.DataKey;
import org.thingsboard.server.edqs.query.EdqsDataQuery;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.query.SortableEntityData;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.thingsboard.server.edqs.util.RepositoryUtils.checkFilters;
import static org.thingsboard.server.edqs.util.RepositoryUtils.getSortValue;

public abstract class AbstractQueryProcessor<T extends EntityFilter> implements EntityQueryProcessor {

    protected final TenantRepo repository;
    protected final QueryContext ctx;
    protected final EdqsQuery query;
    protected final DataKey sortKey;
    protected final T filter;

    public AbstractQueryProcessor(TenantRepo repository, QueryContext ctx, EdqsQuery query, T filter) {
        this.repository = repository;
        this.ctx = ctx;
        this.query = query;
        this.sortKey = query instanceof EdqsDataQuery dataQuery ? dataQuery.getSortKey() : null;
        this.filter = filter;
    }

    protected CombinedPermissions getCombinedPermissions(UUID id, boolean genericRead, boolean genericAttrs, boolean genericTs, List<GroupPermissions> groupPermissions) {
        return getCombinedPermissionsInternal(id, genericRead, genericRead && genericAttrs, genericRead && genericTs, groupPermissions);
    }

    protected CombinedPermissions getCombinedPermissions(UUID id, List<GroupPermissions> groupPermissions) {
        return getCombinedPermissionsInternal(id, false, false, false, groupPermissions);
    }

    protected CombinedPermissions getCombinedPermissionsInternal(UUID id, boolean read, boolean readAttrs, boolean readTs, List<GroupPermissions> groupPermissions) {
        for (GroupPermissions eg : groupPermissions) {
            if (read && readAttrs && readTs) {
                break;
            }
            boolean hasMorePermissions = !read || (!readAttrs && eg.readAttrs) || (!readTs && eg.readTs);
            if (hasMorePermissions && repository.contains(eg.groupId, id)) {
                read = true;
                readAttrs = readAttrs || eg.readAttrs;
                readTs = readTs || eg.readTs;
            }
        }
        return new CombinedPermissions(read, readAttrs, readTs);
    }

    protected SortableEntityData toSortDataGroupsOnly(EntityData<?> ed, List<GroupPermissions> groupPermissions) {
        SortableEntityData sortData;
        CombinedPermissions permissions = getCombinedPermissions(ed.getId(), groupPermissions);
        if (permissions.isRead()) {
            sortData = toSortData(ed, permissions);
        } else {
            sortData = null;
        }
        return sortData;
    }

    protected SortableEntityData toSortData(EntityData<?> ed, boolean readAttrs, boolean readTs) {
        SortableEntityData sortData = new SortableEntityData(ed);
        sortData.setSortValue(getSortValue(ed, sortKey));
        sortData.setReadAttrs(readAttrs);
        sortData.setReadTs(readTs);
        return sortData;
    }

    protected SortableEntityData toSortData(EntityData<?> ed, Permissions permissions) {
        return toSortData(ed, permissions.isReadAttrs(), permissions.isReadTs());
    }

    protected static List<GroupPermissions> toGroupPermissions(MergedGroupTypePermissionInfo readPermissions,
                                                               MergedGroupTypePermissionInfo readAttrPermissions,
                                                               MergedGroupTypePermissionInfo readTsPermissions) {
        List<GroupPermissions> permissions = new ArrayList<>();
        for (EntityGroupId egId : readPermissions.getEntityGroupIds()) {
            permissions.add(new GroupPermissions(egId.getId(),
                    readAttrPermissions.getEntityGroupIds() != null && readAttrPermissions.getEntityGroupIds().contains(egId),
                    readTsPermissions.getEntityGroupIds() != null && readTsPermissions.getEntityGroupIds().contains(egId)));
        }
        return permissions;
    }

    protected static boolean checkCustomerHierarchy(Set<UUID> customers, EntityData<?> ed) {
        return ed.getCustomerId() != null && customers.contains(ed.getCustomerId());
    }

    protected void process(Collection<EntityData<?>> entities, Consumer<EntityData<?>> processor) {
        for (EntityData<?> ed : entities) {
            if (matches(ed)) {
                processor.accept(ed);
            }
        }
    }

    protected boolean matches(EntityData<?> ed) {
        return checkFilters(query, ed);
    }

}
