// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.data.dp;

import lombok.Getter;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.common.util.TbStringPool;

public class JsonDataPoint extends AbstractDataPoint {

    @Getter
    private final String value;

    public JsonDataPoint(long ts, String value) {
        super(ts);
        this.value = TbStringPool.intern(value);
    }

    @Override
    public DataType getType() {
        return DataType.JSON;
    }

    @Override
    public String getJson() {
        return value;
    }

    @Override
    public String valueToString() {
        return value;
    }

}
