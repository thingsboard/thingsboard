// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state.aggregation.function;

import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;
import org.thingsboard.common.util.NumberUtils;

public class MaxAggEntry extends BaseAggEntry {

    private double max = Double.MIN_VALUE;

    @Override
    protected void doUpdate(double value) {
        if (value > max) {
            max = value;
        }
    }

    @Override
    protected Object prepareResult(Integer precision) {
        return NumberUtils.roundResult(max, precision);
    }

    @Override
    public AggFunction getType() {
        return AggFunction.MAX;
    }
}
