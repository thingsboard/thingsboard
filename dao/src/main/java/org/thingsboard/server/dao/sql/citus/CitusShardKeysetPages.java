// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Keyset paging over a table hash-distributed by {@code id} that keeps every page on one Citus shard.
 * <p>
 * Plain keyset pages ({@code id > :lastId ORDER BY id LIMIT n}) fan out to every shard: each shard returns up to {@code n}
 * rows and the coordinator keeps 1/shardCount of them. Citus does not prune shards on a range of {@code worker_hash(id)}, and
 * {@code worker_hash} is volatile, so it cannot be indexed. PostgreSQL's immutable {@code uuid_hash(id)} is the same hash Citus
 * places rows by, so with an index on {@code ((uuid_hash(id)), id)} a page bounded to one shard's hash range
 * ({@code pg_dist_shard.shardminvalue..shardmaxvalue}) is an index range scan on that shard, and every other shard answers from
 * an empty index probe.
 * <p>
 * Like {@link org.thingsboard.server.dao.sql.KeysetPages}, every page is a separate short query handed over after the query has
 * finished, so no connection or transaction is held while the consumer runs. Pages never span shards.
 */
public final class CitusShardKeysetPages {

    private CitusShardKeysetPages() {
    }

    /**
     * Returns the shard hash ranges {@code [min, max]} of a distributed table, ascending; empty if the table is not distributed.
     */
    public static List<int[]> shardHashRanges(NamedParameterJdbcTemplate jdbcTemplate, String table) {
        return jdbcTemplate.query("SELECT shardminvalue::int AS smin, shardmaxvalue::int AS smax FROM pg_dist_shard " +
                        "WHERE logicalrelid = CAST(:table AS regclass) ORDER BY smin",
                Map.of("table", table), (rs, rowNum) -> new int[]{rs.getInt("smin"), rs.getInt("smax")});
    }

    /**
     * @param columns      select list of the page query; must include {@code id} (rows are case-insensitive column maps)
     * @param table        table hash-distributed by {@code id}, with an index on {@code ((uuid_hash(id)), id)}
     * @param shardRanges  the table's shard hash ranges, see {@link #shardHashRanges}
     * @param mapper       maps a row to an item; a null result is left out of the page
     * @param pageConsumer receives every non-empty page, shard by shard in hash order
     */
    public static <T> void forEachPage(NamedParameterJdbcTemplate jdbcTemplate, String columns, String table, List<int[]> shardRanges,
                                       int pageSize, Function<Map<String, Object>, T> mapper, Consumer<List<T>> pageConsumer) {
        String select = "SELECT " + columns + ", uuid_hash(id) AS id_hash FROM " + table +
                " WHERE uuid_hash(id) BETWEEN :shardMin AND :shardMax ";
        String firstPageQuery = select + "ORDER BY uuid_hash(id), id LIMIT :limit";
        String nextPageQuery = select + "AND (uuid_hash(id), id) > (:lastHash, :lastId) ORDER BY uuid_hash(id), id LIMIT :limit";
        for (int[] range : shardRanges) {
            Map<String, Object> args = new HashMap<>();
            args.put("shardMin", range[0]);
            args.put("shardMax", range[1]);
            args.put("limit", pageSize);
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(firstPageQuery, args);
            while (true) {
                List<T> page = rows.stream().map(mapper).filter(Objects::nonNull).toList();
                if (!page.isEmpty()) {
                    pageConsumer.accept(page);
                }
                if (rows.size() < pageSize) {
                    break;
                }
                Map<String, Object> last = rows.get(rows.size() - 1);
                args.put("lastHash", ((Number) last.get("id_hash")).intValue());
                args.put("lastId", (UUID) last.get("id"));
                rows = jdbcTemplate.queryForList(nextPageQuery, args);
            }
        }
    }

}
