/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
import delight.nashornsandbox.NashornSandbox;
import delight.nashornsandbox.NashornSandboxes;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class AbstractNashornJsSandboxService implements JsSandboxService {

    private NashornSandbox sandbox;
    private ScriptEngine engine;
    private ExecutorService monitorExecutorService;

    private Map<UUID, String> functionsMap = new ConcurrentHashMap<>();

    private Map<UUID,AtomicInteger> blackListedFunctions = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (useJsSandbox()) {
            sandbox = NashornSandboxes.create();
            monitorExecutorService = Executors.newFixedThreadPool(getMonitorThreadPoolSize());
            sandbox.setExecutor(monitorExecutorService);
            sandbox.setMaxCPUTime(getMaxCpuTime());
            sandbox.allowNoBraces(false);
            sandbox.setMaxPreparedStatements(30);
        } else {
            NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
            engine = factory.getScriptEngine(new String[]{"--no-java"});
        }
    }

    @PreDestroy
    public void stop() {
        if  (monitorExecutorService != null) {
            monitorExecutorService.shutdownNow();
        }
    }

    protected abstract boolean useJsSandbox();

    protected abstract int getMonitorThreadPoolSize();

    protected abstract long getMaxCpuTime();

    protected abstract int getMaxErrors();

    @Override
    public ListenableFuture<UUID> eval(JsScriptType scriptType, String scriptBody, String... argNames) {
        UUID scriptId = UUID.randomUUID();
        String functionName = "invokeInternal_" + scriptId.toString().replace('-','_');
        String jsScript = generateJsScript(scriptType, functionName, scriptBody, argNames);
        try {
            if (useJsSandbox()) {
                sandbox.eval(jsScript);
            } else {
                engine.eval(jsScript);
            }
            functionsMap.put(scriptId, functionName);
        } catch (Exception e) {
            log.warn("Failed to compile JS script: {}", e.getMessage(), e);
            return Futures.immediateFailedFuture(e);
        }
        return Futures.immediateFuture(scriptId);
    }

    @Override
    public ListenableFuture<Object> invokeFunction(UUID scriptId, Object... args) {
        String functionName = functionsMap.get(scriptId);
        if (functionName == null) {
            return Futures.immediateFailedFuture(new RuntimeException("No compiled script found for scriptId: [" + scriptId + "]!"));
        }
        if (!isBlackListed(scriptId)) {
            try {
                Object result;
                if (useJsSandbox()) {
                    result = sandbox.getSandboxedInvocable().invokeFunction(functionName, args);
                } else {
                    result = ((Invocable)engine).invokeFunction(functionName, args);
                }
                return Futures.immediateFuture(result);
            } catch (Exception e) {
                blackListedFunctions.computeIfAbsent(scriptId, key -> new AtomicInteger(0)).incrementAndGet();
                return Futures.immediateFailedFuture(e);
            }
        } else {
            return Futures.immediateFailedFuture(
                    new RuntimeException("Script is blacklisted due to maximum error count " + getMaxErrors() + "!"));
        }
    }

    @Override
    public ListenableFuture<Void> release(UUID scriptId) {
        String functionName = functionsMap.get(scriptId);
        if (functionName != null) {
            try {
                if (useJsSandbox()) {
                    sandbox.eval(functionName + " = undefined;");
                } else {
                    engine.eval(functionName + " = undefined;");
                }
                functionsMap.remove(scriptId);
                blackListedFunctions.remove(scriptId);
            } catch (ScriptException e) {
                return Futures.immediateFailedFuture(e);
            }
        }
        return Futures.immediateFuture(null);
    }

    private boolean isBlackListed(UUID scriptId) {
        if (blackListedFunctions.containsKey(scriptId)) {
            AtomicInteger errorCount = blackListedFunctions.get(scriptId);
            return errorCount.get() >= getMaxErrors();
        } else {
            return false;
        }
    }

    private String generateJsScript(JsScriptType scriptType, String functionName, String scriptBody, String... argNames) {
        switch (scriptType) {
            case RULE_NODE_SCRIPT:
                return RuleNodeScriptFactory.generateRuleNodeScript(functionName, scriptBody, argNames);
            default:
                throw new RuntimeException("No script factory implemented for scriptType: " + scriptType);
        }
    }
}
