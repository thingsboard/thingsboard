// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(callSuper = true)
public class DummyJobConfiguration extends JobConfiguration {

    private long taskProcessingTimeMs;
    private int successfulTasksCount;
    private int failedTasksCount;
    private int permanentlyFailedTasksCount;
    private List<String> errors;
    private int retries;
    private long taskProcessingTimeoutMs;

    private String generalError;
    private int submittedTasksBeforeGeneralError;

    @Override
    public JobType getType() {
        return JobType.DUMMY;
    }

}
