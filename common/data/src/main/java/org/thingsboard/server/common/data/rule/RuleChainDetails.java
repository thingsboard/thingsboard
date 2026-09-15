// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.rule;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class RuleChainDetails extends RuleChain {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Schema(description = "List of sticky notes placed on the rule chain canvas")
    private List<RuleChainNote> notes;

    public RuleChainDetails() {
        super();
    }

    public RuleChainDetails(RuleChain ruleChain) {
        super(ruleChain);
    }

    public RuleChainDetails(RuleChainDetails ruleChainDetails) {
        super(ruleChainDetails);
        this.notes = ruleChainDetails.getNotes() != null ? new ArrayList<>(ruleChainDetails.getNotes()) : null;
    }

}
