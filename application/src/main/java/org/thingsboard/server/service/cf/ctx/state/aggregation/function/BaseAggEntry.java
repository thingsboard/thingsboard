// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state.aggregation.function;

import java.util.Optional;

public abstract class BaseAggEntry implements AggEntry {

    private boolean hasResult = false;

    @Override
    public void update(Object value) {
        doUpdate(extractDoubleValue(value));
        hasResult = true;
    }

    @Override
    public Optional<Object> result(Integer precision) {
        if (hasResult) {
            hasResult = false;
            return Optional.of(prepareResult(precision));
        } else {
            return Optional.empty();
        }
    }

    protected abstract void doUpdate(double value);

    protected abstract Object prepareResult(Integer precision);

    protected double extractDoubleValue(Object value) {
        try {
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            throw new NumberFormatException("Cannot parse value " + value.toString());
        }
    }

}
