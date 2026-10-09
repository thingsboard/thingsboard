// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install.lts;

import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * Registration-only migration with no {@link #apply()} (no data migration), kept intentionally.
 * <p>
 * {@link LtsMigrationService} selects migrations from the injected {@link LtsMigration} beans, not from the
 * on-disk {@code data/upgrade/lts/<version>/} directories. So this bean is what makes the runner discover
 * version {@code 4.3.1.2} and execute its {@code data/upgrade/lts/4.3.1.2/schema_update.sql} (which adds the
 * 4.3.1.2 columns: {@code api_usage_state.ai}, {@code calculated_field.additional_info},
 * {@code role.excluded_permissions}, {@code tb_user.external_id} plus the {@code tb_user_external_id_unq_key}
 * constraint). A directory holding a {@code schema_update.sql} but lacking a matching bean would be silently skipped.
 * <p>
 * The dir/bean consistency (both ways) is guarded by a test in {@code LtsMigrationIntegrationTest}.
 */
@Component
@TbCoreComponent
public class V4_3_1_2Migration implements LtsMigration {

    @Override
    public String getVersion() {
        return "4.3.1.2";
    }
}
