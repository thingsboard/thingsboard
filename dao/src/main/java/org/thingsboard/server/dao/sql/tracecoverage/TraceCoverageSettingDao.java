// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.tracecoverage;

import org.thingsboard.server.common.data.trace.TraceCoverageSetting;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

public interface TraceCoverageSettingDao extends Dao<TraceCoverageSetting> {
    TraceCoverageSetting findByTenantId(UUID tenantId);

    void removeByTenantId(UUID tenantId);

    /**
     * Inserts or updates the tenant's only setting. Any id or created time on {@code setting} is ignored.
     */
    TraceCoverageSetting upsertByTenantId(UUID tenantId, TraceCoverageSetting setting);
}
