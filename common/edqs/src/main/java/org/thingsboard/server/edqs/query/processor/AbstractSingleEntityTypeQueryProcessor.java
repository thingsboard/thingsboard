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
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.query.SortableEntityData;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public abstract class AbstractSingleEntityTypeQueryProcessor<T extends EntityFilter> extends AbstractQueryProcessor<T> {

    public AbstractSingleEntityTypeQueryProcessor(TenantRepo repository, QueryContext ctx, EdqsQuery query, T filter) {
        super(repository, ctx, query, filter);
    }

    @Override
    public List<SortableEntityData> processQuery() {
        var readPermissions = ctx.getMergedReadPermissionsByEntityType();
        if (readPermissions == null) {
            return Collections.emptyList();
        }
        var readAttrPermissions = ctx.getMergedReadAttrPermissionsByEntityType();
        var readTsPermissions = ctx.getMergedReadTsPermissionsByEntityType();

        boolean hasGenericRead = readPermissions.isHasGenericRead();
        boolean hasGroups = readPermissions.getEntityGroupIds() != null && !readPermissions.getEntityGroupIds().isEmpty();
        if (!hasGenericRead && !hasGroups) {
            return Collections.emptyList();
        }
        boolean hasGenericAttrRead = readAttrPermissions.isHasGenericRead();
        boolean hasGenericTsRead = readTsPermissions.isHasGenericRead();

        if (hasGenericRead) {
            if (ctx.isTenantUser()) {
                if (hasGroups && (!hasGenericAttrRead || !hasGenericTsRead)) {
                    return processTenantGenericReadWithGroups(hasGenericAttrRead, hasGenericTsRead,
                            toGroupPermissions(readPermissions, readAttrPermissions, readTsPermissions));
                } else {
                    return processTenantGenericRead(hasGenericAttrRead, hasGenericTsRead);
                }
            } else {
                if (hasGroups) {
                    return processCustomerGenericReadWithGroups(ctx.getCustomerId().getId(), hasGenericAttrRead, hasGenericTsRead,
                            toGroupPermissions(readPermissions, readAttrPermissions, readTsPermissions));
                } else {
                    return processCustomerGenericRead(ctx.getCustomerId().getId(), hasGenericAttrRead, hasGenericTsRead);
                }
            }
        } else {
            return processGroupsOnly(toGroupPermissions(readPermissions, readAttrPermissions, readTsPermissions));
        }
    }

    @Override
    public long count() { // TODO: get rid of the duplicates
        var readPermissions = ctx.getMergedReadPermissionsByEntityType();
        if (readPermissions == null) {
            return 0;
        }
        var readAttrPermissions = ctx.getMergedReadAttrPermissionsByEntityType();
        var readTsPermissions = ctx.getMergedReadTsPermissionsByEntityType();
        boolean hasGenericRead = readPermissions.isHasGenericRead();
        boolean hasGroups = readPermissions.getEntityGroupIds() != null && !readPermissions.getEntityGroupIds().isEmpty();

        if (!hasGenericRead && !hasGroups && !ctx.isIgnorePermissionCheck()) {
            return 0;
        }

        AtomicLong result = new AtomicLong();
        Consumer<EntityData<?>> counter = ed -> result.incrementAndGet();

        if (ctx.isIgnorePermissionCheck()) {
            processAll(counter);
        } else if (ctx.isTenantUser()) {
            if (hasGenericRead) {
                processAll(counter);
            } else {
                processGroupsOnly(toGroupPermissions(readPermissions, readAttrPermissions, readTsPermissions), counter);
            }
        } else {
            if (hasGenericRead) {
                if (hasGroups) {
                    result.addAndGet(processCustomerGenericReadWithGroups(ctx.getCustomerId().getId(), readAttrPermissions.isHasGenericRead(), readTsPermissions.isHasGenericRead(),
                            toGroupPermissions(readPermissions, readAttrPermissions, readTsPermissions)).size()); // FIXME: not efficient
                } else {
                    processCustomerGenericRead(ctx.getCustomerId().getId(), counter);
                }
            } else {
                processGroupsOnly(toGroupPermissions(readPermissions, readAttrPermissions, readTsPermissions), counter);
            }
        }
        return result.get();
    }

    protected List<SortableEntityData> processTenantGenericRead(boolean readAttrPermissions,
                                                                boolean readTsPermissions) {
        List<SortableEntityData> result = new ArrayList<>(getProbableResultSize());
        processAll(ed -> {
            result.add(toSortData(ed, readAttrPermissions, readTsPermissions));
        });
        return result;
    }

    protected List<SortableEntityData> processCustomerGenericRead(UUID customerId,
                                                                  boolean readAttrPermissions,
                                                                  boolean readTsPermissions) {
        List<SortableEntityData> result = new ArrayList<>(getProbableResultSize());
        processCustomerGenericRead(customerId, ed -> {
            result.add(toSortData(ed, readAttrPermissions, readTsPermissions));
        });
        return result;
    }

    protected abstract void processCustomerGenericRead(UUID customerId, Consumer<EntityData<?>> processor);

    protected List<SortableEntityData> processTenantGenericReadWithGroups(boolean readAttrPermissions,
                                                                          boolean readTsPermissions,
                                                                          List<GroupPermissions> groupPermissions) {
        List<SortableEntityData> result = new ArrayList<>(getProbableResultSize());
        processAll(ed -> {
            CombinedPermissions permissions = getCombinedPermissions(ed.getId(), true, readAttrPermissions, readTsPermissions, groupPermissions);
            SortableEntityData sortData = toSortData(ed, permissions);
            result.add(sortData);
        });
        return result;
    }

    protected abstract List<SortableEntityData> processCustomerGenericReadWithGroups(UUID customerId,
                                                                                     boolean readAttrPermissions,
                                                                                     boolean readTsPermissions,
                                                                                     List<GroupPermissions> groupPermissions);

    protected List<SortableEntityData> processGroupsOnly(List<GroupPermissions> groupPermissions) {
        List<SortableEntityData> result = new ArrayList<>(getProbableResultSize());
        processGroupsOnly(groupPermissions, ed -> {
            SortableEntityData sortData = toSortDataGroupsOnly(ed, groupPermissions);
            if (sortData != null) {
                result.add(sortData);
            }
        });
        return result;
    }

    protected abstract void processGroupsOnly(List<GroupPermissions> groupPermissions, Consumer<EntityData<?>> processor);

    protected abstract void processAll(Consumer<EntityData<?>> processor);

    protected abstract int getProbableResultSize();

}
