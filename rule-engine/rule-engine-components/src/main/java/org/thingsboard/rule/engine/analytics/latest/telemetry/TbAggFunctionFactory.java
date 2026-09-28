// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import org.thingsboard.rule.engine.analytics.incoming.MathFunction;

public final class TbAggFunctionFactory {

    private TbAggFunctionFactory() {}

    public static TbAggFunction createAggFunction(MathFunction mathFunction) {
        return switch (mathFunction) {
            case MIN -> new TbMinAggFunction();
            case MAX -> new TbMaxAggFunction();
            case SUM -> new TbSumAggFunction();
            case AVG -> new TbAvgAggFunction();
            case COUNT -> new TbCountAggFunction();
            case COUNT_UNIQUE -> new TbCountUniqueAggFunction();
        };
    }

}
