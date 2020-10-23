/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.usagestats.TbUsageStatsClient;

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

    private final TbUsageStatsClient apiUsageStatsClient;
    protected ScheduledExecutorService timeoutExecutorService;
    protected Map<UUID, String> scriptIdToNameMap = new ConcurrentHashMap<>();
    protected Map<UUID, BlackListInfo> blackListedFunctions = new ConcurrentHashMap<>();

    protected AbstractJsInvokeService(TbUsageStatsClient apiUsageStatsClient) {
        this.apiUsageStatsClient = apiUsageStatsClient;
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
        UUID scriptId = UUID.randomUUID();
        String functionName = "invokeInternal_" + scriptId.toString().replace('-', '_');
        String jsScript = generateJsScript(scriptType, functionName, scriptBody, argNames);
        return doEval(scriptId, functionName, jsScript);
    }

    @Override
    public ListenableFuture<Object> invokeFunction(TenantId tenantId, UUID scriptId, Object... args) {
        String functionName = scriptIdToNameMap.get(scriptId);
        if (functionName == null) {
            return Futures.immediateFailedFuture(new RuntimeException("No compiled script found for scriptId: [" + scriptId + "]!"));
        }
        if (!isBlackListed(scriptId)) {
            apiUsageStatsClient.report(tenantId, ApiUsageRecordKey.JS_EXEC_COUNT, 1);
            return doInvokeFunction(scriptId, functionName, args);
        } else {
            return Futures.immediateFailedFuture(
                    new RuntimeException("Script is blacklisted due to maximum error count " + getMaxErrors() + "!"));
        }
    }

    @Override
    public ListenableFuture<Void> release(UUID scriptId) {
        String functionName = scriptIdToNameMap.get(scriptId);
        if (functionName != null) {
            try {
                scriptIdToNameMap.remove(scriptId);
                blackListedFunctions.remove(scriptId);
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
        blackListedFunctions.computeIfAbsent(scriptId, key -> new BlackListInfo()).incrementAndGet();
    }

    private String generateJsScript(JsScriptType scriptType, String functionName, String scriptBody, String... argNames) {
        if (scriptType == JsScriptType.RULE_NODE_SCRIPT) {
            return RuleNodeScriptFactory.generateRuleNodeScript(functionName, scriptBody, argNames);
        }
        throw new RuntimeException("No script factory implemented for scriptType: " + scriptType);
    }

    private boolean isBlackListed(UUID scriptId) {
        BlackListInfo errorCount = blackListedFunctions.get(scriptId);
        if (errorCount != null) {
            if (errorCount.getExpirationTime() <= System.currentTimeMillis()) {
                blackListedFunctions.remove(scriptId);
                return false;
            } else {
                return errorCount.get() >= getMaxErrors();
            }
        } else {
            return false;
        }
    }

    private class BlackListInfo {
        private final AtomicInteger counter;
        private long expirationTime;

        private BlackListInfo() {
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
