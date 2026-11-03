// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.nosql;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.cassandra.guava.GuavaSession;
import org.thingsboard.server.dao.util.BufferedRateExecutor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public abstract class CassandraAbstractDao {

    @Autowired
    @Qualifier("CassandraCluster")
    protected CassandraCluster cluster;

    private ConcurrentMap<String, PreparedStatement> preparedStatementMap = new ConcurrentHashMap<>();

    @Autowired
    private CassandraBufferedRateReadExecutor rateReadLimiter;

    @Autowired
    private CassandraBufferedRateWriteExecutor rateWriteLimiter;

    private GuavaSession session;

    private ConsistencyLevel defaultReadLevel;
    private ConsistencyLevel defaultWriteLevel;

    private GuavaSession getSession() {
        if (session == null) {
            session = cluster.getSession();
            defaultReadLevel = cluster.getDefaultReadConsistencyLevel();
            defaultWriteLevel = cluster.getDefaultWriteConsistencyLevel();
        }
        return session;
    }

    protected PreparedStatement prepare(String query) {
        return preparedStatementMap.computeIfAbsent(query, i -> getSession().prepare(i));
    }

    protected AsyncResultSet executeRead(TenantId tenantId, Statement statement) {
        return execute(tenantId, statement, defaultReadLevel, rateReadLimiter);
    }

    protected AsyncResultSet executeWrite(TenantId tenantId, Statement statement) {
        return execute(tenantId, statement, defaultWriteLevel, rateWriteLimiter);
    }

    protected TbResultSetFuture executeAsyncRead(TenantId tenantId, Statement statement) {
        return executeAsync(tenantId, statement, defaultReadLevel, rateReadLimiter);
    }

    protected TbResultSetFuture executeAsyncWrite(TenantId tenantId, Statement statement) {
        return executeAsync(tenantId, statement, defaultWriteLevel, rateWriteLimiter);
    }

    private AsyncResultSet execute(TenantId tenantId, Statement statement, ConsistencyLevel level,
                                   BufferedRateExecutor<CassandraStatementTask, TbResultSetFuture> rateExecutor) {
        if (log.isDebugEnabled()) {
            log.debug("Execute cassandra statement {}", statementToString(statement));
        }
        return executeAsync(tenantId, statement, level, rateExecutor).getUninterruptibly();
    }

    private TbResultSetFuture executeAsync(TenantId tenantId, Statement statement, ConsistencyLevel level,
                                           BufferedRateExecutor<CassandraStatementTask, TbResultSetFuture> rateExecutor) {
        if (log.isDebugEnabled()) {
            log.debug("Execute cassandra async statement {}", statementToString(statement));
        }
        if (statement.getConsistencyLevel() == null) {
            statement = statement.setConsistencyLevel(level);
        }
        return rateExecutor.submit(new CassandraStatementTask(tenantId, getSession(), statement));
    }

    private static String statementToString(Statement statement) {
        if (statement instanceof BoundStatement) {
            return ((BoundStatement) statement).getPreparedStatement().getQuery();
        } else {
            return statement.toString();
        }
    }
}
