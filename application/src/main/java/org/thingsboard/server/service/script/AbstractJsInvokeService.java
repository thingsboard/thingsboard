/**
 * Copyright © 2016-2019 The Thingsboard Authors
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 26.09.18.
 */
@Slf4j
public abstract class AbstractJsInvokeService implements JsInvokeService {

    protected Map<UUID, String> scriptIdToNameMap = new ConcurrentHashMap<>();
    protected Map<UUID, AtomicInteger> blackListedFunctions = new ConcurrentHashMap<>();

    @Override
    public ListenableFuture<UUID> eval(JsScriptType scriptType, String scriptBody, String... argNames) {
        UUID scriptId = UUID.randomUUID();
        String functionName = "invokeInternal_" + scriptId.toString().replace('-', '_');
        String jsScript = generateJsScript(scriptType, functionName, scriptBody, argNames);
        return doEval(scriptId, functionName, jsScript);
    }

    @Override
    public ListenableFuture<Object> invokeFunction(UUID scriptId, Object... args) {
        String functionName = scriptIdToNameMap.get(scriptId);
        if (functionName == null) {
            return Futures.immediateFailedFuture(new RuntimeException("No compiled script found for scriptId: [" + scriptId + "]!"));
        }
        if (!isBlackListed(scriptId)) {
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

    protected void onScriptExecutionError(UUID scriptId) {
        blackListedFunctions.computeIfAbsent(scriptId, key -> new AtomicInteger(0)).incrementAndGet();
    }

    private String generateJsScript(JsScriptType scriptType, String functionName, String scriptBody, String... argNames) {
        switch (scriptType) {
            case RULE_NODE_SCRIPT:
                return RuleNodeScriptFactory.generateRuleNodeScript(functionName, scriptBody, argNames);
            default:
                throw new RuntimeException("No script factory implemented for scriptType: " + scriptType);
        }
    }

    private boolean isBlackListed(UUID scriptId) {
        if (blackListedFunctions.containsKey(scriptId)) {
            AtomicInteger errorCount = blackListedFunctions.get(scriptId);
            return errorCount.get() >= getMaxErrors();
        } else {
            return false;
        }
    }
}
