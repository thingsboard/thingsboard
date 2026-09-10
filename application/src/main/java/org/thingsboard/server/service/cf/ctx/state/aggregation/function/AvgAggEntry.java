// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state.aggregation.function;

import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;
import org.thingsboard.common.util.NumberUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AvgAggEntry extends BaseAggEntry {

    private BigDecimal sum = BigDecimal.ZERO;
    private long count = 0L;

    @Override
    protected void doUpdate(double value) {
        if (value != 0.0) {
            sum = sum.add(BigDecimal.valueOf(value));
        }
        this.count++;
    }

    @Override
    protected Object prepareResult(Integer precision) {
        double result = sum.divide(BigDecimal.valueOf(count), RoundingMode.HALF_UP).doubleValue();
        return NumberUtils.roundResult(result, precision);
    }

    @Override
    public AggFunction getType() {
        return AggFunction.AVG;
    }
}
