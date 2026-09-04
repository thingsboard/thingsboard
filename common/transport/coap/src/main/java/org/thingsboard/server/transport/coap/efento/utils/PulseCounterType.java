// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap.efento.utils;

public enum PulseCounterType {

    WATER_CNT_ACC("water_cnt_acc_", 100),
    PULSE_CNT_ACC("pulse_cnt_acc_", 1000),
    ELEC_METER_ACC("elec_meter_acc_", 1000),
    PULSE_CNT_ACC_WIDE("pulse_cnt_acc_wide_", 1000000);

    private final String prefix;
    private final int majorResolution;

    PulseCounterType(String prefix, int majorResolution) {
        this.prefix = prefix;
        this.majorResolution = majorResolution;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getMajorResolution() {
        return majorResolution;
    }
}
