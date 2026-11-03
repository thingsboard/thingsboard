// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util.geo;

public enum RangeUnit {
    METER(1000.0), KILOMETER(1.0), FOOT(3280.84), MILE(0.62137), NAUTICAL_MILE(0.539957);

    private final double fromKm;

    RangeUnit(double fromKm) {
        this.fromKm = fromKm;
    }

    public double fromKm(double v) {
        return v * fromKm;
    }
}
