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
