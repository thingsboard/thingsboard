// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Reads a table page by page with keyset paging by {@code id}: {@code ... ORDER BY id LIMIT :limit} for the first page and
 * {@code ... AND id > :lastId ORDER BY id LIMIT :limit} for the next ones. Unlike LIMIT/OFFSET pages there is no count and no
 * skipped rows, so a full iteration reads every row once, each page through an index range scan.
 * <p>
 * Every page is a separate short query: its connection goes back to the pool and no transaction or snapshot stays open while
 * the consumer processes the page. A row deleted during the iteration cannot shift the remaining pages.
 */
public final class KeysetPages {

    private KeysetPages() {
    }

    /**
     * @param firstPageQuery query with a {@code :limit} parameter, ordered by {@code id}
     * @param nextPageQuery  the same query with an additional {@code id > :lastId} condition
     * @param params         other named parameters of both queries
     * @param mapper         maps a row (case-insensitive column map with an {@code id} column) to an item; a null result is left out of the page
     * @param pageConsumer   receives every non-empty page in {@code id} order
     */
    public static <T> void forEachPage(NamedParameterJdbcTemplate jdbcTemplate, String firstPageQuery, String nextPageQuery,
                                       Map<String, ?> params, int pageSize,
                                       Function<Map<String, Object>, T> mapper, Consumer<List<T>> pageConsumer) {
        Map<String, Object> args = new HashMap<>(params);
        args.put("limit", pageSize);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(firstPageQuery, args);
        while (true) {
            List<T> page = rows.stream().map(mapper).filter(Objects::nonNull).toList();
            if (!page.isEmpty()) {
                pageConsumer.accept(page);
            }
            if (rows.size() < pageSize) {
                return;
            }
            args.put("lastId", (UUID) rows.get(rows.size() - 1).get("id"));
            rows = jdbcTemplate.queryForList(nextPageQuery, args);
        }
    }

}
