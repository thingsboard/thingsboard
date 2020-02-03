/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.exceptions.CodecNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.model.type.AuthorityCodec;
import org.thingsboard.server.dao.model.type.ComponentLifecycleStateCodec;
import org.thingsboard.server.dao.model.type.ComponentScopeCodec;
import org.thingsboard.server.dao.model.type.ComponentTypeCodec;
import org.thingsboard.server.dao.model.type.DeviceCredentialsTypeCodec;
import org.thingsboard.server.dao.model.type.EntityTypeCodec;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public abstract class CassandraAbstractDao {

    @Autowired
    @Qualifier("CassandraCluster")
    protected CassandraCluster cluster;

    private ConcurrentMap<String, PreparedStatement> preparedStatementMap = new ConcurrentHashMap<>();

    @Autowired
    private CassandraBufferedRateExecutor rateLimiter;

    private Session session;

    private ConsistencyLevel defaultReadLevel;
    private ConsistencyLevel defaultWriteLevel;

    private Session getSession() {
        if (session == null) {
            session = cluster.getSession();
            defaultReadLevel = cluster.getDefaultReadConsistencyLevel();
            defaultWriteLevel = cluster.getDefaultWriteConsistencyLevel();
            CodecRegistry registry = session.getCluster().getConfiguration().getCodecRegistry();
            registerCodecIfNotFound(registry, new JsonCodec());
            registerCodecIfNotFound(registry, new DeviceCredentialsTypeCodec());
            registerCodecIfNotFound(registry, new AuthorityCodec());
            registerCodecIfNotFound(registry, new ComponentLifecycleStateCodec());
            registerCodecIfNotFound(registry, new ComponentTypeCodec());
            registerCodecIfNotFound(registry, new ComponentScopeCodec());
            registerCodecIfNotFound(registry, new EntityTypeCodec());
        }
        return session;
    }

    protected PreparedStatement prepare(String query) {
        return preparedStatementMap.computeIfAbsent(query, i -> getSession().prepare(i));
    }

    private void registerCodecIfNotFound(CodecRegistry registry, TypeCodec<?> codec) {
        try {
            registry.codecFor(codec.getCqlType(), codec.getJavaType());
        } catch (CodecNotFoundException e) {
            registry.register(codec);
        }
    }

    protected ResultSet executeRead(TenantId tenantId, Statement statement) {
        return execute(tenantId, statement, defaultReadLevel);
    }

    protected ResultSet executeWrite(TenantId tenantId, Statement statement) {
        return execute(tenantId, statement, defaultWriteLevel);
    }

    protected ResultSetFuture executeAsyncRead(TenantId tenantId, Statement statement) {
        return executeAsync(tenantId, statement, defaultReadLevel);
    }

    protected ResultSetFuture executeAsyncWrite(TenantId tenantId, Statement statement) {
        return executeAsync(tenantId, statement, defaultWriteLevel);
    }

    private ResultSet execute(TenantId tenantId, Statement statement, ConsistencyLevel level) {
        if (log.isDebugEnabled()) {
            log.debug("Execute cassandra statement {}", statementToString(statement));
        }
        return executeAsync(tenantId, statement, level).getUninterruptibly();
    }

    private ResultSetFuture executeAsync(TenantId tenantId, Statement statement, ConsistencyLevel level) {
        if (log.isDebugEnabled()) {
            log.debug("Execute cassandra async statement {}", statementToString(statement));
        }
        if (statement.getConsistencyLevel() == null) {
            statement.setConsistencyLevel(level);
        }
        return rateLimiter.submit(new CassandraStatementTask(tenantId, getSession(), statement));
    }

    private static String statementToString(Statement statement) {
        if (statement instanceof BoundStatement) {
            return ((BoundStatement) statement).preparedStatement().getQueryString();
        } else {
            return statement.toString();
        }
    }
}