// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.transform;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.id.EntityGroupId;

@Data
public class TbDuplicateMsgToGroupNodeConfiguration implements NodeConfiguration<TbDuplicateMsgToGroupNodeConfiguration> {

    private EntityGroupId entityGroupId;

    private boolean entityGroupIsMessageOriginator;

    @Override
    public TbDuplicateMsgToGroupNodeConfiguration defaultConfiguration() {
        var configuration = new TbDuplicateMsgToGroupNodeConfiguration();
        configuration.setEntityGroupIsMessageOriginator(true);
        return configuration;
    }
}
