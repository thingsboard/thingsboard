/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import lombok.Getter;

public enum TbRuleNodeMathFunctionType {

    ADD(2), SUB(2), MULT(2), DIV(2), SIN(1), COS(1), SQRT(1), ABS(1);

    @Getter
    private final int minArgs;
    @Getter
    private final int maxArgs;

    TbRuleNodeMathFunctionType(int args) {
        this(args, args);
    }

    TbRuleNodeMathFunctionType(int minArgs, int maxArgs) {
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
    }
}
