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
package org.thingsboard.server.dao.aspect;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Data
public class DbCallStats {

    private final TenantId tenantId;
    private final ConcurrentMap<String, MethodCallStats> methodStats = new ConcurrentHashMap<>();
    private final AtomicInteger successCalls = new AtomicInteger();
    private final AtomicInteger failureCalls = new AtomicInteger();

    public void onMethodCall(String methodName, boolean success, long executionTime) {
        var methodCallStats = methodStats.computeIfAbsent(methodName, m -> new MethodCallStats());
        methodCallStats.getExecutions().incrementAndGet();
        methodCallStats.getTiming().addAndGet(executionTime);
        if (success) {
            successCalls.incrementAndGet();
        } else {
            failureCalls.incrementAndGet();
            methodCallStats.getFailures().incrementAndGet();
        }
    }

    public DbCallStatsSnapshot snapshot() {
        return DbCallStatsSnapshot.builder()
                .tenantId(tenantId)
                .totalSuccess(successCalls.get())
                .totalFailure(failureCalls.get())
                .methodStats(methodStats.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().snapshot())))
                .totalTiming(methodStats.values().stream().map(MethodCallStats::getTiming).map(AtomicLong::get).reduce(0L, Long::sum))
                .build();
    }

}
