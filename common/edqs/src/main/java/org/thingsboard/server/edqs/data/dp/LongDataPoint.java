// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.data.dp;

import lombok.Getter;
import org.thingsboard.server.common.data.edqs.DataPoint;
import org.thingsboard.server.common.data.kv.DataType;

public class LongDataPoint extends AbstractDataPoint {

    @Getter
    private final long value;

    public LongDataPoint(long ts, long value) {
        super(ts);
        this.value = value;
    }

    @Override
    public DataType getType() {
        return DataType.LONG;
    }

    @Override
    public long getLong() {
        return value;
    }

    @Override
    public double getDouble() {
        return value;
    }

    @Override
    public String valueToString() {
        return Long.toString(value);
    }

    @Override
    public int compareTo(DataPoint dataPoint) {
        if (dataPoint.getType() == DataType.DOUBLE) {
            return Double.compare(getDouble(), dataPoint.getDouble());
        } else if (dataPoint.getType() == DataType.LONG) {
            return Long.compare(value, dataPoint.getLong());
        } else {
            return super.compareTo(dataPoint);
        }
    }
}
