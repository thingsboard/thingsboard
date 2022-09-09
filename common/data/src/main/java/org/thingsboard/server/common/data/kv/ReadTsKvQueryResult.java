/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.common.data.kv;

import lombok.Data;
import org.thingsboard.server.common.data.query.TsValue;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReadTsKvQueryResult {

    private final String key;
    // Holds the data list;
    private final List<TsKvEntry> data;
    // Holds the max ts of the records that match aggregation intervals (not the ts of the aggregation window, but the ts of the last record among all the intervals)
    private final long lastEntryTs;


    public TsValue[] toTsValues() {
        if (data != null && !data.isEmpty()) {
            List<TsValue> queryValues = new ArrayList<>();
            for (TsKvEntry v : data) {
                queryValues.add(v.toTsValue()); // TODO: add count here.
            }
            return queryValues.toArray(new TsValue[queryValues.size()]);
        } else {
            return new TsValue[0];
        }
    }

}
