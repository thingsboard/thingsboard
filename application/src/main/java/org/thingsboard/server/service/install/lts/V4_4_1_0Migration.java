// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.lts;

import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * Registration-only migration with no {@link #apply()} (no data migration).
 * <p>
 * {@link MigrationService} selects migrations from the injected {@link Migration} beans, not from the
 * on-disk {@code data/upgrade/lts/<version>/} directories. So this bean is what makes the runner discover
 * version {@code 4.4.1.0} and execute its {@code data/upgrade/lts/4.4.1.0/schema_update.sql}, which adds the
 * {@code idx_calculated_field_tenant_id_id} index used by tenant-scoped calculated field paging and the rule chain
 * tracing and rule engine monitoring tables.
 * <p>
 * It needs a slot of its own rather than {@code 4.4.0.0}: that version is already released, and
 * {@link Version#isInRange} is exclusive at the lower bound, so an installation already on {@code 4.4.0.0}
 * would never pick up a change added there.
 * <p>
 * The dir/bean consistency (both ways) is guarded by a test in {@code MigrationIntegrationTest}.
 */
@Component
@TbCoreComponent
public class V4_4_1_0Migration implements Migration {

    @Override
    public String getVersion() {
        return "4.4.1.0";
    }
}
