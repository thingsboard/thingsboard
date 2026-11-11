// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.job.task;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@ToString(callSuper = true)
public class ReportTask extends Task<ReportTaskResult> {

    private CustomerId customerId;
    private ReportTemplateId reportTemplateId;
    @ToString.Exclude
    private ReportTemplateConfig reportTemplateConfig;

    private String timezone;
    private boolean makePublic;
    private UserId userId;
    private EntityId userOwnerId;
    @ToString.Exclude
    private String accessToken;
    private long accessTokenExpirationTs;

    private EntityId originator;

    /**
     * Whether the tenant is in development mode at the moment the task is submitted. Set by the caller, which
     * has access to the licensing state; carried through to the report generation modules, which do not.
     */
    private boolean nonProduction;

    @Override
    public ReportTaskResult toFailed(Throwable error) {
        return ReportTaskResult.failed(this, error);
    }

    @Override
    public ReportTaskResult toDiscarded() {
        return ReportTaskResult.discarded(this);
    }

    @Override
    public EntityId getEntityId() {
        return getJobId();
    }

    @Override
    public JobType getJobType() {
        return JobType.REPORT;
    }

}
