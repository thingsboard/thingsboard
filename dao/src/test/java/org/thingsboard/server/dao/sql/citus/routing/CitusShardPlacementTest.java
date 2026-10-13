// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.thingsboard.server.dao.sql.citus.routing.CitusShardPlacement.PlacementRow;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CitusShardPlacementTest {

    private static final String ANCHOR = "attribute_kv";

    private static final CitusWorkerNode WORKER_1 = new CitusWorkerNode(1, "citus-worker-2", 5432);
    private static final CitusWorkerNode WORKER_2 = new CitusWorkerNode(2, "citus-worker-1", 5432);

    // The production CitusShardPlacement relies on the SQL "order by (shardminvalue)::int" to produce
    // bucket order; a unit test cannot exercise the database sort, so the canned rows are returned to
    // the placement already sorted ascending — exactly what the coordinator returns for that query.
    // Each row also carries its shardid (as the production RowMapper now does), so refresh() can verify
    // one-row-per-shard alignment. The tests then assert the placement preserves that order verbatim
    // (no reorder, no off-by-one), which is what guarantees placement index i lines up with
    // CitusShardLocator.bucket() (design 4.3).

    // Wraps workers into placement rows with consecutive, distinct shard ids (the normal RF=1 case:
    // exactly one active placement per shard).
    private static List<PlacementRow> rows(CitusWorkerNode... workers) {
        return IntStream.range(0, workers.length)
                .mapToObj(i -> new PlacementRow(100L + i, workers[i]))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private CitusShardPlacement placementReturning(List<PlacementRow> canned) {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(ANCHOR))).thenReturn(canned);
        return new CitusShardPlacement(jdbcTemplate, ANCHOR);
    }

    @Test
    void bucketsFollowAscendingShardMinValueOrderingFromTheQuery() {
        // Rows as the coordinator would return them: sorted ascending by (shardminvalue)::int.
        // shardminvalues: -2147483648, -2058005163, -1968526678, -1879048193 -> workers 2,1,2,1
        List<PlacementRow> canned = rows(WORKER_2, WORKER_1, WORKER_2, WORKER_1);
        CitusShardPlacement placement = placementReturning(canned);

        placement.refresh();

        assertThat(placement.shardCount()).isEqualTo(4);
        assertThat(placement.workerForBucket(0)).isEqualTo(WORKER_2);
        assertThat(placement.workerForBucket(1)).isEqualTo(WORKER_1);
        assertThat(placement.workerForBucket(2)).isEqualTo(WORKER_2);
        assertThat(placement.workerForBucket(3)).isEqualTo(WORKER_1);
    }

    @Test
    void preservesQueryOrderExactlyWithoutReorderingOrOffByOne() {
        // The 4.3 invariant: bucket index i must equal the position of the shard in the
        // ascending-(shardminvalue)::int ordering. We feed a known order and assert it is preserved
        // verbatim, so placement index i lines up with CitusShardLocator.bucket().
        List<PlacementRow> canned = rows(WORKER_1, WORKER_1, WORKER_2);
        CitusShardPlacement placement = placementReturning(canned);

        placement.refresh();

        assertThat(placement.bucketOwners()).containsExactly(WORKER_1, WORKER_1, WORKER_2);
    }

    @Test
    void workerForBucketReturnsGroupIdNodeNameAndPort() {
        CitusShardPlacement placement = placementReturning(rows(WORKER_1, WORKER_2));
        placement.refresh();

        CitusWorkerNode bucket0 = placement.workerForBucket(0);
        assertThat(bucket0.groupId()).isEqualTo(1);
        assertThat(bucket0.nodeName()).isEqualTo("citus-worker-2");
        assertThat(bucket0.nodePort()).isEqualTo(5432);

        CitusWorkerNode bucket1 = placement.workerForBucket(1);
        assertThat(bucket1.groupId()).isEqualTo(2);
        assertThat(bucket1.nodeName()).isEqualTo("citus-worker-1");
        assertThat(bucket1.nodePort()).isEqualTo(5432);
    }

    @Test
    void shardCountMatchesNumberOfShards() {
        CitusShardPlacement placement = placementReturning(rows(WORKER_1, WORKER_2, WORKER_1));
        placement.refresh();
        assertThat(placement.shardCount()).isEqualTo(3);
    }

    @Test
    void notYetRefreshedShardCountIsZeroAndBucketOwnersEmpty() {
        CitusShardPlacement placement = placementReturning(rows(WORKER_1));
        assertThat(placement.shardCount()).isZero();
        assertThat(placement.bucketOwners()).isEmpty();
    }

    @Test
    void workerForBucketBeforeRefreshThrowsIllegalState() {
        CitusShardPlacement placement = placementReturning(rows(WORKER_1));
        assertThatThrownBy(() -> placement.workerForBucket(0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has not been loaded");
    }

    @Test
    void workerForBucketOutOfRangeThrows() {
        CitusShardPlacement placement = placementReturning(rows(WORKER_1, WORKER_2));
        placement.refresh();

        assertThatThrownBy(() -> placement.workerForBucket(2))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> placement.workerForBucket(-1))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void bucketOwnersSnapshotIsUnmodifiable() {
        CitusShardPlacement placement = placementReturning(rows(WORKER_1, WORKER_2));
        placement.refresh();

        assertThatThrownBy(() -> placement.bucketOwners().add(WORKER_1))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void refreshIsRepeatableAndReplacesSnapshot() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(ANCHOR)))
                .thenReturn(rows(WORKER_1, WORKER_2))
                .thenReturn(rows(WORKER_2));
        CitusShardPlacement placement = new CitusShardPlacement(jdbcTemplate, ANCHOR);

        placement.refresh();
        assertThat(placement.bucketOwners()).containsExactly(WORKER_1, WORKER_2);

        placement.refresh();
        assertThat(placement.bucketOwners()).containsExactly(WORKER_2);
        assertThat(placement.shardCount()).isEqualTo(1);
    }

    @Test
    void duplicateShardPlacementFailsLoudWithoutStoringSnapshot() {
        // Simulates an unexpected topology (e.g. an orphaned placement the shardstate=1 filter did not
        // remove, or replication_factor > 1): the same shardid appears on two workers. refresh() must
        // throw rather than store a misaligned bucket -> worker snapshot that shifts every later bucket.
        List<PlacementRow> canned = List.of(
                new PlacementRow(100L, WORKER_1),
                new PlacementRow(100L, WORKER_2),
                new PlacementRow(101L, WORKER_1));
        CitusShardPlacement placement = placementReturning(canned);

        assertThatThrownBy(placement::refresh)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(ANCHOR)
                .hasMessageContaining("more than one active placement");

        // Nothing was stored; the placement is still in its unloaded state.
        assertThat(placement.shardCount()).isZero();
        assertThat(placement.bucketOwners()).isEmpty();
    }

    // --- expected-bucket-count guard (the three-arg constructor, as production wires it with the
    // locator's shardCount) ---

    @Test
    @SuppressWarnings("unchecked")
    void refreshCountMismatchAfterGoodLoadKeepsPreviousSnapshot() {
        // Mid-rebalance the shardstate=1 filter can momentarily drop a shard from the result. Storing the
        // shorter list would make workerForBucket(i) throw IndexOutOfBounds for the top buckets; the guard
        // must keep the previous good snapshot instead.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(ANCHOR)))
                .thenReturn(rows(WORKER_1, WORKER_2))
                .thenReturn(rows(WORKER_2));
        CitusShardPlacement placement = new CitusShardPlacement(jdbcTemplate, ANCHOR, () -> 2);

        placement.refresh();
        assertThat(placement.bucketOwners()).containsExactly(WORKER_1, WORKER_2);

        placement.refresh();
        assertThat(placement.bucketOwners())
                .as("a short refresh must not replace the previous good snapshot")
                .containsExactly(WORKER_1, WORKER_2);
        assertThat(placement.shardCount()).isEqualTo(2);
        assertThat(placement.workerForBucket(1)).isEqualTo(WORKER_2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void initialLoadCountMismatchLeavesPlacementUnloaded() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(ANCHOR))).thenReturn(rows(WORKER_1));
        CitusShardPlacement placement = new CitusShardPlacement(jdbcTemplate, ANCHOR, () -> 2);

        placement.refresh();

        assertThat(placement.shardCount()).isZero();
        assertThat(placement.bucketOwners()).isEmpty();
        assertThatThrownBy(() -> placement.workerForBucket(0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has not been loaded");
    }

    @Test
    @SuppressWarnings("unchecked")
    void matchingCountWithActiveSupplierStoresNormally() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(ANCHOR))).thenReturn(rows(WORKER_1, WORKER_2));
        CitusShardPlacement placement = new CitusShardPlacement(jdbcTemplate, ANCHOR, () -> 2);

        placement.refresh();

        assertThat(placement.bucketOwners()).containsExactly(WORKER_1, WORKER_2);
        assertThat(placement.shardCount()).isEqualTo(2);
    }
}
