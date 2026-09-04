// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import lombok.Data;

import java.io.Serializable;

@Data
public class ApiUsageRecordState implements Serializable {

    private final ApiFeature apiFeature;
    private final ApiUsageRecordKey key;
    private final long threshold;
    private final long value;

    public String getValueAsString() {
        return valueAsString(value);
    }

    public String getThresholdAsString() {
        return valueAsString(threshold);
    }

    private String valueAsString(long value) {
        if (value > 1_000_000 && value % 1_000_000 < 10_000) {
            return value / 1_000_000 + "M";
        } else if (value > 10_000) {
            return String.format("%.2fM", ((double) value) / 1_000_000);
        } else {
            return value + "";
        }
    }

}
