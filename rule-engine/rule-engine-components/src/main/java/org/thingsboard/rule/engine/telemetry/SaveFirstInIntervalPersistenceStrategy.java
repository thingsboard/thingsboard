/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.telemetry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.BloomFilter;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public final class SaveFirstInIntervalPersistenceStrategy implements PersistenceStrategy {

    private final long intervalDurationMillis;
    private final BloomFilter<Key> filter;

    @JsonCreator
    public SaveFirstInIntervalPersistenceStrategy(@JsonProperty("intervalDurationMillis") long intervalDurationMillis) {
        this.intervalDurationMillis = intervalDurationMillis;
        // TODO: implement funnel as an enum
        filter = BloomFilter.create((key, sink) ->
                sink.putLong(key.intervalNumber())
                        .putLong(key.originatorUuid().getMostSignificantBits())
                        .putLong(key.originatorUuid().getLeastSignificantBits())
                        .putString(key.timeseriesKey(), StandardCharsets.UTF_8), 1_000_000);
    }

    // TODO: this should not be hardcoded here but should be defined by clients
    //       should be generified (what to do with funnel then?)
    private record Key(long intervalNumber, UUID originatorUuid, String timeseriesKey) {
    }

    @Override
    public boolean shouldPersist(UUID originatorUuid, TsKvEntry timeseriesEntry) {
        long intervalNumber = timeseriesEntry.getTs() / intervalDurationMillis;
        return filter.put(new Key(intervalNumber, originatorUuid, timeseriesEntry.getKey()));
    }

}
