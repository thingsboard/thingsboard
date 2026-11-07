// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.ExtensionId;

import java.io.Serializable;
import java.util.Map;

@Data
@Schema
public class ExtensionStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "JSON object with the Extension Id.")
    private ExtensionId extensionId;

    @Schema(description = "Routing slug.", example = "acme-billing-9f3a")
    private String slug;

    @Schema(description = "Derived lifecycle state.")
    private ExtensionState state;

    @Schema(description = "Desired replica count from the container spec.", example = "2")
    private Integer desiredReplicas;

    @Schema(description = "Ready replica count observed in the cluster; null when orchestration is disabled.", example = "2")
    private Integer readyReplicas;

    @Schema(description = "Count of pods by phase, e.g. {\"Running\": 2}.")
    private Map<String, Integer> podPhaseCounts;

    @Schema(description = "Public route path for the extension.", example = "/api/extension/route/acme-billing-9f3a")
    private String url;

    @Schema(description = "Last error message, if any.")
    private String lastError;

    @Schema(description = "Timestamp (ms) of the last error.")
    private Long lastErrorAt;

    @Schema(description = "Timestamp (ms) when this snapshot was taken.")
    private Long snapshotAt;

}
