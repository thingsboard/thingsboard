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
@Schema(description = "Filter options of the trace list and the execution path list")
public class TraceFilters {
    @Schema(description = "Rule engine service ids; returned by the trace list filters only")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> ruleEngines;

    @Schema(description = "Queues of the tenant and the system queues")
    private List<Option> queues;

    @Schema(description = "Rule chains of the tenant")
    private List<Option> ruleChains;

    @Schema(description = "Rule nodes of the tenant's rule chains")
    private List<RuleNodeOption> ruleNodes;

    @Schema(description = "All message types")
    private List<String> messageTypes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "TraceFilterOption")
    public static class Option {
        private String id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "TraceFilterRuleNodeOption")
    public static class RuleNodeOption {
        private String id;
        private String name;
        private String ruleChainId;
    }
}
