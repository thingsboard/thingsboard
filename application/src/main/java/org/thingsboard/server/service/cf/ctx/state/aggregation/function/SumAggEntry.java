// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state.aggregation.function;

import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;
import org.thingsboard.common.util.NumberUtils;

import java.math.BigDecimal;

public class SumAggEntry extends BaseAggEntry {

    private BigDecimal sum = BigDecimal.ZERO;

    @Override
    protected void doUpdate(double value) {
        if (value != 0.0) {
            sum = sum.add(BigDecimal.valueOf(value));
        }
    }

    @Override
    protected Object prepareResult(Integer precision) {
        return NumberUtils.roundResult(sum.doubleValue(), precision);
    }

    @Override
    public AggFunction getType() {
        return AggFunction.SUM;
    }
}
