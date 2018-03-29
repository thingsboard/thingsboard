/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.CodecNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.model.type.*;
import org.thingsboard.server.dao.util.BufferedRateLimiter;

@Slf4j
public abstract class CassandraAbstractDao {

    @Autowired
    protected CassandraCluster cluster;

    @Autowired
    private BufferedRateLimiter rateLimiter;

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
        return getSession().prepare(query);
    }

    private void registerCodecIfNotFound(CodecRegistry registry, TypeCodec<?> codec) {
        try {
            registry.codecFor(codec.getCqlType(), codec.getJavaType());
        } catch (CodecNotFoundException e) {
            registry.register(codec);
        }
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

    private ResultSet execute(Statement statement, ConsistencyLevel level) {
        log.debug("Execute cassandra statement {}", statement);
        return executeAsync(statement, level).getUninterruptibly();
    }

    private ResultSetFuture executeAsync(Statement statement, ConsistencyLevel level) {
        log.debug("Execute cassandra async statement {}", statement);
        if (statement.getConsistencyLevel() == null) {
            statement.setConsistencyLevel(level);
        }
        return new RateLimitedResultSetFuture(getSession(), rateLimiter, statement);
    }
}