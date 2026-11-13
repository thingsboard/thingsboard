// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.LogConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Ulimit;
import com.github.dockerjava.core.command.PullImageResultCallback;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionMode;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.extension.spi.DeploymentHealth;
import org.thingsboard.server.service.extension.spi.ExtensionReconciler;
import org.thingsboard.server.service.extension.spi.ExtensionResourceNames;
import org.thingsboard.server.service.extension.spi.ExtensionSpecHasher;
import org.thingsboard.server.service.extension.spi.ReconcilerExceptions;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Docker Engine implementation of {@link ExtensionReconciler} — the "docker-compose" deployment
 * backend. Each extension is a single labeled container {@code tb-ext-{slug}} attached to a shared
 * user-defined bridge network; the operator's router reaches it by that name over Docker's embedded
 * DNS. Spec convergence is by recreate-on-change, keyed on the same spec-hash the K8s backend uses.
 *
 * <p>Hardening mirrors the K8s pod where Docker can express it: drop all capabilities,
 * no-new-privileges, read-only root filesystem with a writable {@code /tmp} tmpfs, a CPU/memory ceiling
 * from the spec (with swap pinned to the memory limit), a pids limit, a nofile ulimit and a rotated log
 * driver. Two K8s controls have no equivalent here and are NOT applied: {@code runAsNonRoot} (the image's
 * own USER decides) and egress filtering — on a shared bridge a tenant container reaches the host, the
 * cloud metadata endpoint and every other container. See {@code extensions.docker.network.internal}.
 *
 * <p>Single instance per extension: a Docker host has no built-in load balancer behind one DNS name,
 * so {@code replicas} is treated as on/off (0 = stopped, ≥1 = one running container). Tenant
 * network isolation is NOT enforced on the shared bridge — it relies on the extension's own callback
 * auth, exactly as documented for the self-managed Docker setup.
 */
@Service
@TbCoreComponent
// Active only in docker-compose mode. The mode enum makes the backends mutually exclusive by
// construction, so at most one ExtensionReconciler bean ever exists.
@ConditionalOnExpression(ExtensionMode.WHEN_DOCKER)
@Slf4j
public class DefaultExtensionDockerReconciler implements ExtensionReconciler {

    // Restarts before a container counts as looping rather than as one bad start, and how long after the
    // last start that verdict still holds — see isRestartLooping.
    private static final int CRASH_LOOP_RESTARTS = 3;
    private static final long CRASH_LOOP_WINDOW_MS = 60_000L;

    private final ExtensionDockerClientProvider clientProvider;
    private final ExtensionContainerPlanner planner;
    private final int stopTimeoutSec;
    private final long pullTimeoutMs;
    private final long logTimeoutMs;
    private final boolean alwaysPull;
    private final int pidsLimit;
    private final int openFilesLimit;
    private final String logMaxSize;
    private final int logMaxFiles;

    private DockerClient client;
    private String networkName;

    // Optional server-side salt. When set, the spec-hash is a keyed HMAC so the value stamped as a
    // container label is not an offline-brute-forceable fingerprint of resolved secret values.
    @Value("${extensions.spec-hash-salt:}")
    private String specHashSalt = "";

    @Autowired
    public DefaultExtensionDockerReconciler(
            ExtensionDockerClientProvider clientProvider,
            @Value("${extensions.docker.tb-internal-url:http://tb-core:8080}") String tbInternalUrl,
            @Value("${extensions.routing.base-path:/api/extension/route}") String basePath,
            @Value("${extensions.docker.stop-timeout-seconds:10}") int stopTimeoutSec,
            @Value("${extensions.docker.image.pull-timeout-ms:300000}") long pullTimeoutMs,
            @Value("${extensions.docker.log-timeout-ms:15000}") long logTimeoutMs,
            @Value("${extensions.image.always-pull:true}") boolean alwaysPull,
            @Value("${extensions.docker.pids-limit:256}") int pidsLimit,
            @Value("${extensions.docker.open-files-limit:1024}") int openFilesLimit,
            @Value("${extensions.docker.log.max-size:10m}") String logMaxSize,
            @Value("${extensions.docker.log.max-files:3}") int logMaxFiles) {
        this.clientProvider = clientProvider;
        this.planner = new ExtensionContainerPlanner(tbInternalUrl, basePath);
        this.stopTimeoutSec = stopTimeoutSec;
        this.pullTimeoutMs = pullTimeoutMs;
        this.logTimeoutMs = logTimeoutMs;
        this.alwaysPull = alwaysPull;
        this.pidsLimit = pidsLimit;
        this.openFilesLimit = openFilesLimit;
        this.logMaxSize = logMaxSize;
        this.logMaxFiles = logMaxFiles;
    }

    // Test seam: inject a (mock) client + planner directly, bypassing the Spring-managed provider.
    DefaultExtensionDockerReconciler(DockerClient client, String networkName, ExtensionContainerPlanner planner,
                                     int stopTimeoutSec, long pullTimeoutMs, long logTimeoutMs) {
        this(client, networkName, planner, stopTimeoutSec, pullTimeoutMs, logTimeoutMs, true);
    }

    DefaultExtensionDockerReconciler(DockerClient client, String networkName, ExtensionContainerPlanner planner,
                                     int stopTimeoutSec, long pullTimeoutMs, long logTimeoutMs, boolean alwaysPull) {
        this.clientProvider = null;
        this.client = client;
        this.networkName = networkName;
        this.planner = planner;
        this.stopTimeoutSec = stopTimeoutSec;
        this.pullTimeoutMs = pullTimeoutMs;
        this.logTimeoutMs = logTimeoutMs;
        this.alwaysPull = alwaysPull;
        this.pidsLimit = 256;
        this.openFilesLimit = 1024;
        this.logMaxSize = "10m";
        this.logMaxFiles = 3;
    }

    @PostConstruct
    void init() {
        this.client = clientProvider.getClient();
        this.networkName = clientProvider.getNetworkName();
        log.info("Extension Docker reconciler initialized for network '{}'", networkName);
    }

    // Planner context plus everything the RECONCILER (not the planner) puts on the container: the
    // network it joins and the host-config limits. All of it is invisible to the planner, so without it
    // an operator can change the extensions network or the pids/log/ulimit caps and existing containers
    // stay on the old settings forever with an unchanged spec-hash and no self-heal.
    private String renderContext() {
        return String.join("\n",
                planner.renderContext(),
                networkName == null ? "" : networkName,
                Integer.toString(pidsLimit),
                Integer.toString(openFilesLimit),
                logMaxSize == null ? "" : logMaxSize,
                Integer.toString(logMaxFiles));
        // alwaysPull is deliberately NOT part of the context: it only decides whether pullIfNeeded()
        // skips the pull and leaves nothing on the container, so folding it in would recreate every
        // container on the resync after an operator flips it. (On k8s it sets imagePullPolicy and IS
        // part of the rendered spec, hence hashed there.)
    }

    @Override
    public void apply(Extension extension) {
        String slug = extension.getSlug();
        String desiredHash = ExtensionSpecHasher.hash(extension, specHashSalt, renderContext());
        ContainerPlan plan = planner.plan(extension, desiredHash);
        boolean desiredRunning = extension.getState() != ExtensionState.STOPPED;

        InspectContainerResponse existing = inspectQuietly(plan.name());
        if (existing != null) {
            // Defence in depth against a slug colliding with an operator-owned container (e.g. the
            // shared tb-ext-router): never start/stop/recreate a container we did not create. Reserved
            // slugs are already rejected at creation; this guards the apply path too.
            requireManaged(existing, plan.name());
        }
        if (existing == null) {
            log.info("Creating container for extension {} (hash {})", slug, desiredHash);
            pullIfNeeded(plan);
            createContainer(plan, scratchMounts(plan.imageRef()));
            if (desiredRunning) {
                startQuietly(plan.name());
            }
            return;
        }

        String observedHash = labelOf(existing, ExtensionResourceNames.ANNOTATION_SPEC_HASH);
        boolean running = isRunning(existing);
        // The spec-hash covers the container spec only, so a container left behind by a DIFFERENT
        // extension that once held this slug can carry the very same hash. Adopting it would run one
        // tenant's traffic into another tenant's still-running container; recreate it instead, which also
        // restamps the owner labels the read paths below check.
        if (desiredHash.equals(observedHash) && ownedBy(existing, extension)) {
            // Spec unchanged — converge only the running state (cheap start/stop, no recreate).
            if (desiredRunning && !running) {
                startQuietly(plan.name());
            } else if (!desiredRunning && running) {
                stopQuietly(plan.name());
            } else {
                log.debug("apply no-op for extension {} — already at hash {}", slug, desiredHash);
            }
            return;
        }

        // Spec changed — recreate. A Docker container's image/env/limits are immutable after create.
        log.info("Recreating container for extension {} ({} -> {})", slug, observedHash, desiredHash);
        Map<String, String> mounts;
        if (!ownedBy(existing, extension)) {
            // Previous slug owner's container: its name IS the DNS name serving THIS slug's traffic,
            // so remove it BEFORE the pull — a failing pull must not keep it serving.
            removeQuietly(plan.name());
            pullIfNeeded(plan);
            mounts = scratchMounts(plan.imageRef());
        } else {
            // Same owner: pull AND inspect first, so a failed pull or a failed image inspection leaves
            // the healthy old container in place instead of removing it and then failing to replace it.
            pullIfNeeded(plan);
            mounts = scratchMounts(plan.imageRef());
            removeQuietly(plan.name());
        }
        createContainer(plan, mounts);
        if (desiredRunning) {
            startQuietly(plan.name());
        }
    }

    @Override
    public void scaleTo(Extension extension, int replicas) {
        String name = ExtensionResourceNames.container(extension.getSlug());
        InspectContainerResponse existing = managedContainer(extension, "scale");
        if (existing == null) {
            return; // nothing to scale; apply() owns creation
        }
        boolean running = isRunning(existing);
        if (replicas <= 0) {
            if (running) {
                stopQuietly(name);
                log.info("Stopped container for extension {}", extension.getSlug());
            }
        } else if (!running) {
            startQuietly(name);
            log.info("Started container for extension {}", extension.getSlug());
        }
    }

    @Override
    public void restart(Extension extension) {
        String name = ExtensionResourceNames.container(extension.getSlug());
        if (managedContainer(extension, "restart") == null) {
            return;
        }
        client.restartContainerCmd(name).withTimeout(stopTimeoutSec).exec();
        log.info("Restarted container for extension {}", extension.getSlug());
    }

    // Owner-checked: the container must carry THIS extension's tenant/extension-id labels. Teardown
    // retries for seconds after the row is gone and the slug is reusable at once, so a name-only delete
    // could remove the container a fast re-create with the same name has already built for a new
    // extension. The orphan sweep has no owner to compare against and keeps using deleteBySlug.
    @Override
    public void delete(Extension extension) {
        String name = ExtensionResourceNames.container(extension.getSlug());
        InspectContainerResponse existing = inspectQuietly(name);
        if (existing == null) {
            return;
        }
        if (!ownedBy(existing, extension)) {
            log.warn("Refusing to delete container '{}': it does not belong to extension {} ({})",
                    name, extension.getId(), extension.getTenantId());
            return;
        }
        // Remove by the inspected container's ID, not by name: the name is the reusable slug, and between
        // the inspect above and the remove a new owner of the slug can have recreated the container under
        // it. An ID is never reused, so a remove that races that way hits "not found" instead.
        removeQuietly(existing.getId() != null ? existing.getId() : name);
        log.info("Deleted container for extension {}", extension.getSlug());
    }

    // Skips a foreign container rather than throwing: teardown also runs from the orphan sweep, where an
    // abort would retry against the same name collision forever. Removal is forced, so getting this wrong
    // would destroy an operator-owned container outright.
    @Override
    public void deleteBySlug(String slug) {
        String name = ExtensionResourceNames.container(slug);
        InspectContainerResponse existing = inspectQuietly(name);
        if (existing == null) {
            return;
        }
        if (!isManaged(existing)) {
            log.warn("Refusing to delete container '{}': it is not managed by ThingsBoard (missing {}={} label)",
                    name, ExtensionResourceNames.LABEL_MANAGED_BY, ExtensionResourceNames.MANAGED_BY);
            return;
        }
        removeQuietly(name);
        log.info("Deleted container for extension {}", slug);
    }

    @Override
    public Set<String> listManagedSlugs() {
        List<Container> containers = client.listContainersCmd()
                .withShowAll(true)
                .withLabelFilter(Map.of(ExtensionResourceNames.LABEL_MANAGED_BY, ExtensionResourceNames.MANAGED_BY))
                .exec();
        return containers.stream()
                .map(c -> c.getLabels() != null ? c.getLabels().get(ExtensionResourceNames.LABEL_EXTENSION_SLUG) : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public DeploymentHealth describe(Extension extension) {
        InspectContainerResponse c = ownedContainer(extension, "describe");
        if (c == null) {
            return DeploymentHealth.missing();
        }
        InspectContainerResponse.ContainerState state = c.getState();
        boolean running = isRunning(c);
        String healthStatus = state != null && state.getHealth() != null ? state.getHealth().getStatus() : null;
        // Single instance: desired is 0 when the extension is stopped, otherwise 1.
        int desired = extension.getState() == ExtensionState.STOPPED ? 0 : 1;
        // Ready only when running and either no healthcheck or the image's healthcheck reports healthy.
        boolean ready = running && (healthStatus == null || "healthy".equalsIgnoreCase(healthStatus));
        int readyReplicas = ready ? 1 : 0;
        String status = state != null && state.getStatus() != null ? state.getStatus() : "unknown";
        String errorReason = dockerErrorReason(state, c.getRestartCount(), healthStatus);
        // No rolling concept on Docker: updatedReplicas tracks ready so a healthy container reads ACTIVE.
        return new DeploymentHealth(true, desired, readyReplicas, readyReplicas, Map.of(status, 1), errorReason);
    }

    // Deliberately a per-container loop rather than one listContainersCmd: the healthcheck status and the
    // restart count that describe() reads exist only on the inspect response, and a list would force
    // parsing them out of the daemon's human-readable status string. That trade-off is worth making on
    // Kubernetes, where each describe is a remote API-server round-trip, but not here — the daemon is on a
    // local socket, and docker-compose mode runs a single core node, so the status cache almost always
    // answers the list endpoint before this is reached at all.
    @Override
    public Map<String, DeploymentHealth> describeAll(Collection<Extension> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return Map.of();
        }
        Map<String, DeploymentHealth> result = new HashMap<>();
        for (Extension extension : extensions) {
            DeploymentHealth health = describe(extension);
            if (health.exists()) {
                result.put(extension.getSlug(), health);
            }
        }
        return result;
    }

    @Override
    public String tailLogs(Extension extension, int lines) {
        String name = ExtensionResourceNames.container(extension.getSlug());
        if (ownedContainer(extension, "logs") == null) {
            return "No container found for extension " + extension.getSlug() + ".";
        }
        LogCollector collector = new LogCollector();
        try {
            client.logContainerCmd(name)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTail(lines)
                    .exec(collector);
            if (!collector.awaitCompletion(logTimeoutMs, TimeUnit.MILLISECONDS)) {
                collector.close();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return collector.text() + "\n(log read interrupted)";
        } catch (Exception e) {
            // Never the raw message: a docker-java failure can carry the daemon endpoint.
            return collector.text() + "\n(could not read logs: " + ReconcilerExceptions.sanitizeMessage(e) + ")";
        }
        String text = collector.text();
        return text.isEmpty() ? "No logs for extension " + extension.getSlug() + "." : text;
    }

    @Override
    public boolean isRetryable(Throwable t) {
        return isRetryableDocker(t);
    }

    // Transient Docker-daemon failures (5xx, throttling, connect/timeout) are retried in-task; terminal
    // ones (404 not-found, 409 conflict, 400 bad-request) fail fast. Static so it is unit-testable
    // without a live client.
    public static boolean isRetryableDocker(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof NotModifiedException) {
                return false; // "already started/stopped" — never an error we should retry
            }
            if (t instanceof DockerException) {
                break; // a real daemon error: classify by HTTP status below (NotModified already ruled out)
            }
        }
        return ReconcilerExceptions.isRetryable(e, DockerException.class, DockerException::getHttpStatus);
    }

    // ---- internals -------------------------------------------------------------------------------

    private void createContainer(ContainerPlan plan, Map<String, String> scratchMounts) {
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withNetworkMode(networkName)
                // Survive daemon/host restarts but honor an explicit stop (matches K8s "keep it running").
                .withRestartPolicy(RestartPolicy.unlessStoppedRestart())
                .withCapDrop(Capability.ALL)
                .withReadonlyRootfs(true)
                .withSecurityOpts(List.of("no-new-privileges:true"))
                // Read-only rootfs needs a writable scratch dir, mirroring the K8s emptyDir at /tmp. Every
                // path the image declares as VOLUME gets the same capped tmpfs: otherwise Docker backs it
                // with an anonymous read-write volume on the host disk, with no size cap at all — a hole
                // through the read-only rootfs that also holds tb-core and the database. A tmpfs mount at
                // the path wins over the image's VOLUME.
                .withTmpFs(scratchMounts);
        if (pidsLimit > 0) {
            // A shared Docker host has one global pid_max: without this a fork loop in one tenant's
            // container stops the daemon, tb-core and every other tenant from forking.
            hostConfig.withPidsLimit((long) pidsLimit);
        }
        if (openFilesLimit > 0) {
            hostConfig.withUlimits(new Ulimit[]{new Ulimit("nofile", openFilesLimit, openFilesLimit)});
        }
        if (StringUtils.isNotBlank(logMaxSize)) {
            // The daemon default (json-file, no rotation) lets one chatty container fill the host disk that
            // also holds tb-core and the database. The K8s side caps the same abuse via ephemeral-storage.
            hostConfig.withLogConfig(new LogConfig(LogConfig.LoggingType.JSON_FILE, Map.of(
                    "max-size", logMaxSize,
                    "max-file", Integer.toString(Math.max(1, logMaxFiles)))));
        }
        if (plan.memoryBytes() != null) {
            // Pin swap to the memory limit: with --memory set and --memory-swap unset Docker allows the
            // limit again in host swap, so the quota would account for half of what the tenant can commit.
            hostConfig.withMemory(plan.memoryBytes()).withMemorySwap(plan.memoryBytes());
        }
        if (plan.nanoCpus() != null) {
            hostConfig.withNanoCPUs(plan.nanoCpus());
        }
        client.createContainerCmd(plan.imageRef())
                // Same ambient-credential leak as in pullIfNeeded: docker-java pre-fills this command from
                // the TB process's Docker config too. Create never pulls, so nothing here needs auth.
                .withAuthConfig(null)
                .withName(plan.name())
                .withLabels(plan.labels())
                .withEnv(plan.env())
                // Declared (not host-published): the router reaches it over the shared network only.
                .withExposedPorts(ExposedPort.tcp(plan.exposedPort()))
                .withHostConfig(hostConfig)
                .exec();
    }

    static final String SCRATCH_TMPFS_OPTS = "rw,size=64m";

    // /tmp plus each VOLUME path the (already pulled) image declares, all as size-capped tmpfs.
    private Map<String, String> scratchMounts(String imageRef) {
        Map<String, String> mounts = new LinkedHashMap<>();
        mounts.put("/tmp", SCRATCH_TMPFS_OPTS);
        for (String path : declaredVolumes(imageRef)) {
            mounts.putIfAbsent(path, SCRATCH_TMPFS_OPTS);
        }
        return mounts;
    }

    // Fails CLOSED: if the image cannot be inspected, the set of paths that need a cap is unknown, and a
    // container created without them would run with exactly the uncapped host volumes this exists to
    // prevent — stamped with the desired hash, so no later resync would repair its mounts either. The
    // exception propagates out of apply(); the daemon error is classified by isRetryable() and the apply
    // is retried or recorded as FAILED like any other substrate failure.
    private Set<String> declaredVolumes(String imageRef) {
        InspectImageResponse image = client.inspectImageCmd(imageRef).exec();
        Map<String, ?> volumes = image != null && image.getConfig() != null ? image.getConfig().getVolumes() : null;
        if (volumes == null || volumes.isEmpty()) {
            return Set.of();
        }
        Set<String> paths = new LinkedHashSet<>();
        for (String path : volumes.keySet()) {
            if (path != null && path.startsWith("/") && !"/".equals(path)) {
                paths.add(path);
            }
        }
        if (!paths.isEmpty()) {
            log.info("Image {} declares VOLUME {} — mounting capped tmpfs instead of host volumes", imageRef, paths);
        }
        return paths;
    }

    // The pull is where registry credentials are checked, so by default we always pull: image presence
    // is a property of the shared Docker host, not of the requesting tenant, and reusing a local copy
    // would let any tenant run an image:tag another tenant pulled with credentials it does not hold.
    // Layers already cached make this a manifest round-trip. Operators of a single-tenant install can
    // trade that check back for offline starts via extensions.image.always-pull=false, which restores
    // the old behaviour: reuse the local copy unless the tag is mutable ('latest'). For a private
    // registry the credentials come from the extension's registryAuth (resolved upstream from a
    // ThingsBoard Secret); a public image needs none.
    private void pullIfNeeded(ContainerPlan plan) {
        if (!alwaysPull && !plan.pullAlways() && imagePresent(plan.imageRef())) {
            return;
        }
        try {
            var pull = client.pullImageCmd(plan.repository()).withTag(plan.tag());
            // ALWAYS set the auth explicitly, including to null. docker-java pre-fills the command with
            // DockerClientConfig.effectiveAuthConfig(repository) — the credentials of the TB process's own
            // Docker config (~/.docker/config.json, DOCKER_* env). Leaving that in place would pull a
            // tenant's image with the operator's credentials, so any tenant that knows the name of a
            // private image the host is logged in to could run it. The command's auth config is the only
            // thing the exec layer sends, so clearing it drops the X-Registry-Auth header entirely.
            pull.withAuthConfig(plan.registryAuth() != null ? authConfig(plan) : null);
            // PullImageResultCallback (not the bare ResultCallback.Adapter) is what inspects the stream: the
            // daemon reports a failure that happens after the first progress bytes as an in-band
            // {"error": ...} item on an HTTP 200 stream, and Adapter.onNext drops it. Swallowing that would
            // recreate the container from the stale cached image and stamp the new spec hash as converged.
            boolean done = pull
                    .exec(new PullImageResultCallback())
                    .awaitCompletion(pullTimeoutMs, TimeUnit.MILLISECONDS);
            if (!done) {
                throw new DockerException("Image pull timed out after " + pullTimeoutMs + "ms: " + plan.imageRef(), 0);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DockerException("Image pull interrupted: " + plan.imageRef(), 0, e);
        }
    }

    private static AuthConfig authConfig(ContainerPlan plan) {
        var auth = plan.registryAuth();
        String registry = ExtensionResourceNames.resolveRegistry(auth, plan.repository());
        return new AuthConfig()
                .withUsername(auth.getUsername())
                .withPassword(auth.getPassword())
                .withRegistryAddress(registry);
    }

    private boolean imagePresent(String imageRef) {
        try {
            client.inspectImageCmd(imageRef).exec();
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    private InspectContainerResponse inspectQuietly(String name) {
        try {
            return client.inspectContainerCmd(name).exec();
        } catch (NotFoundException e) {
            return null;
        }
    }

    private void startQuietly(String name) {
        try {
            client.startContainerCmd(name).exec();
        } catch (NotModifiedException e) {
            // already running
        }
    }

    private void stopQuietly(String name) {
        try {
            client.stopContainerCmd(name).withTimeout(stopTimeoutSec).exec();
        } catch (NotModifiedException | NotFoundException e) {
            // already stopped / gone
        }
    }

    private void removeQuietly(String name) {
        try {
            // withRemoveVolumes = 'docker rm -v': an image-declared VOLUME gets an anonymous, host-backed
            // read-write volume that bypasses the read-only rootfs and the 64m tmpfs cap. Without this every
            // recreate and delete leaves one behind forever.
            client.removeContainerCmd(name).withForce(true).withRemoveVolumes(true).exec();
        } catch (NotFoundException e) {
            // already gone — delete is idempotent
        }
    }

    private static boolean isRunning(InspectContainerResponse c) {
        return c.getState() != null && Boolean.TRUE.equals(c.getState().getRunning());
    }

    private static String labelOf(InspectContainerResponse c, String key) {
        if (c.getConfig() == null || c.getConfig().getLabels() == null) {
            return null;
        }
        return c.getConfig().getLabels().get(key);
    }

    private static boolean isManaged(InspectContainerResponse c) {
        return ExtensionResourceNames.MANAGED_BY.equals(labelOf(c, ExtensionResourceNames.LABEL_MANAGED_BY));
    }

    private static boolean ownedBy(InspectContainerResponse c, Extension extension) {
        Map<String, String> labels = c.getConfig() != null ? c.getConfig().getLabels() : null;
        return ExtensionResourceNames.ownedBy(labels, extension);
    }

    // Read paths (state + logs) resolve the container by its slug-derived name, and that name outlives the
    // extension that created it: the row is deleted synchronously while teardown runs asynchronously, and
    // once another extension owns the slug the orphan sweep leaves the old container alone. Without an
    // owner check the new extension would read the previous TENANT's container — and, since the name is
    // all that is matched, an unmanaged container of the operator's too. Absent-or-foreign is reported as
    // "no container": the caller must not be able to tell one from the other.
    private InspectContainerResponse ownedContainer(Extension extension, String action) {
        String name = ExtensionResourceNames.container(extension.getSlug());
        InspectContainerResponse existing = inspectQuietly(name);
        if (existing == null) {
            return null;
        }
        if (!ownedBy(existing, extension)) {
            log.warn("Refusing {} on container '{}': it does not belong to extension {} ({})",
                    action, name, extension.getId(), extension.getTenantId());
            return null;
        }
        return existing;
    }

    private static void requireManaged(InspectContainerResponse c, String name) {
        if (!isManaged(c)) {
            throw new IllegalStateException("Refusing to act on container '" + name + "' that is not managed by "
                    + "ThingsBoard (missing " + ExtensionResourceNames.LABEL_MANAGED_BY + "=" + ExtensionResourceNames.MANAGED_BY + " label)");
        }
    }

    // Guard for scale/restart: absent -> null, unmanaged (operator-owned) -> throw, managed but
    // foreign (previous slug owner, possibly another tenant) -> treated as absent, like the read paths.
    private InspectContainerResponse managedContainer(Extension extension, String action) {
        String name = ExtensionResourceNames.container(extension.getSlug());
        InspectContainerResponse existing = inspectQuietly(name);
        if (existing == null) {
            log.info("{} skipped: container for {} is absent", action, extension.getSlug());
            return null;
        }
        requireManaged(existing, name);
        if (!ownedBy(existing, extension)) {
            log.warn("Refusing {} on container '{}': it does not belong to extension {} ({})",
                    action, name, extension.getId(), extension.getTenantId());
            return null;
        }
        return existing;
    }

    // Maps Docker container state to a short, tenant-facing failure reason (or null when healthy /
    // merely still coming up). Drives ExtensionState.FAILED in the deriver.
    private static String dockerErrorReason(InspectContainerResponse.ContainerState state, Integer restartCount, String health) {
        if (state == null) {
            return null;
        }
        if (Boolean.TRUE.equals(state.getOOMKilled())) {
            return "OOMKilled";
        }
        if ("unhealthy".equalsIgnoreCase(health)) {
            return "Unhealthy";
        }
        if (Boolean.TRUE.equals(state.getDead())) {
            return "Dead";
        }
        String status = state.getStatus();
        if (isRestartLooping(state, restartCount)) {
            return "CrashLoopBackOff";
        }
        if ("exited".equalsIgnoreCase(status)) {
            Long code = state.getExitCodeLong();
            return "Exited(" + (code != null ? code : "?") + ")";
        }
        String err = state.getError();
        if (err == null || err.isBlank()) {
            return null;
        }
        // Never surface the daemon's raw error string. It routinely embeds host filesystem paths,
        // overlay2 ids and runc/containerd internals, and from here it would flow — unsanitized and
        // untruncated — into the tenant-facing status AND into the unbounded last_error column. The
        // reconcile path runs its failures through sanitizeError for exactly this reason, and the K8s
        // backend surfaces only short allow-listed reason codes; match that and leave the detail to the
        // operator's log.
        log.debug("Docker daemon reported a container error: {}", err);
        return "ContainerError";
    }

    // The same blink the K8s backend had: the daemon reports 'restarting' only BETWEEN attempts, so a
    // poll landing while the container is briefly up saw a plain 'running' and the extension flipped
    // Failed -> Active -> Failed at the poll rate. A container that has restarted repeatedly AND came up
    // within the last minute is still looping in both phases: Docker caps its restart back-off at 60s, so
    // a real loop always restarts inside that window, while a container that has been up longer than it
    // has stopped looping.
    private static boolean isRestartLooping(InspectContainerResponse.ContainerState state, Integer restartCount) {
        if (restartCount == null || restartCount < CRASH_LOOP_RESTARTS) {
            return false;
        }
        if ("restarting".equalsIgnoreCase(state.getStatus())) {
            return true;
        }
        Long startedAt = parseInstant(state.getStartedAt());
        return startedAt != null && System.currentTimeMillis() - startedAt < CRASH_LOOP_WINDOW_MS;
    }

    private static Long parseInstant(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        try {
            return Instant.parse(raw).toEpochMilli();
        } catch (DateTimeParseException e) {
            // A container that has never started carries a zero timestamp in a format Instant rejects.
            return null;
        }
    }

    // Collects a finite (non-following) log tail into a buffer; stdout/stderr frames are concatenated
    // in arrival order.
    private static final class LogCollector extends ResultCallback.Adapter<Frame> {
        private final StringBuilder sb = new StringBuilder();
        private boolean truncated;

        // The requested line count bounds nothing on its own: the tenant owns the container and so owns
        // how long each line is. Cap what we accumulate and keep draining the stream — stopping the read
        // early would leave the daemon connection half-consumed.
        @Override
        public synchronized void onNext(Frame frame) {
            if (frame == null || frame.getPayload() == null || truncated) {
                return;
            }
            String chunk = new String(frame.getPayload(), StandardCharsets.UTF_8);
            int room = MAX_LOG_RESPONSE_CHARS - sb.length();
            if (chunk.length() >= room) {
                sb.append(chunk, 0, Math.max(0, room))
                        .append("\n(truncated: log output exceeded ").append(MAX_LOG_RESPONSE_CHARS)
                        .append(" characters)");
                truncated = true;
                return;
            }
            sb.append(chunk);
        }

        // Frames arrive on a docker-java callback thread; synchronize reads against onNext for the
        // timeout/interrupt paths that read before the stream has fully drained.
        private synchronized String text() {
            return sb.toString();
        }
    }

}
