// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Rule node tree node")
public class RuleNodeTreeNodeDTO {
    @Schema(description = "Rule node id (UUID)")
    private String ruleNodeId;

    @Schema(description = "User-entered rule node name, resolved server-side")
    private String ruleNodeName;

    @Schema(description = "System rule node name")
    private String ruleNodeType;

    @Schema(description = "Owning rule chain id (UUID)")
    private String ruleChainId;

    @Schema(description = "Owning rule chain name, resolved server-side")
    private String ruleChainName;

    @Schema(description = "Queue id (UUID)")
    private String queueId;

    @Schema(description = "Queue name, resolved server-side")
    private String queueName;

    @Schema(description = "Label on the edge into this node; null at the root (not captured today)")
    private String relation;

    @Schema(description = "Total time spent in this node across the range (ms)")
    private long totalTime;

    @Schema(description = "Total executions of this node across the range")
    private long totalCount;

    @Schema(description = "Error executions of this node across the range")
    private long errors;

    @Schema(description = "Downstream nodes")
    private List<RuleNodeTreeNodeDTO> children;
}
