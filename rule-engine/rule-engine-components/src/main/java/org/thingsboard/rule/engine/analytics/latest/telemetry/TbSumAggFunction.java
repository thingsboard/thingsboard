// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import java.math.BigDecimal;

public class TbSumAggFunction extends TbBaseAggFunction {

    private BigDecimal sum = BigDecimal.ZERO;

    @Override
    protected void doUpdate(double value) {
        if (value != 0.0) {
            sum = sum.add(BigDecimal.valueOf(value));
        }
    }

    @Override
    protected double prepareResult() {
        return sum.doubleValue();
    }

}
