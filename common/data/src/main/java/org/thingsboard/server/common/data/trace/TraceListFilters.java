// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.trace;

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
@Schema(description = "Trace list filter options")
public class TraceListFilters {
    @Schema(description = "Rule engine service ids")
    private List<String> ruleEngines;

    @Schema(description = "Current queues")
    private List<Option> queues;

    @Schema(description = "Current rule chains")
    private List<Option> ruleChains;

    @Schema(description = "Current rule nodes")
    private List<RuleNodeOption> ruleNodes;

    @Schema(description = "Known message types")
    private List<String> messageTypes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private String id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleNodeOption {
        private String id;
        private String name;
        private String ruleChainId;
    }
}
