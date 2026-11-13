// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * Predefined container resource tiers ("t-shirt sizes") for an extension deployment.
 *
 * <p>A size is the single knob a user picks instead of hand-writing Kubernetes quantities. Each tier
 * carries a CPU request/limit pair (CPU is compressible, so a burst headroom is allowed) and a single
 * memory value used for BOTH request and limit (memory is non-compressible, so pinning request==limit
 * avoids node overcommit and unpredictable OOM kills). All tiers stay within the platform ceilings
 * (defaults {@code 4} CPU / {@code 8Gi} memory) so a stock install accepts every size.
 */
public enum ResourceSize {

    XSMALL("125m", "250m", "128Mi"),
    SMALL("250m", "500m", "256Mi"),
    MEDIUM("500m", "1", "512Mi"),
    LARGE("1", "2", "1Gi"),
    XLARGE("2", "4", "2Gi"),
    XXLARGE("4", "4", "8Gi");

    /**
     * The tier applied when an extension's resources or size are left unset. It has to fit inside the
     * default tenant-profile extension quota, which is itself sized for a single extension of this tier
     * (see {@code DefaultTenantProfileConfiguration}). Raising this without raising that quota makes the
     * simplest possible create request — image and tag, no resources — fail out of the box on a stock
     * install.
     *
     * <p>SMALL rather than XSMALL: 128Mi does not hold a JVM. A stock Spring Boot extension is OOM-killed
     * before its context starts, so XSMALL as the default made the Java starter unusable by default. A
     * measured Spring Boot 3.5 / JVM 25 extension idles at ~170Mi and starts in ~27s under the SMALL CPU
     * limit, so SMALL is the smallest tier that works for every supported language.
     */
    public static final ResourceSize DEFAULT = SMALL;

    @Schema(description = "CPU request, Kubernetes quantity notation.", example = "250m")
    @Getter
    private final String cpuRequest;

    @Schema(description = "CPU limit (burst headroom above the request).", example = "500m")
    @Getter
    private final String cpuLimit;

    @Schema(description = "Memory request and limit (pinned equal), Kubernetes quantity notation.", example = "256Mi")
    @Getter
    private final String memory;

    ResourceSize(String cpuRequest, String cpuLimit, String memory) {
        this.cpuRequest = cpuRequest;
        this.cpuLimit = cpuLimit;
        this.memory = memory;
    }

    /** Memory request equals the tier memory — request and limit are deliberately pinned together. */
    public String getMemoryRequest() {
        return memory;
    }

    /** Memory limit equals the tier memory — request and limit are deliberately pinned together. */
    public String getMemoryLimit() {
        return memory;
    }

}
