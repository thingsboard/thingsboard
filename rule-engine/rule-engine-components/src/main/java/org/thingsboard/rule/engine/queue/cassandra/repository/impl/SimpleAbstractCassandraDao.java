/**
 * Copyright © 2016-2017 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.queue.cassandra.repository.impl;

import com.datastax.driver.core.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public abstract class SimpleAbstractCassandraDao {

    private ConsistencyLevel defaultReadLevel = ConsistencyLevel.QUORUM;
    private ConsistencyLevel defaultWriteLevel = ConsistencyLevel.QUORUM;
    private Session session;
    private Map<String, PreparedStatement> preparedStatementMap = new ConcurrentHashMap<>();

    public SimpleAbstractCassandraDao(Session session) {
        this.session = session;
    }

    protected Session getSession() {
        return session;
    }

    protected ResultSet executeRead(Statement statement) {
        return execute(statement, defaultReadLevel);
    }

    protected ResultSet executeWrite(Statement statement) {
        return execute(statement, defaultWriteLevel);
    }

    protected ResultSetFuture executeAsyncRead(Statement statement) {
        return executeAsync(statement, defaultReadLevel);
    }

    protected ResultSetFuture executeAsyncWrite(Statement statement) {
        return executeAsync(statement, defaultWriteLevel);
    }

    protected PreparedStatement prepare(String query) {
        return preparedStatementMap.computeIfAbsent(query, i -> getSession().prepare(i));
    }

    private ResultSet execute(Statement statement, ConsistencyLevel level) {
        log.debug("Execute cassandra statement {}", statement);
        if (statement.getConsistencyLevel() == null) {
            statement.setConsistencyLevel(level);
        }
        return getSession().execute(statement);
    }

    private ResultSetFuture executeAsync(Statement statement, ConsistencyLevel level) {
        log.debug("Execute cassandra async statement {}", statement);
        if (statement.getConsistencyLevel() == null) {
            statement.setConsistencyLevel(level);
        }
        return getSession().executeAsync(statement);
    }
}
