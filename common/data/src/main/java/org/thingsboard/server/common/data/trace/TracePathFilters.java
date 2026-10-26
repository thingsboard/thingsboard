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
@Schema(description = "Execution path filter options")
public class TracePathFilters {
    @Schema(description = "Distinct root queues across paths")
    private List<Option> queues;

    @Schema(description = "Distinct root rule chains across paths")
    private List<Option> ruleChains;

    @Schema(description = "Distinct rule nodes visited by paths")
    private List<RuleNodeOption> ruleNodes;

    @Schema(description = "Distinct root message types across paths")
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
