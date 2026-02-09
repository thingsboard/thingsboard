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

import lombok.Getter;

public enum TbRuleNodeMathFunctionType {

    ADD(2), SUB(2), MULT(2), DIV(2),
    SIN, SINH, COS, COSH, TAN, TANH, ACOS, ASIN, ATAN, ATAN2(2),
    EXP, EXPM1, SQRT, CBRT, GET_EXP(1, 1, true), HYPOT(2), LOG, LOG10, LOG1P,
    CEIL(1, 1, true), FLOOR(1, 1, true), FLOOR_DIV(2), FLOOR_MOD(2),
    ABS, MIN(2), MAX(2), POW(2), SIGNUM, RAD, DEG,

    CUSTOM(0, 16, false); //Custom function based on exp4j

    @Getter
    private final int minArgs;
    @Getter
    private final int maxArgs;
    @Getter
    private final boolean integerResult;

    TbRuleNodeMathFunctionType() {
        this(1, 1, false);
    }

    TbRuleNodeMathFunctionType(int args) {
        this(args, args, false);
    }

    TbRuleNodeMathFunctionType(int minArgs, int maxArgs, boolean integerResult) {
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
        this.integerResult = integerResult;
    }

}
