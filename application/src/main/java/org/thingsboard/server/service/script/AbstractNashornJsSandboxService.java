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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EntityId;

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

    private final Map<UUID, String> functionsMap = new ConcurrentHashMap<>();
    private final Map<BlackListKey, BlackListInfo> blackListedFunctions = new ConcurrentHashMap<>();

    private final Map<String, ScriptInfo> scriptKeyToInfo = new ConcurrentHashMap<>();
    private final Map<UUID, ScriptInfo> scriptIdToInfo = new ConcurrentHashMap<>();

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
        if (monitorExecutorService != null) {
            monitorExecutorService.shutdownNow();
        }
    }

    protected abstract boolean useJsSandbox();

    protected abstract int getMonitorThreadPoolSize();

    protected abstract long getMaxCpuTime();

    protected abstract int getMaxErrors();

    @Override
    public ListenableFuture<UUID> eval(JsScriptType scriptType, String scriptBody, String... argNames) {
        ScriptInfo scriptInfo = deduplicate(scriptType, scriptBody);
        UUID scriptId = scriptInfo.getId();
        AtomicInteger duplicateCount = scriptInfo.getCount();

        synchronized (scriptInfo.getLock()) {
            if (duplicateCount.compareAndSet(0, 1)) {
                try {
                    evaluate(scriptId, scriptType, scriptBody, argNames);
                } catch (Exception e) {
                    duplicateCount.decrementAndGet();
                    log.warn("Failed to compile JS script: {}", e.getMessage(), e);
                    return Futures.immediateFailedFuture(e);
                }
            } else {
                duplicateCount.incrementAndGet();
            }
        }
        return Futures.immediateFuture(scriptId);
    }

    private void evaluate(UUID scriptId, JsScriptType scriptType, String scriptBody, String... argNames) throws ScriptException {
        String functionName = "invokeInternal_" + scriptId.toString().replace('-', '_');
        String jsScript = generateJsScript(scriptType, functionName, scriptBody, argNames);
        if (useJsSandbox()) {
            sandbox.eval(jsScript);
        } else {
            engine.eval(jsScript);
        }
        functionsMap.put(scriptId, functionName);
    }

    @Override
    public ListenableFuture<Object> invokeFunction(UUID scriptId, EntityId entityId, Object... args) {
        String functionName = functionsMap.get(scriptId);
        if (functionName == null) {
            String message = "No compiled script found for scriptId: [" + scriptId + "]!";
            log.warn(message);
            return Futures.immediateFailedFuture(new RuntimeException(message));
        }

        BlackListInfo blackListInfo = blackListedFunctions.get(new BlackListKey(scriptId, entityId));
        if (blackListInfo != null && blackListInfo.getCount() >= getMaxErrors()) {
            RuntimeException throwable = new RuntimeException("Script is blacklisted due to maximum error count " + getMaxErrors() + "!", blackListInfo.getCause());
            throwable.printStackTrace();
            return Futures.immediateFailedFuture(throwable);
        }

        try {
            return invoke(functionName, args);
        } catch (Exception e) {
            BlackListKey blackListKey = new BlackListKey(scriptId, entityId);
            blackListedFunctions.computeIfAbsent(blackListKey, key -> new BlackListInfo()).incrementWithReason(e);
            return Futures.immediateFailedFuture(e);
        }
    }

    private ListenableFuture<Object> invoke(String functionName, Object... args) throws ScriptException, NoSuchMethodException {
        Object result;
        if (useJsSandbox()) {
            result = sandbox.getSandboxedInvocable().invokeFunction(functionName, args);
        } else {
            result = ((Invocable) engine).invokeFunction(functionName, args);
        }
        return Futures.immediateFuture(result);
    }

    @Override
    public ListenableFuture<Void> release(UUID scriptId, EntityId entityId) {
        ScriptInfo scriptInfo = scriptIdToInfo.get(scriptId);
        if (scriptInfo == null) {
            log.warn("Script release called for not existing script id [{}]", scriptId);
            return Futures.immediateFuture(null);
        }

        synchronized (scriptInfo.getLock()) {
            int remainingDuplicates = scriptInfo.getCount().decrementAndGet();
            if (remainingDuplicates > 0) {
                return Futures.immediateFuture(null);
            }

            String functionName = functionsMap.get(scriptId);
            if (functionName != null) {
                try {
                    if (useJsSandbox()) {
                        sandbox.eval(functionName + " = undefined;");
                    } else {
                        engine.eval(functionName + " = undefined;");
                    }
                    functionsMap.remove(scriptId);
                    blackListedFunctions.remove(new BlackListKey(scriptId, entityId));
                } catch (ScriptException e) {
                    log.error("Could not release script [{}] [{}]", scriptId, remainingDuplicates);
                    return Futures.immediateFailedFuture(e);
                }
            } else {
                log.warn("Function name do not exist for script [{}] [{}]", scriptId, remainingDuplicates);
            }
        }
        return Futures.immediateFuture(null);
    }


    private String generateJsScript(JsScriptType scriptType, String functionName, String scriptBody, String... argNames) {
        switch (scriptType) {
            case RULE_NODE_SCRIPT:
                return RuleNodeScriptFactory.generateRuleNodeScript(functionName, scriptBody, argNames);
            default:
                throw new RuntimeException("No script factory implemented for scriptType: " + scriptType);
        }
    }

    private ScriptInfo deduplicate(JsScriptType scriptType, String scriptBody) {
        ScriptInfo meta = ScriptInfo.preInit();
        String key = deduplicateKey(scriptType, scriptBody);
        ScriptInfo latestMeta = scriptKeyToInfo.computeIfAbsent(key, i -> meta);
        return scriptIdToInfo.computeIfAbsent(latestMeta.getId(), i -> latestMeta);
    }

    private String deduplicateKey(JsScriptType scriptType, String scriptBody) {
        return scriptType + "_" + scriptBody;
    }

    @Getter
    private static class ScriptInfo {
        private final UUID id;
        private final Object lock;
        private final AtomicInteger count;

        ScriptInfo(UUID id, Object lock, AtomicInteger count) {
            this.id = id;
            this.lock = lock;
            this.count = count;
        }

        static ScriptInfo preInit() {
            UUID preId = UUID.randomUUID();
            AtomicInteger preCount = new AtomicInteger();
            Object preLock = new Object();
            return new ScriptInfo(preId, preLock, preCount);
        }
    }

    @EqualsAndHashCode
    @Getter
    @RequiredArgsConstructor
    private static class BlackListKey {
        private final UUID scriptId;
        private final EntityId entityId;

    }

    @Data
    private static class BlackListInfo {
        private final AtomicInteger count;
        private Exception ex;

        BlackListInfo() {
            this.count = new AtomicInteger(0);
        }

        void incrementWithReason(Exception e) {
            count.incrementAndGet();
            ex = e;
        }

        int getCount() {
            return count.get();
        }

        Exception getCause() {
            return ex;
        }
    }
}
