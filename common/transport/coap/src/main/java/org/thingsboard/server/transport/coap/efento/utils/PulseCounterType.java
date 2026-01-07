/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
