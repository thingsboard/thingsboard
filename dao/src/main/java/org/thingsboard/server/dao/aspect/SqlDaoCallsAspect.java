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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.join;

@Aspect
@ConditionalOnProperty(prefix = "sql", value = "log_tenant_stats", havingValue = "true")
@Component
@Slf4j
public class SqlDaoCallsAspect {

    private final Set<String> invalidTenantDbCallMethods = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<TenantId, DbCallStats> statsMap = new ConcurrentHashMap<>();

    @Value("${sql.batch_sort:true}")
    private boolean batchSortEnabled;

    private static final String DEADLOCK_DETECTED_ERROR = "deadlock detected";


    @Scheduled(initialDelayString = "${sql.log_tenant_stats_interval_ms:60000}",
            fixedDelayString = "${sql.log_tenant_stats_interval_ms:60000}")
    public void printStats() {
        List<DbCallStatsSnapshot> snapshots = snapshot();
        if (snapshots.isEmpty()) return;
        try {
            if (log.isTraceEnabled()) {
                logTopNTenants(snapshots, Comparator.comparing(DbCallStatsSnapshot::getTotalTiming).reversed(), 0, snapshot -> {
                    logSnapshot(snapshot, 0, Comparator.comparing(MethodCallStatsSnapshot::getTiming).reversed(), "timing", log::trace);
                });

                Map<String, Map<TenantId, MethodCallStatsSnapshot>> byMethodStats = new HashMap<>();
                for (DbCallStatsSnapshot snapshot : snapshots) {
                    snapshot.getMethodStats().forEach((method, stats) -> {
                        byMethodStats.computeIfAbsent(method, m -> new HashMap<>())
                                .put(snapshot.getTenantId(), stats);
                    });
                }
                byMethodStats.forEach((method, byTenantStats) -> {
                    log.trace("Top tenants for method {} by calls:", method);
                    byTenantStats.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.comparing(MethodCallStatsSnapshot::getExecutions).reversed()))
                            .limit(10)
                            .forEach(e -> {
                                TenantId tenantId = e.getKey();
                                MethodCallStatsSnapshot methodStats = e.getValue();
                                log.trace("[{}] calls: {}, failures: {}, timing: {}", tenantId,
                                        methodStats.getExecutions(), methodStats.getFailures(), methodStats.getTiming());
                            });
                });
            } else if (log.isDebugEnabled()) {
                log.debug("Total calls statistics below:");
                logTopNTenants(snapshots, Comparator.comparingInt(DbCallStatsSnapshot::getTotalCalls).reversed(), 10,
                        s -> logSnapshot(s, 10, Comparator.comparing(MethodCallStatsSnapshot::getExecutions).reversed(), "executions", log::debug));
                log.debug("Total timing statistics below:");
                logTopNTenants(snapshots, Comparator.comparingLong(DbCallStatsSnapshot::getTotalTiming).reversed(), 10,
                        s -> logSnapshot(s, 10, Comparator.comparing(MethodCallStatsSnapshot::getTiming).reversed(), "timing", log::debug));
                log.debug("Total errors statistics below:");
                logTopNTenants(snapshots, Comparator.comparingInt(DbCallStatsSnapshot::getTotalFailure).reversed(), 10,
                        s -> logSnapshot(s, 10, Comparator.comparing(MethodCallStatsSnapshot::getFailures).reversed(), "failures", log::debug));
            } else if (log.isInfoEnabled()) {
                log.info("Total timing statistics below:");
                logTopNTenants(snapshots, Comparator.comparingLong(DbCallStatsSnapshot::getTotalTiming).reversed(), 3,
                        s -> logSnapshot(s, 3, Comparator.comparing(MethodCallStatsSnapshot::getTiming).reversed(), "timing", log::info));
            }
        } finally {
            statsMap.clear();
        }
    }

    private void logSnapshot(DbCallStatsSnapshot snapshot, int limit, Comparator<MethodCallStatsSnapshot> methodStatsComparator, String sortingKey, Consumer<String> logger) {
        logger.accept(String.format("[%s]: calls: %s, failures: %s, exec time: %s ",
                snapshot.getTenantId(), snapshot.getTotalCalls(), snapshot.getTotalFailure(), snapshot.getTotalTiming()));
        var stream = snapshot.getMethodStats().entrySet().stream()
                .sorted(Map.Entry.comparingByValue(methodStatsComparator));
        if (limit > 0) {
            logger.accept(String.format("[%s] Top %s methods by %s:", snapshot.getTenantId(), limit, sortingKey));
            stream = stream.limit(limit);
        }
        stream.forEach(e -> {
            MethodCallStatsSnapshot methodStats = e.getValue();
            logger.accept(String.format("[%s]: method: %s, calls: %s, failures: %s, exec time: %s", snapshot.getTenantId(), e.getKey(),
                    methodStats.getExecutions(), methodStats.getFailures(), methodStats.getTiming()));
        });
    }

    private List<DbCallStatsSnapshot> snapshot() {
        return statsMap.values().stream().map(DbCallStats::snapshot).collect(Collectors.toList());
    }

    private void logTopNTenants(List<DbCallStatsSnapshot> snapshots, Comparator<DbCallStatsSnapshot> comparator,
                                int n, Consumer<DbCallStatsSnapshot> logFunction) {
        var stream = snapshots.stream().sorted(comparator);
        if (n > 0) {
            stream = stream.limit(n);
        }
        stream.forEach(logFunction);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Around("@within(org.thingsboard.server.dao.util.SqlDao)")
    public Object handleSqlCall(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        var methodName = signature.toShortString();
        if (invalidTenantDbCallMethods.contains(methodName)) {
            //Simply call the method if tenant is not found
            return joinPoint.proceed();
        }
        var tenantId = getTenantId(signature, methodName, joinPoint.getArgs());
        if (tenantId == null || tenantId.isNullUid()) {
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
                                reportSuccessfulMethodExecution(tenantId, methodName, startTime);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                reportFailedMethodExecution(tenantId, methodName, startTime, t, joinPoint);
                            }
                        },
                        MoreExecutors.directExecutor());
            } else {
                reportSuccessfulMethodExecution(tenantId, methodName, startTime);
            }
            return result;
        } catch (Throwable t) {
            reportFailedMethodExecution(tenantId, methodName, startTime, t, joinPoint);
            throw t;
        }
    }

    private void reportFailedMethodExecution(TenantId tenantId, String method, long startTime, Throwable t, ProceedingJoinPoint joinPoint) {
        if (t != null) {
            if (ExceptionUtils.indexOfThrowable(t, JDBCConnectionException.class) >= 0) {
                return;
            }
            if (StringUtils.containedByAny(DEADLOCK_DETECTED_ERROR, ExceptionUtils.getRootCauseMessage(t), ExceptionUtils.getMessage(t))) {
                if (!batchSortEnabled) {
                    log.warn("Deadlock was detected for method {} (tenant: {}). You might need to enable 'sql.batch_sort' option.", method, tenantId);
                } else {
                    log.error("Deadlock was detected for method {} (tenant: {}). Arguments passed: \n{}\n The error: ",
                            method, tenantId, join(joinPoint.getArgs(), System.lineSeparator()), t);
                }
            }
        }
        reportMethodExecution(tenantId, method, false, startTime);
    }

    private void reportSuccessfulMethodExecution(TenantId tenantId, String method, long startTime) {
        reportMethodExecution(tenantId, method, true, startTime);
    }

    private void reportMethodExecution(TenantId tenantId, String method, boolean success, long startTime) {
        statsMap.computeIfAbsent(tenantId, DbCallStats::new)
                .onMethodCall(method, success, System.currentTimeMillis() - startTime);
    }

    TenantId getTenantId(MethodSignature signature, String methodName, Object[] args) {
        if (args == null || args.length == 0) {
            addAndLogInvalidMethods(methodName);
            return null;
        }
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof TenantId) {
                return (TenantId) arg;
            } else if (arg instanceof UUID) {
                if (signature.getParameterNames() != null && StringUtils.equals(signature.getParameterNames()[i], "tenantId")) {
                    log.trace("Method {} uses UUID for tenantId param instead of TenantId class", methodName);
                    return TenantId.fromUUID((UUID) arg);
                }
            }
        }
        if (ArrayUtils.contains(signature.getParameterTypes(), TenantId.class) ||
                ArrayUtils.contains(signature.getParameterNames(), "tenantId")) {
            log.debug("Null was submitted as tenantId to method {}. Args: {}", methodName, Arrays.toString(args));
        } else {
            addAndLogInvalidMethods(methodName);
        }
        return null;
    }

    private void addAndLogInvalidMethods(String methodName) {
        invalidTenantDbCallMethods.add(methodName);
    }

}
