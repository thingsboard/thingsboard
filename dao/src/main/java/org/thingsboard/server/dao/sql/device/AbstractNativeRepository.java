// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.device;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.page.PageData;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
@Slf4j
public class AbstractNativeRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    protected <T> PageData<T> find(String countQuery, String findQuery, Pageable pageable, Function<Map<String, Object>, T> mapper) {
        return transactionTemplate.execute(status -> {
            long startTs = System.currentTimeMillis();
            int totalElements = jdbcTemplate.queryForObject(countQuery, Collections.emptyMap(), Integer.class);
            log.debug("Count query took {} ms", System.currentTimeMillis() - startTs);
            startTs = System.currentTimeMillis();
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(String.format(findQuery, pageable.getPageSize(), pageable.getOffset()), Collections.emptyMap());
            log.debug("Main query took {} ms", System.currentTimeMillis() - startTs);
            int totalPages = pageable.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageable.getPageSize()) : 1;
            boolean hasNext = pageable.getPageSize() > 0 && totalElements > pageable.getOffset() + rows.size();
            var data = rows.stream().map(mapper).collect(Collectors.toList());
            return new PageData<>(data, totalPages, totalElements, hasNext);
        });
    }

    /**
     * Runs {@code query} once and passes every row to {@code consumer}, reading {@code fetchSize} rows per round trip through a
     * server-side cursor: no OFFSET, no count, one snapshot, client memory bounded by {@code fetchSize}. Rows are mapped with the
     * same case-insensitive column maps as {@link #find}, so the same mappers work for both.
     * <p>
     * One pooled connection is held for the whole iteration (a cursor cannot release it between batches). This is still the
     * shortest total hold: one pass instead of many paged queries (about 0.5 s per 1M devices on PostgreSQL, about 1 s on Citus,
     * where keyset or OFFSET pages fan out to every shard). To keep the hold that short, the consumer runs while the cursor is
     * open and must only do fast in-memory work: no blocking calls and no waiting on other database work. An exception thrown
     * by the consumer stops the iteration and is rethrown (Spring's repository exception translation may wrap it).
     */
    protected <T> void stream(String query, int fetchSize, Function<Map<String, Object>, T> mapper, Consumer<T> consumer, Object... params) {
        long startTs = System.currentTimeMillis();
        int[] rowCount = {0};
        ColumnMapRowMapper columnMapRowMapper = new ColumnMapRowMapper();
        RowCallbackHandler handler = rs -> consumer.accept(mapper.apply(columnMapRowMapper.mapRow(rs, rowCount[0]++)));
        // the cursor only streams inside a transaction (autocommit off); otherwise the driver loads the full result
        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.getJdbcOperations().query(connection -> {
            PreparedStatement ps = connection.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ps.setFetchSize(fetchSize);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps;
        }, handler));
        log.debug("Streamed {} rows, connection held for {} ms: {}", rowCount[0], System.currentTimeMillis() - startTs, query);
    }
}
