// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.data.dp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.edqs.DataPoint;

@RequiredArgsConstructor
public abstract class AbstractDataPoint implements DataPoint {

    @Getter
    private final long ts;

    @Override
    public String getStr() {
        throw new RuntimeException(NOT_SUPPORTED);
    }

    @Override
    public long getLong() {
        throw new RuntimeException(NOT_SUPPORTED);
    }

    @Override
    public double getDouble() {
        throw new RuntimeException(NOT_SUPPORTED);
    }

    @Override
    public boolean getBool() {
        throw new RuntimeException(NOT_SUPPORTED);
    }

    @Override
    public String getJson() {
        throw new RuntimeException(NOT_SUPPORTED);
    }

    public String toString() {
        return valueToString();
    }

    @Override
    public int compareTo(DataPoint dataPoint) {
        return StringUtils.compareIgnoreCase(valueToString(), dataPoint.valueToString());
    }

}
