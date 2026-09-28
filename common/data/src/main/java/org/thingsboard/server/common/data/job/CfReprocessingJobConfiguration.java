// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.job;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.CalculatedFieldId;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CfReprocessingJobConfiguration extends JobConfiguration {

    @NotNull
    private CalculatedFieldId calculatedFieldId;
    private String calculatedFieldName;
    private long startTs;
    private long endTs;

    @Override
    public JobType getType() {
        return JobType.CF_REPROCESSING;
    }

}
