// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.aspect;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;

@Data
@Builder
public class DbCallStatsSnapshot {

    private final TenantId tenantId;
    private final int totalSuccess;
    private final int totalFailure;
    private final long totalTiming;
    private final Map<String, MethodCallStatsSnapshot> methodStats;

    public int getTotalCalls() {
        return totalSuccess + totalFailure;
    }

}
