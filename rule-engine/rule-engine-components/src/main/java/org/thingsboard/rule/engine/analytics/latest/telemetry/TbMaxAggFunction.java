// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

public class TbMaxAggFunction extends TbBaseAggFunction {

    private double max = -Double.MAX_VALUE;

    @Override
    protected void doUpdate(double value) {
        if (value > max) {
            max = value;
        }
    }

    @Override
    protected double prepareResult() {
        return max;
    }

}
