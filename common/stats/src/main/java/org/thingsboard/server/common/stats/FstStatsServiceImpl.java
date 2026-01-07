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
package org.thingsboard.server.common.stats;

import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.FstStatsService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class FstStatsServiceImpl implements FstStatsService {
    private final ConcurrentHashMap<String, StatsCounter> encodeCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, StatsCounter> decodeCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> encodeTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> decodeTimer = new ConcurrentHashMap<>();

    @Autowired
    private StatsFactory statsFactory;

    @Override
    public void incrementEncode(Class<?> clazz) {
        encodeCounters.computeIfAbsent(clazz.getSimpleName(), key -> statsFactory.createStatsCounter("fst_encode", key)).increment();
    }

    @Override
    public void incrementDecode(Class<?> clazz) {
        decodeCounters.computeIfAbsent(clazz.getSimpleName(), key -> statsFactory.createStatsCounter("fst_decode", key)).increment();
    }

    @Override
    public void recordEncodeTime(Class<?> clazz, long startTime) {
        encodeTimers.computeIfAbsent(clazz.getSimpleName(),
                key -> statsFactory.createTimer("fst_encode_time", "statsName", key)).record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordDecodeTime(Class<?> clazz, long startTime) {
        decodeTimer.computeIfAbsent(clazz.getSimpleName(),
                key -> statsFactory.createTimer("fst_decode_time", "statsName", key)).record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
    }

}
