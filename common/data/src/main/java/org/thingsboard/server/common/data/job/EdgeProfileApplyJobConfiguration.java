// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.job;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.RuleChainId;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EdgeProfileApplyJobConfiguration extends JobConfiguration {

    @NotNull
    private EdgeProfileApplyOp op;
    private EdgeProfileId edgeProfileId;
    private String edgeProfileName;
    private List<EdgeProfileJobRow> rows;
    private RuleChainId rootRuleChainId;
    private RuleChainId previousRootRuleChainId;
    private List<CustomerId> customerIds;

    @Override
    public JobType getType() {
        return JobType.EDGE_PROFILE_APPLY;
    }

}
