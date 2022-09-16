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
package org.thingsboard.server.dao.aspect;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Aspect
@ConditionalOnProperty(prefix = "sql", value = "log_tenant_stats", havingValue = "true")
@Component
@Slf4j
public class TenantDbCallAspect {

    private final Set<String> invalidTenantDbCallMethods = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<TenantId, DbCallStats> statsMap = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${sql.log_tenant_stats.log_tenant_stats_interval:60000}")
    public void printStats() {
        try {
            if (log.isTraceEnabled()) {
                List<DbCallStatsSnapshot> snapshots = snapshot();
                logTopNTenants(snapshots, Comparator.comparing(DbCallStatsSnapshot::getTotalTiming).reversed(), 0, snapshot -> {
                    log.trace("[{}]: calls: {}, exec time: {} ", snapshot.getTenantId(), snapshot.getTotalCalls(), snapshot.getTotalTiming());
                    snapshot.getMethodExecutions().forEach((method, count) -> {
                        log.trace("[{}]: method: {}, count: {}, exec time: {}",
                                snapshot.getTenantId(), method, count, snapshot.getMethodTimings().getOrDefault(method, 0L));
                    });
                });
                // todo: log top 10 tenants for each method sorted by number of execution.
            } else if (log.isDebugEnabled()) {
                List<DbCallStatsSnapshot> snapshots = snapshot();
                log.debug("Total calls statistics below:");
                logTopNTenants(snapshots, Comparator.comparingInt(DbCallStatsSnapshot::getTotalCalls).reversed(),
                        10, s -> logSnapshotWithDebugLevel(s, 10));
                log.debug("Total timing statistics below:");
                logTopNTenants(snapshots, Comparator.comparingLong(DbCallStatsSnapshot::getTotalTiming).reversed(),
                        10, s -> logSnapshotWithDebugLevel(s, 10));
                log.debug("Total errors statistics below:");
                logTopNTenants(snapshots, Comparator.comparingInt(DbCallStatsSnapshot::getTotalFailure).reversed(),
                        10, s -> logSnapshotWithDebugLevel(s, 10));
            } else if (log.isInfoEnabled()) {
                log.debug("Total calls statistics below:");
                List<DbCallStatsSnapshot> snapshots = snapshot();
                logTopNTenants(snapshots, Comparator.comparingInt(DbCallStatsSnapshot::getTotalFailure).reversed(),
                        3, s -> logSnapshotWithDebugLevel(s, 3));
            }
        } finally {
            statsMap.clear();
        }
    }

    private void logSnapshotWithDebugLevel(DbCallStatsSnapshot snapshot, int limit) {
        log.debug("[{}]: calls: {}, failures: {}, exec time: {} ",
                snapshot.getTenantId(), snapshot.getTotalCalls(), snapshot.getTotalFailure(), snapshot.getTotalTiming());
        var stream = snapshot.getMethodTimings().entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        if (limit > 0) {
            stream = stream.limit(limit);
        }
        stream.forEach(e -> {
            long timing = snapshot.getMethodTimings().getOrDefault(e.getKey(), 0L);
            log.debug("[{}]: method: {}, count: {}, exec time: {}", snapshot.getTenantId(), e.getKey(), e.getValue(), timing);
        });
    }

    private List<DbCallStatsSnapshot> snapshot() {
        return statsMap.values().stream().map(DbCallStats::snapshot).collect(Collectors.toList());
    }

//    private void logTopNMethods(List<DbCallStatsSnapshot> snapshots, Comparator<DbCallStatsSnapshot> comparator,
//                                int n, Consumer<DbCallStatsSnapshot> logFunction) {
////        var stream = snapshots.stream().sorted(comparator).sorted();
//        // find top methods by execution time and then top
//        if (n > 0) {
//            stream = stream.limit(n);
//        }
//        stream.forEach(logFunction);
//    }

    private void logTopNTenants(List<DbCallStatsSnapshot> snapshots, Comparator<DbCallStatsSnapshot> comparator,
                                int n, Consumer<DbCallStatsSnapshot> logFunction) {
        var stream = snapshots.stream().sorted(comparator).sorted();
        if (n > 0) {
            stream = stream.limit(n);
        }
        stream.forEach(logFunction);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Around("@annotation(org.thingsboard.server.dao.util.TenantDbCall)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        var signature = joinPoint.getSignature();
        var method = signature.toShortString();
        if (invalidTenantDbCallMethods.contains(method)) {
            //Simply call the method if tenant is not found
            return joinPoint.proceed();
        }
        var tenantId = getTenantId(method, joinPoint.getArgs());
        if (tenantId == null) {
            //Simply call the method if tenant is null
            return joinPoint.proceed();
        }
        var startTime = System.currentTimeMillis();
        try {
            var result = joinPoint.proceed();
            if (result instanceof ListenableFuture) {
                Futures.addCallback((ListenableFuture) result,
                        new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable Object result) {
                                logTenantMethodExecution(tenantId, method, true, startTime);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                logTenantMethodExecution(tenantId, method, false, startTime);
                            }
                        },
                        MoreExecutors.directExecutor());
            } else {
                logTenantMethodExecution(tenantId, method, true, startTime);
            }
            return result;
        } catch (Throwable t) {
            logTenantMethodExecution(tenantId, method, false, startTime);
            throw t;
        }
    }

    private void logTenantMethodExecution(TenantId tenantId, String method, boolean success, long startTime) {
        statsMap.computeIfAbsent(tenantId, DbCallStats::new)
                .onMethodCall(method, success, System.currentTimeMillis() - startTime);
    }

    TenantId getTenantId(String methodName, Object[] args) {
        if (args == null || args.length == 0) {
            addAndLogInvalidMethods(methodName);
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof TenantId) {
                log.debug("Method: {} is annotated with @TenantDbCall but the TenantId is null. Args: {}", methodName, Arrays.toString(args));
                return (TenantId) arg;
            }
        }
        addAndLogInvalidMethods(methodName);
        return null;
    }

    private void addAndLogInvalidMethods(String methodName) {
        log.warn("Method: {} is annotated with @TenantDbCall but no TenantId in args", methodName);
        invalidTenantDbCallMethods.add(methodName);
    }

}
