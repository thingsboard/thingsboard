// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.job;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class JobFilter {

    private final List<JobType> types;
    private final List<JobStatus> statuses;
    private final List<UUID> entities;
    private final Long startTime;
    private final Long endTime;

}
