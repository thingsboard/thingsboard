// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.StatementCallback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns one small {@link HikariDataSource} + {@link JdbcTemplate} per active Citus worker node, keyed
 * by Citus worker {@code groupId}, for smart-client shard routing. The app routes a single-shard KV
 * operation straight to the worker that owns that shard (resolved by {@link CitusShardPlacement}),
 * which requires a direct, app-reachable connection per worker — this registry holds those pools.
 *
 * <p><b>Discovery.</b> Active primary workers are read from the coordinator catalog
 * ({@code pg_dist_node}, excluding the coordinator group 0). Each worker pool connects to the SAME
 * database with the SAME credentials as the coordinator (the primary app datasource); only the
 * {@code host:port} differs.
 *
 * <p><b>Host overrides.</b> A worker's catalog {@code nodename} is what the coordinator advertises;
 * it is not always reachable from the app (NAT, docker networks, split DNS). {@code
 * database.citus.smart_routing.worker_host_overrides} maps {@code nodename -> host:port}; when an
 * override is present for a worker it wins, otherwise the raw catalog {@code nodename:nodeport} is
 * used. See {@link #effectiveHostPort}.
 *
 * <p><b>Startup reachability gate (fail-fast).</b> {@link #start()} (run at bean init) discovers
 * workers, builds every pool, and runs {@code SELECT 1} on each. If ANY worker is unreachable it
 * throws, naming the unreachable node(s) and the effective {@code host:port} it tried, and closes
 * pools opened so far. Smart routing has a hard app->worker reachability requirement, so the design
 * surfaces it at boot rather than silently degrading to coordinator-only.
 *
 * <p><b>Refresh.</b> {@link #refresh()} reconciles against the catalog: it adds pools for newly
 * appearing groups (with the same {@code SELECT 1} gate), retires (closes) pools for groups no
 * longer present, and rebuilds the pool of any still-present group whose advertised endpoint changed
 * (see the address-change section below). An unchanged-endpoint group is additionally probed for
 * WRITABILITY through its existing pool ({@link #probeWritable}) and rebuilt on failure — see the
 * failover-hardening section below. A scheduled {@code refresh()} prefers to LOG and keep the existing
 * good pool on a transient build/probe failure rather than kill a running app; startup
 * ({@code start()}) instead fails loud. The startup-vs-refresh distinction is the {@code failFast}
 * parameter of {@link #reconcile(boolean)}.
 *
 * <p><b>Failover hardening (Patroni-style HA workers).</b> In production each worker can be an HA
 * pair whose leader Patroni swaps on failover, rewriting {@code pg_dist_node} to the new leader's
 * name. Three unconditional measures make worker pools structurally unable to hold or create
 * connections to a non-writable (standby / read-only) server:
 * <ol>
 *   <li>every worker pool URL pins {@code targetServerType=primary} (see {@link #workerJdbcUrl}), so
 *       the driver refuses to CREATE a connection to a server reporting
 *       {@code transaction_read_only = on};</li>
 *   <li>Hikari borrow validation runs {@link #WRITABILITY_PROBE_QUERY}, so pooled connections pinned
 *       to a demoted machine are evicted at next borrow (and the replacement connect re-resolves DNS
 *       and is gated by 1);</li>
 *   <li>the scheduled reconcile probes unchanged-endpoint groups for writability and rebuilds on
 *       failure, healing topologies where the endpoint string never changes (VIP/proxy,
 *       re-promotion behind the same name).</li>
 * </ol>
 * The fast path after a failover, though, is the error-triggered refresh: {@code CitusShardRouter}
 * observes routed-operation failures and asks {@code CitusFailoverRefreshTrigger} for an immediate
 * (debounced) {@code CitusRoutingRefresher} run, which lands here as a {@link #refresh()} — the
 * scheduled tick remains only the backstop.
 *
 * <p><b>Time-bounded probes.</b> Both reconcile-path probes ({@link #probeWritable} and
 * {@link #buildAndProbe}'s {@code select 1}) run with a statement-scoped query timeout derived from
 * {@code worker_connection_timeout_ms}, and every worker pool URL carries a {@code socketTimeout}
 * default (see {@link #workerJdbcUrl}). Without these, a worker going silent mid-connection (network
 * partition, frozen VM — the very failover scenarios above) would block a probe until the OS TCP
 * retransmission timeout (~15 minutes on Linux); since {@link #reconcile(boolean)} runs under
 * {@code synchronized(this)}, one hung probe would wedge the scheduled tick, the failover trigger's
 * executor and {@code @PreDestroy} {@link #close()} with it.
 *
 * <p><b>Worker address changes self-heal on refresh.</b> A pool is keyed by {@code groupId}; if a
 * worker keeps its {@code groupId} but the catalog (or a changed {@code worker_host_overrides} entry)
 * advertises a NEW {@code host:port} for it (e.g. a worker pod restart changed its IP), reconcile
 * rebuilds that group's pool build-first-then-swap: it builds AND probes a pool against the new
 * endpoint first, atomically replaces the map entry, and only then closes the old pool. If the new
 * endpoint's probe fails during a scheduled refresh, the old pool is kept and the swap is retried on
 * the next refresh cycle — endpoint drift heals as soon as the new address becomes reachable, without
 * an application restart.
 *
 * <p><b>Thread-safety.</b> Reconfiguration ({@link #start()}/{@link #refresh()}/{@link #close()}) is
 * {@code synchronized}; reads ({@link #templateForGroup}/{@link #workerGroupIds})
 * are lock-free against the {@link ConcurrentHashMap}.
 *
 * <p><b>Lock-free-read invariant (load-bearing).</b> A lock-free reader is safe against the
 * {@code synchronized} {@link #reconcile(boolean)} mutations ONLY because of two properties:
 * <ul>
 *   <li>An unchanged worker group's pool is NEVER touched, and a group whose endpoint changed is
 *       swapped build-first: the replacement pool is fully built and probed BEFORE it atomically
 *       replaces the old entry in the {@link ConcurrentHashMap}, and the old pool is closed only AFTER
 *       the swap. A lock-free reader therefore always finds an open pool for a live group — either the
 *       old one or the new one, never a missing entry and never a closed-with-no-replacement gap.</li>
 *   <li>A retired worker's pool IS closed during reconcile, and a swapped-out old pool is closed right
 *       after its replacement is published. A routed op that fetched such a template microseconds
 *       earlier and then queries the just-closed pool gets a transient error. This is ACCEPTABLE and
 *       consistent with the design's explicit no-fallback stance: a worker that was removed /
 *       rebalanced / re-addressed away surfacing one transient error is the honest outcome, not
 *       something to mask.</li>
 * </ul>
 * A future maintainer must preserve the build-publish-close ordering: closing a live group's pool
 * BEFORE its replacement is published (or mutating a live pool's {@code HikariDataSource} in place)
 * WOULD BREAK lock-free reads — a reader could observe a closed pool for a still-live group with no
 * replacement. If you need anything richer, switch to an {@code AtomicReference} snapshot-swap
 * (publish a new immutable map and let old readers drain), as the sibling {@code CitusShardPlacement}
 * does.
 *
 * <p>The bean is active only outside the install profile (mirroring {@code CitusShardLocator}):
 * during install the schema / {@code pg_dist_node} may be empty and we do not route. It is a plain
 * class wired as a {@code @Bean} in {@code CitusDaoConfiguration}, gated there on
 * {@code @ConditionalOnProperty(database.citus.smart_routing.enabled=true)} + {@code @Profile("!install")}
 * AND, by virtue of living under that {@code @ConditionalOnProperty(database.citus.enabled=true)}
 * configuration, on Citus being enabled — so smart routing activates only when BOTH flags are on.
 * Spring still honours the {@code @PostConstruct} fail-fast {@code SELECT 1} gate and
 * {@code @PreDestroy} on a {@code @Bean}-created instance. The {@code database.citus.smart_routing.enabled}
 * key is always defined in YAML (it defaults to {@code database.citus.enabled}), so the condition
 * resolves deterministically.
 */
@Slf4j
public class CitusWorkerRegistry {

    private static final String WORKER_DISCOVERY_QUERY =
            "select groupid, nodename, nodeport from pg_dist_node " +
                    "where " + CitusRoutingQueries.activePrimaryWorkerPredicate("");

    // Canonical read_only_sql_transaction SQLSTATE: lets probeWritable() tell a real demotion from an inconclusive
    // failure (e.g. a Hikari borrow timeout where the probe never ran).
    static final String READ_ONLY_SQL_TRANSACTION_SQL_STATE = "25006";

    /**
     * Write-aware connection validation, shared by Hikari's {@code connectionTestQuery} on every worker
     * pool and by {@link #probeWritable} on the reconcile path: raises when the server is a standby
     * ({@code pg_is_in_recovery()}) or otherwise non-writable ({@code transaction_read_only = on}, e.g.
     * {@code default_transaction_read_only = on} after a Patroni demotion). The predicate deliberately
     * matches the condition the JDBC driver's {@code targetServerType=primary} keys off (see
     * {@link #workerJdbcUrl}), so connection creation, borrow-time validation and the scheduled probe
     * all agree on what "writable" means. A pooled connection pinned to a machine that got demoted is
     * evicted at next borrow, and its replacement connect re-resolves DNS and is gated by the driver.
     */
    static final String WRITABILITY_PROBE_QUERY =
            "DO $$ BEGIN IF pg_is_in_recovery() OR current_setting('transaction_read_only') = 'on' " +
                    "THEN RAISE EXCEPTION 'server is not writable (standby or read-only)' USING ERRCODE = '"
                    + READ_ONLY_SQL_TRANSACTION_SQL_STATE + "'; END IF; END $$";

    /**
     * Floor for the {@code socketTimeout} default merged into every worker pool URL (see
     * {@link #workerJdbcUrl}). Derived as three connect budgets so a routed op on a healthy-but-busy
     * worker is never cut short by a value tuned for connection establishment, and floored here so an
     * aggressively short {@code worker_connection_timeout_ms} cannot produce a socket timeout that kills
     * legitimate single-shard ops. Second granularity — pgjdbc's {@code socketTimeout} unit.
     */
    static final long MIN_WORKER_SOCKET_TIMEOUT_SECONDS = 30L;

    private static final RowMapper<CitusWorkerNode> WORKER_ROW_MAPPER = (rs, rowNum) ->
            new CitusWorkerNode(rs.getInt("groupid"), rs.getString("nodename"), rs.getInt("nodeport"));

    private final JdbcTemplate coordinatorJdbcTemplate;
    private final CitusSmartRoutingSettings settings;
    private final String coordinatorUrl;
    private final String dbName;
    private final String username;
    private final String password;

    /** groupId -> per-worker pool. Reads are lock-free; reconfiguration happens under {@code this}. */
    private final Map<Integer, WorkerPool> pools = new ConcurrentHashMap<>();

    public CitusWorkerRegistry(JdbcTemplate coordinatorJdbcTemplate,
                               CitusSmartRoutingSettings settings,
                               String coordinatorUrl,
                               String username,
                               String password) {
        this.coordinatorJdbcTemplate = coordinatorJdbcTemplate;
        this.settings = settings;
        this.coordinatorUrl = coordinatorUrl;
        this.dbName = databaseName(coordinatorUrl);
        this.username = username;
        this.password = password;
    }

    /**
     * Startup entry point (bean init): discover workers, build every pool, and run the fail-fast
     * {@code SELECT 1} reachability gate. Throws if any worker is unreachable.
     */
    @PostConstruct
    public synchronized void start() {
        log.info("Starting Citus worker registry (db={}, workerPoolSize={})", dbName, settings.getWorkerPoolSize());
        reconcile(true);
        log.info("Citus worker registry started with {} worker pool(s): {}", pools.size(), workerGroupIds());
    }

    /**
     * Reconciles pools against the live catalog. Adds pools for new groups, retires pools for groups
     * that disappeared, and rebuilds (build-first-then-swap) pools for groups whose advertised endpoint
     * changed. On a build/probe failure during a scheduled refresh ({@code failFast=false}) the group
     * is logged and skipped — a new group stays unpooled and a changed group keeps its old pool —
     * keeping any existing good pools intact rather than killing a running app.
     */
    public synchronized void refresh() {
        reconcile(false);
    }

    // Mutates {@code pools} under {@code synchronized(this)}. Upholds the class-level "Lock-free-read
    // invariant": unchanged groups are left untouched, endpoint-drifted groups are swapped build-first
    // (the new pool is built+probed, published atomically, and only THEN the old pool is closed) and
    // retired groups are closed. Do NOT reorder to close-before-publish and do NOT mutate a live pool
    // in place without switching reads to a snapshot-swap.
    private void reconcile(boolean failFast) {
        List<CitusWorkerNode> workers = discoverWorkers(coordinatorJdbcTemplate);

        Set<Integer> liveGroups = new HashSet<>();
        // Track pools opened in THIS startup pass so we can roll them all back on a fail-fast failure.
        List<WorkerPool> openedThisPass = new ArrayList<>();
        for (CitusWorkerNode worker : workers) {
            liveGroups.add(worker.groupId());
            HostPort hostPort = effectiveHostPort(worker, settings.getWorkerHostOverrides());
            String catalogEndpoint = hostPort.host() + ":" + hostPort.port();
            WorkerPool existing = pools.get(worker.groupId());
            if (existing != null && existing.endpoint().equals(catalogEndpoint)) {
                if (probeWritable(existing)) {
                    // Unchanged worker — keep its pool untouched (the lock-free-read invariant).
                    continue;
                }
                // The catalog endpoint did NOT change but the pool can no longer reach a writable server
                // — a demotion behind a stable name (VIP/proxy topology, or a re-promotion behind the
                // same DNS name). Fall through to the build-first-then-swap path: a healthy rebuild
                // swaps in with fresh DNS resolution; a failed rebuild (the endpoint still resolves to a
                // standby — connection creation is refused via targetServerType=primary) keeps the old
                // pool and retries next cycle, same as the endpoint-drift path.
                log.warn("Citus worker group {} (nodename '{}') pool at {} failed the writability probe; " +
                        "attempting a pool rebuild.", worker.groupId(), worker.nodeName(), catalogEndpoint);
            }
            // A new group, a still-present group whose catalog endpoint changed (a host/port change, or
            // an added/changed host override), or an unchanged-endpoint group that failed the writability
            // probe. All take the build-first path: probe the new endpoint BEFORE touching the map, so a
            // failure leaves the current state (no pool / old pool) fully intact.
            try {
                WorkerPool pool = buildAndProbe(worker, hostPort);
                // Atomic CHM replace: lock-free readers observe either the old pool or the new one,
                // never a missing entry. The old pool is closed only AFTER the new one is published —
                // an in-flight reader still holding the just-closed old pool gets a transient error,
                // the same accepted-transient class as the retirement path below.
                pools.put(worker.groupId(), pool);
                openedThisPass.add(pool);
                if (existing != null && existing.endpoint().equals(catalogEndpoint)) {
                    existing.close();
                    log.info("Rebuilt Citus worker pool for group {} (nodename '{}') at {} after a failed writability probe",
                            worker.groupId(), worker.nodeName(), pool.endpoint());
                } else if (existing != null) {
                    existing.close();
                    log.info("Rebuilt Citus worker pool for group {} (nodename '{}'): endpoint changed from {} to {}",
                            worker.groupId(), worker.nodeName(), existing.endpoint(), pool.endpoint());
                } else {
                    log.info("Opened Citus worker pool for group {} at {}", worker.groupId(), pool.endpoint());
                }
            } catch (Exception e) {
                if (failFast) {
                    // Roll back pools opened during this startup pass, then fail loud naming the node.
                    // This rollback assumes failFast runs only at boot with an empty pools map (the single
                    // @PostConstruct caller): every removed entry was opened this pass, never a rebuilt
                    // replacement whose predecessor is already closed. Keep that invariant if adding callers.
                    for (WorkerPool opened : openedThisPass) {
                        pools.remove(opened.groupId());
                        opened.close();
                    }
                    throw new IllegalStateException("Citus worker group " + worker.groupId() + " (nodename '" +
                            worker.nodeName() + "') is unreachable at effective endpoint " + hostPort.host() + ":" +
                            hostPort.port() + "; smart routing requires every active worker to be reachable from the " +
                            "application. Check database.citus.smart_routing.worker_host_overrides and network/firewall.", e);
                }
                if (existing != null && existing.endpoint().equals(catalogEndpoint)) {
                    log.warn("Citus worker group {} (nodename '{}') failed the writability probe at {} and the rebuild " +
                                    "attempt found no writable server there either (still a standby / read-only); keeping " +
                                    "the old pool until a later refresh succeeds.",
                            worker.groupId(), worker.nodeName(), catalogEndpoint, e);
                } else if (existing != null) {
                    log.warn("Citus worker group {} (nodename '{}') endpoint changed from {} to {}, but the new endpoint " +
                                    "is not reachable yet; keeping the old pool until a later refresh succeeds.",
                            worker.groupId(), worker.nodeName(), existing.endpoint(), catalogEndpoint, e);
                } else {
                    log.warn("Citus worker group {} (nodename '{}') is unreachable at {} during refresh; keeping existing " +
                                    "pools unchanged and skipping this group until the next refresh.",
                            worker.groupId(), worker.nodeName(), catalogEndpoint, e);
                }
            }
        }

        // Retire pools for groups no longer present in the catalog.
        for (Integer groupId : new ArrayList<>(pools.keySet())) {
            if (!liveGroups.contains(groupId)) {
                WorkerPool removed = pools.remove(groupId);
                if (removed != null) {
                    log.info("Retiring Citus worker pool for group {} (no longer in pg_dist_node)", groupId);
                    removed.close();
                }
            }
        }
    }

    /**
     * Runs {@link #WRITABILITY_PROBE_QUERY} through an unchanged-endpoint group's existing pool during a
     * scheduled reconcile. Returns {@code false} ONLY on definitive not-writable evidence: a
     * {@code read_only_sql_transaction} error ({@link #READ_ONLY_SQL_TRANSACTION_SQL_STATE 25006}) somewhere in
     * the failure's cause chain, meaning the probe actually EXECUTED and the server refused the write — a real
     * demotion behind a stable endpoint (VIP/proxy topology, re-promotion behind the same DNS name). A {@code false}
     * sends the group down the build-first-then-swap rebuild path.
     * <p>
     * Every other failure is treated as INCONCLUSIVE and returns {@code true} (KEEP the pool): a bare Hikari borrow
     * timeout means the pool is saturated (the probe never ran), and a query timeout / transient drop is not proof
     * of demotion. Tearing the pool down on those would churn a healthy-but-busy worker every reconcile
     * (~10s), re-triggering via the failover path. A genuinely DOWN worker with an unchanged endpoint already keeps
     * its old pool today because the subsequent buildAndProbe fails and the catch there retains it, so keeping the
     * pool on an inconclusive probe is no worse; re-promotion behind DNS is self-healed by borrow-validation +
     * creation-gated re-resolve. Package-private (and overridable) so reconcile() is unit-testable without live
     * worker databases.
     */
    boolean probeWritable(WorkerPool pool) {
        try {
            // Statement-scoped query timeout: a connection handed out inside Hikari's aliveness-bypass
            // window skips borrow validation, so a worker gone silent mid-connection could otherwise hang
            // this probe until the OS TCP timeout and wedge reconcile()'s monitor (see the class javadoc).
            // Scoped to the probe statement only — the pool's routed-op template is deliberately NOT
            // globally bounded (the URL-level socketTimeout covers it).
            StatementCallback<Void> boundedProbe = statement -> {
                statement.setQueryTimeout(probeQueryTimeoutSeconds());
                statement.execute(WRITABILITY_PROBE_QUERY);
                return null;
            };
            pool.template().execute(boundedProbe);
            return true;
        } catch (Exception e) {
            if (isReadOnlySqlTransaction(e)) {
                log.warn("Citus worker group {} at {} refused the writability probe as read-only (SQLSTATE {}); " +
                        "treating it as a demotion and rebuilding the pool.", pool.groupId(), pool.endpoint(),
                        READ_ONLY_SQL_TRANSACTION_SQL_STATE, e);
                return false;
            }
            // One line at INFO: under sustained saturation every debounced failover refresh re-runs this probe,
            // so a full stack trace here would repeat every few seconds for a deliberately benign outcome.
            log.info("Writability probe for Citus worker group {} at {} was inconclusive (no read_only_sql_transaction " +
                    "evidence — likely borrow timeout / saturation or a transient error); keeping the pool: {}",
                    pool.groupId(), pool.endpoint(), e.toString());
            log.debug("Inconclusive writability probe details for Citus worker group {} at {}", pool.groupId(), pool.endpoint(), e);
            return true;
        }
    }

    // True when the read_only_sql_transaction SQLSTATE (25006) appears anywhere in the failure's cause chain
    // (including the getNextException chain PostgreSQL uses on SQLExceptions).
    static boolean isReadOnlySqlTransaction(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && sqlStateChainContainsReadOnly(sqlException)) {
                return true;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return false;
    }

    private static boolean sqlStateChainContainsReadOnly(SQLException sqlException) {
        for (SQLException current = sqlException; current != null; current = current.getNextException()) {
            if (READ_ONLY_SQL_TRANSACTION_SQL_STATE.equals(current.getSQLState())) {
                return true;
            }
            if (current.getNextException() == current) {
                break;
            }
        }
        return false;
    }

    // Package-private (and overridable) so reconcile() is unit-testable without live worker databases.
    WorkerPool buildAndProbe(CitusWorkerNode worker, HostPort hostPort) {
        String endpoint = hostPort.host() + ":" + hostPort.port();
        String jdbcUrl = workerJdbcUrl(coordinatorUrl, endpoint, workerSocketTimeoutSeconds());
        HikariDataSource dataSource = null;
        try {
            HikariDataSource ds = new HikariDataSource();
            ds.setPoolName("citus-worker-" + worker.groupId());
            ds.setJdbcUrl(jdbcUrl);
            ds.setUsername(username);
            ds.setPassword(password);
            // Keep pools minimal: these are short single-shard ops.
            ds.setMaximumPoolSize(settings.getWorkerPoolSize());
            ds.setMinimumIdle(0);
            // Single, configurable connect budget governing BOTH the boot reachability probe (the
            // {@code SELECT 1} below, whose first connect must complete within this budget) AND the
            // runtime borrow-wait for a routed single-shard op. Deliberately short: workers live on the
            // same Citus cluster / LAN as the coordinator, so a healthy connect is sub-second; the
            // design's promise is to surface an unreachable worker FAST at boot rather than let Hikari's
            // 30s default stack up to minutes across several down workers before the fail-fast gate
            // fires. Hikari enforces a 250ms floor; the 10s default leaves comfortable headroom for a
            // momentarily busy-but-healthy worker while still failing boot quickly when one is truly down.
            long connectionTimeoutMs = settings.getWorkerConnectionTimeoutMs();
            ds.setConnectionTimeout(connectionTimeoutMs);
            // Validation timeout must stay strictly < connectionTimeout so probe/borrow validation cannot
            // outlast the connect budget; derived as half of it (no separate YAML knob — one is cleaner).
            long validationTimeoutMs = connectionTimeoutMs / 2;
            ds.setValidationTimeout(validationTimeoutMs);
            // Write-aware borrow validation (replaces the driver-level isValid() ping, which a read-only
            // standby answers happily): a pooled connection pinned to a demoted server is evicted at next
            // borrow, and the replacement connect re-resolves DNS and is itself gated by
            // targetServerType=primary on the pool URL.
            ds.setConnectionTestQuery(WRITABILITY_PROBE_QUERY);
            dataSource = ds;
            JdbcTemplate template = new JdbcTemplate(ds);
            // Boot reachability probe. Deliberately just "select 1": creation-time WRITABILITY is already
            // enforced by the driver (targetServerType=primary), so this only asserts the endpoint answers.
            // Statement-scoped query timeout for the same reason as probeWritable(): an endpoint that goes
            // silent after connecting must not hold reconcile()'s monitor until the OS TCP timeout.
            StatementCallback<Void> boundedReachabilityProbe = statement -> {
                statement.setQueryTimeout(probeQueryTimeoutSeconds());
                try (ResultSet resultSet = statement.executeQuery("select 1")) {
                    resultSet.next();
                }
                return null;
            };
            template.execute(boundedReachabilityProbe);
            return new WorkerPool(worker.groupId(), endpoint, ds, template);
        } catch (RuntimeException e) {
            if (dataSource != null) {
                dataSource.close();
            }
            throw e;
        }
    }

    /**
     * Returns the {@link JdbcTemplate} for a worker group, or throws {@link CitusNoWorkerPoolException}
     * if no pool exists for it — a dedicated type so {@code CitusShardRouter} can observe the no-pool
     * state as a failover signature and trigger the debounced refresh that heals it.
     *
     * <p>Lock-free read against the {@link ConcurrentHashMap} — see the class-level "Lock-free-read
     * invariant": this is safe only because a live group's entry always holds an open pool (unchanged
     * groups are untouched; endpoint changes are swapped build-first, old pool closed after publish).
     */
    public JdbcTemplate templateForGroup(int groupId) {
        WorkerPool pool = pools.get(groupId);
        if (pool == null) {
            throw new CitusNoWorkerPoolException("No Citus worker pool for group " + groupId +
                    "; known worker groups: " + workerGroupIds());
        }
        return pool.template();
    }

    public Set<Integer> workerGroupIds() {
        return Set.copyOf(pools.keySet());
    }

    @PreDestroy
    public synchronized void close() {
        for (Integer groupId : new ArrayList<>(pools.keySet())) {
            WorkerPool pool = pools.remove(groupId);
            if (pool != null) {
                pool.close();
            }
        }
        log.info("Closed all Citus worker pools");
    }

    /** Discovers active primary worker nodes from the coordinator catalog. Package-private for testing. */
    static List<CitusWorkerNode> discoverWorkers(JdbcTemplate coordinatorJdbcTemplate) {
        return coordinatorJdbcTemplate.query(WORKER_DISCOVERY_QUERY, WORKER_ROW_MAPPER);
    }

    /**
     * Computes a worker's effective {@code host:port}. If {@code overrides} contains the worker's
     * {@code nodeName}, the override value wins (parsed as {@code host:port}); otherwise the raw
     * catalog {@code nodeName:nodePort} is used.
     *
     * <p><b>Override contract.</b> The value is a {@code host:port} pair; surrounding and internal
     * whitespace around the components is trimmed. The port is the substring after the LAST colon, so
     * a bracketed IPv6 form like {@code [::1]:5432} parses to host {@code [::1]} (kept bracketed) and
     * port {@code 5432}. A value with no colon, a blank host, or a non-numeric port is malformed and
     * throws an {@link IllegalArgumentException} naming the offending node and value.
     *
     * <p>Package-private and static so the override logic is unit-testable without a database.
     */
    static HostPort effectiveHostPort(CitusWorkerNode node, Map<String, String> overrides) {
        String override = overrides.get(node.nodeName());
        if (override == null) {
            return new HostPort(node.nodeName(), node.nodePort());
        }
        String value = override.trim();
        int lastColon = value.lastIndexOf(':');
        if (lastColon <= 0 || lastColon == value.length() - 1) {
            throw new IllegalArgumentException("Malformed worker_host_overrides entry for node '" + node.nodeName() +
                    "': '" + override + "' (expected 'host:port')");
        }
        String host = value.substring(0, lastColon).trim();
        String portStr = value.substring(lastColon + 1).trim();
        if (host.isEmpty()) {
            throw new IllegalArgumentException("Malformed worker_host_overrides entry for node '" + node.nodeName() +
                    "': '" + override + "' (blank host)");
        }
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Malformed worker_host_overrides entry for node '" + node.nodeName() +
                    "': '" + override + "' (port '" + portStr + "' is not a number)", e);
        }
        return new HostPort(host, port);
    }

    /**
     * Derives a worker pool JDBC URL from the coordinator URL by substituting ONLY its {@code host:port}
     * authority with {@code endpoint}, preserving the database path AND the full query string. This keeps
     * coordinator connection parameters (e.g. {@code ssl}, {@code sslmode}, {@code currentSchema},
     * {@code ApplicationName}, {@code socketTimeout}) on every worker pool instead of dropping them — a
     * worker pool talks to the SAME database with the SAME settings as the coordinator, only on a
     * different node. Expects the standard {@code jdbc:postgresql://host:port/db[?query]} form; the
     * authority is the segment between {@code //} and the next {@code /} (or {@code ?}, or end of string).
     *
     * <p>{@code targetServerType=primary} is then merged into the query string (unconditionally, replacing
     * any coordinator-supplied value): the PostgreSQL JDBC driver refuses to ESTABLISH a connection to a
     * server reporting {@code transaction_read_only = on} — a real standby ({@code pg_is_in_recovery()})
     * or a {@code default_transaction_read_only = on} node. A worker pool must never talk to a
     * non-writable server (e.g. a Patroni replica that was just demoted-into / still answers on the
     * pool's endpoint), and enforcing this at connection creation complements the borrow-time
     * {@link #WRITABILITY_PROBE_QUERY} validation.
     *
     * <p>{@code socketTimeout=<socketTimeoutSeconds>} is merged as a DEFAULT (a coordinator-supplied value
     * wins): it bounds every read on a worker connection, so a worker host that freezes mid-connection
     * (network partition, frozen VM) surfaces as a bounded failure instead of an OS-TCP-timeout hang —
     * acceptable pool-wide because routed ops are short single-shard statements (see the class javadoc's
     * time-bounded-probes section). Package-private for testing.
     */
    static String workerJdbcUrl(String coordinatorUrl, String endpoint, long socketTimeoutSeconds) {
        int authorityStart = coordinatorUrl.indexOf("//");
        if (authorityStart < 0) {
            throw new IllegalArgumentException("Cannot derive worker url from coordinator datasource url: " + coordinatorUrl);
        }
        authorityStart += 2;
        int authorityEnd = authorityStart;
        while (authorityEnd < coordinatorUrl.length()) {
            char c = coordinatorUrl.charAt(authorityEnd);
            if (c == '/' || c == '?') {
                break;
            }
            authorityEnd++;
        }
        String workerUrl = coordinatorUrl.substring(0, authorityStart) + endpoint + coordinatorUrl.substring(authorityEnd);
        workerUrl = mergeQueryParameter(workerUrl, "targetServerType", "primary", true);
        return mergeQueryParameter(workerUrl, "socketTimeout", String.valueOf(socketTimeoutSeconds), false);
    }

    /**
     * Merges {@code name=value} into a JDBC URL's query string: appends it when absent (creating the
     * query string if the URL has none). When {@code name} is already present, {@code replaceExisting}
     * decides who wins: {@code true} replaces every occurrence (the merged value is re-appended at the
     * end, other parameters keep their order), {@code false} returns the URL unchanged (the existing,
     * operator-supplied value wins). Package-private for testing via {@link #workerJdbcUrl}.
     */
    private static String mergeQueryParameter(String url, String name, String value, boolean replaceExisting) {
        int queryStart = url.indexOf('?');
        if (queryStart < 0) {
            return url + "?" + name + "=" + value;
        }
        StringBuilder merged = new StringBuilder(url.substring(0, queryStart)).append('?');
        for (String parameter : url.substring(queryStart + 1).split("&")) {
            if (parameter.isEmpty()) {
                continue;
            }
            if (parameter.split("=", 2)[0].equals(name)) {
                if (!replaceExisting) {
                    return url;
                }
                continue;
            }
            merged.append(parameter).append('&');
        }
        return merged.append(name).append('=').append(value).toString();
    }

    /**
     * Query timeout (seconds — pgjdbc granularity) for the two reconcile-path probes, derived from the
     * connect budget: a probe against a healthy worker answers well within the time a connect is allowed
     * to take. Rounded up so a sub-second budget never truncates to 0 (which pgjdbc treats as unbounded).
     */
    private int probeQueryTimeoutSeconds() {
        return (int) ((settings.getWorkerConnectionTimeoutMs() + 999) / 1000);
    }

    /** The {@code socketTimeout} default for worker pool URLs — see {@link #MIN_WORKER_SOCKET_TIMEOUT_SECONDS}. */
    private long workerSocketTimeoutSeconds() {
        return Math.max(MIN_WORKER_SOCKET_TIMEOUT_SECONDS, 3 * ((settings.getWorkerConnectionTimeoutMs() + 999) / 1000));
    }

    /**
     * Parses the database name from a JDBC URL: the path segment after {@code host:port}, with any
     * {@code ?query} stripped. Package-private for testing.
     */
    static String databaseName(String jdbcUrl) {
        String url = jdbcUrl;
        int query = url.indexOf('?');
        if (query >= 0) {
            url = url.substring(0, query);
        }
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == url.length() - 1) {
            throw new IllegalArgumentException("Cannot parse database name from datasource url: " + jdbcUrl);
        }
        return url.substring(lastSlash + 1);
    }

    /** A parsed {@code host:port} endpoint. */
    record HostPort(String host, int port) {
    }

    /** One worker's pool: its Hikari datasource, the wrapping template, and a display endpoint. Package-private for testing. */
    record WorkerPool(int groupId, String endpoint, HikariDataSource dataSource, JdbcTemplate template) {
        void close() {
            dataSource.close();
        }
    }
}
