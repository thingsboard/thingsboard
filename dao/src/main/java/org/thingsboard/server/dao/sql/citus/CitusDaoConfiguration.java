// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.dao.sql.citus.routing.CitusFailoverRefreshTrigger;
import org.thingsboard.server.dao.sql.citus.routing.CitusRoutingRefreshScheduler;
import org.thingsboard.server.dao.sql.citus.routing.CitusRoutingRefresher;
import org.thingsboard.server.dao.sql.citus.routing.CitusShardPlacement;
import org.thingsboard.server.dao.sql.citus.routing.CitusShardRouter;
import org.thingsboard.server.dao.sql.citus.routing.CitusSmartRoutingSettings;
import org.thingsboard.server.dao.sql.citus.routing.CitusWorkerRegistry;

@Configuration
@ConditionalOnProperty(prefix = "database.citus", name = "enabled", havingValue = "true")
public class CitusDaoConfiguration {

    /**
     * Provides the {@link CitusShardLocator} and, in the running server, loads the Citus shard ranges once at
     * startup. The eager refresh is restricted to the running server via {@code @Profile}: the install/upgrade
     * context gets a no-refresh variant ({@link #citusShardLocatorNoRefresh}) because it builds the bean before
     * the schema exists yet still needs it for {@code CitusQueuePartitioner}.
     * Shard ranges are stable: a Citus rebalance moves shard placements between workers but does not
     * change the hash ranges themselves, so a single startup {@link CitusShardLocator#refresh()} is
     * sufficient for correct shard-aligned routing. There is intentionally no scheduled shard-range
     * re-refresh: picking up an admin-driven shard-count change requires a restart by design.
     * <p>
     * Co-location invariant: this single locator is bound to {@code attribute_kv}
     * ({@link CitusTables#DISTRIBUTED_TABLES}.get(0), the {@link CitusShardLocator} default), yet it is
     * injected (via {@code CitusQueuePartitioner}) into both {@code JpaAttributeDao} (attribute_kv) and
     * {@code SqlTimeseriesLatestDao} (ts_kv_latest). Its {@link CitusShardLocator#bucket} mapping is valid
     * for ts_kv_latest too only because the two KV tables are co-located: they are created with
     * {@code colocate_with} in {@code DefaultCitusSchemaService}, so they share identical shard hash ranges.
     * If a future distributed table that is NOT co-located with attribute_kv is added, it would need its own
     * locator instance.
     */
    @Bean
    @Profile("!install")
    public CitusShardLocator citusShardLocator(JdbcTemplate jdbcTemplate, CitusSettings settings) {
        CitusShardLocator locator = new CitusShardLocator(jdbcTemplate, settings);
        // Eager, synchronous load so routing is shard-aligned from the first KV write — it completes before any
        // bucket() call, so there is no refresh-vs-write race. Active only outside the install profile (see
        // citusShardLocatorNoRefresh): the running server always starts after the schema has been installed.
        locator.refresh();
        return locator;
    }

    @Bean
    @Profile("install")
    public CitusShardLocator citusShardLocatorNoRefresh(JdbcTemplate jdbcTemplate, CitusSettings settings) {
        // The install/upgrade application builds this context before the schema exists on a fresh database and never
        // routes KV writes through the locator, yet CitusQueuePartitioner still depends on the bean. Provide it WITHOUT
        // the eager refresh: querying 'attribute_kv'::regclass here would throw "relation does not exist" and fail
        // context startup. Same bean type, so it is injected wherever CitusShardLocator is required.
        return new CitusShardLocator(jdbcTemplate, settings);
    }

    @Bean
    public CitusSchemaService citusSchemaService(JdbcTemplate jdbcTemplate, CitusSettings settings) {
        return new DefaultCitusSchemaService(jdbcTemplate, settings);
    }

    /**
     * Smart-routing worker registry: one connection pool per active Citus worker, keyed by worker
     * {@code groupId}. Defined as a {@code @Bean} (the class is no longer a {@code @Component}) so
     * activation requires BOTH {@code database.citus.enabled} (this whole configuration is gated on it)
     * AND {@code database.citus.smart_routing.enabled} — a {@code @Component} gated only on the latter
     * could wrongly activate with Citus off. The {@code @Value} parameters supply the
     * coordinator datasource coordinates each worker pool reuses (same db/credentials, different
     * host:port). Spring still runs the registry's {@code @PostConstruct} fail-fast reachability gate
     * and {@code @PreDestroy} on this {@code @Bean}-created instance.
     */
    @Bean
    @CitusSmartRoutingComponent
    public CitusWorkerRegistry citusWorkerRegistry(JdbcTemplate jdbcTemplate,
                                                   CitusSmartRoutingSettings settings,
                                                   @Value("${spring.datasource.url}") String url,
                                                   @Value("${spring.datasource.username}") String username,
                                                   @Value("${spring.datasource.password}") String password) {
        return new CitusWorkerRegistry(jdbcTemplate, settings, url, username, password);
    }

    /**
     * Bucket -> owning-worker mapping for the KV co-location group, anchored on the first distributed
     * table ({@code attribute_kv}; {@code ts_kv_latest} is co-located so it shares the same shards).
     * Eagerly {@link CitusShardPlacement#refresh() refreshed} here so routing is correct from the first
     * KV op, mirroring how {@code citusShardLocator} eager-loads — and like that bean, only the
     * {@code !install} variant refreshes (the install context has no schema yet). There is no install
     * variant because smart routing never runs during install.
     * <p>
     * {@code @DependsOn("citusWorkerRegistry")} enforces the registry-before-placement ordering at boot
     * that {@code CitusRoutingRefresher} documents and upholds on every refresh: the
     * registry's {@code @PostConstruct} builds the per-worker pools before this bean's eager
     * {@link CitusShardPlacement#refresh()} maps buckets to {@code groupId}s, so a bucket never resolves to
     * a not-yet-pooled worker.
     */
    @Bean
    @CitusSmartRoutingComponent
    @DependsOn("citusWorkerRegistry")
    public CitusShardPlacement citusShardPlacement(JdbcTemplate jdbcTemplate, CitusShardLocator citusShardLocator) {
        // The locator (already eager-refreshed in its own !install bean) supplies the expected bucket count
        // so a scheduled refresh that reads a mid-rebalance shard-count skew keeps the previous good snapshot
        // instead of shrinking the list and tripping workerForBucket() with IndexOutOfBounds.
        CitusShardPlacement placement = new CitusShardPlacement(jdbcTemplate, CitusTables.DISTRIBUTED_TABLES.get(0),
                citusShardLocator::shardCount);
        placement.refresh();
        return placement;
    }

    /**
     * The single router consumers inject, by the bean name {@code kvShardRouter} / by type.
     * It is built on the EXISTING {@code citusShardLocator} bean — the very same instance
     * {@code CitusQueuePartitioner} uses — so write-queue bucket {@code i} and router bucket {@code i}
     * are the identical function (the bucket-alignment invariant); a second locator is NOT created.
     * <p>
     * After both the locator (eager-refreshed in its {@code !install} {@code @Bean}) and the placement
     * (eager-refreshed above) are loaded, it hard-asserts {@code placement.shardCount() ==
     * shardLocator.shardCount()}: a mismatch means the placement bucket indices do not line up with the
     * locator's buckets (duplicate/missing shards or wrong ordering) and would silently route every op
     * to the wrong worker, so it fails startup naming both counts and the anchor table. The method
     * parameters make placement+locator constructor dependencies (so both are loaded first), and the
     * registry's {@code @PostConstruct} reachability gate has already run since it too is a dependency.
     */
    @Bean
    @CitusSmartRoutingComponent
    public CitusShardRouter kvShardRouter(CitusShardLocator citusShardLocator,
                                          CitusShardPlacement citusShardPlacement,
                                          CitusWorkerRegistry citusWorkerRegistry,
                                          CitusSmartRoutingSettings settings,
                                          CitusFailoverRefreshTrigger citusFailoverRefreshTrigger) {
        CitusShardRouter.assertBucketAlignment(citusShardLocator.shardCount(), citusShardPlacement.shardCount(),
                CitusTables.DISTRIBUTED_TABLES.get(0));
        return new CitusShardRouter(citusShardLocator, citusShardPlacement, citusWorkerRegistry, settings,
                citusFailoverRefreshTrigger);
    }

    /**
     * The shared refresh routine (worker pools first, placement last) behind BOTH refresh entry points:
     * the fixed-delay scheduler below and the failover trigger. Same conditional + profile gating as the
     * other smart-routing beans.
     */
    @Bean
    @CitusSmartRoutingComponent
    public CitusRoutingRefresher citusRoutingRefresher(CitusShardPlacement citusShardPlacement,
                                                       CitusWorkerRegistry citusWorkerRegistry) {
        return new CitusRoutingRefresher(citusShardPlacement, citusWorkerRegistry);
    }

    /**
     * Debounced failover-driven refresh entry point: {@code kvShardRouter} notifies it when a routed
     * operation fails with a failover signature, collapsing the post-failover outage to roughly one
     * failed operation plus a pool rebuild instead of waiting out the scheduled tick.
     */
    @Bean
    @CitusSmartRoutingComponent
    public CitusFailoverRefreshTrigger citusFailoverRefreshTrigger(CitusRoutingRefresher citusRoutingRefresher,
                                                                   CitusSmartRoutingSettings settings) {
        return new CitusFailoverRefreshTrigger(citusRoutingRefresher, settings.getFailoverRefreshDebounceMs());
    }

    /**
     * Periodically refreshes the placement snapshot and worker pools on the
     * {@code placement_refresh_interval_ms} cadence so a rebalance is picked up without a restart —
     * the backstop behind the failover trigger's fast path. Same conditional + profile gating as the
     * other smart-routing beans.
     */
    @Bean
    @CitusSmartRoutingComponent
    public CitusRoutingRefreshScheduler citusRoutingRefreshScheduler(CitusRoutingRefresher citusRoutingRefresher) {
        return new CitusRoutingRefreshScheduler(citusRoutingRefresher);
    }

}
