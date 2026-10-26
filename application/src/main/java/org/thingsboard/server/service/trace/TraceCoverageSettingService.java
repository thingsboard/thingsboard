// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.trace.TraceCoverageSetting;

public interface TraceCoverageSettingService {
    TraceCoverageSetting findTraceCoverageSettingByTenantId(TenantId tenantId);

    TraceCoverageSetting saveTraceCoverageSetting(TenantId tenantId, TraceCoverageSetting setting);
}
