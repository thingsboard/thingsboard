// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.job;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class JobFilter {

    private final CustomerId customerId;
    private final List<JobType> types;
    private final List<JobStatus> statuses;
    private final List<UUID> entities;
    private final Long startTime;
    private final Long endTime;
    private final boolean includeCustomers;

}
