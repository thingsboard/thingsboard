/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Created by ashvayka on 26.09.18.
 */
@Slf4j
public abstract class AbstractJsInvokeService implements JsInvokeService {

    private final TbApiUsageStateService apiUsageStateService;
    private final TbApiUsageClient apiUsageClient;
    protected ScheduledExecutorService timeoutExecutorService;
    protected Map<UUID, String> scriptIdToNameMap = new ConcurrentHashMap<>();
    protected Map<UUID, DisableListInfo> disabledFunctions = new ConcurrentHashMap<>();

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
            UUID scriptId = UUID.randomUUID();
            String functionName = "invokeInternal_" + scriptId.toString().replace('-', '_');
            String jsScript = generateJsScript(scriptType, functionName, scriptBody, argNames);
            return doEval(scriptId, functionName, jsScript);
        } else {
            return Futures.immediateFailedFuture(new RuntimeException("JS Execution is disabled due to API limits!"));
        }
    }

    @Override
    public ListenableFuture<Object> invokeFunction(TenantId tenantId, CustomerId customerId, UUID scriptId, Object... args) {
        if (apiUsageStateService.getApiUsageState(tenantId).isJsExecEnabled()) {
            String functionName = scriptIdToNameMap.get(scriptId);
            if (functionName == null) {
                return Futures.immediateFailedFuture(new RuntimeException("No compiled script found for scriptId: [" + scriptId + "]!"));
            }
            if (!isDisabled(scriptId)) {
                apiUsageClient.report(tenantId, customerId, ApiUsageRecordKey.JS_EXEC_COUNT, 1);
                return doInvokeFunction(scriptId, functionName, args);
            } else {
                return Futures.immediateFailedFuture(
                        new RuntimeException("Script invocation is blocked due to maximum error count " + getMaxErrors() + "!"));
            }
        } else {
            return Futures.immediateFailedFuture(new RuntimeException("JS Execution is disabled due to API limits!"));
        }
    }

    @Override
    public ListenableFuture<Void> release(UUID scriptId) {
        String functionName = scriptIdToNameMap.get(scriptId);
        if (functionName != null) {
            try {
                scriptIdToNameMap.remove(scriptId);
                disabledFunctions.remove(scriptId);
                doRelease(scriptId, functionName);
            } catch (Exception e) {
                return Futures.immediateFailedFuture(e);
            }
        }
        return Futures.immediateFuture(null);
    }

    protected abstract ListenableFuture<UUID> doEval(UUID scriptId, String functionName, String scriptBody);

    protected abstract ListenableFuture<Object> doInvokeFunction(UUID scriptId, String functionName, Object[] args);

    protected abstract void doRelease(UUID scriptId, String functionName) throws Exception;

    protected abstract int getMaxErrors();

    protected abstract long getMaxBlacklistDuration();

    protected void onScriptExecutionError(UUID scriptId) {
        disabledFunctions.computeIfAbsent(scriptId, key -> new DisableListInfo()).incrementAndGet();
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
