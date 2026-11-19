// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.extension;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * What the tenant may still spend on extensions: the effective limits, the part already taken, and
 * how many more extensions of every {@link ResourceSize} tier would still fit.
 *
 * <p>A {@code null} limit means "unlimited" — the tenant profile stores {@code 0} for that, which is
 * remapped here so a UI never has to know the {@code 0 = unlimited} convention. Used CPU/memory count
 * the pod limits (times replicas) of every extension that is not STOPPED, matching the quota check
 * that runs on save; the extension count is the number of rows, in any state.
 */
@Schema(description = "Extension limits of a tenant with the used and the still available part.")
public record ExtensionQuota(
        @Schema(description = "Max number of extensions; null = unlimited.", example = "10")
        Long maxExtensions,
        @Schema(description = "Number of extensions the tenant already has, in any state.", example = "3")
        long usedExtensions,
        @Schema(description = "Max total CPU of all running extensions, in millicores; null = unlimited.", example = "4000")
        Long maxCpuMillicores,
        @Schema(description = "CPU taken by the running extensions, in millicores.", example = "1500")
        long usedCpuMillicores,
        @Schema(description = "Max total memory of all running extensions, in MB; null = unlimited.", example = "8192")
        Long maxMemoryMb,
        @Schema(description = "Memory taken by the running extensions, in MB.", example = "1024")
        long usedMemoryMb,
        @Schema(description = "How many more extensions of each resource tier still fit.")
        List<ExtensionSizeAvailability> availableBySize) {

}
