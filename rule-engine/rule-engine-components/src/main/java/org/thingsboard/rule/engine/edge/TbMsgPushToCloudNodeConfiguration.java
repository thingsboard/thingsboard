// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.edge;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.AttributeScope;

@EqualsAndHashCode(callSuper = true)
@Data
public class TbMsgPushToCloudNodeConfiguration extends BaseTbMsgPushNodeConfiguration {

    @Override
    public TbMsgPushToCloudNodeConfiguration defaultConfiguration() {
        TbMsgPushToCloudNodeConfiguration configuration = new TbMsgPushToCloudNodeConfiguration();
        configuration.setScope(AttributeScope.SERVER_SCOPE.name());
        return configuration;
    }

}
