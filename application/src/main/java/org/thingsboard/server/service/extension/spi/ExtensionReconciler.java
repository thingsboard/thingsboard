// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.spi;

import org.thingsboard.server.common.data.extension.Extension;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Substrate-neutral contract for the component that runs an {@link Extension}'s container workload.
 * {@code ExtensionReconcileService} (the partition-gated brain: retries, resync, orphan sweep,
 * per-extension locking) talks ONLY to this interface, so the substrate — Kubernetes or a Docker
 * Engine (docker-compose host) — is swapped purely by which implementation bean is active.
 *
 * <p>Implementations are expected to be idempotent: {@link #apply} converges to the desired spec,
 * {@link #delete}/{@link #deleteBySlug} tolerate a missing target.
 */
public interface ExtensionReconciler {

    void apply(Extension extension);

    void scaleTo(Extension extension, int replicas);

    void restart(Extension extension);

    void delete(Extension extension);

    /**
     * Slugs of every workload the substrate currently holds under the managed-by marker.
     * Used by the resync orphan sweep to find workloads whose DB row is gone.
     */
    Set<String> listManagedSlugs();

    /**
     * Deletes the full object/container set for a slug. Idempotent; used for orphans where no
     * Extension row exists anymore.
     */
    void deleteBySlug(String slug);

    DeploymentHealth describe(Extension extension);

    /**
     * Health of many extensions at once, keyed by slug. Serves list endpoints, where calling
     * {@link #describe} per row multiplies a single HTTP request into two substrate calls per extension.
     * Implementations answer as cheaply as their substrate allows — Kubernetes with a fixed number of
     * label-filtered list calls, so the cost stops scaling with page size. An extension with no workload
     * is absent from the result, which the caller reads as {@link DeploymentHealth#missing()}.
     */
    Map<String, DeploymentHealth> describeAll(Collection<Extension> extensions);

    /**
     * Hard ceiling on the size of a {@link #tailLogs} response, in characters.
     *
     * <p>The line count the caller passes is not a bound on anything: the tenant owns the container, so
     * it owns the length of every line, and on Kubernetes the tail is applied PER POD — a 20-replica
     * extension multiplies it again. Without a byte budget one authenticated request can materialize
     * hundreds of megabytes on the core's heap, repeatable at will. Implementations must stop
     * accumulating once this is reached and say so in the returned text.
     */
    int MAX_LOG_RESPONSE_CHARS = 1024 * 1024;

    String tailLogs(Extension extension, int lines);

    /**
     * Whether a failure thrown by this substrate's client is worth retrying (transient: network
     * blip, throttling, 5xx) versus terminal (4xx-style: bad request, forbidden, not found). The
     * reconcile brain calls this to decide between an in-task backoff retry and failing fast.
     */
    boolean isRetryable(Throwable t);

}
