// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.extension;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * How many more extensions of one {@link ResourceSize} tier the tenant can still deploy. The count is
 * the smallest of what the extension-count, CPU and memory limits allow, and assumes a single replica.
 * {@code null} means no limit caps this tier.
 */
@Schema(description = "Free capacity of a tenant for one extension resource tier.")
public record ExtensionSizeAvailability(
        @Schema(description = "Resource tier.", example = "SMALL")
        ResourceSize size,
        @Schema(description = "Number of single-replica extensions of this tier that still fit; null = unlimited.", example = "2")
        Long count) {

}
