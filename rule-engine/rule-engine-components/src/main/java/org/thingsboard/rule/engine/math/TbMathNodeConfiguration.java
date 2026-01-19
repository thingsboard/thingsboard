/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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