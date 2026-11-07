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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JpaTraceCoverageSettingDaoTest extends AbstractJpaDaoTest {

    @Autowired
    protected TraceCoverageSettingDao traceCoverageSettingDao;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected final UUID tenantId = UUID.randomUUID();
    protected final UUID otherTenantId = UUID.randomUUID();

    @After
    public void cleanUp() {
        jdbcTemplate.update("DELETE FROM trace_coverage_setting WHERE tenant_id IN (?, ?)", tenantId, otherTenantId);
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

    @Test
    public void givenSavedSetting_whenUpsertedAgain_thenSameRowIsUpdated() {
        TraceCoverageSetting first = traceCoverageSettingDao.upsertByTenantId(tenantId, coverageSetting(42));
        TraceCoverageSetting second = traceCoverageSettingDao.upsertByTenantId(tenantId, coverageSetting(7));

        assertEquals(first.getId(), second.getId());
        assertEquals(first.getCreatedTime(), second.getCreatedTime());
        assertEquals(7, second.getTracesPerInterval());
        assertEquals(7, traceCoverageSettingDao.findByTenantId(tenantId).getTracesPerInterval());
        assertEquals(1, countRows(tenantId));
    }

    @Test
    public void givenOtherTenantsId_whenUpserted_thenOtherTenantsRowIsUntouched() {
        TraceCoverageSetting other = traceCoverageSettingDao.upsertByTenantId(otherTenantId, coverageSetting(42));
        TraceCoverageSetting forged = coverageSetting(7);
        forged.setId(other.getId());

        TraceCoverageSetting saved = traceCoverageSettingDao.upsertByTenantId(tenantId, forged);

        assertNotEquals(other.getId(), saved.getId());
        assertEquals(tenantId, saved.getTenantId());
        TraceCoverageSetting otherAfter = traceCoverageSettingDao.findByTenantId(otherTenantId);
        assertEquals(other.getId(), otherAfter.getId());
        assertEquals(42, otherAfter.getTracesPerInterval());
    }

    @Test
    public void givenConcurrentFirstSaves_whenUpserted_thenOneRowRemains() throws Exception {
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<TraceCoverageSetting>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int tracesPerInterval = i + 1;
                results.add(executor.submit(() -> {
                    start.await();
                    return traceCoverageSettingDao.upsertByTenantId(tenantId, coverageSetting(tracesPerInterval));
                }));
            }
            start.countDown();
            Set<UUID> ids = new HashSet<>();
            for (Future<TraceCoverageSetting> result : results) {
                ids.add(result.get(30, TimeUnit.SECONDS).getId());
            }
            assertEquals(1, ids.size());
            assertEquals(1, countRows(tenantId));
        } finally {
            executor.shutdownNow();
        }
    }

    private int countRows(UUID tenantId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trace_coverage_setting WHERE tenant_id = ?", Integer.class, tenantId);
    }

    private TraceCoverageSetting coverageSetting(int tracesPerInterval) {
        TraceCoverageSetting setting = new TraceCoverageSetting();
        setting.setTenantId(tenantId);
        setting.setEnabled(true);
        setting.setTracesPerInterval(tracesPerInterval);
        return setting;
    }

}
