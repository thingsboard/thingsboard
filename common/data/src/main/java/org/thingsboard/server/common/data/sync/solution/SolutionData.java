// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.sync.solution;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;

import java.util.List;
import java.util.Map;

@Schema(description = "Portable solution package containing exported entities grouped by type. " +
                      "Represents a self-contained snapshot that can be imported into another tenant.")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolutionData {

    @Schema(description = "Exported entities grouped by entity type. Each key is an entity type (e.g. DEVICE_PROFILE, RULE_CHAIN) " +
                          "and the value is a list of entity export data objects.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<EntityType, List<EntityExportData<?>>> entities;

}
