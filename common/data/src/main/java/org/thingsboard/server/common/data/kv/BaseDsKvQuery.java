/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

@Data
public class BaseDsKvQuery implements DsKvQuery {

    private final String key;
    private final Double startDs;
    private final Double endDs;
    private final Double interval;
    private final int limit;
    private final Aggregation aggregation;

    public BaseDsKvQuery(String key, Double startDs, Double endDs, Double interval, int limit, Aggregation aggregation) {
        this.key = key;
        this.startDs = startDs;
        this.endDs = endDs;
        this.interval = interval;
        this.limit = limit;
        this.aggregation = aggregation;
    }

    public BaseDsKvQuery(String key, Double startDs, Double endDs) {
        this(key, startDs, endDs, endDs-startDs, 1, Aggregation.AVG);
    }

}
