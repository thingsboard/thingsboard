// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.data.dp;

import lombok.Getter;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.common.util.TbStringPool;

public class StringDataPoint extends AbstractDataPoint {

    @Getter
    private final String value;

    public StringDataPoint(long ts, String value) {
        this(ts, value, true);
    }

    public StringDataPoint(long ts, String value, boolean deduplicate) {
        super(ts);
        this.value = deduplicate ? TbStringPool.intern(value) : value;
    }

    @Override
    public boolean getBool() {
        return Boolean.parseBoolean(value);
    }

    @Override
    public double getDouble() {
        return Double.parseDouble(value);
    }

    @Override
    public long getLong() {
        return Long.parseLong(value);
    }

    @Override
    public DataType getType() {
        return DataType.STRING;
    }

    @Override
    public String getStr() {
        return value;
    }

    @Override
    public String valueToString() {
        return value;
    }

}
