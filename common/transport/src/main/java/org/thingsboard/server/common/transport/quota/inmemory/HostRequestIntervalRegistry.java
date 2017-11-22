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
package org.thingsboard.server.common.transport.quota.inmemory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Vitaliy Paromskiy
 * @version 1.0
 */
@Component
@Slf4j
public class HostRequestIntervalRegistry {

    private final Map<String, IntervalCount> hostCounts = new ConcurrentHashMap<>();
    private final long intervalDurationMs;
    private final long ttlMs;

    public HostRequestIntervalRegistry(@Value("${quota.host.intervalMs}") long intervalDurationMs,
                                       @Value("${quota.host.ttlMs}") long ttlMs) {
        this.intervalDurationMs = intervalDurationMs;
        this.ttlMs = ttlMs;
    }

    @PostConstruct
    public void init() {
        if (ttlMs < intervalDurationMs) {
            log.warn("TTL for IntervalRegistry [{}] smaller than interval duration [{}]", ttlMs, intervalDurationMs);
        }
    }

    public long tick(String clientHostId) {
        IntervalCount intervalCount = hostCounts.computeIfAbsent(clientHostId, s -> new IntervalCount(intervalDurationMs));
        return intervalCount.resetIfExpiredAndTick();
    }

    public void clean() {
        hostCounts.entrySet().removeIf(entry -> entry.getValue().silenceDuration() > ttlMs);
    }

    public Map<String, Long> getContent() {
        return hostCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        interval -> interval.getValue().getCount()));
    }
}
