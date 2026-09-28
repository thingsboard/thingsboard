// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

public class TbMinAggFunction extends TbBaseAggFunction {

    private double min = Double.MAX_VALUE;

    @Override
    protected void doUpdate(double value) {
        if (value < min) {
            min = value;
        }
    }

    @Override
    protected double prepareResult() {
        return min;
    }

}
