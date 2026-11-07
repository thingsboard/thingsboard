// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.edge;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.RuleChainId;

import java.util.ArrayList;
import java.util.List;

@Schema
@Data
public class EdgeAssignments {
    private EdgeProfileId edgeProfileId;
    private String edgeProfileName;
    private RuleChainId rootRuleChainId;
    @Schema(description = "Root rule chain of the edge profile; on an edge it differs from rootRuleChainId when the root was overridden on that edge")
    private RuleChainId profileRootRuleChainId;
    @Schema(description = "Profile view: number of edges carrying the profile")
    private long edgesCount;
    private List<EdgeAssignmentRow> ruleChains = new ArrayList<>();
    private List<EdgeAssignmentRow> entityGroups = new ArrayList<>();
    private List<EdgeAssignmentRow> integrations = new ArrayList<>();
    private List<EdgeAssignmentRow> schedulerEvents = new ArrayList<>();
}
