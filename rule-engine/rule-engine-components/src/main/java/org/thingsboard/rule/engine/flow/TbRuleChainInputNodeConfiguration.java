// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.flow;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbRuleChainInputNodeConfiguration implements NodeConfiguration<TbRuleChainInputNodeConfiguration> {

    private String ruleChainId;
    private boolean forwardMsgToDefaultRuleChain;

    @Override
    public TbRuleChainInputNodeConfiguration defaultConfiguration() {
        TbRuleChainInputNodeConfiguration configuration = new TbRuleChainInputNodeConfiguration();
        configuration.setForwardMsgToDefaultRuleChain(false);
        return configuration;
    }

}
