// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.update;

import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.UUIDBased;

public abstract class GroupAllPaginatedUpdater<I, D extends BaseDataWithAdditionalInfo<? extends UUIDBased>> extends PaginatedUpdater<I,D> {

    protected final EntityGroup groupAll;

    public GroupAllPaginatedUpdater(EntityGroup groupAll) {
        this.groupAll = groupAll;
    }

    @Override
    protected void updateEntity(D entity) {
        updateGroupEntity(entity, groupAll);
    }

    protected abstract void updateGroupEntity(D entity, EntityGroup groupAll);

}
