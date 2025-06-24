/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.rule.engine.action;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.cassandra.guava.GuavaSession;
import org.thingsboard.server.dao.nosql.CassandraStatementTask;
import org.thingsboard.server.dao.nosql.TbResultSetFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(type = ComponentType.ACTION,
        name = "save to custom table",
        configClazz = TbSaveToCustomCassandraTableNodeConfiguration.class,
        version = 1,
        nodeDescription = "Node stores data from incoming Message payload to the Cassandra database into the predefined custom table" +
                " that should have <b>cs_tb_</b> prefix, to avoid the data insertion to the common TB tables.<br>" +
                "<b>Note:</b> rule node can be used only for Cassandra DB.",
        nodeDetails = "Administrator should set the custom table name without prefix: <b>cs_tb_</b>. <br>" +
                "Administrator can configure the mapping between the Message field names and Table columns name.<br>" +
                "<b>Note:</b>If the mapping key is <b>$entity_id</b>, that is identified by the Message Originator, then to the appropriate column name(mapping value) will be write the message originator id.<br><br>" +
                "If specified message field does not exist or is not a JSON Primitive, the outbound message will be routed via <b>failure</b> chain," +
                " otherwise, the message will be routed via <b>success</b> chain.",
        configDirective = "tbActionNodeCustomTableConfig",
        icon = "file_upload",
        ruleChainTypes = RuleChainType.CORE)
public class TbSaveToCustomCassandraTableNode implements TbNode {

    private static final String TABLE_PREFIX = "cs_tb_";
    private static final String ENTITY_ID = "$entityId";

    private TbSaveToCustomCassandraTableNodeConfiguration config;
    private GuavaSession session;
    private CassandraCluster cassandraCluster;
    private ConsistencyLevel defaultWriteLevel;
    private PreparedStatement saveStmt;
    private ExecutorService readResultsProcessingExecutor;
    private Map<String, String> fieldsMap;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = TbNodeUtils.convert(configuration, TbSaveToCustomCassandraTableNodeConfiguration.class);
        cassandraCluster = ctx.getCassandraCluster();
        if (cassandraCluster == null) {
            throw new TbNodeException("Unable to connect to Cassandra database", true);
        }
        if (!isTableExists()) {
            throw new TbNodeException("Table '" + TABLE_PREFIX + config.getTableName() + "' does not exist in Cassandra cluster.");
        }
        startExecutor();
        saveStmt = getSaveStmt();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        withCallback(save(msg, ctx), aVoid -> ctx.tellSuccess(msg), e -> ctx.tellFailure(msg, e), ctx.getDbCallbackExecutor());
    }

    @Override
    public void destroy() {
        stopExecutor();
        saveStmt = null;
    }

    private void startExecutor() {
        readResultsProcessingExecutor = Executors.newCachedThreadPool();
    }

    private void stopExecutor() {
        if (readResultsProcessingExecutor != null) {
            readResultsProcessingExecutor.shutdownNow();
        }
    }

    private boolean isTableExists() {
        var keyspaceMdOpt = getSession().getMetadata().getKeyspace(cassandraCluster.getKeyspaceName());
        return keyspaceMdOpt.map(keyspaceMetadata ->
                keyspaceMetadata.getTable(TABLE_PREFIX + config.getTableName()).isPresent()).orElse(false);
    }

    private PreparedStatement prepare(String query) {
        return getSession().prepare(query);
    }

    private GuavaSession getSession() {
        if (session == null) {
            session = cassandraCluster.getSession();
            defaultWriteLevel = cassandraCluster.getDefaultWriteConsistencyLevel();
        }
        return session;
    }

    private PreparedStatement getSaveStmt() throws TbNodeException {
        fieldsMap = config.getFieldsMapping();
        if (fieldsMap.isEmpty()) {
            throw new TbNodeException("Fields(key,value) map is empty!", true);
        } else {
            return prepareStatement(new ArrayList<>(fieldsMap.values()));
        }
    }

    private PreparedStatement prepareStatement(List<String> fieldsList) {
        return prepare(createQuery(fieldsList));
    }

    private String createQuery(List<String> fieldsList) {
        int size = fieldsList.size();
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ")
                .append(TABLE_PREFIX)
                .append(config.getTableName())
                .append("(");
        for (String field : fieldsList) {
            query.append(field);
            if (fieldsList.get(size - 1).equals(field)) {
                query.append(")");
            } else {
                query.append(",");
            }
        }
        query.append(" VALUES(");
        for (int i = 0; i < size; i++) {
            if (i == size - 1) {
                query.append("?)");
            } else {
                query.append("?, ");
            }
        }
        if (config.getDefaultTtl() > 0) {
            query.append(" USING TTL ?");
        }
        return query.toString();
    }

    private ListenableFuture<Void> save(TbMsg msg, TbContext ctx) {
        JsonElement data = JsonParser.parseString(msg.getData());
        if (!data.isJsonObject()) {
            throw new IllegalStateException("Invalid message structure, it is not a JSON Object: " + data);
        } else {
            JsonObject dataAsObject = data.getAsJsonObject();
            BoundStatementBuilder stmtBuilder = getStmtBuilder();
            AtomicInteger i = new AtomicInteger(0);
            fieldsMap.forEach((key, value) -> {
                if (key.equals(ENTITY_ID)) {
                    stmtBuilder.setUuid(i.get(), msg.getOriginator().getId());
                } else if (dataAsObject.has(key)) {
                    JsonElement dataKeyElement = dataAsObject.get(key);
                    if (dataKeyElement.isJsonPrimitive()) {
                        JsonPrimitive primitive = dataKeyElement.getAsJsonPrimitive();
                        if (primitive.isNumber()) {
                            if (primitive.getAsString().contains(".")) {
                                stmtBuilder.setDouble(i.get(), primitive.getAsDouble());
                            } else {
                                stmtBuilder.setLong(i.get(), primitive.getAsLong());
                            }
                        } else if (primitive.isBoolean()) {
                            stmtBuilder.setBoolean(i.get(), primitive.getAsBoolean());
                        } else if (primitive.isString()) {
                            stmtBuilder.setString(i.get(), primitive.getAsString());
                        } else {
                            stmtBuilder.setToNull(i.get());
                        }
                    } else if (dataKeyElement.isJsonObject()) {
                        stmtBuilder.setString(i.get(), dataKeyElement.getAsJsonObject().toString());
                    } else {
                        throw new IllegalStateException("Message data key: '" + key + "' with value: '" + dataKeyElement + "' is not a JSON Object or JSON Primitive!");
                    }
                } else {
                    throw new RuntimeException("Message data doesn't contain key: " + "'" + key + "'!");
                }
                i.getAndIncrement();
            });
            if (config.getDefaultTtl() > 0) {
                stmtBuilder.setInt(i.get(), config.getDefaultTtl());
            }
            return getFuture(executeAsyncWrite(ctx, stmtBuilder.build()), rs -> null);
        }
    }

    BoundStatementBuilder getStmtBuilder() {
        return new BoundStatementBuilder(saveStmt.bind());
    }

    private TbResultSetFuture executeAsyncWrite(TbContext ctx, Statement statement) {
        return executeAsync(ctx, statement, defaultWriteLevel);
    }

    private TbResultSetFuture executeAsync(TbContext ctx, Statement statement, ConsistencyLevel level) {
        if (log.isDebugEnabled()) {
            log.debug("Execute cassandra async statement {}", statementToString(statement));
        }
        if (statement.getConsistencyLevel() == null) {
            statement.setConsistencyLevel(level);
        }
        return ctx.submitCassandraWriteTask(new CassandraStatementTask(ctx.getTenantId(), getSession(), statement));
    }

    private static String statementToString(Statement statement) {
        if (statement instanceof BoundStatement) {
            return ((BoundStatement) statement).getPreparedStatement().getQuery();
        } else {
            return statement.toString();
        }
    }

    private <T> ListenableFuture<T> getFuture(TbResultSetFuture future, java.util.function.Function<AsyncResultSet, T> transformer) {
        return Futures.transform(future, new Function<AsyncResultSet, T>() {
            @Nullable
            @Override
            public T apply(@Nullable AsyncResultSet input) {
                return transformer.apply(input);
            }
        }, readResultsProcessingExecutor);
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                if (!oldConfiguration.has("defaultTtl")) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).put("defaultTtl", 0);
                }
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
