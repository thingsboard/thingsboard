// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.docker;

import org.thingsboard.server.common.data.extension.RegistryAuth;

import java.util.List;
import java.util.Map;

/**
 * Pure, substrate-translatable description of the single container that backs one extension. Produced
 * by {@link ExtensionContainerPlanner} (no Docker client needed, so it is fully unit-testable) and
 * turned into Docker Engine calls by {@code DefaultExtensionDockerReconciler}. Captures only what
 * varies per extension; the uniform hardening (read-only rootfs, drop-all-caps, no-new-privileges,
 * tmpfs /tmp, restart policy, network attachment) is applied by the reconciler.
 *
 * @param name        container name, {@code tb-ext-{slug}} (also its DNS name on the shared network)
 * @param repository  image repository without tag
 * @param tag         image tag
 * @param pullAlways  whether the image must be pulled on every apply (mutable {@code latest} tag)
 * @param env         environment as {@code KEY=VALUE} entries (built-in TB_EXT_* first, then user env)
 * @param labels      identifying labels incl. managed-by / slug / spec-hash (drives the orphan sweep)
 * @param exposedPort the container's HTTP port (declared, not host-published)
 * @param memoryBytes hard memory ceiling in bytes, or {@code null} to leave unbounded
 * @param nanoCpus     hard CPU ceiling in NanoCPUs, or {@code null} to leave unbounded
 * @param registryAuth resolved private-registry pull credentials, or {@code null} for a public image
 * @param specHash     SHA-256 of the container spec; the idempotency marker compared on re-apply
 */
public record ContainerPlan(String name,
                            String repository,
                            String tag,
                            boolean pullAlways,
                            List<String> env,
                            Map<String, String> labels,
                            int exposedPort,
                            Long memoryBytes,
                            Long nanoCpus,
                            RegistryAuth registryAuth,
                            String specHash) {

    /** Full {@code repository:tag} reference used for create/pull/inspect-image. */
    public String imageRef() {
        return repository + ":" + tag;
    }

}
