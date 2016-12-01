/**
 * Copyright © 2016 The Thingsboard Authors
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

import java.util.Optional;

public class BaseTsKvQuery implements TsKvQuery {

    private String key;
    private Optional<Long> startTs;
    private Optional<Long> endTs;
    private Optional<Integer> limit;

    public BaseTsKvQuery(String key, Optional<Long> startTs, Optional<Long> endTs, Optional<Integer> limit) {
        this.key = key;
        this.startTs = startTs;
        this.endTs = endTs;
        this.limit = limit;
    }
    
    public BaseTsKvQuery(String key, Long startTs, Long endTs, Integer limit) {
        this(key, Optional.ofNullable(startTs), Optional.ofNullable(endTs), Optional.ofNullable(limit));
    }

    public BaseTsKvQuery(String key, Long startTs, Integer limit) {
        this(key, startTs, null, limit);
    }

    public BaseTsKvQuery(String key, Long startTs, Long endTs) {
        this(key, startTs, endTs, null);
    }

    public BaseTsKvQuery(String key, Long startTs) {
        this(key, startTs, null, null);
    }

    public BaseTsKvQuery(String key, Integer limit) {
        this(key, null, null, limit);
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Optional<Long> getStartTs() {
        return startTs;
    }

    @Override
    public Optional<Long> getEndTs() {
        return endTs;
    }

    @Override
    public Optional<Integer> getLimit() {
        return limit;
    }
}
