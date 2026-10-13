// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.thingsboard.server.dao.sql.citus.CitusShardLocator;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * The single entry point consumers touch for Citus smart-client shard routing: given a distribution
 * key (or a pre-computed bucket) it returns the {@link JdbcTemplate} bound to the worker node that
 * physically owns that shard, so a single-shard KV operation goes straight to the owning worker
 * instead of round-tripping through the coordinator.
 *
 * <p>It composes the three collaborators built in the earlier tasks:
 * <ol>
 *   <li>{@link CitusShardLocator#bucket(UUID)} maps the distribution key to a stable bucket index;</li>
 *   <li>{@link CitusShardPlacement#workerForBucket(int)} maps that bucket to its owning worker node;</li>
 *   <li>{@link CitusWorkerRegistry#templateForGroup(int)} returns the pooled template for that worker
 *       group.</li>
 * </ol>
 *
 * <p><b>Per co-location group.</b> A router instance is bound to one co-located shard family. The KV
 * tables ({@code attribute_kv} + {@code ts_kv_latest}) are a single co-location group, so there is
 * exactly one router bean ({@code kvShardRouter}) for them, built on the KV {@link CitusShardLocator}
 * and a {@link CitusShardPlacement} anchored on the same group. The class is deliberately generic and
 * holds no table-specific logic: a future sharded-and-co-located family is supported by constructing
 * {@code new CitusShardRouter(<its locator>, <its placement>, workerRegistry, settings)} and a
 * consumer that injects it — no new routing code is required.
 *
 * <p><b>Bucket-alignment invariant (load-bearing).</b> Bucket {@code i} produced by the
 * locator MUST name the same shard that the placement reports for bucket {@code i}; otherwise every
 * op silently routes to the wrong worker. The two are aligned by construction (both order shards
 * ascending by {@code (shardminvalue)::int}), and the alignment is hard-asserted once at startup by
 * {@link #assertBucketAlignment(int, int, String)}. The router is also built on the SAME
 * {@link CitusShardLocator} bean instance that the write-queue partitioner uses, so write-queue
 * bucket {@code i} and router bucket {@code i} are the identical function.
 *
 * <p><b>Failover observation.</b> As the documented single entry point for routed operations, the
 * router is also where worker failovers are DETECTED: {@link #routedQuery(UUID, String, RowMapper,
 * Object...)}, its extractor variant, and {@link #routedWrite(int, Function)} wrap execution and, when
 * a failure carries a failover signature ({@link #isFailoverFailure} — SQLSTATE {@code 25006} from a
 * demoted worker, a worker connection-acquisition failure, or a missing worker pool), notify the
 * {@link CitusFailoverRefreshTrigger} (debounced, asynchronous) and rethrow the original exception
 * UNCHANGED — caller semantics (write-queue retries, DAO error handling) are exactly as before.
 */
@Slf4j
public class CitusShardRouter {

    private final CitusShardLocator shardLocator;
    private final CitusShardPlacement placement;
    private final CitusWorkerRegistry workerRegistry;
    private final CitusSmartRoutingSettings settings;
    private final CitusFailoverRefreshTrigger failoverTrigger;

    public CitusShardRouter(CitusShardLocator shardLocator,
                            CitusShardPlacement placement,
                            CitusWorkerRegistry workerRegistry,
                            CitusSmartRoutingSettings settings) {
        this(shardLocator, placement, workerRegistry, settings, null);
    }

    public CitusShardRouter(CitusShardLocator shardLocator,
                            CitusShardPlacement placement,
                            CitusWorkerRegistry workerRegistry,
                            CitusSmartRoutingSettings settings,
                            CitusFailoverRefreshTrigger failoverTrigger) {
        this.shardLocator = shardLocator;
        this.placement = placement;
        this.workerRegistry = workerRegistry;
        this.settings = settings;
        this.failoverTrigger = failoverTrigger;
    }

    /**
     * Returns the {@link JdbcTemplate} for the worker that owns the shard the given distribution key
     * hashes to: {@code bucket = locator.bucket(key)} then {@link #forBucket(int)}.
     * <p>
     * Package-private on purpose: production routing must go through {@link #routedQuery}/{@link #routedWrite},
     * which wrap execution with failover observation. This helper exposes the raw template (no failover
     * detection) and exists only for the routing unit/integration tests in this package.
     */
    @VisibleForTesting
    JdbcTemplate forKey(UUID distributionKey) {
        return forBucket(shardLocator.bucket(distributionKey));
    }

    /**
     * Returns the {@link JdbcTemplate} for the worker that owns the given bucket. An internal helper behind
     * {@link #forKey}; package-private for the same reason (see {@link #forKey}).
     *
     * @throws IllegalStateException     if the placement snapshot has not been loaded
     * @throws IndexOutOfBoundsException if {@code bucket} is out of range for the loaded placement
     */
    @VisibleForTesting
    JdbcTemplate forBucket(int bucket) {
        CitusWorkerNode node = placement.workerForBucket(bucket);
        return workerRegistry.templateForGroup(node.groupId());
    }

    /** Number of shard buckets in this co-location group (delegates to the locator). */
    public int shardCount() {
        return shardLocator.shardCount();
    }

    /** Whether Citus smart routing is enabled (delegates to settings). */
    public boolean isEnabled() {
        return settings.isEnabled();
    }

    /**
     * Null-tolerant routing check. The {@code kvShardRouter} bean is optional (absent unless smart routing is on)
     * and left unset in sliced unit tests, so consumers inject it with {@code required = false}; this centralizes
     * the {@code router != null && router.isEnabled()} guard.
     */
    public static boolean isRouting(CitusShardRouter router) {
        return router != null && router.isEnabled();
    }

    /**
     * Runs a single-shard read straight on the worker that owns the shard the distribution key hashes to:
     * {@code forKey(distributionKey).query(sql, mapper, args)}. Centralizes the per-DAO smart-read so a change
     * to how a routed read is issued lives in one place. Failover-observed (see the class Javadoc).
     */
    public <R> List<R> routedQuery(UUID distributionKey, String sql, RowMapper<R> mapper, Object... args) {
        return observed(shardLocator.bucket(distributionKey), template -> template.query(sql, mapper, args));
    }

    /**
     * {@link ResultSetExtractor} variant of {@link #routedQuery(UUID, String, RowMapper, Object...)}. Also fits
     * single-shard row-returning DML ({@code DELETE ... RETURNING}): the worker pools are read-write and address
     * the distributed table by its logical name, and a single statement is atomic on its own, so no coordinator
     * transaction wrapper is needed around it. Failover-observed (see the class Javadoc).
     */
    public <R> R routedQuery(UUID distributionKey, String sql, ResultSetExtractor<R> extractor, Object... args) {
        return observed(shardLocator.bucket(distributionKey), template -> template.query(sql, extractor, args));
    }

    /**
     * Runs a single-shard write against the worker template owning the given bucket, with the same failover
     * observation as {@link #routedQuery(UUID, String, RowMapper, Object...)}. This is the write-queue flush
     * path ({@code CitusKvWriteQueueSupport}), which already partitioned its work by bucket; {@code work}
     * receives the routed template and its result is returned as-is.
     */
    public <R> R routedWrite(int bucket, Function<JdbcTemplate, R> work) {
        return observed(bucket, work);
    }

    /**
     * Resolves the bucket's worker template and runs {@code work} on it. On failure: if the exception
     * carries a failover signature ({@link #isFailoverFailure}), the {@link CitusFailoverRefreshTrigger}
     * is notified (asynchronous, debounced, never throws); the original exception is then rethrown
     * UNCHANGED either way, so callers observe exactly the pre-observation failure behavior. Template
     * resolution runs INSIDE the observed try: a bucket mapped to an unpooled group (a reconcile skipped
     * a worker whose pool build failed) throws {@link CitusNoWorkerPoolException}, which must fire the
     * trigger too — otherwise that state would only heal on the next scheduled tick.
     */
    private <R> R observed(int bucket, Function<JdbcTemplate, R> work) {
        CitusWorkerNode node = placement.workerForBucket(bucket);
        try {
            JdbcTemplate template = workerRegistry.templateForGroup(node.groupId());
            return work.apply(template);
        } catch (RuntimeException e) {
            if (failoverTrigger != null && isFailoverFailure(e)) {
                failoverTrigger.requestRefresh("routed operation on worker group " + node.groupId() +
                        " failed with a failover signature: " + e.getMessage());
            }
            throw e;
        }
    }

    /**
     * Whether a routed-operation failure looks like a worker failover — the signatures a Patroni-style
     * leader swap (or its reconcile aftermath) produces, and nothing broader (constraint violations, syntax
     * errors and in-query timeouts must never trigger a refresh):
     * <ul>
     *   <li>SQLSTATE {@code 25006} ("cannot execute ... in a read-only transaction") anywhere in the cause
     *       chain — a pooled connection pinned to the demoted old leader (Spring wraps the
     *       {@link SQLException} in a {@code DataAccessException} subtype);</li>
     *   <li>a connection-acquisition failure: {@link CannotGetJdbcConnectionException} at the top of the
     *       chain, or {@link SQLTransientConnectionException} (Hikari's borrow-timeout type) anywhere in
     *       it — connect-refused and driver-rejected-standby ({@code targetServerType=primary}) failures
     *       both surface through these wrappers on the routed path;</li>
     *   <li>{@link CitusNoWorkerPoolException} — the bucket resolved to a worker group the registry has
     *       no pool for (a reconcile skipped a worker whose pool build failed while the placement still
     *       maps buckets to it); a refresh re-attempts the pool build. Deliberately matched by the
     *       dedicated type, NOT by generic {@link IllegalStateException} (which also covers
     *       placement-not-loaded and bucket-alignment states that a refresh cannot fix).</li>
     * </ul>
     * Package-private for testing.
     */
    static boolean isFailoverFailure(Throwable failure) {
        if (failure instanceof CannotGetJdbcConnectionException || failure instanceof CitusNoWorkerPoolException) {
            return true;
        }
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLTransientConnectionException) {
                return true;
            }
            if (cause instanceof SQLException sqlException && "25006".equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Hard guard for the bucket-alignment invariant: the placement's bucket count MUST
     * equal the locator's bucket count, otherwise the placement bucket indices do not line up with the
     * locator's buckets (duplicate / missing shards or wrong ordering) and every op would silently
     * route to the wrong worker. Throws an {@link IllegalStateException} naming both counts and the
     * anchor table. Run once at startup (bean wiring) where both are loaded; not re-run on the
     * scheduled refresh path, where a transient mid-rebalance skew must not crash a running app.
     */
    public static void assertBucketAlignment(int locatorShardCount, int placementShardCount, String anchorTable) {
        if (locatorShardCount != placementShardCount) {
            throw new IllegalStateException("Citus bucket-alignment invariant violated for anchor table '" +
                    anchorTable + "': CitusShardLocator reports " + locatorShardCount + " bucket(s) but " +
                    "CitusShardPlacement reports " + placementShardCount + " bucket(s). The placement bucket " +
                    "indices do not line up with the locator's buckets (duplicate or missing shards, or a " +
                    "different shard ordering), which would silently route every operation to the wrong worker. " +
                    "Refusing to start smart routing.");
        }
    }
}
