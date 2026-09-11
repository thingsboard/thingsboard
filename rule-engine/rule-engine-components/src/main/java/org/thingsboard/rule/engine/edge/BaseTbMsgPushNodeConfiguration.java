// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.edge;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.AttributeScope;

@Data
public class BaseTbMsgPushNodeConfiguration implements NodeConfiguration<BaseTbMsgPushNodeConfiguration> {

    private String scope;

    @Override
    public BaseTbMsgPushNodeConfiguration defaultConfiguration() {
        BaseTbMsgPushNodeConfiguration configuration = new BaseTbMsgPushNodeConfiguration();
        configuration.setScope(AttributeScope.SERVER_SCOPE.name());
        return configuration;
    }

}
