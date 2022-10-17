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
package org.thingsboard.script.api;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

@Slf4j
public abstract class AbstractScriptInvokeService implements ScriptInvokeService {

    protected Map<UUID, DisableListInfo> disabledScripts = new ConcurrentHashMap<>();

    private final Optional<TbApiUsageStateClient> apiUsageStateClient;
    private final Optional<TbApiUsageReportClient> apiUsageReportClient;
    private final AtomicInteger pushedMsgs = new AtomicInteger(0);
    private final AtomicInteger invokeMsgs = new AtomicInteger(0);
    private final AtomicInteger evalMsgs = new AtomicInteger(0);
    protected final AtomicInteger failedMsgs = new AtomicInteger(0);
    protected final AtomicInteger timeoutMsgs = new AtomicInteger(0);

    private final FutureCallback<UUID> evalCallback = new ScriptStatCallback<>(evalMsgs, timeoutMsgs, failedMsgs);
    private final FutureCallback<Object> invokeCallback = new ScriptStatCallback<>(invokeMsgs, timeoutMsgs, failedMsgs);

    protected ScheduledExecutorService timeoutExecutorService;

    protected AbstractScriptInvokeService(Optional<TbApiUsageStateClient> apiUsageStateClient, Optional<TbApiUsageReportClient> apiUsageReportClient) {
        this.apiUsageStateClient = apiUsageStateClient;
        this.apiUsageReportClient = apiUsageReportClient;
    }

    protected long getMaxEvalRequestsTimeout() {
        return getMaxInvokeRequestsTimeout();
    }

    protected abstract long getMaxInvokeRequestsTimeout();

    protected abstract long getMaxScriptBodySize();

    protected abstract long getMaxTotalArgsSize();

    protected abstract long getMaxResultSize();

    protected abstract int getMaxBlackListDurationSec();

    protected abstract int getMaxErrors();

    protected abstract boolean isStatsEnabled();

    protected abstract String getStatsName();

    protected abstract Executor getCallbackExecutor();

    protected abstract boolean isScriptPresent(UUID scriptId);

    protected abstract ListenableFuture<UUID> doEvalScript(ScriptType scriptType, String scriptBody, UUID scriptId, String[] argNames);

    protected abstract ListenableFuture<Object> doInvokeFunction(UUID scriptId, Object[] args);

    protected abstract void doRelease(UUID scriptId) throws Exception;

    public void init() {
        if (getMaxEvalRequestsTimeout() > 0 || getMaxInvokeRequestsTimeout() > 0) {
            timeoutExecutorService = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("script-timeout"));
        }
    }

    public void stop() {
        if (timeoutExecutorService != null) {
            timeoutExecutorService.shutdownNow();
        }
    }

    public void printStats() {
        if (isStatsEnabled()) {
            int pushed = pushedMsgs.getAndSet(0);
            int invoked = invokeMsgs.getAndSet(0);
            int evaluated = evalMsgs.getAndSet(0);
            int failed = failedMsgs.getAndSet(0);
            int timedOut = timeoutMsgs.getAndSet(0);
            if (pushed > 0 || invoked > 0 || evaluated > 0 || failed > 0 || timedOut > 0) {
                log.info("{}: pushed [{}] received [{}] invoke [{}] eval [{}] failed [{}] timedOut [{}]",
                        getStatsName(), pushed, invoked + evaluated, invoked, evaluated, failed, timedOut);
            }
        }
    }


    @Override
    public ListenableFuture<UUID> eval(TenantId tenantId, ScriptType scriptType, String scriptBody, String... argNames) {
        if (!apiUsageStateClient.isPresent() || apiUsageStateClient.get().getApiUsageState(tenantId).isJsExecEnabled()) {
            if (scriptBodySizeExceeded(scriptBody)) {
                return error(format("Script body exceeds maximum allowed size of %s symbols", getMaxScriptBodySize()));
            }
            UUID scriptId = UUID.randomUUID();
            pushedMsgs.incrementAndGet();
            return withTimeoutAndStatsCallback(doEvalScript(scriptType, scriptBody, scriptId, argNames), evalCallback, getMaxEvalRequestsTimeout());
        } else {
            return error("Script Execution is disabled due to API limits!");
        }
    }

    @Override
    public ListenableFuture<String> invokeScript(TenantId tenantId, CustomerId customerId, UUID scriptId, Object... args) {
        if (!apiUsageStateClient.isPresent() || apiUsageStateClient.get().getApiUsageState(tenantId).isJsExecEnabled()) {
            if (!isScriptPresent(scriptId)) {
                return error("No compiled script found for scriptId: [" + scriptId + "]!");
            }
            if (!isDisabled(scriptId)) {
                if (argsSizeExceeded(args)) {
                    return scriptExecutionError(scriptId, format("Script input arguments exceed maximum allowed total args size of %s symbols", getMaxTotalArgsSize()));
                }
                apiUsageReportClient.ifPresent(client -> client.report(tenantId, customerId, ApiUsageRecordKey.JS_EXEC_COUNT, 1));
                pushedMsgs.incrementAndGet();
                log.trace("invokeScript uuid {} with timeout {}ms", scriptId, getMaxInvokeRequestsTimeout());
                var resultFuture = Futures.transformAsync(doInvokeFunction(scriptId, args), output -> {
                    String result = output.toString();
                    if (resultSizeExceeded(result)) {
                        return scriptExecutionError(scriptId, format("Script invocation result exceeds maximum allowed size of %s symbols", getMaxResultSize()));
                    }
                    return Futures.immediateFuture(result);
                }, MoreExecutors.directExecutor());

                return withTimeoutAndStatsCallback(resultFuture, invokeCallback, getMaxInvokeRequestsTimeout());
            } else {
                String message = "Script invocation is blocked due to maximum error count "
                        + getMaxErrors() + ", scriptId " + scriptId + "!";
                log.warn(message);
                return error(message);
            }
        } else {
            return error("JS Execution is disabled due to API limits!");
        }
    }

    private <T extends V, V> ListenableFuture<T> withTimeoutAndStatsCallback(ListenableFuture<T> future, FutureCallback<V> statsCallback, long timeout) {
        if (timeout > 0) {
            future = Futures.withTimeout(future, timeout, TimeUnit.MILLISECONDS, timeoutExecutorService);
        }
        Futures.addCallback(future, statsCallback, getCallbackExecutor());
        return future;
    }

    @Override
    public ListenableFuture<Void> release(UUID scriptId) {
        if (isScriptPresent(scriptId)) {
            try {
                disabledScripts.remove(scriptId);
                doRelease(scriptId);
            } catch (Exception e) {
                return Futures.immediateFailedFuture(e);
            }
        }
        return Futures.immediateFuture(null);
    }

    private boolean isDisabled(UUID scriptId) {
        DisableListInfo errorCount = disabledScripts.get(scriptId);
        if (errorCount != null) {
            if (errorCount.getExpirationTime() <= System.currentTimeMillis()) {
                disabledScripts.remove(scriptId);
                return false;
            } else {
                return errorCount.get() >= getMaxErrors();
            }
        } else {
            return false;
        }
    }

    private boolean scriptBodySizeExceeded(String scriptBody) {
        if (getMaxScriptBodySize() <= 0) return false;
        return scriptBody.length() > getMaxScriptBodySize();
    }

    private boolean argsSizeExceeded(Object[] args) {
        if (getMaxTotalArgsSize() <= 0) return false;
        long totalArgsSize = 0;
        for (Object arg : args) {
            if (arg instanceof CharSequence) {
                totalArgsSize += ((CharSequence) arg).length();
            }
        }
        return totalArgsSize > getMaxTotalArgsSize();
    }

    private boolean resultSizeExceeded(String result) {
        if (getMaxResultSize() <= 0) return false;
        return result.length() > getMaxResultSize();
    }

    private <T> ListenableFuture<T> error(String message) {
        return Futures.immediateFailedFuture(new RuntimeException(message));
    }

    protected void onScriptExecutionError(UUID scriptId, Throwable t, String scriptBody) {
        DisableListInfo disableListInfo = disabledScripts.computeIfAbsent(scriptId, key -> new DisableListInfo());
        log.warn("Script has exception and will increment counter {} on disabledFunctions for id {}, exception {}, cause {}, scriptBody {}",
                disableListInfo.get(), scriptId, t, t.getCause(), scriptBody);
        disableListInfo.incrementAndGet();
    }

    private <T> ListenableFuture<T> scriptExecutionError(UUID scriptId, String errorMsg) {
        RuntimeException error = new RuntimeException(errorMsg);
        onScriptExecutionError(scriptId, error, null);
        return Futures.immediateFailedFuture(error);
    }

    private class DisableListInfo {
        private final AtomicInteger counter;
        private long expirationTime;

        private DisableListInfo() {
            this.counter = new AtomicInteger(0);
        }

        public int get() {
            return counter.get();
        }

        public int incrementAndGet() {
            int result = counter.incrementAndGet();
            expirationTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(getMaxBlackListDurationSec());
            return result;
        }

        public long getExpirationTime() {
            return expirationTime;
        }
    }

}
