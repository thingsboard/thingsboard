// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.tracecoverage;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.trace.TraceCoverageSetting;
import org.thingsboard.server.dao.AbstractJpaDaoTest;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JpaTraceCoverageSettingDaoTest extends AbstractJpaDaoTest {

    @Autowired
    protected TraceCoverageSettingDao traceCoverageSettingDao;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected final UUID tenantId = UUID.randomUUID();

    @After
    public void cleanUp() {
        jdbcTemplate.update("DELETE FROM trace_coverage_setting WHERE tenant_id = ?", tenantId);
    }

    @Test
    public void givenSavedSetting_whenFindByTenantId_thenSameSettingIsReturned() {
        TraceCoverageSetting saved = traceCoverageSettingDao.save(TenantId.fromUUID(tenantId), coverageSetting(42));

        TraceCoverageSetting found = traceCoverageSettingDao.findByTenantId(tenantId);
        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertEquals(42, found.getTracesPerInterval());
        // Tenant config lives in the main database.
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trace_coverage_setting WHERE tenant_id = ?", Integer.class, tenantId));

        traceCoverageSettingDao.removeByTenantId(tenantId);
        assertNull(traceCoverageSettingDao.findByTenantId(tenantId));
    }

    private TraceCoverageSetting coverageSetting(int tracesPerInterval) {
        TraceCoverageSetting setting = new TraceCoverageSetting();
        setting.setTenantId(tenantId);
        setting.setEnabled(true);
        setting.setTracesPerInterval(tracesPerInterval);
        return setting;
    }

}
