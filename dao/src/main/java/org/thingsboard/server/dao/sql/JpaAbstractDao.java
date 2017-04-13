/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.sql.user.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
public abstract class JpaAbstractDao<E extends BaseEntity<D>, D> implements Dao<D> {

    protected abstract Class<E> getColumnFamilyClass();

    protected abstract String getColumnFamilyName();

    protected abstract JpaRepository<E, UUID> getCrudRepository();

    protected boolean isSearchTextDao() {
        return false;
    }

    @Override
    public D save(D domain) {
        E entity;
        try {
            entity = getColumnFamilyClass().getConstructor(domain.getClass()).newInstance(domain);
        } catch (Exception e) {
            log.error("Can't create entity for domain object {}", domain, e);
            throw new IllegalArgumentException("Can't create entity for domain object {" + domain + "}", e);
        } if (isSearchTextDao()) {
            ((SearchTextEntity) entity).setSearchText(((SearchTextEntity) entity).getSearchTextSource().toLowerCase());
        }
        log.debug("Saving entity {}", entity);
        entity = getCrudRepository().save(entity);
        return DaoUtil.getData(entity);
    }

    @Override
    public D findById(UUID key) {
        log.debug("Get entity by key {}", key);
        E entity = getCrudRepository().findOne(key);
        return DaoUtil.getData(entity);
    }

    @Override
    public ListenableFuture<D> findByIdAsync(UUID key) {
        log.debug("Get entity by key {}", key);
        org.springframework.util.concurrent.ListenableFuture<E> entityFuture = getCrudRepository().findByIdAsync(key);
        // TODO: vsosliuk implement
        return null;
    }

    @Override
    public boolean removeById(UUID key) {
        getCrudRepository().delete(key);
        log.debug("Remove request: {}", key);
        return getCrudRepository().equals(key);
    }

    @Override
    public List<D> find() {
        log.debug("Get all entities from column family {}", getColumnFamilyName());
        List<E> entities = Lists.newArrayList(getCrudRepository().findAll());
        return DaoUtil.convertDataList(entities);
    }
}
