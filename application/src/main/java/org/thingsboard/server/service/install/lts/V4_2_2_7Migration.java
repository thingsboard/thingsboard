// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install.lts;

import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class V4_2_2_7Migration implements LtsMigration {

    @Override
    public String getVersion() {
        return "4.2.2.7";
    }

}
