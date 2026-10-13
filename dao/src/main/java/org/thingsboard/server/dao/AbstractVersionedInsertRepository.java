// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.dao.sqlts.insert.AbstractInsertRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.thingsboard.server.dao.model.ModelConstants.VERSION_COLUMN;

public abstract class AbstractVersionedInsertRepository<T> extends AbstractInsertRepository implements VersionedInsertRepository<T> {

    /**
     * Version assigned to a freshly INSERTed row under Citus. The per-row version starts at 1 (instead of a
     * global sequence value) because each shard maintains its own monotonically incrementing version per key.
     */
    protected static final String CITUS_INSERT_VERSION = "1";

    /**
     * Per-worker {@link TransactionTemplate} cache, keyed by the worker's {@link DataSource} (keying on the
     * DataSource is intentional and correct). The worker set is tiny and stable, so this map stays small.
     * When a worker is retired its {@link com.zaxxer.hikari.HikariDataSource} is closed and dropped; if that
     * worker later reappears it gets a brand-new {@code DataSource} instance, i.e. a new cache key, leaving the
     * old entry orphaned. That leak is bounded (at most one small {@code TransactionTemplate} per ever-retired
     * DataSource) and never harmful: routing only ever hands out templates for LIVE pools, so a stale entry is
     * never looked up or used again.
     */
    private final ConcurrentHashMap<DataSource, TransactionTemplate> workerTxTemplates = new ConcurrentHashMap<>();

    public List<Long> saveOrUpdate(List<T> entities) {
        return saveOrUpdate(jdbcTemplate, entities);
    }

    public List<Long> saveOrUpdate(JdbcTemplate target, List<T> entities) {
        return txTemplateFor(target).execute(status -> {
            List<Long> seqNumbers = new ArrayList<>(entities.size());

            KeyHolder keyHolder = new GeneratedKeyHolder();

            int[] updateResult = onBatchUpdate(target, entities, keyHolder);

            List<Map<String, Object>> seqNumbersList = keyHolder.getKeyList();

            int notUpdatedCount = entities.size() - seqNumbersList.size();

            List<Integer> toInsertIndexes = new ArrayList<>(notUpdatedCount);
            List<T> insertEntities = new ArrayList<>(notUpdatedCount);
            for (int i = 0, keyHolderIndex = 0; i < updateResult.length; i++) {
                if (updateResult[i] == 0) {
                    insertEntities.add(entities.get(i));
                    seqNumbers.add(null);
                    toInsertIndexes.add(i);
                } else {
                    seqNumbers.add((Long) seqNumbersList.get(keyHolderIndex).get(VERSION_COLUMN));
                    keyHolderIndex++;
                }
            }

            if (insertEntities.isEmpty()) {
                return seqNumbers;
            }

            int[] insertResult = onInsertOrUpdate(target, insertEntities, keyHolder);

            seqNumbersList = keyHolder.getKeyList();

            for (int i = 0, keyHolderIndex = 0; i < insertResult.length; i++) {
                if (insertResult[i] != 0) {
                    seqNumbers.set(toInsertIndexes.get(i), (Long) seqNumbersList.get(keyHolderIndex).get(VERSION_COLUMN));
                    keyHolderIndex++;
                }
            }

            return seqNumbers;
        });
    }

    private int[] onBatchUpdate(JdbcTemplate target, List<T> entities, KeyHolder keyHolder) {
        return target.batchUpdate(new SequencePreparedStatementCreator(getBatchUpdateQuery()), new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                setOnBatchUpdateValues(ps, i, entities);
            }

            @Override
            public int getBatchSize() {
                return entities.size();
            }
        }, keyHolder);
    }

    private int[] onInsertOrUpdate(JdbcTemplate target, List<T> insertEntities, KeyHolder keyHolder) {
        return target.batchUpdate(new SequencePreparedStatementCreator(getInsertOrUpdateQuery()), new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                setOnInsertOrUpdateValues(ps, i, insertEntities);
            }

            @Override
            public int getBatchSize() {
                return insertEntities.size();
            }
        }, keyHolder);
    }

    protected abstract void setOnBatchUpdateValues(PreparedStatement ps, int i, List<T> entities) throws SQLException;

    protected abstract void setOnInsertOrUpdateValues(PreparedStatement ps, int i, List<T> entities) throws SQLException;

    protected abstract String getBatchUpdateQuery();

    protected abstract String getInsertOrUpdateQuery();

    /**
     * Returns the {@link TransactionTemplate} the two-phase batch must run in for the given target template.
     * For the coordinator path ({@code target == jdbcTemplate}) the autowired {@code transactionTemplate} is
     * returned unchanged, preserving the exact behavior of the non-Citus / coordinator path. For a Citus
     * worker template, a {@link TransactionTemplate} backed by a {@link DataSourceTransactionManager} bound to
     * the worker's own {@link DataSource} is returned (cached per DataSource). Because
     * {@code JdbcTemplate.batchUpdate} obtains its connection via {@code DataSourceUtils.getConnection}, which
     * is transaction-aware, the batch participates in this worker-local (single-shard, single-node)
     * transaction rather than a distributed coordinator transaction.
     */
    private TransactionTemplate txTemplateFor(JdbcTemplate target) {
        if (target == jdbcTemplate) {
            return transactionTemplate;
        }
        return workerTxTemplates.computeIfAbsent(target.getDataSource(),
                ds -> new TransactionTemplate(new DataSourceTransactionManager(ds)));
    }

    private record SequencePreparedStatementCreator(String sql) implements PreparedStatementCreator, SqlProvider {

        private static final String[] COLUMNS = {VERSION_COLUMN};

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            return con.prepareStatement(sql, COLUMNS);
        }

        @Override
        public String getSql() {
            return this.sql;
        }
    }
}
