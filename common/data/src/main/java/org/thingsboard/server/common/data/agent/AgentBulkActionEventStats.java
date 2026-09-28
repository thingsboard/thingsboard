// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema
public class AgentBulkActionEventStats {

    @Schema(description = "Number of events produced by the bulk action per processing status")
    private Map<AgentProcessingStatus, Long> countsByStatus;
    @Schema(description = "Total number of events produced by the bulk action")
    private long total;

}
