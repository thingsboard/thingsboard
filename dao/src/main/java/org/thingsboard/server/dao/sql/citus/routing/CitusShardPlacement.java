// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

/**
 * Maps a shard-bucket index to the Citus worker node that currently owns it, for a single
 * co-location group anchored on one distributed table.
 *
 * <p>The bucket index is the position of a shard within its co-location group when shards are
 * ordered ascending by {@code (shardminvalue)::int}. This MUST match the ordering used by
 * {@link org.thingsboard.server.dao.sql.citus.CitusShardLocator}, which assigns
 * {@code entityId -> bucket} by binary-searching the same ascending {@code (shardminvalue)::int}
 * ranges. Because of that shared ordering, placement index {@code i} names the owner of exactly the
 * shard the locator assigns to bucket {@code i}: the locator says "this entity lives in bucket i",
 * and this class says "bucket i lives on this worker". A mismatch in ordering or casting (e.g.
 * {@code ::bigint} vs {@code ::int}) would silently route every operation to the wrong worker, so
 * the {@code ::int} cast here is deliberately identical to the locator's.
 *
 * <p>Owners are resolved against the coordinator catalog by joining {@code pg_dist_shard},
 * {@code pg_dist_placement} and {@code pg_dist_node}, filtered to active placements
 * ({@code pg_dist_placement.shardstate = 1}) on active primary worker nodes (excluding the
 * coordinator group 0). The snapshot is held in an {@link AtomicReference} and replaced atomically
 * on {@link #refresh()}; reads are lock-free.
 *
 * <p><b>Replication-factor-1 assumption.</b> Smart routing targets a co-location group with
 * {@code replication_factor = 1}: HA/replication is an explicit non-goal of this design. With RF=1
 * <em>and</em> the {@code shardstate = 1} filter, the shard -> placement join yields exactly one
 * active placement per shard, so the result has exactly one row per shard and the row index equals
 * the bucket index. The {@code shardstate = 1} predicate additionally guards against orphaned or
 * inactive placements (e.g. {@code shardstate = 4} rows left behind by a failed rebalance), which
 * would otherwise shift every subsequent bucket index. {@link #refresh()} fails loud (throws
 * {@link IllegalStateException}) if any shard still appears more than once after filtering, rather
 * than storing a misaligned snapshot — a duplicate signals an unexpected topology (e.g. RF&gt;1)
 * the router cannot safely handle.
 */
@Slf4j
public class CitusShardPlacement {

    /**
     * Joins shard -> placement -> node for the anchor table, restricted to active placements
     * ({@code p.shardstate = 1}) on active primary workers (excluding the coordinator group 0),
     * ordered ascending by {@code (shardminvalue)::int} so the row index equals the bucket index.
     * The {@code ::int} cast mirrors {@code CitusShardLocator} exactly and must not be changed to
     * {@code ::bigint}.
     *
     * <p>The {@code p.shardstate = 1} filter, combined with the RF=1 assumption documented on the
     * class, makes the shard -> placement join yield exactly one row per shard; it discards inactive
     * or orphaned placements (e.g. {@code shardstate = 4}) that would otherwise duplicate a shard and
     * shift every subsequent bucket index.
     */
    private static final String PLACEMENT_QUERY =
            "select s.shardid as shardid, p.groupid as groupid, n.nodename as nodename, n.nodeport as nodeport " +
                    "from pg_dist_shard s " +
                    "join pg_dist_placement p on p.shardid = s.shardid " +
                    "join pg_dist_node n on n.groupid = p.groupid " +
                    "where s.logicalrelid = ?::regclass " +
                    "and p.shardstate = 1 " +
                    "and " + CitusRoutingQueries.activePrimaryWorkerPredicate("n.") + " " +
                    "order by (s.shardminvalue)::int";

    private static final RowMapper<PlacementRow> ROW_MAPPER = (rs, rowNum) ->
            new PlacementRow(rs.getLong("shardid"),
                    new CitusWorkerNode(rs.getInt("groupid"), rs.getString("nodename"), rs.getInt("nodeport")));

    /**
     * A single ordered row from {@link #PLACEMENT_QUERY}: the shard id together with its owning
     * worker. The shard id is carried only so {@link #refresh()} can verify one-row-per-shard
     * alignment; it is not part of the stored snapshot.
     */
    record PlacementRow(long shardId, CitusWorkerNode worker) {
    }

    private final JdbcTemplate coordinatorJdbcTemplate;
    private final String anchorTable;
    /**
     * Supplies the expected number of buckets (shards) the placement snapshot must contain, evaluated
     * fresh on every {@link #refresh()} — typically the live {@code CitusShardLocator.shardCount()}. A
     * non-positive value disables the size check (used by unit tests that have no locator to compare
     * against). See {@link #refresh()} for why a size mismatch keeps the previous snapshot.
     */
    private final IntSupplier expectedBucketCount;
    private final AtomicReference<List<CitusWorkerNode>> bucketOwners = new AtomicReference<>(null);

    public CitusShardPlacement(JdbcTemplate coordinatorJdbcTemplate, String anchorTable) {
        this(coordinatorJdbcTemplate, anchorTable, () -> 0);
    }

    public CitusShardPlacement(JdbcTemplate coordinatorJdbcTemplate, String anchorTable, IntSupplier expectedBucketCount) {
        this.coordinatorJdbcTemplate = coordinatorJdbcTemplate;
        this.anchorTable = anchorTable;
        this.expectedBucketCount = expectedBucketCount;
    }

    /**
     * Re-reads the bucket -> worker mapping from the coordinator catalog and atomically replaces the
     * current snapshot. Safe to call periodically (e.g. after a rebalance moves placements).
     */
    public synchronized void refresh() {
        List<PlacementRow> rows = coordinatorJdbcTemplate.query(PLACEMENT_QUERY, ROW_MAPPER, anchorTable);
        long distinctShards = rows.stream().map(PlacementRow::shardId).distinct().count();
        if (rows.size() != distinctShards) {
            throw new IllegalStateException("Citus shard placement for " + anchorTable +
                    " returned " + rows.size() + " active placement row(s) across " + distinctShards +
                    " distinct shard(s): at least one shard has more than one active placement, which " +
                    "breaks the bucket -> worker alignment invariant. Smart routing requires " +
                    "replication_factor = 1 for this co-location group; refusing to store a misaligned snapshot");
        }
        int expected = expectedBucketCount.getAsInt();
        if (expected > 0 && rows.size() != expected) {
            // The PLACEMENT_QUERY filters on shardstate = 1 and active primaries, so mid-rebalance a shard
            // can momentarily drop out of the result, shrinking the list. Storing a shorter list would make
            // workerForBucket(i) throw IndexOutOfBounds for the now-missing top buckets. Keep the previous
            // good snapshot and warn rather than crashing an already-running app or storing a misaligned one;
            // the next scheduled refresh re-reads once the rebalance settles. (Unlike the duplicate-shard
            // case above, a transient count skew is expected during a rebalance and is not a topology error.)
            log.warn("Citus shard placement for {} returned {} active placement(s) but {} buckets were expected; " +
                            "keeping the previous placement snapshot until the count matches (likely a transient rebalance).",
                    anchorTable, rows.size(), expected);
            return;
        }
        List<CitusWorkerNode> owners = rows.stream().map(PlacementRow::worker).collect(Collectors.toList());
        bucketOwners.set(List.copyOf(owners));
        log.info("Loaded {} Citus shard placements for {}", owners.size(), anchorTable);
    }

    /**
     * Returns the worker node owning the given bucket.
     *
     * @throws IllegalStateException     if {@link #refresh()} has not yet completed successfully
     * @throws IndexOutOfBoundsException if {@code bucket} is negative or {@code >= shardCount()}
     */
    public CitusWorkerNode workerForBucket(int bucket) {
        List<CitusWorkerNode> owners = bucketOwners.get();
        if (owners == null) {
            throw new IllegalStateException("Citus shard placement for " + anchorTable +
                    " has not been loaded yet; call refresh() before workerForBucket()");
        }
        if (bucket < 0 || bucket >= owners.size()) {
            throw new IndexOutOfBoundsException("Bucket " + bucket + " is out of range for " +
                    anchorTable + " with " + owners.size() + " shard(s)");
        }
        return owners.get(bucket);
    }

    /** Number of buckets (shards) currently loaded; {@code 0} if {@link #refresh()} has not run. */
    public int shardCount() {
        List<CitusWorkerNode> owners = bucketOwners.get();
        return owners == null ? 0 : owners.size();
    }

    /**
     * Immutable snapshot of the bucket -> worker mapping, indexed by bucket. Returns an empty list if
     * {@link #refresh()} has not run. The returned list is unmodifiable.
     */
    public List<CitusWorkerNode> bucketOwners() {
        List<CitusWorkerNode> owners = bucketOwners.get();
        return owners == null ? Collections.emptyList() : owners;
    }
}
