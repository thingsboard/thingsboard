// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql;

import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.util.SqlDao;

@SqlDao
public abstract class JpaPartitionedAbstractDao<E extends BaseEntity<D>, D> extends JpaAbstractDao<E, D> {

    @Override
    protected E doSave(E entity, boolean isNew, boolean flush) {
        createPartition(entity);
        return super.doSave(entity, isNew, flush);
    }

    public abstract void createPartition(E entity);

}
