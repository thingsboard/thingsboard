// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.thingsboard.server.dao.sqlts.insert.AbstractInsertRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.dao.model.ModelConstants.VERSION_COLUMN;

public abstract class AbstractVersionedInsertRepository<T> extends AbstractInsertRepository {

    public List<Long> saveOrUpdate(List<T> entities) {
        return transactionTemplate.execute(status -> {
            List<Long> seqNumbers = new ArrayList<>(entities.size());

            KeyHolder keyHolder = new GeneratedKeyHolder();

            int[] updateResult = onBatchUpdate(entities, keyHolder);

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

            int[] insertResult = onInsertOrUpdate(insertEntities, keyHolder);

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

    private int[] onBatchUpdate(List<T> entities, KeyHolder keyHolder) {
        return jdbcTemplate.batchUpdate(new SequencePreparedStatementCreator(getBatchUpdateQuery()), new BatchPreparedStatementSetter() {
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

    private int[] onInsertOrUpdate(List<T> insertEntities, KeyHolder keyHolder) {
        return jdbcTemplate.batchUpdate(new SequencePreparedStatementCreator(getInsertOrUpdateQuery()), new BatchPreparedStatementSetter() {
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
