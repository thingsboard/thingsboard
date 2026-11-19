// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.spi;

import java.util.Map;

/**
 * Substrate-neutral health snapshot of an extension workload, fed to {@code ExtensionStateDeriver}.
 * The fields map cleanly onto both Kubernetes (Deployment replicas / Pod phases) and a Docker Engine
 * (a single container's running/health state). {@code podPhaseCounts} is a free-form
 * phase-or-status histogram (e.g. {@code {"Running": 2}} on K8s, {@code {"running": 1}} on Docker).
 */
public record DeploymentHealth(boolean exists,
                               int desiredReplicas,
                               int readyReplicas,
                               int updatedReplicas,
                               Map<String, Integer> podPhaseCounts,
                               String errorReason) {

    public static DeploymentHealth missing() {
        return new DeploymentHealth(false, 0, 0, 0, Map.of(), null);
    }

}
