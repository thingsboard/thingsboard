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
package org.thingsboard.script.api;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.script.api.tbel.TbelCfObject;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;

@Slf4j
public abstract class AbstractScriptInvokeService implements ScriptInvokeService {

    private static final String REQUESTS = "requests";
    private static final String INVOKE_RESPONSES = "invoke_responses";
    private static final String EVAL_RESPONSES = "eval_responses";
    private static final String FAILURES = "failures";
    private static final String TIMEOUTS = "timeouts";

    protected final Map<UUID, BlockedScriptInfo> disabledScripts = new ConcurrentHashMap<>();

    private StatsCounter requestsCounter;
    private StatsCounter invokeResponsesCounter;
    private StatsCounter evalResponsesCounter;
    private StatsCounter failuresCounter;
    private StatsCounter timeoutsCounter;

    private FutureCallback<UUID> evalCallback;
    private FutureCallback<Object> invokeCallback;

    @Autowired
    private StatsFactory statsFactory;

    protected ScheduledExecutorService timeoutExecutorService;

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

    protected abstract boolean isExecEnabled(TenantId tenantId);

    protected abstract void reportExecution(TenantId tenantId, CustomerId customerId);

    protected abstract ListenableFuture<UUID> doEvalScript(TenantId tenantId, ScriptType scriptType, String scriptBody, UUID scriptId, String[] argNames);

    protected abstract TbScriptExecutionTask doInvokeFunction(UUID scriptId, Object[] args);

    protected abstract void doRelease(UUID scriptId) throws Exception;

    public void init() {
        String key = getStatsType().getName();
        this.requestsCounter = statsFactory.createStatsCounter(key, REQUESTS);
        this.invokeResponsesCounter = statsFactory.createStatsCounter(key, INVOKE_RESPONSES);
        this.evalResponsesCounter = statsFactory.createStatsCounter(key, EVAL_RESPONSES);
        this.failuresCounter = statsFactory.createStatsCounter(key, FAILURES);
        this.timeoutsCounter = statsFactory.createStatsCounter(key, TIMEOUTS);
        this.evalCallback = new ScriptStatCallback<>(evalResponsesCounter, timeoutsCounter, failuresCounter);
        this.invokeCallback = new ScriptStatCallback<>(invokeResponsesCounter, timeoutsCounter, failuresCounter);
        if (getMaxEvalRequestsTimeout() > 0 || getMaxInvokeRequestsTimeout() > 0) {
            timeoutExecutorService = ThingsBoardExecutors.newSingleThreadScheduledExecutor("script-timeout");
        }
    }

    public void stop() {
        if (timeoutExecutorService != null) {
            timeoutExecutorService.shutdownNow();
        }
    }

    public void printStats() {
        if (isStatsEnabled()) {
            int pushed = requestsCounter.getAndClear();
            int invoked = invokeResponsesCounter.getAndClear();
            int evaluated = evalResponsesCounter.getAndClear();
            int failed = failuresCounter.getAndClear();
            int timedOut = timeoutsCounter.getAndClear();
            if (pushed > 0 || invoked > 0 || evaluated > 0 || failed > 0 || timedOut > 0) {
                log.info("{}: pushed [{}] received [{}] invoke [{}] eval [{}] failed [{}] timedOut [{}]",
                        getStatsName(), pushed, invoked + evaluated, invoked, evaluated, failed, timedOut);
            }
        }
    }

    public String validate(TenantId tenantId, String scriptBody) {
        if (isExecEnabled(tenantId)) {
            if (scriptBodySizeExceeded(scriptBody)) {
                return format("Script body exceeds maximum allowed size of %s symbols", getMaxScriptBodySize());
            }
        } else {
            return "Script Execution is disabled due to API limits!";
        }

        return null;
    }

    @Override
    public ListenableFuture<UUID> eval(TenantId tenantId, ScriptType scriptType, String scriptBody, String... argNames) {
        String validationError = validate(tenantId, scriptBody);
        if (validationError != null) {
            return error(validationError);
        }

        UUID scriptId = UUID.randomUUID();
        requestsCounter.increment();
        return withTimeoutAndStatsCallback(scriptId, null,
                doEvalScript(tenantId, scriptType, scriptBody, scriptId, argNames), evalCallback, getMaxEvalRequestsTimeout());
    }

    @Override
    public ListenableFuture<Object> invokeScript(TenantId tenantId, CustomerId customerId, UUID scriptId, Object... args) {
        if (isExecEnabled(tenantId)) {
            if (!isScriptPresent(scriptId)) {
                return error("No compiled script found for scriptId: [" + scriptId + "]!");
            }
            if (!isDisabled(scriptId)) {
                if (argsSizeExceeded(args)) {
                    TbScriptException t = new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, null, new IllegalArgumentException(
                            format("Script input arguments exceed maximum allowed total args size of %s symbols", getMaxTotalArgsSize())
                    ));
                    return Futures.immediateFailedFuture(handleScriptException(scriptId, null, t));
                }
                reportExecution(tenantId, customerId);
                requestsCounter.increment();
                log.trace("[{}] InvokeScript uuid {} with timeout {}ms", tenantId, scriptId, getMaxInvokeRequestsTimeout());
                var task = doInvokeFunction(scriptId, args);

                var resultFuture = Futures.transform(task.getResultFuture(), output -> {
                    String result = JacksonUtil.toString(output);
                    if (resultSizeExceeded(result)) {
                        throw new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, null, new RuntimeException(
                                format("Script invocation result exceeds maximum allowed size of %s symbols", getMaxResultSize())
                        ));
                    }
                    return output;
                }, MoreExecutors.directExecutor());

                return withTimeoutAndStatsCallback(scriptId, task, resultFuture, invokeCallback, getMaxInvokeRequestsTimeout());
            } else {
                String message = "Script invocation is blocked due to maximum error count "
                        + getMaxErrors() + ", scriptId " + scriptId + "!";
                log.warn("[{}] " + message, tenantId);
                return error(message);
            }
        } else {
            return error("Script execution is disabled due to API limits!");
        }
    }

    private <T extends V, V> ListenableFuture<T> withTimeoutAndStatsCallback(UUID scriptId, TbScriptExecutionTask task, ListenableFuture<T> future, FutureCallback<V> statsCallback, long timeout) {
        if (timeout > 0) {
            future = Futures.withTimeout(future, timeout, TimeUnit.MILLISECONDS, timeoutExecutorService);
        }
        Futures.addCallback(future, statsCallback, getCallbackExecutor());
        return Futures.catchingAsync(future, Exception.class,
                input -> Futures.immediateFailedFuture(handleScriptException(scriptId, task, input)),
                MoreExecutors.directExecutor());
    }

    private Throwable handleScriptException(UUID scriptId, TbScriptExecutionTask task, Throwable t) {
        boolean timeout = t instanceof TimeoutException || (t.getCause() != null && t.getCause() instanceof TimeoutException);
        if (timeout && task != null) {
            task.stop();
        }
        boolean blockList = timeout;
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
            blockList = timeout || scriptException.getErrorCode() != TbScriptException.ErrorCode.RUNTIME;
        }
        if (blockList) {
            BlockedScriptInfo disableListInfo = disabledScripts.computeIfAbsent(scriptId, key -> new BlockedScriptInfo(getMaxBlackListDurationSec()));
            int counter = disableListInfo.incrementAndGet();
            if (log.isDebugEnabled()) {
                log.debug("Script has exception counter {} on disabledFunctions for id {}, exception {}, cause {}, scriptBody {}",
                        counter, scriptId, t, t.getCause(), scriptBody);
            } else {
                log.warn("Script has exception counter {} on disabledFunctions for id {}, exception {}",
                        counter, scriptId, t.getMessage());
            }
        }
        if (timeout) {
            return new TimeoutException("Script timeout!");
        } else {
            return t;
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

    public boolean scriptBodySizeExceeded(String scriptBody) {
        if (getMaxScriptBodySize() <= 0) return false;
        return scriptBody.length() > getMaxScriptBodySize();
    }

    private boolean argsSizeExceeded(Object[] args) {
        if (getMaxTotalArgsSize() <= 0) return false;
        long totalArgsSize = 0;
        for (Object arg : args) {
            if (arg instanceof CharSequence) {
                totalArgsSize += ((CharSequence) arg).length();
            } else if (arg instanceof TbelCfObject tbelCfObj) {
                totalArgsSize += tbelCfObj.memorySize();
            } else {
                var str = JacksonUtil.toString(arg);
                if (str != null) {
                    totalArgsSize += str.length();
                }
            }
        }
        return totalArgsSize > getMaxTotalArgsSize();
    }

    private boolean resultSizeExceeded(String result) {
        if (getMaxResultSize() <= 0) return false;
        return result != null && result.length() > getMaxResultSize();
    }

    public <T> ListenableFuture<T> error(String message) {
        return Futures.immediateFailedFuture(new RuntimeException(message));
    }

    protected abstract StatsType getStatsType();
}
