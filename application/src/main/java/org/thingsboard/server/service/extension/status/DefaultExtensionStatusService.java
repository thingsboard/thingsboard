// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.status;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionMode;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.extension.ExtensionStatus;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.dao.extension.ExtensionService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.extension.spi.DeploymentHealth;
import org.thingsboard.server.service.extension.spi.ExtensionReconciler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@TbCoreComponent
// Active whenever the feature is enabled (any non-disabled mode); injects whichever single
// ExtensionReconciler bean the selected backend contributes.
@ConditionalOnExpression(ExtensionMode.WHEN_ENABLED)
@Slf4j
public class DefaultExtensionStatusService implements ExtensionStatusService {

    // Backlog of distinct (extension, lines) log reads waiting for a slot. Single-flight already collapses
    // duplicates, so this bounds the number of DISTINCT extensions being read at once, not the number of
    // viewers. Well past any realistic figure; it exists so the queue cannot grow without limit.
    private static final int LOG_QUEUE_CAPACITY = 256;

    private final ExtensionReconciler reconciler;
    private final ExtensionService extensionService;
    private final PartitionService partitionService;
    private final String basePath;
    private final long pollIntervalMs;
    private final int hysteresisPolls;
    private final int logCacheMaxEntryChars;
    private final long logReadTimeoutMs;

    private final ConcurrentHashMap<ExtensionId, Entry> registry = new ConcurrentHashMap<>();
    // Short-lived, node-local, bounded by weight. Deliberately NOT the shared TB cache: an entry is up to
    // ExtensionReconciler.MAX_LOG_RESPONSE_CHARS of text, and pushing megabyte blobs through Redis on a
    // 3-second TTL would cost more than the API-server call it saves, while competing for memory with the
    // hot caches (attributes, device credentials) under an allkeys-lru eviction policy.
    private final Cache<LogKey, String> logCache;
    // Single-flight: one in-progress read per key, so N simultaneous viewers of the same extension cost
    // one orchestrator call rather than N. Entries live only for the duration of a call.
    private final ConcurrentHashMap<LogKey, CompletableFuture<String>> logsInFlight = new ConcurrentHashMap<>();
    // Bulkhead: log reads run here, never on the caller's Tomcat thread pool, so a slow orchestrator can
    // stall log reads without taking the rest of the REST API down with it.
    private final ThreadPoolExecutor logExecutor;
    private ScheduledExecutorService scheduler;

    public DefaultExtensionStatusService(ExtensionReconciler reconciler,
                                         ExtensionService extensionService,
                                         PartitionService partitionService,
                                         @Value("${extensions.routing.base-path:/api/extension/route}") String basePath,
                                         @Value("${extensions.status.poll-interval-ms:10000}") long pollIntervalMs,
                                         @Value("${extensions.status.hysteresis-polls:2}") int hysteresisPolls,
                                         @Value("${extensions.logs.cache-ttl-ms:3000}") long logCacheTtlMs,
                                         @Value("${extensions.logs.cache-max-entry-chars:131072}") int logCacheMaxEntryChars,
                                         @Value("${extensions.logs.cache-max-total-chars:8388608}") long logCacheMaxTotalChars,
                                         @Value("${extensions.logs.max-concurrent-reads:4}") int logMaxConcurrentReads,
                                         @Value("${extensions.logs.read-timeout-ms:20000}") long logReadTimeoutMs) {
        this.reconciler = reconciler;
        this.extensionService = extensionService;
        this.partitionService = partitionService;
        this.basePath = basePath;
        this.pollIntervalMs = pollIntervalMs;
        this.hysteresisPolls = Math.max(1, hysteresisPolls);
        this.logCacheMaxEntryChars = logCacheMaxEntryChars;
        this.logReadTimeoutMs = logReadTimeoutMs;
        this.logCache = logCacheTtlMs > 0 && logCacheMaxTotalChars > 0
                ? Caffeine.newBuilder()
                        .expireAfterWrite(logCacheTtlMs, TimeUnit.MILLISECONDS)
                        // Weigh by characters, not entry count: 1000 entries of a megabyte each would be a
                        // gigabyte of heap under a plain maximumSize.
                        .maximumWeight(logCacheMaxTotalChars)
                        .weigher((LogKey k, String v) -> v.length())
                        .build()
                : null;
        // Constructed here rather than in @PostConstruct so the service is usable without Spring lifecycle
        // (unit tests instantiate it directly). Threads are created lazily on first execute, so an unused
        // pool costs nothing.
        int logThreads = Math.max(1, logMaxConcurrentReads);
        this.logExecutor = new ThreadPoolExecutor(logThreads, logThreads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(LOG_QUEUE_CAPACITY),
                ThingsBoardThreadFactory.forName("ext-logs"),
                new ThreadPoolExecutor.AbortPolicy());
    }

    @PostConstruct
    void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("ext-status-poll"));
        scheduler.scheduleWithFixedDelay(this::pollAll, pollIntervalMs, pollIntervalMs, TimeUnit.MILLISECONDS);
        log.info("Extension status collector started (interval={}ms, hysteresis={})", pollIntervalMs, hysteresisPolls);
    }

    @PreDestroy
    void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        // Log reads are read-only and idempotent, so there is nothing to drain on the way out.
        logExecutor.shutdownNow();
    }

    @Override
    public void register(Extension extension) {
        registry.computeIfAbsent(extension.getId(), id -> new Entry(extension.getTenantId()));
    }

    @Override
    public void unregister(ExtensionId extensionId) {
        registry.remove(extensionId);
    }

    @Override
    public void retainRegistered(Set<ExtensionId> ownedIds) {
        registry.keySet().removeIf(id -> !ownedIds.contains(id));
    }

    @Override
    public Set<ExtensionId> registeredIds() {
        return Set.copyOf(registry.keySet());
    }

    @Override
    public ExtensionStatus describe(TenantId tenantId, Extension extension) {
        Entry entry = registry.get(extension.getId());
        if (entry != null && entry.cached != null) {
            return entry.cached;
        }
        Snapshot snapshot = compute(extension);
        if (entry != null) {
            entry.cached = snapshot.status();
        }
        return snapshot.status();
    }

    // The registry only holds extensions this node owns, so on any other node every row would otherwise
    // fall through to a live per-row describe() — two orchestrator calls each, uncached, on the HTTP
    // thread. Cached rows are served from the registry and the remainder resolved in one batched call.
    // A substrate failure is swallowed: the caller still gets the rows it can, and the ones it cannot are
    // simply absent, rather than one unreachable row failing the whole page.
    @Override
    public Map<ExtensionId, ExtensionStatus> describeAll(TenantId tenantId, List<? extends Extension> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return Map.of();
        }
        Map<ExtensionId, ExtensionStatus> result = new HashMap<>();
        List<Extension> toResolve = new ArrayList<>();
        for (Extension extension : extensions) {
            Entry entry = registry.get(extension.getId());
            if (entry != null && entry.cached != null) {
                result.put(extension.getId(), entry.cached);
            } else {
                toResolve.add(extension);
            }
        }
        if (toResolve.isEmpty()) {
            return result;
        }
        Map<String, DeploymentHealth> health;
        try {
            health = reconciler.describeAll(toResolve);
        } catch (Exception e) {
            log.debug("Batched status lookup failed for {} extension(s): {}", toResolve.size(), e.getMessage());
            return result;
        }
        for (Extension extension : toResolve) {
            DeploymentHealth h = health.get(extension.getSlug());
            if (h != null) {
                result.put(extension.getId(), compute(extension, h).status());
            }
        }
        return result;
    }

    // A log read is the most expensive read path the feature exposes and the only one a user can trigger
    // in a tight loop: the UI dialog polls every few seconds for as long as it is open, and on Kubernetes
    // each call is a pod LIST plus a log GET per pod, straight through to the API server. Three guards,
    // in order of what they defend against:
    //   - the cache absorbs repeat polls of the same extension within its TTL;
    //   - single-flight collapses SIMULTANEOUS readers of one extension into one orchestrator call, which
    //     the cache alone cannot do (on a cold key every concurrent caller misses at once);
    //   - the bounded executor keeps a slow orchestrator from parking Tomcat threads, since the call
    //     itself no longer runs on one.
    // Permission checks stay in the controller and run on every request; nothing here is reachable without
    // them, and the key is per extension id, so a cached entry can never be served across tenants.
    @Override
    public String tailLogs(TenantId tenantId, Extension extension, int lines) {
        LogKey key = new LogKey(extension.getId(), lines);
        if (logCache != null) {
            String cached = logCache.getIfPresent(key);
            if (cached != null) {
                return cached;
            }
        }
        CompletableFuture<String> future = readLogs(key, extension, lines);
        try {
            return future.get(logReadTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // The read is left running: it stays registered in logsInFlight, so the client's next poll
            // joins the same call instead of starting a second one, and the result lands in the cache.
            log.debug("Log read for {} still running after {} ms", extension.getId(), logReadTimeoutMs);
            return "Still reading logs for " + extension.getSlug() + " — the orchestrator has not answered "
                    + "within " + logReadTimeoutMs + " ms. Retry shortly.";
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while reading extension logs", e);
        }
    }

    // Returns the in-progress read for this key, starting one if there is none. Deliberately not written
    // as computeIfAbsent: the completion callback has to remove its own key, and doing that from inside a
    // ConcurrentHashMap mapping function is a recursive update on the same key — forbidden, and it would
    // hit exactly when the read finishes fastest.
    private CompletableFuture<String> readLogs(LogKey key, Extension extension, int lines) {
        CompletableFuture<String> running = logsInFlight.get(key);
        if (running != null) {
            return running;
        }
        CompletableFuture<String> created = new CompletableFuture<>();
        CompletableFuture<String> existing = logsInFlight.putIfAbsent(key, created);
        if (existing != null) {
            return existing;
        }
        try {
            logExecutor.execute(() -> {
                try {
                    String text = reconciler.tailLogs(extension, lines);
                    // Cache before completing, so a caller waking on completion and re-reading the cache
                    // finds the value. Oversized responses are the rare ones and the expensive ones to
                    // hold, so they are served but not stored — that keeps the cache footprint a product
                    // of the per-entry ceiling, not of MAX_LOG_RESPONSE_CHARS.
                    if (logCache != null && text != null && text.length() <= logCacheMaxEntryChars) {
                        logCache.put(key, text);
                    }
                    created.complete(text);
                } catch (Throwable t) {
                    created.completeExceptionally(t);
                } finally {
                    logsInFlight.remove(key, created);
                }
            });
        } catch (RejectedExecutionException e) {
            logsInFlight.remove(key, created);
            throw new TbRateLimitsException("Too many extension log reads in progress. Please retry shortly.");
        }
        return created;
    }

    // Scheduler entry point; package-private so tests can drive one poll pass without a live scheduler.
    // One batched describeAll per pass instead of a describe() per registered extension: on Kubernetes
    // the per-entry form is a Deployment GET plus a Pod LIST each, so 300 owned extensions cost ~600 API
    // calls and a pass silently stretches to minutes when the API server is slow — delaying every
    // CrashLoopBackOff -> FAILED transition with it. describeAll is one LIST of each kind, whatever the
    // count. A row the batch cannot answer for falls back to a single describe().
    void pollAll() {
        if (registry.isEmpty()) {
            return;
        }
        long startTs = System.currentTimeMillis();
        Map<ExtensionId, Extension> rows = readRows();
        if (rows.isEmpty()) {
            return;
        }
        Map<String, DeploymentHealth> health;
        try {
            health = reconciler.describeAll(rows.values().stream().toList());
        } catch (Exception ex) {
            log.debug("Batched status poll failed for {} extension(s), falling back to per-extension "
                    + "lookups: {}", rows.size(), ex.getMessage());
            health = Map.of();
        }
        for (var row : rows.entrySet()) {
            Entry entry = registry.get(row.getKey());
            if (entry == null) {
                continue; // unregistered while this pass was running
            }
            try {
                pollOne(row.getKey(), entry, row.getValue(), health.get(row.getValue().getSlug()));
            } catch (Exception ex) {
                log.debug("Status poll failed for {}: {}", row.getKey(), ex.getMessage());
            }
        }
        long elapsed = System.currentTimeMillis() - startTs;
        if (pollIntervalMs > 0 && elapsed > pollIntervalMs) {
            log.warn("Extension status poll pass took {} ms, longer than the {} ms interval — state "
                    + "transitions are being reported late", elapsed, pollIntervalMs);
        }
    }

    /**
     * Reads every registered row in one bulk query instead of a lookup per extension: the per-id form
     * cost one SELECT per registered extension on EVERY pass (no cache sits in front of the extension
     * DAO), so a node owning a thousand of them ran a hundred queries a second just to poll status.
     *
     * <p>Rows missing from the answer are unregistered — they are gone from the table. That is only safe
     * because a failed query returns no rows at all and is handled separately: treating a read failure as
     * "absent" would silently unregister every extension on the node the first time the DB hiccups.
     */
    private Map<ExtensionId, Extension> readRows() {
        Set<ExtensionId> registered = Set.copyOf(registry.keySet());
        List<Extension> found;
        try {
            found = extensionService.findExtensionsByIds(registered);
        } catch (Exception ex) {
            log.debug("Status poll row read failed for {} extension(s): {}", registered.size(), ex.getMessage());
            return Map.of();
        }
        Map<ExtensionId, Extension> rows = new HashMap<>();
        for (Extension extension : found) {
            Entry entry = registry.get(extension.getId());
            if (entry == null) {
                continue; // unregistered while the query was running
            }
            // The bulk read is not tenant-scoped, so the tenant of every row is re-checked here against
            // the one captured at registration — the same guarantee the per-id, tenant-scoped read gave.
            if (!entry.tenantId.equals(extension.getTenantId())) {
                log.warn("Status poll skipped extension {} — tenant mismatch", extension.getId());
                continue;
            }
            rows.put(extension.getId(), extension);
        }
        registered.stream().filter(id -> !rows.containsKey(id)).forEach(registry::remove);
        return rows;
    }

    private void pollOne(ExtensionId id, Entry entry, Extension extension, DeploymentHealth batched) {
        Snapshot snapshot = batched != null ? compute(extension, batched) : compute(extension);
        ExtensionState published = publishState(entry, snapshot.state());
        if (published != snapshot.state()) {
            republish(snapshot.status(), published, snapshot.errorReason(), extension);
        }
        entry.cached = snapshot.status();

        boolean owner = partitionService.isMyPartition(ServiceType.TB_CORE, entry.tenantId, id);
        if (!owner) {
            return; // only the owner persists transitions
        }
        // A hold (published != derived) is never written to the row. It is a display-side debounce of the
        // LIVE reading; the row may meanwhile have been moved by the reconciler (FAILED after an apply
        // failure, PROVISIONING after a start) and writing the held value back would revert that writer.
        // Once the derived state is stable the debounce releases and the transition is persisted.
        if (published != snapshot.state()) {
            return;
        }
        ExtensionState current = extension.getState();
        if (published != current) {
            persist(entry.tenantId, id, current, published, snapshot.status().getLastError());
        }
    }

    /**
     * Debounces the state this node reports, and returns the one to report now.
     *
     * <p>The hysteresis used to guard only the write to the row, while {@code entry.cached} — what
     * {@code GET /status} and the list page actually serve — took the raw derived state on every pass.
     * A single flickering signal (the classic one: a pod that crash-loops behind replicas that are still
     * serving, so the reason is present only between restarts) therefore made the reported state
     * alternate at the poll rate even though the stored state stayed put. Holding the last stable state
     * here fixes the blink at its only source and keeps the reported and the persisted value equal by
     * construction.
     *
     * <p>The first observation of an entry is adopted as-is: there is no history to debounce against,
     * and reporting nothing while one is built would be worse than reporting the truth a poll early.
     * DELETING and STOPPED skip the debounce because they follow a user action already committed to the
     * row, and PROVISIONING -> ACTIVE because a deploy that just came up should not read as still
     * provisioning for another interval.
     */
    private ExtensionState publishState(Entry entry, ExtensionState derived) {
        ExtensionState published = entry.published;
        if (published != null && derived != published) {
            boolean immediate = derived == ExtensionState.DELETING
                    || derived == ExtensionState.STOPPED
                    || (published == ExtensionState.PROVISIONING && derived == ExtensionState.ACTIVE);
            if (!immediate) {
                if (derived == entry.candidate) {
                    entry.streak++;
                } else {
                    entry.candidate = derived;
                    entry.streak = 1;
                }
                if (entry.streak < hysteresisPolls) {
                    return published; // not stable yet — keep reporting the last state that was
                }
            }
        }
        entry.published = derived;
        entry.candidate = null;
        entry.streak = 0;
        return derived;
    }

    // Rewrites a snapshot held back by the debounce so its error matches the state being reported: an
    // ACTIVE hold must not carry the blip's reason, and a held non-ACTIVE state keeps showing why.
    private static void republish(ExtensionStatus status, ExtensionState state, String liveReason, Extension extension) {
        status.setState(state);
        String error = ExtensionStateDeriver.effectiveError(state, liveReason, extension.getLastError());
        status.setLastError(error);
        status.setLastErrorAt(error != null ? extension.getLastErrorAt() : null);
    }

    // `error` is the EFFECTIVE error (status.getLastError()), not the raw live reason: it preserves the
    // stored error on a non-ACTIVE transition and clears it only on recovery to ACTIVE. Passing the raw
    // reason here would wipe a reconciler-stored apply failure with null on every routine transition.
    //
    // `expected` is the state read BEFORE describe(). That call is a round-trip to the orchestrator, and
    // a user start/stop committed inside that window would otherwise be overwritten by this derived
    // state — permanently, because applyQuietly routes on the current row, so the cluster then converges
    // on the overwritten value and no resync ever repairs it. The write is therefore conditional and is
    // dropped when the row moved on; the next poll re-derives from the new state.
    private void persist(TenantId tenantId, ExtensionId id, ExtensionState expected, ExtensionState state, String error) {
        extensionService.updateStateIf(tenantId, id, expected, state, error);
    }

    private Snapshot compute(Extension extension) {
        return compute(extension, reconciler.describe(extension));
    }

    private Snapshot compute(Extension extension, DeploymentHealth health) {
        String reason = health.errorReason();
        ExtensionState derived = ExtensionStateDeriver.derive(extension.getState(), health, reason, extension.getLastError());

        ExtensionStatus status = new ExtensionStatus();
        status.setExtensionId(extension.getId());
        status.setSlug(extension.getSlug());
        status.setState(derived);
        status.setDesiredReplicas(health.exists() ? health.desiredReplicas() : null);
        status.setReadyReplicas(health.exists() ? health.readyReplicas() : null);
        status.setPodPhaseCounts(health.podPhaseCounts());
        status.setUrl(basePath + "/" + extension.getSlug());
        // ACTIVE carries no error, so a stale stored error is never surfaced after recovery
        String effectiveError = ExtensionStateDeriver.effectiveError(derived, reason, extension.getLastError());
        status.setLastError(effectiveError);
        status.setLastErrorAt(effectiveError != null ? extension.getLastErrorAt() : null);
        status.setSnapshotAt(System.currentTimeMillis());
        return new Snapshot(status, derived, reason);
    }

    private static final class Entry {
        private final TenantId tenantId;
        private volatile ExtensionStatus cached;
        // Poll-thread state: the last state published to readers, plus the one waiting out the debounce.
        private ExtensionState published;
        private ExtensionState candidate;
        private int streak;

        private Entry(TenantId tenantId) {
            this.tenantId = tenantId;
        }
    }

    private record Snapshot(ExtensionStatus status, ExtensionState state, String errorReason) {}

    // `lines` is part of the key: it changes the response, so two viewers on different tail sizes must not
    // share an entry.
    private record LogKey(ExtensionId extensionId, int lines) {}

}
