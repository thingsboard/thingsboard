// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TbAvgAggFunction extends TbBaseAggFunction {

    private BigDecimal sum = BigDecimal.ZERO;
    private long count = 0L;

    @Override
    protected void doUpdate(double value) {
        if (value != 0.0) {
            sum = sum.add(BigDecimal.valueOf(value));
        }
        count++;
    }

    @Override
    protected double prepareResult() {
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP).doubleValue();
    }

}
