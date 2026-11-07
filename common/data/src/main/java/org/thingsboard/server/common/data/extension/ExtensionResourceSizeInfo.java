// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.extension;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Read-only view of a {@link ResourceSize} tier and the concrete cpu/memory it resolves to.
 * Returned by {@code GET /api/extension/resourceSizes} so UIs and the CLI can list the available
 * tiers without hard-coding the values.
 */
@Schema(description = "A predefined extension resource tier and the cpu/memory it maps to.")
public record ExtensionResourceSizeInfo(
        @Schema(description = "Tier name to pass as resources.size.", example = "MEDIUM") ResourceSize name,
        @Schema(description = "CPU request, Kubernetes quantity notation.", example = "500m") String cpuRequest,
        @Schema(description = "CPU limit (burst headroom above the request).", example = "1") String cpuLimit,
        @Schema(description = "Memory request (pinned equal to the limit).", example = "512Mi") String memoryRequest,
        @Schema(description = "Memory limit (pinned equal to the request).", example = "512Mi") String memoryLimit,
        @Schema(description = "Default tier applied when resources.size is unset.", example = "false") boolean isDefault) {

    public static ExtensionResourceSizeInfo of(ResourceSize size) {
        return new ExtensionResourceSizeInfo(
                size,
                size.getCpuRequest(),
                size.getCpuLimit(),
                size.getMemoryRequest(),
                size.getMemoryLimit(),
                size == ResourceSize.DEFAULT);
    }

}
