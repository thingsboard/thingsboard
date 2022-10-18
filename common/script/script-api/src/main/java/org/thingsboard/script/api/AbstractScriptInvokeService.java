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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.common.util.JacksonUtil;
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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

@Slf4j
public abstract class AbstractScriptInvokeService implements ScriptInvokeService {

    protected Map<UUID, BlockedScriptInfo> disabledScripts = new ConcurrentHashMap<>();

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
            return withTimeoutAndStatsCallback(scriptId, doEvalScript(scriptType, scriptBody, scriptId, argNames), evalCallback, getMaxEvalRequestsTimeout());
        } else {
            return error("Script Execution is disabled due to API limits!");
        }
    }

    @Override
    public ListenableFuture<Object> invokeScript(TenantId tenantId, CustomerId customerId, UUID scriptId, Object... args) {
        if (!apiUsageStateClient.isPresent() || apiUsageStateClient.get().getApiUsageState(tenantId).isJsExecEnabled()) {
            if (!isScriptPresent(scriptId)) {
                return error("No compiled script found for scriptId: [" + scriptId + "]!");
            }
            if (!isDisabled(scriptId)) {
                if (argsSizeExceeded(args)) {
                    TbScriptException t = new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, null, new IllegalArgumentException(
                            format("Script input arguments exceed maximum allowed total args size of %s symbols", getMaxTotalArgsSize())
                    ));
                    handleScriptException(scriptId, t);
                    return Futures.immediateFailedFuture(t);
                }
                apiUsageReportClient.ifPresent(client -> client.report(tenantId, customerId, ApiUsageRecordKey.JS_EXEC_COUNT, 1));
                pushedMsgs.incrementAndGet();
                log.trace("InvokeScript uuid {} with timeout {}ms", scriptId, getMaxInvokeRequestsTimeout());
                var resultFuture = Futures.transformAsync(doInvokeFunction(scriptId, args), output -> {
                    String result = JacksonUtil.toString(output);
                    if (resultSizeExceeded(result)) {
                        throw new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, null, new RuntimeException(
                                format("Script invocation result exceeds maximum allowed size of %s symbols", getMaxResultSize())
                        ));
                    }
                    return Futures.immediateFuture(output);
                }, MoreExecutors.directExecutor());

                return withTimeoutAndStatsCallback(scriptId, resultFuture, invokeCallback, getMaxInvokeRequestsTimeout());
            } else {
                String message = "Script invocation is blocked due to maximum error count "
                        + getMaxErrors() + ", scriptId " + scriptId + "!";
                log.warn(message);
                return error(message);
            }
        } else {
            return error("Script execution is disabled due to API limits!");
        }
    }

    private <T extends V, V> ListenableFuture<T> withTimeoutAndStatsCallback(UUID scriptId, ListenableFuture<T> future, FutureCallback<V> statsCallback, long timeout) {
        if (timeout > 0) {
            future = Futures.withTimeout(future, timeout, TimeUnit.MILLISECONDS, timeoutExecutorService);
        }
        Futures.addCallback(future, statsCallback, getCallbackExecutor());
        Futures.addCallback(future, new FutureCallback<T>() {
            @Override
            public void onSuccess(@Nullable T result) {
                //do nothing
            }

            @Override
            public void onFailure(Throwable t) {
                handleScriptException(scriptId, t);
            }
        }, getCallbackExecutor());
        return future;
    }

    private void handleScriptException(UUID scriptId, Throwable t) {
        boolean blockList = t instanceof TimeoutException || (t.getCause() != null && t.getCause() instanceof TimeoutException);
        String scriptBody = null;
        if (t instanceof TbScriptException) {
            var scriptException = (TbScriptException) t;
            scriptBody = scriptException.getBody();
            var cause = scriptException.getCause();
            switch (scriptException.getErrorCode()) {
                case COMPILATION:
                    log.debug("[{}] Failed to compile script: {}", scriptId, scriptException.getBody(), cause);
                    break;
                case TIMEOUT:
                    log.debug("[{}] Timeout to execute script: {}", scriptId, scriptException.getBody(), cause);
                    break;
                case OTHER:
                case RUNTIME:
                    log.debug("[{}] Failed to execute script: {}", scriptId, scriptException.getBody(), cause);
                    break;
            }
            blockList = blockList || scriptException.getErrorCode() != TbScriptException.ErrorCode.RUNTIME;
        }
        if (blockList) {
            BlockedScriptInfo disableListInfo = disabledScripts.computeIfAbsent(scriptId, key -> new BlockedScriptInfo(getMaxBlackListDurationSec()));
            if (log.isDebugEnabled()) {
                log.debug("Script has exception and will increment counter {} on disabledFunctions for id {}, exception {}, cause {}, scriptBody {}",
                        disableListInfo.get(), scriptId, t, t.getCause(), scriptBody);
            } else {
                log.warn("Script has exception and will increment counter {} on disabledFunctions for id {}, exception {}",
                        disableListInfo.get(), scriptId, t.getMessage());
            }
            disableListInfo.incrementAndGet();
        }
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
        BlockedScriptInfo errorCount = disabledScripts.get(scriptId);
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
}
