// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.lts;

import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * Adds the {@code device_certificate_revocation} table that holds X.509 device certificates revoked by tenants.
 * <p>
 * {@link LtsMigrationService} selects migrations from the injected {@link LtsMigration} beans, not from the on-disk
 * {@code data/upgrade/lts/<version>/} directories, so this bean is what makes the runner discover version
 * {@code 4.4.0.1} and execute its {@code schema_update.sql}. The change is schema-only.
 */
@Component
@TbCoreComponent
public class V4_4_0_1Migration implements LtsMigration {

    @Override
    public String getVersion() {
        return "4.4.0.1";
    }

}
