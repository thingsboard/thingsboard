// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.rpc;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbSendRpcRequestNodeConfiguration implements NodeConfiguration<TbSendRpcRequestNodeConfiguration> {

    private int timeoutInSeconds;

    @Override
    public TbSendRpcRequestNodeConfiguration defaultConfiguration() {
        TbSendRpcRequestNodeConfiguration configuration = new TbSendRpcRequestNodeConfiguration();
        configuration.setTimeoutInSeconds(60);
        return configuration;
    }
}
