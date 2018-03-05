/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.google.common.collect.MinMaxPriorityQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Vitaliy Paromskiy
 * @version 1.0
 */
@Component
@Slf4j
public class IntervalRegistryLogger {

    private final int topSize;
    private final HostRequestIntervalRegistry intervalRegistry;
    private final long logIntervalMin;
    private ScheduledExecutorService executor;

    public IntervalRegistryLogger(@Value("${quota.log.topSize}") int topSize, @Value("${quota.log.intervalMin}") long logIntervalMin,
                                  HostRequestIntervalRegistry intervalRegistry) {
        this.topSize = topSize;
        this.logIntervalMin = logIntervalMin;
        this.intervalRegistry = intervalRegistry;
    }

    public void schedule() {
        if (executor != null) {
            throw new IllegalStateException("Registry Cleaner already scheduled");
        }
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::logStatistic, logIntervalMin, logIntervalMin, TimeUnit.MINUTES);
    }

    public void stop() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    public void logStatistic() {
        Map<String, Long> registryContent = intervalRegistry.getContent();
        int uniqHosts = registryContent.size();
        long requestsCount = registryContent.values().stream().mapToLong(i -> i).sum();
        Map<String, Long> top = getTopElements(registryContent);
        log(top, uniqHosts, requestsCount);
    }

    protected Map<String, Long> getTopElements(Map<String, Long> countMap) {
        MinMaxPriorityQueue<Map.Entry<String, Long>> topQueue = MinMaxPriorityQueue
                .orderedBy(Comparator.comparing((Function<Map.Entry<String, Long>, Long>) Map.Entry::getValue).reversed())
                .maximumSize(topSize)
                .create(countMap.entrySet());

        return topQueue.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void log(Map<String, Long> top, int uniqHosts, long requestsCount) {
        long rps = requestsCount / TimeUnit.MINUTES.toSeconds(logIntervalMin);
        StringBuilder builder = new StringBuilder("Quota Statistic : ");
        builder.append("uniqHosts : ").append(uniqHosts).append("; ");
        builder.append("requestsCount : ").append(requestsCount).append("; ");
        builder.append("RPS : ").append(rps).append(" ");
        builder.append("top -> ");
        for (Map.Entry<String, Long> host : top.entrySet()) {
            builder.append(host.getKey()).append(" : ").append(host.getValue()).append("; ");
        }

        log.info(builder.toString());
    }
}
