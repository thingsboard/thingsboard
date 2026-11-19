// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Striped;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.extension.ExtensionService;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.extension.spi.ExtensionReconciler;
import org.thingsboard.server.service.extension.spi.ReconcilerExceptions;
import org.thingsboard.server.service.extension.status.ExtensionStateDeriver;
import org.thingsboard.server.service.extension.status.ExtensionStatusService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@TbCoreComponent
@Slf4j
public class ExtensionReconcileService {

    private final Optional<ExtensionReconciler> reconciler;
    private final Optional<ExtensionStatusService> statusService;
    private final ExtensionService extensionService;
    private final PartitionService partitionService;
    private final SecretConfigurationService secretConfigurationService;
    private final SecretService secretService;
    private final long resyncIntervalMs;

    public ExtensionReconcileService(Optional<ExtensionReconciler> reconciler,
                                     Optional<ExtensionStatusService> statusService,
                                     ExtensionService extensionService,
                                     PartitionService partitionService,
                                     SecretConfigurationService secretConfigurationService,
                                     SecretService secretService,
                                     // Shared across substrates by design; there is exactly one key for it.
                                     @Value("${extensions.reconcile.resync-interval-ms:300000}") long resyncIntervalMs) {
        this.reconciler = reconciler;
        this.statusService = statusService;
        this.extensionService = extensionService;
        this.partitionService = partitionService;
        this.secretConfigurationService = secretConfigurationService;
        this.secretService = secretService;
        this.resyncIntervalMs = resyncIntervalMs;
    }

    // a plain re-apply is a spec-hash no-op, so restart is requested via this lifecycle-msg info marker
    public static final String RESTART_ACTION = "restart";

    private static final int RECONCILE_QUEUE_CAPACITY = 10_000;
    private static final int RETRY_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MS = 1000;
    // Same placeholder shape DefaultSecretConfigurationService resolves; group(1) is the secret name.
    private static final Pattern SECRET_REF_PATTERN = Pattern.compile("\\$\\{secret:([^;{}]+);type:[^;{}]+}");

    private ExecutorService reconcileExecutor;
    // Fan-out width of the periodic resync: at most this many applies in flight at once. Set from the
    // pool size in init(); the initial value only matters to a test that drives a resync without it.
    private int reconcileThreads = 2;
    private ScheduledExecutorService resyncExecutor;
    // Striped per-extension lock so apply/restart/delete never run concurrently for one extension.
    // Strongly referenced on purpose: a weak/soft-keyed map (ConcurrentReferenceHashMap) can drop a
    // lock under memory pressure even while it is held, silently breaking this guarantee. A stripe
    // collision between two extensions only serializes them — never the reverse.
    private final Striped<Lock> locks = Striped.lock(64);
    // Single-flight guard for the full-table resync. scheduleWithFixedDelay only measures the time to
    // hand the task to the pool, so without this a slow pass (degraded substrate) gets a fresh one
    // enqueued every interval, and onPartitionChange piles on more; they then occupy the small reconcile
    // pool and delay user-driven start/stop/restart tasks. A request that arrives while a pass runs is
    // NOT simply dropped: a partition change that lands mid-pass may hand this node rows the running scan
    // has already passed, and with resync-interval-ms=0 there is no periodic pass to pick them up later.
    //
    // One counter does both jobs — "is a pass running" and "was another one asked for" — so the handoff
    // is a single atomic step. Two separate flags had a lost wake-up: the runner reads "no rerun", a
    // requester fails to take the guard and sets the flag, the runner releases the guard; nobody runs.
    // Here the count is the number of outstanding requests including the running one. A requester's
    // increment either lands before the runner's final check (seen as > 1, so the runner goes again) or
    // after it (the runner has already set 0, so the requester's own increment starts from 0 and it runs).
    private final AtomicInteger resyncPending = new AtomicInteger();

    @PostConstruct
    void init() {
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        reconcileThreads = threads;
        // Bounded queue + caller-runs: a slow/unreachable API server applies backpressure instead of
        // letting reconcile tasks accumulate without limit (OOM risk).
        reconcileExecutor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(RECONCILE_QUEUE_CAPACITY),
                ThingsBoardThreadFactory.forName("ext-reconcile"),
                new ThreadPoolExecutor.CallerRunsPolicy());
        // Durable retry net: partition changes may never happen, so FAILED extensions and orphaned
        // k8s objects are healed by a periodic resync as well (0 disables it).
        if (reconciler.isPresent() && resyncIntervalMs > 0) {
            resyncExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("ext-resync"));
            resyncExecutor.scheduleWithFixedDelay(() -> submitLogged("resync", this::resyncOwned),
                    resyncIntervalMs, resyncIntervalMs, TimeUnit.MILLISECONDS);
        }
    }

    @PreDestroy
    void destroy() {
        if (resyncExecutor != null) {
            resyncExecutor.shutdownNow();
        }
        if (reconcileExecutor != null) {
            reconcileExecutor.shutdown();
            try {
                if (!reconcileExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    reconcileExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                reconcileExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // Save/stop/start/restart triggers arrive as cluster-broadcast ComponentLifecycleMsg, not local
    // Spring events: the REST call lands on an arbitrary core node, every node receives the broadcast,
    // and only the partition owner acts. The row is re-read here so the apply uses the committed state.
    @EventListener
    public void onComponentLifecycleMsg(ComponentLifecycleMsg msg) {
        if (reconciler.isEmpty() || msg.getEntityId() == null
                || msg.getEntityId().getEntityType() != EntityType.EXTENSION) {
            return;
        }
        ExtensionId extensionId = new ExtensionId(msg.getEntityId().getId());
        if (msg.getEvent() == ComponentLifecycleEvent.DELETED) {
            // teardown already ran on the node that handled the delete; the owner only drops its status-poll registration
            statusService.ifPresent(s -> s.unregister(extensionId));
            return;
        }
        if (!owns(msg.getTenantId(), extensionId)) {
            return;
        }
        Extension extension = extensionService.findExtensionById(msg.getTenantId(), extensionId);
        if (extension == null || extension.getState() == ExtensionState.DELETING) {
            return;
        }
        if (msg.getInfo() != null && RESTART_ACTION.equals(msg.getInfo().path("action").asText())) {
            submitLogged("restart " + extensionId, () -> restartQuietly(extension));
        } else {
            submitLogged("apply " + extensionId, () -> applyQuietly(extension));
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onDelete(DeleteEntityEvent<?> event) {
        if (reconciler.isEmpty() || event.getEntityId() == null
                || event.getEntityId().getEntityType() != EntityType.EXTENSION
                || !(event.getEntity() instanceof Extension extension)) {
            return;
        }
        // teardown is NOT partition-gated: on tenant deletion the tenant row is already gone (owns() would throw),
        // and the local delete event may never reach the owner; deletes are idempotent so this is safe
        submitLogged("teardown " + extension.getId(), () -> deleteQuietly(extension));
    }

    @EventListener
    public void onPartitionChange(PartitionChangeEvent event) {
        if (reconciler.isEmpty() || event.getServiceType() != ServiceType.TB_CORE) {
            return;
        }
        submitLogged("resync", this::resyncOwned);
    }

    // package-private for tests
    void resyncOwned() {
        if (resyncPending.getAndIncrement() > 0) {
            log.info("Extension resync deferred — the previous pass is still running and will re-run once it finishes");
            return;
        }
        long startTs = System.currentTimeMillis();
        try {
            do {
                try {
                    doResyncOwned();
                } catch (Exception e) {
                    // logged and swallowed so the counter below is still settled — a throw here would
                    // leave the count > 0 forever and every later request would be "deferred" to nobody
                    log.error("Extension resync pass failed", e);
                }
                // Requests that arrived during the pass (count > 1) collapse into ONE more pass; if none
                // did, the count goes to 0 and the guard is released in the same atomic step.
            } while (resyncPending.updateAndGet(n -> n > 1 ? 1 : 0) == 1 && !reconcileExecutor.isShutdown());
        } finally {
            if (reconcileExecutor.isShutdown()) {
                resyncPending.set(0);
            }
            long elapsed = System.currentTimeMillis() - startTs;
            if (resyncIntervalMs > 0 && elapsed > resyncIntervalMs) {
                log.warn("Extension resync pass took {} ms, longer than the {} ms interval — passes are being deferred",
                        elapsed, resyncIntervalMs);
            }
        }
    }

    private void doResyncOwned() {
        // Orphan-sweep ordering matters: list k8s objects BEFORE reading DB rows. Objects are created
        // only after the row commit, so a listed slug that has no row by the time the DB is read can
        // only belong to a deleted extension (a failed teardown left its objects behind).
        Set<String> orphanCandidates = listManagedSlugsQuietly();

        // Snapshot the registry BEFORE the scan: applyQuietly registers on other pool threads while this
        // runs, and offset pagination can shift a live row past a page boundary on a concurrent insert.
        // Pruning by scan membership alone would therefore drop registrations this pass simply raced with,
        // leaving their state/lastError column stale until some later pass re-registers them.
        Set<ExtensionId> registeredBeforeScan = statusService
                .map(ExtensionStatusService::registeredIds).orElseGet(Set::of);

        PageLink pageLink = new PageLink(100);
        boolean hasNext = true;
        int reconciled = 0;
        Set<ExtensionId> ownedIds = new HashSet<>();
        // Applies run on the pool instead of inline: each one is a handful of sequential round trips to
        // the substrate, so a node owning hundreds of extensions spent minutes of a 5-minute interval
        // waiting on the network. Submitted in batches of at most one per pool thread and awaited before
        // the next batch, so the shared queue never fills with resync work and a user-driven
        // start/stop/restart still gets a thread promptly.
        List<Future<?>> batch = new ArrayList<>();
        while (hasNext && !reconcileExecutor.isShutdown()) {
            PageData<Extension> page = extensionService.findAllExtensions(pageLink);
            for (Extension extension : page.getData()) {
                orphanCandidates.remove(extension.getSlug());
                if (extension.getState() == ExtensionState.DELETING) {
                    continue;
                }
                if (!owns(extension.getTenantId(), extension.getId())) {
                    continue;
                }
                ownedIds.add(extension.getId());
                batch.add(submitTracked("resync-apply", () -> applyQuietly(extension)));
                reconciled++;
                if (batch.size() >= reconcileThreads) {
                    awaitAll(batch);
                }
            }
            hasNext = page.hasNext();
            pageLink = pageLink.nextPageLink();
        }
        awaitAll(batch);
        // Drop status-poll registrations for extensions this node no longer owns. Only ids that were
        // already registered when the scan started are candidates; anything registered during the scan is
        // newer than our view of the table and must survive.
        statusService.ifPresent(s -> {
            Set<ExtensionId> keep = new HashSet<>(s.registeredIds());
            keep.removeAll(registeredBeforeScan); // registered while the scan ran — newer than our view
            keep.addAll(ownedIds);                // plus everything the scan saw as ours
            s.retainRegistered(keep);
        });
        if (reconciled > 0) {
            log.info("Extension resync re-applied {} owned extension(s)", reconciled);
        }
        sweepOrphans(orphanCandidates);
    }

    private Set<String> listManagedSlugsQuietly() {
        try {
            return new HashSet<>(reconciler.get().listManagedSlugs());
        } catch (Exception e) {
            log.warn("Orphan sweep skipped — could not list managed objects: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    private void sweepOrphans(Set<String> orphanSlugs) {
        for (String slug : orphanSlugs) {
            try {
                // Re-check ownership before deleting: the resync scan pages with offset pagination, which
                // can skip a live row when a concurrent insert/delete shifts it across a page boundary,
                // leaving it wrongly marked orphan. A slug is globally unique, so a non-null lookup means
                // a live extension owns this workload — never sweep it.
                if (extensionService.findBySlug(slug) != null) {
                    continue;
                }
                reconciler.get().deleteBySlug(slug);
                log.info("Orphan sweep removed k8s objects of deleted extension {}", slug);
            } catch (Exception e) {
                log.warn("Orphan sweep failed for slug {} — will retry next resync: {}", slug, e.getMessage());
            }
        }
    }

    // Executor.submit() captures a Throwable in the Future and, since we discard the Future, loses it
    // entirely — a teardown that throws before reaching its retry loop would leave the workload (and, on
    // K8s, both plaintext Secrets) running with not one line in the log. Route every dispatch through here.
    private void submitLogged(String what, Runnable body) {
        submitTracked(what, body);
    }

    private Future<?> submitTracked(String what, Runnable body) {
        return reconcileExecutor.submit(() -> {
            try {
                body.run();
            } catch (Throwable t) {
                log.error("Extension reconcile task failed ({})", what, t);
            }
        });
    }

    // Barrier for one resync batch, not an error channel: submitTracked already logs and swallows, so a
    // future here carries a cause only if the logging itself blew up. The wait is deliberately unbounded
    // — an apply legitimately runs for minutes against a slow substrate (three retries with backoff), and
    // any timeout short enough to be useful would abandon healthy work. Shutdown unblocks it: destroy()
    // follows shutdown() with shutdownNow(), whose interrupt lands on this thread.
    private void awaitAll(List<Future<?>> batch) {
        for (Future<?> f : batch) {
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                batch.clear();
                return;
            } catch (ExecutionException e) {
                log.error("Extension reconcile task failed (resync-apply)", e.getCause());
            }
        }
        batch.clear();
    }

    private void restartQuietly(Extension extension) {
        runLocked(extension.getId(), () -> {
            Extension fresh = freshRow(extension);
            if (fresh == null || fresh.getState() == ExtensionState.DELETING || fresh.getState() == ExtensionState.STOPPED) {
                return; // gone or scaled to zero meanwhile — nothing to roll
            }
            try {
                runWithRetry(() -> reconciler.get().restart(fresh));
                statusService.ifPresent(s -> s.register(fresh));
            } catch (Exception e) {
                log.warn("[{}] Restart failed for extension {}: {}", fresh.getTenantId(), fresh.getSlug(), e.getMessage());
                safeMarkFailed(fresh, e);
            }
        });
    }

    private void runLocked(ExtensionId id, Runnable body) {
        Lock lock = locks.get(id);
        lock.lock();
        try {
            body.run();
        } finally {
            lock.unlock();
        }
    }

    private void applyQuietly(Extension extension) {
        runLocked(extension.getId(), () -> {
            Extension fresh = freshRow(extension);
            if (fresh == null || fresh.getState() == ExtensionState.DELETING) {
                return; // row gone — the delete event owns teardown
            }
            if (fresh.getState() == ExtensionState.STOPPED) {
                // Unregister on BOTH outcomes. derive() pins STOPPED, so polling a stopped row has nothing
                // to report; and an entry kept alive across a failed stop still carries published=STOPPED,
                // which the poller would hold onto after the user's next start and write back over the
                // PROVISIONING row — silently reverting the start.
                statusService.ifPresent(s -> s.unregister(fresh.getId()));
                try {
                    runWithRetry(() -> reconciler.get().scaleTo(fresh, 0));
                    clearStopErrorIfAny(fresh);
                } catch (Exception e) {
                    log.warn("[{}] Stop failed for extension {}: {}", fresh.getTenantId(), fresh.getSlug(), e.getMessage());
                    // Keep STOPPED: a FAILED row would be full-applied by the next resync, restarting
                    // the workload the user stopped. Record the error; the resync retries the stop.
                    safeRecordStopError(fresh, e);
                }
                return;
            }
            try {
                Extension resolved = resolveSecrets(fresh);
                runWithRetry(() -> reconciler.get().apply(resolved));
                clearApplyFailureIfAny(fresh);
                statusService.ifPresent(s -> s.register(fresh));
                undoIfDeletedMeanwhile(fresh);
            } catch (Exception e) {
                log.warn("[{}] Reconcile failed for extension {}: {}", fresh.getTenantId(), fresh.getSlug(), e.getMessage());
                safeMarkFailed(fresh, e);
            }
        });
    }

    // Tasks capture the Extension at submit time, the pool is multi-threaded, and CallerRunsPolicy
    // can run a later-submitted task first. The row re-read under the per-id lock is the only
    // correct desired state — the last writer then wins with CURRENT data, not a stale snapshot.
    private Extension freshRow(Extension snapshot) {
        try {
            return extensionService.findExtensionById(snapshot.getTenantId(), snapshot.getId());
        } catch (Exception e) {
            log.warn("[{}] Skipping reconcile of {} — row re-read failed: {}",
                    snapshot.getTenantId(), snapshot.getSlug(), e.getMessage());
            return null;
        }
    }

    // Resolve ${secret:...} placeholders (env values + registry password) into a transient copy just
    // before handing the spec to the substrate. The DB row keeps the references; plaintext exists only
    // for this reconcile pass — and because the resolved values feed the spec hash, rotating a secret
    // changes the hash and re-renders the workload on the next apply (incl. the periodic resync).
    private Extension resolveSecrets(Extension extension) {
        // The resolver substitutes an empty string for a reference it cannot find, so a typo in a secret
        // name would otherwise deploy silently: env Secret written with a blank value, pods ready, state
        // ACTIVE, and the extension talking to its upstream with an empty credential. Fail the apply
        // instead — applyQuietly's catch records FAILED with the offending name, and nothing rolls a
        // healthy workload onto blank values because this runs before reconciler.apply().
        List<String> missing = missingSecretNames(extension);
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Unresolved secret reference(s): " + String.join(", ", missing));
        }
        return secretConfigurationService.replaceSecretUsages(extension.getTenantId(), extension, Extension.class);
    }

    // Names referenced as ${secret:name;type:TYPE} anywhere in the spec that no tenant Secret matches.
    private List<String> missingSecretNames(Extension extension) {
        JsonNode spec = JacksonUtil.valueToTree(extension.getContainerSpec());
        if (spec == null || spec.isNull()) {
            return List.of();
        }
        Set<String> referenced = new LinkedHashSet<>();
        Matcher matcher = SECRET_REF_PATTERN.matcher(spec.toString());
        while (matcher.find()) {
            referenced.add(matcher.group(1));
        }
        List<String> missing = new ArrayList<>();
        for (String name : referenced) {
            if (secretService.findSecretByName(extension.getTenantId(), name) == null) {
                missing.add(name);
            }
        }
        return missing;
    }

    private static final int TEARDOWN_ATTEMPTS = 3;

    private void deleteQuietly(Extension extension) {
        runLocked(extension.getId(), () -> teardown(extension));
    }

    /**
     * Undoes an apply whose extension was deleted while it was in flight.
     *
     * <p>Teardown is deliberately not partition-gated, so it runs on whichever node handled the REST
     * delete, while apply runs on the partition owner — and the per-id lock is JVM-local, so it cannot
     * serialize the two. The row can therefore disappear between {@code freshRow()} and the multi-call
     * apply, and the creates that follow resurrect the workload of an extension that no longer exists.
     *
     * <p>Leaving this to the orphan sweep is not enough: the sweep is bounded by the resync interval and,
     * when {@code extensions.reconcile.resync-interval-ms} is 0, runs only on a partition change — in a
     * stable cluster, potentially never. Re-checking here closes the window down to a crash between the
     * apply and this check, which the sweep does cover.
     */
    private void undoIfDeletedMeanwhile(Extension extension) {
        if (extensionService.findExtensionById(extension.getTenantId(), extension.getId()) != null) {
            return;
        }
        log.info("[{}] Extension {} was deleted while its apply was in flight — removing the workload it recreated",
                extension.getTenantId(), extension.getSlug());
        // teardown carries the slug-reclaim guard and the retry loop; the per-id lock is already held
        // here and is reentrant, so this is safe to call from inside applyQuietly.
        teardown(extension);
    }

    private void teardown(Extension extension) {
        statusService.ifPresent(s -> s.unregister(extension.getId()));
        // Slug is reused in no-suffix modes: delete-then-quick-recreate with the same name hands the new
        // extension the same tb-ext-{slug} workload. This teardown runs after the delete has committed,
        // so a non-null lookup with a DIFFERENT id means a live extension has re-claimed the slug — never
        // tear down its workload. The per-id lock does not serialize these two: their ids differ.
        Extension current;
        try {
            current = extensionService.findBySlug(extension.getSlug());
        } catch (Exception e) {
            // Outside the retry loop below, so without this catch the exception escapes the whole task.
            log.error("[{}] Teardown of extension {} aborted — slug re-claim check failed; the orphan sweep "
                    + "will remove tb-ext-{} on a later resync: {}",
                    extension.getTenantId(), extension.getSlug(), extension.getSlug(), e.getMessage());
            return;
        }
        if (current != null && !current.getId().equals(extension.getId())) {
            log.info("[{}] Teardown of extension {} skipped — slug {} re-created by live extension {}",
                    extension.getTenantId(), extension.getId(), extension.getSlug(), current.getId());
            return;
        }
        // deletes are idempotent: retry transient blips with backoff, stop early on terminal errors
        Exception last = null;
        for (int attempt = 1; attempt <= TEARDOWN_ATTEMPTS; attempt++) {
            try {
                reconciler.get().delete(extension);
                return;
            } catch (Exception e) {
                last = e;
                log.warn("[{}] Teardown attempt {}/{} failed for extension {}: {}",
                        extension.getTenantId(), attempt, TEARDOWN_ATTEMPTS, extension.getSlug(), e.getMessage());
                if (!reconciler.get().isRetryable(e)) {
                    break;
                }
                if (attempt < TEARDOWN_ATTEMPTS) {
                    sleepQuietly(RETRY_BACKOFF_MS * attempt);
                }
            }
        }
        log.error("[{}] Teardown FAILED for extension {} — orphaned workload tb-ext-{} " +
                        "will be removed by the resync orphan sweep: {}",
                extension.getTenantId(), extension.getSlug(), extension.getSlug(),
                last != null ? last.getMessage() : "unknown");
    }

    // Transient substrate failures (5xx, timeouts, throttling, IO errors) are retried in-task with a
    // short linear backoff; the periodic resync is the durable net for anything that still slips
    // through. The terminal-vs-transient classification is the active backend's (K8s/Docker).
    private void runWithRetry(Runnable body) {
        for (int attempt = 1; ; attempt++) {
            try {
                body.run();
                return;
            } catch (Exception e) {
                if (attempt >= RETRY_ATTEMPTS || !reconciler.get().isRetryable(e)) {
                    throw e;
                }
                log.info("Retryable reconcile failure (attempt {}/{}), backing off: {}", attempt, RETRY_ATTEMPTS, e.getMessage());
                sleepQuietly(RETRY_BACKOFF_MS * attempt);
            }
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during reconcile retry backoff", ie);
        }
    }

    // `extension` is the row re-read under the per-id lock, i.e. the state this apply acted on. The
    // retry backoffs mean seconds can pass before we get here, so the write is conditional on that
    // state still holding: an unconditional FAILED could land on top of a STOPPED the user committed
    // meanwhile, and the next resync would then run a full apply and restart what they stopped.
    private void safeMarkFailed(Extension extension, Exception e) {
        try {
            // Store a sanitized message for the tenant; the full exception is already logged above.
            extensionService.updateStateIf(extension.getTenantId(), extension.getId(), extension.getState(),
                    ExtensionState.FAILED, ExtensionStateDeriver.APPLY_FAILED_PREFIX + sanitizeError(e));
        } catch (Exception ignored) {
            // best-effort; the next status poll / resync will retry
        }
    }

    // Lifts the FAILED pin the status deriver keeps on a reconciler-recorded apply failure. Only the
    // reconciler knows that the desired spec has now reached the cluster; the poller cannot tell the old
    // healthy workload from the new one. FAILED -> PROVISIONING with a null error: the next poll derives
    // the real state from the (now desired) workload, and PROVISIONING -> ACTIVE is not debounced.
    private void clearApplyFailureIfAny(Extension extension) {
        if (extension.getState() != ExtensionState.FAILED || !ExtensionStateDeriver.isApplyFailure(extension.getLastError())) {
            return;
        }
        try {
            extensionService.updateStateIf(extension.getTenantId(), extension.getId(), ExtensionState.FAILED,
                    ExtensionState.PROVISIONING, null);
        } catch (Exception e) {
            log.debug("[{}] Could not clear the apply failure of {}: {}", extension.getTenantId(), extension.getSlug(), e.getMessage());
        }
    }

    // STOPPED -> STOPPED CAS that drops a stale "stop failed" text once a later stop has succeeded. The
    // poller cannot do this: STOPPED == STOPPED is never a transition for it, and a stopped row is not
    // polled at all.
    private void clearStopErrorIfAny(Extension extension) {
        if (!ExtensionStateDeriver.isStopFailure(extension.getLastError())) {
            return;
        }
        try {
            extensionService.updateStateIf(extension.getTenantId(), extension.getId(), ExtensionState.STOPPED,
                    ExtensionState.STOPPED, null);
        } catch (Exception e) {
            log.debug("[{}] Could not clear the stop error of {}: {}", extension.getTenantId(), extension.getSlug(), e.getMessage());
        }
    }

    // STOPPED -> STOPPED CAS: attaches the error only while the stop intent still holds.
    private void safeRecordStopError(Extension extension, Exception e) {
        try {
            extensionService.updateStateIf(extension.getTenantId(), extension.getId(), ExtensionState.STOPPED,
                    ExtensionState.STOPPED, ExtensionStateDeriver.STOP_FAILED_PREFIX + sanitizeError(e));
        } catch (Exception ignored) {
            // best-effort; the next resync retries the stop anyway
        }
    }

    // Tenant-safe error text; shared with the backends' log paths (see ReconcilerExceptions).
    static String sanitizeError(Throwable e) {
        return ReconcilerExceptions.sanitizeMessage(e);
    }

    private boolean owns(TenantId tenantId, EntityId entityId) {
        if (tenantId == null || !(entityId instanceof ExtensionId)) {
            return false;
        }
        try {
            return partitionService.isMyPartition(ServiceType.TB_CORE, tenantId, entityId);
        } catch (Exception e) {
            // a just-deleted tenant cannot be resolved to a partition; treat as "not mine" so apply paths skip
            log.debug("[{}] Ownership check skipped — partition unresolved: {}", tenantId, e.getMessage());
            return false;
        }
    }

}
