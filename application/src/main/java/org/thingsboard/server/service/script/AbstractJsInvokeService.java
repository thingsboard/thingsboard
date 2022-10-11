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
package org.thingsboard.server.service.script;

import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.usagestats.TbApiUsageClient;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

/**
 * Created by ashvayka on 26.09.18.
 */
@Slf4j
@SuppressWarnings("UnstableApiUsage")
public abstract class AbstractJsInvokeService implements JsInvokeService {

    private final TbApiUsageStateService apiUsageStateService;
    private final TbApiUsageClient apiUsageClient;
    protected ScheduledExecutorService timeoutExecutorService;

    protected final Map<UUID, Pair<String, String>> scriptIdToNameAndHashMap = new ConcurrentHashMap<>();
    protected final Map<UUID, DisableListInfo> disabledFunctions = new ConcurrentHashMap<>();

    @Getter
    @Value("${js.max_total_args_size:100000}")
    private long maxTotalArgsSize;
    @Getter
    @Value("${js.max_result_size:300000}")
    private long maxResultSize;
    @Getter
    @Value("${js.max_script_body_size:50000}")
    private long maxScriptBodySize;

    protected AbstractJsInvokeService(TbApiUsageStateService apiUsageStateService, TbApiUsageClient apiUsageClient) {
        this.apiUsageStateService = apiUsageStateService;
        this.apiUsageClient = apiUsageClient;
    }

    public void init(long maxRequestsTimeout) {
        if (maxRequestsTimeout > 0) {
            timeoutExecutorService = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("nashorn-js-timeout"));
        }
    }

    public void stop() {
        if (timeoutExecutorService != null) {
            timeoutExecutorService.shutdownNow();
        }
    }

    @Override
    public ListenableFuture<UUID> eval(TenantId tenantId, JsScriptType scriptType, String scriptBody, String... argNames) {
        if (apiUsageStateService.getApiUsageState(tenantId).isJsExecEnabled()) {
            if (scriptBodySizeExceeded(scriptBody)) {
                return error(format("Script body exceeds maximum allowed size of %s symbols", getMaxScriptBodySize()));
            }
            UUID scriptId = UUID.randomUUID();
            String scriptHash = hash(tenantId, scriptBody);
            String functionName = constructFunctionName(scriptId, scriptHash);
            String jsScript = generateJsScript(scriptType, functionName, scriptBody, argNames);
            return doEval(scriptId, scriptHash, functionName, jsScript);
        } else {
            return error("JS Execution is disabled due to API limits!");
        }
    }

    protected String constructFunctionName(UUID scriptId, String scriptHash) {
        return "invokeInternal_" + scriptId.toString().replace('-', '_');
    }

    @Override
    public ListenableFuture<String> invokeFunction(TenantId tenantId, CustomerId customerId, UUID scriptId, Object... args) {
        if (apiUsageStateService.getApiUsageState(tenantId).isJsExecEnabled()) {
            Pair<String, String> nameAndHash = scriptIdToNameAndHashMap.get(scriptId);
            if (nameAndHash == null) {
                return error("No compiled script found for scriptId: [" + scriptId + "]!");
            }
            String functionName = nameAndHash.getFirst();
            String scriptHash = nameAndHash.getSecond();
            if (!isDisabled(scriptId)) {
                if (argsSizeExceeded(args)) {
                    return scriptExecutionError(scriptId, format("Script input arguments exceed maximum allowed total args size of %s symbols", getMaxTotalArgsSize()));
                }
                apiUsageClient.report(tenantId, customerId, ApiUsageRecordKey.JS_EXEC_COUNT, 1);
                return Futures.transformAsync(doInvokeFunction(scriptId, scriptHash, functionName, args), output -> {
                    String result = output.toString();
                    if (resultSizeExceeded(result)) {
                        return scriptExecutionError(scriptId, format("Script invocation result exceeds maximum allowed size of %s symbols", getMaxResultSize()));
                    }
                    return Futures.immediateFuture(result);
                }, MoreExecutors.directExecutor());
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

    @Override
    public ListenableFuture<Void> release(UUID scriptId) {
        Pair<String, String> nameAndHash = scriptIdToNameAndHashMap.get(scriptId);
        if (nameAndHash != null) {
            try {
                scriptIdToNameAndHashMap.remove(scriptId);
                disabledFunctions.remove(scriptId);
                doRelease(scriptId, nameAndHash.getSecond(), nameAndHash.getFirst());
            } catch (Exception e) {
                return Futures.immediateFailedFuture(e);
            }
        }
        return Futures.immediateFuture(null);
    }

    protected abstract ListenableFuture<UUID> doEval(UUID scriptId, String scriptHash, String functionName, String scriptBody);

    protected abstract ListenableFuture<Object> doInvokeFunction(UUID scriptId, String scriptHash, String functionName, Object[] args);

    protected abstract void doRelease(UUID scriptId, String scriptHash, String functionName) throws Exception;

    protected abstract int getMaxErrors();

    protected abstract long getMaxBlacklistDuration();

    protected String hash(TenantId tenantId, String scriptBody) {
        return Hashing.murmur3_128().newHasher()
                .putLong(tenantId.getId().getMostSignificantBits())
                .putLong(tenantId.getId().getLeastSignificantBits())
                .putUnencodedChars(scriptBody)
                .hash().toString();
    }

    protected void onScriptExecutionError(UUID scriptId, Throwable t, String scriptBody) {
        DisableListInfo disableListInfo = disabledFunctions.computeIfAbsent(scriptId, key -> new DisableListInfo());
        log.warn("Script has exception and will increment counter {} on disabledFunctions for id {}, exception {}, cause {}, scriptBody {}",
                disableListInfo.get(), scriptId, t, t.getCause(), scriptBody);
        disableListInfo.incrementAndGet();
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

    private String generateJsScript(JsScriptType scriptType, String functionName, String scriptBody, String... argNames) {
        if (scriptType == JsScriptType.RULE_NODE_SCRIPT) {
            return RuleNodeScriptFactory.generateRuleNodeScript(functionName, scriptBody, argNames);
        }
        throw new RuntimeException("No script factory implemented for scriptType: " + scriptType);
    }

    private boolean isDisabled(UUID scriptId) {
        DisableListInfo errorCount = disabledFunctions.get(scriptId);
        if (errorCount != null) {
            if (errorCount.getExpirationTime() <= System.currentTimeMillis()) {
                disabledFunctions.remove(scriptId);
                return false;
            } else {
                return errorCount.get() >= getMaxErrors();
            }
        } else {
            return false;
        }
    }

    private <T> ListenableFuture<T> error(String message) {
        return Futures.immediateFailedFuture(new RuntimeException(message));
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
            expirationTime = System.currentTimeMillis() + getMaxBlacklistDuration();
            return result;
        }

        public long getExpirationTime() {
            return expirationTime;
        }
    }
}
