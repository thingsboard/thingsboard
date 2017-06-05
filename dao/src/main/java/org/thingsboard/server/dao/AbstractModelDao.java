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
package org.thingsboard.server.dao;

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
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.wrapper.EntityResultSet;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.lt;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

@Slf4j
public abstract class AbstractModelDao<T extends BaseEntity<?>> extends AbstractDao implements Dao<T> {

    protected abstract Class<T> getColumnFamilyClass();

    protected abstract String getColumnFamilyName();

    protected Mapper<T> getMapper() {
        return cluster.getMapper(getColumnFamilyClass());
    }

    protected List<T> findListByStatement(Statement statement) {
        List<T> list = Collections.emptyList();
        if (statement != null) {
            statement.setConsistencyLevel(cluster.getDefaultReadConsistencyLevel());
            ResultSet resultSet = getSession().execute(statement);
            Result<T> result = getMapper().map(resultSet);
            if (result != null) {
                list = result.all();
            }
        }
        return list;
    }

    protected ListenableFuture<List<T>> findListByStatementAsync(Statement statement) {
        if (statement != null) {
            statement.setConsistencyLevel(cluster.getDefaultReadConsistencyLevel());
            ResultSetFuture resultSetFuture = getSession().executeAsync(statement);
            ListenableFuture<List<T>> result = Futures.transform(resultSetFuture, new Function<ResultSet, List<T>>() {
                @Nullable
                @Override
                public List<T> apply(@Nullable ResultSet resultSet) {
                    Result<T> result = getMapper().map(resultSet);
                    if (result != null) {
                        return result.all();
                    } else {
                        return Collections.emptyList();
                    }
                }
            });
            return result;
        }
        return Futures.immediateFuture(Collections.emptyList());
    }

    protected T findOneByStatement(Statement statement) {
        T object = null;
        if (statement != null) {
            statement.setConsistencyLevel(cluster.getDefaultReadConsistencyLevel());
            ResultSet resultSet = getSession().execute(statement);
            Result<T> result = getMapper().map(resultSet);
            if (result != null) {
                object = result.one();
            }
        }
        return object;
    }

    protected ListenableFuture<T> findOneByStatementAsync(Statement statement) {
        if (statement != null) {
            statement.setConsistencyLevel(cluster.getDefaultReadConsistencyLevel());
            ResultSetFuture resultSetFuture = getSession().executeAsync(statement);
            ListenableFuture<T> result = Futures.transform(resultSetFuture, new Function<ResultSet, T>() {
                @Nullable
                @Override
                public T apply(@Nullable ResultSet resultSet) {
                    Result<T> result = getMapper().map(resultSet);
                    if (result != null) {
                        return result.one();
                    } else {
                        return null;
                    }
                }
            });
            return result;
        }
        return Futures.immediateFuture(null);
    }

    protected Statement getSaveQuery(T dto) {
        return getMapper().saveQuery(dto);
    }

    protected EntityResultSet<T> saveWithResult(T entity) {
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

    public T save(T entity) {
        return saveWithResult(entity).getEntity();
    }

    public T findById(UUID key) {
        log.debug("Get entity by key {}", key);
        Select.Where query = select().from(getColumnFamilyName()).where(eq(ModelConstants.ID_PROPERTY, key));
        log.trace("Execute query {}", query);
        return findOneByStatement(query);
    }

    public ListenableFuture<T> findByIdAsync(UUID key) {
        log.debug("Get entity by key {}", key);
        Select.Where query = select().from(getColumnFamilyName()).where(eq(ModelConstants.ID_PROPERTY, key));
        log.trace("Execute query {}", query);
        return findOneByStatementAsync(query);
    }


    public ResultSet removeById(UUID key) {
        Statement delete = QueryBuilder.delete().all().from(getColumnFamilyName()).where(eq(ModelConstants.ID_PROPERTY, key));
        log.debug("Remove request: {}", delete.toString());
        return getSession().execute(delete);
    }

    public List<T> find() {
        log.debug("Get all entities from column family {}", getColumnFamilyName());
        return findListByStatement(QueryBuilder.select().all().from(getColumnFamilyName()).setConsistencyLevel(cluster.getDefaultReadConsistencyLevel()));
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
