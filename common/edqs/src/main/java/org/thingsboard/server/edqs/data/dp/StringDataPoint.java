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
