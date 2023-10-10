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
package org.thingsboard.server.common.data;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.nustaq.serialization.FSTConfiguration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
public class FSTUtils {

    public static final FSTConfiguration CONFIG = FSTConfiguration.createDefaultConfiguration();

    private static final ConcurrentHashMap<String, Stats> STATS = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> T decode(byte[] byteArray) {
        long startTime = System.nanoTime();
        T result = byteArray != null && byteArray.length > 0 ? (T) CONFIG.asObject(byteArray) : null;
        long endTime = System.nanoTime();

        if (log.isDebugEnabled() && result != null) {
            String className = result.getClass().getSimpleName();
            STATS.computeIfAbsent(className, k -> new Stats()).incrementDecode(endTime - startTime);
        }

        return result;
    }

    public static <T> byte[] encode(T msg) {
        long startTime = System.nanoTime();
        byte[] result = CONFIG.asByteArray(msg);
        long endTime = System.nanoTime();

        if (log.isDebugEnabled() && msg != null) {
            String className = msg.getClass().getSimpleName();
            STATS.computeIfAbsent(className, k -> new Stats()).incrementEncode(endTime - startTime);
        }

        return result;
    }

    public static void printStats() {
        if (log.isDebugEnabled()) {
            List<String> topDecode = STATS.entrySet().stream()
                    .filter(e -> e.getValue().getAvgDecodeTime() > 0)
                    .sorted((e1, e2) -> Long.compare(e2.getValue().getAvgDecodeTime(), e1.getValue().getAvgDecodeTime()))
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            List<String> topEncode = STATS.entrySet().stream()
                    .filter(e -> e.getValue().getAvgEncodeTime() > 0)
                    .sorted((e1, e2) -> Long.compare(e2.getValue().getAvgEncodeTime(), e1.getValue().getAvgEncodeTime()))
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            List<String> topDecodeCount = STATS.entrySet().stream()
                    .filter(e -> e.getValue().getDecodeCount().get() > 0)
                    .sorted((e1, e2) -> Long.compare(e2.getValue().getDecodeCount().get(), e1.getValue().getDecodeCount().get()))
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            List<String> topEncodeCount = STATS.entrySet().stream()
                    .filter(e -> e.getValue().getEncodeCount().get() > 0)
                    .sorted((e1, e2) -> Long.compare(e2.getValue().getEncodeCount().get(), e1.getValue().getEncodeCount().get()))
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            for (Map.Entry<String, Stats> entry : STATS.entrySet()) {
                Stats stats = entry.getValue();
                if (stats.isNotEmpty()) {
                    log.debug("[FST stats] [{}] {}", entry.getKey(), stats);
                    stats.reset();
                }
            }

            log.debug("[FST stats] Top 5 slowest 'decode' {}", topDecode);
            log.debug("[FST stats] Top 5 slowest 'encode' {}", topEncode);
            log.debug("[FST stats] Top 5 'decode' count {}", topDecodeCount);
            log.debug("[FST stats] Top 5 'encode' count {}", topEncodeCount);
        }
    }

    @Data
    private static class Stats {
        private final AtomicLong encodeCount = new AtomicLong();
        private final AtomicLong decodeCount = new AtomicLong();
        private final AtomicLong totalEncodeTime = new AtomicLong();
        private final AtomicLong totalDecodeTime = new AtomicLong();

        private void incrementEncode(long time) {
            encodeCount.incrementAndGet();
            totalEncodeTime.addAndGet(time);
        }

        private void incrementDecode(long time) {
            decodeCount.incrementAndGet();
            totalDecodeTime.addAndGet(time);
        }

        private boolean isNotEmpty() {
            return encodeCount.get() > 0 || decodeCount.get() > 0;
        }

        private long getAvgEncodeTime() {
            long count = encodeCount.get();
            return count > 0 ? totalEncodeTime.get() / count : 0;
        }

        private long getAvgDecodeTime() {
            long count = decodeCount.get();
            return count > 0 ? totalDecodeTime.get() / count : 0;
        }

        private void reset() {
            encodeCount.set(0);
            decodeCount.set(0);
            totalEncodeTime.set(0);
            totalDecodeTime.set(0);
        }

        @Override
        public String toString() {
            return String.format("decodeCount [%d] avgDecodeTime [%d] encodedCount [%d] avgEncodeTime [%d]", decodeCount.get(), getAvgDecodeTime(), encodeCount.get(), getAvgEncodeTime());
        }
    }

}
