// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.job.task;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.job.JobType;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@ToString(callSuper = true)
public class CfReprocessingTask extends Task<CfReprocessingTaskResult> {

    private CalculatedField calculatedField;
    private EntityInfo entityInfo;
    private long startTs;
    private long endTs;

    @Override
    public CfReprocessingTaskResult toFailed(Throwable error) {
        return CfReprocessingTaskResult.failed(this, error);
    }

    @Override
    public CfReprocessingTaskResult toDiscarded() {
        return CfReprocessingTaskResult.discarded(this);
    }

    @Override
    public EntityId getEntityId() {
        return entityInfo.getId();
    }

    @Override
    public JobType getJobType() {
        return JobType.CF_REPROCESSING;
    }

}
