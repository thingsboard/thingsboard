// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.job.task;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.job.JobType;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@ToString(callSuper = true)
public class DummyTask extends Task<DummyTaskResult> {

    private int number;
    private long processingTimeMs;
    private long processingTimeoutMs;
    private List<String> errors; // errors for each attempt
    private boolean failAlways;

    @Override
    public DummyTaskResult toFailed(Throwable error) {
        return DummyTaskResult.failed(this, error);
    }

    @Override
    public DummyTaskResult toDiscarded() {
        return DummyTaskResult.discarded(this);
    }

    @Override
    public EntityId getEntityId() {
        return new DeviceId(UUID.randomUUID());
    }

    @Override
    public JobType getJobType() {
        return JobType.DUMMY;
    }

}
