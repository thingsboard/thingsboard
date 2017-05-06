package org.thingsboard.server.dao.sql;

import org.thingsboard.server.dao.model.BaseEntity;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public abstract class JpaAbstractSearchTextDao <E extends BaseEntity<D>, D> extends JpaAbstractDao<E, D> {

    @Override
    protected boolean isSearchTextDao() {
        return true;
    }
}
