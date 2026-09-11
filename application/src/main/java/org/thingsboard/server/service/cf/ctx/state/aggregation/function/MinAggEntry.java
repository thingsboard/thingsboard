// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state.aggregation.function;

import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;
import org.thingsboard.common.util.NumberUtils;

public class MinAggEntry extends BaseAggEntry {

    private double min = Double.MAX_VALUE;

    @Override
    protected void doUpdate(double value) {
        if (value < min) {
            min = value;
        }
    }

    @Override
    protected Object prepareResult(Integer precision) {
        return NumberUtils.roundResult(min, precision);
    }

    @Override
    public AggFunction getType() {
        return AggFunction.MIN;
    }
}
