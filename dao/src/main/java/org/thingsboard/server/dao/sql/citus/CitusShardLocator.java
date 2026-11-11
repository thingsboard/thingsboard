// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Maps a UUID entity id to a stable shard-bucket index in {@code [0, shardCount)}, where the bucket
 * is the index of the Citus hash range that contains {@link PostgresHashAny#hashUuid(UUID)}, with
 * ranges sorted ascending by {@code shardminvalue}.
 * <p>
 * The bucket equals the shard Citus actually routes the UUID to (the index of that shard within the
 * ascending-by-range ordering). The bucket is stable across rebalances: rebalancing moves shard to
 * worker placements, not the hash ranges themselves.
 */
@Slf4j
public class CitusShardLocator {

    private final JdbcTemplate jdbcTemplate;
    private final CitusSettings settings;
    private final String distributedTable;
    private final AtomicReference<int[]> shardMins = new AtomicReference<>(new int[0]); // sorted ascending
    private final AtomicBoolean fallbackWarned = new AtomicBoolean(false);

    public CitusShardLocator(JdbcTemplate jdbcTemplate, CitusSettings settings) {
        this(jdbcTemplate, settings, CitusTables.DISTRIBUTED_TABLES.get(0));
    }

    public CitusShardLocator(JdbcTemplate jdbcTemplate, CitusSettings settings, String distributedTable) {
        this.jdbcTemplate = jdbcTemplate;
        this.settings = settings;
        this.distributedTable = distributedTable;
    }

    public synchronized void refresh() {
        List<Integer> mins = jdbcTemplate.queryForList(
                "select (shardminvalue)::int as smin from pg_dist_shard " +
                        "where logicalrelid = ?::regclass order by smin", Integer.class, distributedTable);
        int[] arr = new int[mins.size()];
        for (int i = 0; i < mins.size(); i++) {
            arr[i] = mins.get(i);
        }
        shardMins.set(arr);
        log.info("Loaded {} Citus shard ranges for {}", arr.length, distributedTable);
    }

    public int shardCount() {
        int loaded = shardMins.get().length;
        return loaded > 0 ? loaded : settings.getShardCount();
    }

    /** Returns the shard bucket index in [0, shardCount) for the given entity id. */
    public int bucket(UUID entityId) {
        int[] mins = shardMins.get();
        if (mins.length == 0) {
            // not yet refreshed — fall back to uniform partitioning over configured shard count
            int shardCount = settings.getShardCount();
            if (shardCount <= 0) {
                if (fallbackWarned.compareAndSet(false, true)) {
                    log.warn("CitusShardLocator.bucket() called before shard ranges were loaded (refresh() not invoked?) " +
                            "and shardCount is misconfigured to {} for table {}; routing all entities to bucket 0 until refresh() runs.",
                            shardCount, distributedTable);
                }
                return 0;
            }
            if (fallbackWarned.compareAndSet(false, true)) {
                log.warn("CitusShardLocator.bucket() called before shard ranges were loaded (refresh() not invoked?); " +
                        "using uniform-partition fallback for table {}. This degrades shard alignment until refresh() runs.",
                        distributedTable);
            }
            int hash = PostgresHashAny.hashUuid(entityId);
            int bucket = (int) (((long) hash - Integer.MIN_VALUE) / (0x100000000L / shardCount));
            // shardCount need not divide 2^32 evenly (e.g. 48), so the division can yield shardCount
            // itself for the top hash values; clamp to keep the result within [0, shardCount).
            return Math.min(bucket, shardCount - 1);
        }
        int hash = PostgresHashAny.hashUuid(entityId);
        // largest index whose shardmin <= hash (ranges are contiguous and ascending)
        int lo = 0, hi = mins.length - 1, ans = 0;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (mins[mid] <= hash) {
                ans = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return ans;
    }
}
