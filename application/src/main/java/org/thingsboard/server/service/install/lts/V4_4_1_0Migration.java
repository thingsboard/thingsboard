// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.lts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.update.EdgeProfileMigrationService;

/**
 * LTS migration for version {@code 4.4.1.0}.
 * <p>
 * {@link LtsMigrationService} selects migrations from the injected {@link LtsMigration} beans, not from the on-disk
 * {@code data/upgrade/lts/<version>/} directories. So this bean is what makes the runner discover version
 * {@code 4.4.1.0} and execute its {@code data/upgrade/lts/4.4.1.0/schema_update.sql}, which creates the
 * {@code edge_profile} table and links {@code edge} to it.
 * <p>
 * Edge profiles ride the {@code 4.4.1.0} slot rather than {@code 4.4.0.0} because the feature ships in 4.4.1 only:
 * an installation upgraded to 4.4.0 must not gain the table or the profiles, and must still pick both up when it
 * later moves to 4.4.1. {@link LtsVersion#isInRange} is exclusive at the lower bound, so every source below
 * {@code 4.4.1.0} selects this migration exactly once.
 * <p>
 * The dir/bean consistency (both ways) is guarded by a test in {@code LtsMigrationIntegrationTest}.
 */
@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class V4_4_1_0Migration implements LtsMigration {

    private static final int TENANT_PAGE_SIZE = 1000;

    private final TenantService tenantService;
    private final EdgeProfileMigrationService edgeProfileMigrationService;

    @Override
    public String getVersion() {
        return "4.4.1.0";
    }

    @Override
    public void applyAfterCommit() {
        migrateEdgeProfiles();
    }

    // rides the LTS migration: the release backfill list is cleaned each release and the no-downtime path applies LTS migrations only
    private void migrateEdgeProfiles() {
        int processed = 0;
        int failed = 0;
        for (TenantId tenantId : new PageDataIterable<>(tenantService::findTenantsIds, TENANT_PAGE_SIZE)) {
            try {
                edgeProfileMigrationService.ensureEdgeProfiles(tenantId);
            } catch (Exception e) {
                failed++;
                log.error("[{}] Edge profile migration failed for tenant", tenantId, e);
            }
            processed++;
        }
        log.info("Edge profile migration processed {} tenant(s), {} failed", processed, failed);
        if (failed > 0) {
            throw new IllegalStateException("Edge profile migration failed for " + failed + " of " + processed + " tenant(s); it will be retried on the next startup");
        }
    }

}
