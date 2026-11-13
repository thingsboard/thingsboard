// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install.lts;

import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class V4_3_1_10Migration implements Migration {

    @Override
    public String getVersion() {
        return "4.3.1.10";
    }

}
