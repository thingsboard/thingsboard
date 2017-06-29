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
package org.thingsboard.server.dao.nosql;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.Result;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.wrapper.EntityResultSet;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

@Slf4j
public abstract class CassandraAbstractModelDao<E extends BaseEntity<D>, D> extends CassandraAbstractDao implements Dao<D> {

    protected abstract Class<E> getColumnFamilyClass();

    protected abstract String getColumnFamilyName();

    protected E updateSearchTextIfPresent(E entity) {
        return entity;
    }

    protected Mapper<E> getMapper() {
        return cluster.getMapper(getColumnFamilyClass());
    }

    protected List<E> findListByStatement(Statement statement) {
        List<E> list = Collections.emptyList();
        if (statement != null) {
            statement.setConsistencyLevel(cluster.getDefaultReadConsistencyLevel());
            ResultSet resultSet = getSession().execute(statement);
            Result<E> result = getMapper().map(resultSet);
            if (result != null) {
                list = result.all();
            }
        }
        return list;
    }

    protected ListenableFuture<List<D>> findListByStatementAsync(Statement statement) {
        if (statement != null) {
            statement.setConsistencyLevel(cluster.getDefaultReadConsistencyLevel());
            ResultSetFuture resultSetFuture = getSession().executeAsync(statement);
            return Futures.transform(resultSetFuture, new Function<ResultSet, List<D>>() {
                @Nullable
                @Override
                public List<D> apply(@Nullable ResultSet resultSet) {
                    Result<E> result = getMapper().map(resultSet);
                    if (result != null) {
                        List<E> entities = result.all();
                        return DaoUtil.convertDataList(entities);
                    } else {
                        return Collections.emptyList();
                    }
                }
            });
        }
        return Futures.immediateFuture(Collections.emptyList());
    }

    protected E findOneByStatement(Statement statement) {
        E object = null;
        if (statement != null) {
            statement.setConsistencyLevel(cluster.getDefaultReadConsistencyLevel());
            ResultSet resultSet = getSession().execute(statement);
            Result<E> result = getMapper().map(resultSet);
            if (result != null) {
                object = result.one();
            }
        }
        return object;
    }

    protected ListenableFuture<D> findOneByStatementAsync(Statement statement) {
        if (statement != null) {
            statement.setConsistencyLevel(cluster.getDefaultReadConsistencyLevel());
            ResultSetFuture resultSetFuture = getSession().executeAsync(statement);
            return Futures.transform(resultSetFuture, new Function<ResultSet, D>() {
                @Nullable
                @Override
                public D apply(@Nullable ResultSet resultSet) {
                    Result<E> result = getMapper().map(resultSet);
                    if (result != null) {
                        E entity = result.one();
                        return DaoUtil.getData(entity);
                    } else {
                        return null;
                    }
                }
            });
        }
        return Futures.immediateFuture(null);
    }

    protected Statement getSaveQuery(E dto) {
        return getMapper().saveQuery(dto);
    }

    protected EntityResultSet<E> saveWithResult(E entity) {
        log.debug("Save entity {}", entity);
        if (entity.getId() == null) {
            entity.setId(UUIDs.timeBased());
        } else if (isDeleteOnSave()) {
            removeById(entity.getId());
        }
        Statement saveStatement = getSaveQuery(entity);
        saveStatement.setConsistencyLevel(cluster.getDefaultWriteConsistencyLevel());
        ResultSet resultSet = executeWrite(saveStatement);
        return new EntityResultSet<>(resultSet, entity);
    }

    protected boolean isDeleteOnSave() {
        return true;
    }

    @Override
    public D save(D domain) {
        E entity;
        try {
            entity = getColumnFamilyClass().getConstructor(domain.getClass()).newInstance(domain);
        } catch (Exception e) {
            log.error("Can't create entity for domain object {}", domain, e);
            throw new IllegalArgumentException("Can't create entity for domain object {" + domain + "}", e);
        }
        entity = updateSearchTextIfPresent(entity);
        log.debug("Saving entity {}", entity);
        entity = saveWithResult(entity).getEntity();
        return DaoUtil.getData(entity);
    }

    @Override
    public D findById(UUID key) {
        log.debug("Get entity by key {}", key);
        Select.Where query = select().from(getColumnFamilyName()).where(eq(ModelConstants.ID_PROPERTY, key));
        log.trace("Execute query {}", query);
        E entity = findOneByStatement(query);
        return DaoUtil.getData(entity);
    }

    @Override
    public ListenableFuture<D> findByIdAsync(UUID key) {
        log.debug("Get entity by key {}", key);
        Select.Where query = select().from(getColumnFamilyName()).where(eq(ModelConstants.ID_PROPERTY, key));
        log.trace("Execute query {}", query);
        return findOneByStatementAsync(query);
    }

    @Override
    public boolean removeById(UUID key) {
        Statement delete = QueryBuilder.delete().all().from(getColumnFamilyName()).where(eq(ModelConstants.ID_PROPERTY, key));
        log.debug("Remove request: {}", delete.toString());
        return getSession().execute(delete).wasApplied();
    }

    @Override
    public List<D> find() {
        log.debug("Get all entities from column family {}", getColumnFamilyName());
        List<E> entities = findListByStatement(QueryBuilder.select().all().from(getColumnFamilyName()).setConsistencyLevel(cluster.getDefaultReadConsistencyLevel()));
        return DaoUtil.convertDataList(entities);
    }

    protected static <T> Function<BaseEntity<T>, T> toDataFunction() {
        return new Function<BaseEntity<T>, T>() {
            @Nullable
            @Override
            public T apply(@Nullable BaseEntity<T> entity) {
                return entity != null ? entity.toData() : null;
            }
        };
    }
}
