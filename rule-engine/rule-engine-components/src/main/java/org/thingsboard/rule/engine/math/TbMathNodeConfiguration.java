// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.math;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.List;

@Data
public class TbMathNodeConfiguration implements NodeConfiguration<TbMathNodeConfiguration> {

    private TbRuleNodeMathFunctionType operation;
    private List<TbMathArgument> arguments;
    private String customFunction;
    private TbMathResult result;

    @Override
    public TbMathNodeConfiguration defaultConfiguration() {
        TbMathNodeConfiguration configuration = new TbMathNodeConfiguration();
        configuration.setOperation(TbRuleNodeMathFunctionType.CUSTOM);
        configuration.setCustomFunction("(x - 32) / 1.8");
        configuration.setArguments(List.of(new TbMathArgument("x", TbMathArgumentType.MESSAGE_BODY, "temperature")));
        configuration.setResult(new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "temperatureCelsius", 2, false, false, null));
        return configuration;
    }
}