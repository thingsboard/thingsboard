// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.lts;

import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * LTS migration for version {@code 4.4.1.0}. Registration-only, with no data migration.
 * <p>
 * {@link LtsMigrationService} selects migrations from the injected {@link LtsMigration} beans, not from the on-disk
 * {@code data/upgrade/lts/<version>/} directories. So this bean is what makes the runner discover version
 * {@code 4.4.1.0} and execute its {@code data/upgrade/lts/4.4.1.0/schema_update.sql}, which adds the {@code edge} and
 * {@code integration} feature states to {@code api_usage_state}.
 * <p>
 * The change rides the {@code 4.4.1.0} slot because it ships in 4.4.1, and the no-downtime 4.4.0 -> 4.4.1 path applies
 * LTS migrations only. {@link LtsVersion#isInRange} is exclusive at the lower bound, so every source below
 * {@code 4.4.1.0} selects this migration exactly once.
 * <p>
 * The dir/bean consistency (both ways) is guarded by a test in {@code LtsMigrationIntegrationTest}.
 */
@Component
@TbCoreComponent
public class V4_4_1_0Migration implements LtsMigration {

    @Override
    public String getVersion() {
        return "4.4.1.0";
    }

}
