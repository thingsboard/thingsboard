// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.lts;

import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * Adds the {@code device_certificate_revocation} table. Schema-only: the bean makes the runner execute its {@code schema_update.sql}.
 */
@Component
@TbCoreComponent
public class V4_4_1_0Migration implements LtsMigration {

    @Override
    public String getVersion() {
        return "4.4.1.0";
    }

}
