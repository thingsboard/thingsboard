// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.sync.solution;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.util.CollectionsUtil;

import java.util.Set;

@Schema(description = "Solution export request specifying which entities to include and export settings.")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SolutionExportRequest {

    @Schema(description = "Set of internal entity IDs to export. The 'id' of each EntityId is the server-internal UUID. " +
                          "All listed entities must belong to the current tenant. " +
                          "Optional, but at least one of 'internalIds' or 'externalIds' must be non-empty.")
    @ArraySchema(schema = @Schema(implementation = EntityId.class))
    private Set<EntityId> internalIds;
    @Schema(description = "Set of external entity IDs to export. The 'id' of each EntityId is the external UUID " +
                          "(as stored in the 'externalId' field on the entity in the current tenant). " +
                          "The server looks up each entity by 'externalId' and 'entityType' within the current tenant. " +
                          "Optional, but at least one of 'internalIds' or 'externalIds' must be non-empty.")
    @ArraySchema(schema = @Schema(implementation = EntityId.class))
    private Set<EntityId> externalIds;
    @Schema(description = "Optional export settings controlling what additional data is included (relations, attributes, credentials, etc.). " +
                          "If not specified, default settings will be used that include all available data.")
    private EntityExportSettings settings;


    @JsonIgnore
    @AssertTrue(message = "At least one of 'internalIds' or 'externalIds' must be non-empty")
    public boolean isValid() {
        return CollectionsUtil.isNotEmpty(internalIds) || CollectionsUtil.isNotEmpty(externalIds);
    }

}
