/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.FstStatsService;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class FstStatsServiceImpl implements FstStatsService {
    private final ConcurrentHashMap<String, StatsCounter> encodeCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, StatsCounter> decodeCounters = new ConcurrentHashMap<>();

    @Autowired
    private StatsFactory statsFactory;

    @Override
    public void incrementEncode(Class<?> clazz) {
        encodeCounters.computeIfAbsent(clazz.getSimpleName(), key -> statsFactory.createStatsCounter("fstEncode", key)).increment();
    }

    @Override
    public void incrementDecode(Class<?> clazz) {
        decodeCounters.computeIfAbsent(clazz.getSimpleName(), key -> statsFactory.createStatsCounter("fstDecode", key)).increment();
    }

}
