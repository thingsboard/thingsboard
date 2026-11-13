// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Version and edition of the platform build")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"version", "artifact", "name", "type"})
public record SystemVersionInfo(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "4.4.1",
                description = "Platform version, or 'unknown' when the build information is not available")
        String version,
        @Schema(example = "application", description = "Build artifact; absent when the build information is not available")
        String artifact,
        @Schema(example = "ThingsBoard Server Application", description = "Build name; absent when the build information is not available")
        String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "PE", description = "Platform edition")
        String type
) {
}
