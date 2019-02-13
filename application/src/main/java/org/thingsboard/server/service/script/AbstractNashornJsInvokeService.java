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
import delight.nashornsandbox.NashornSandbox;
import delight.nashornsandbox.NashornSandboxes;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public abstract class AbstractNashornJsInvokeService extends AbstractJsInvokeService {

    private NashornSandbox sandbox;
    private ScriptEngine engine;
    private ExecutorService monitorExecutorService;

    @PostConstruct
    public void init() {
        if (useJsSandbox()) {
            sandbox = NashornSandboxes.create();
            monitorExecutorService = Executors.newWorkStealingPool(getMonitorThreadPoolSize());
            sandbox.setExecutor(monitorExecutorService);
            sandbox.setMaxCPUTime(getMaxCpuTime());
            sandbox.allowNoBraces(false);
            sandbox.allowLoadFunctions(true);
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

    @Override
    protected ListenableFuture<UUID> doEval(UUID scriptId, String functionName, String jsScript) {
        try {
            if (useJsSandbox()) {
                sandbox.eval(jsScript);
            } else {
                engine.eval(jsScript);
            }
            scriptIdToNameMap.put(scriptId, functionName);
        } catch (Exception e) {
            log.warn("Failed to compile JS script: {}", e.getMessage(), e);
            return Futures.immediateFailedFuture(e);
        }
        return Futures.immediateFuture(scriptId);
    }

    @Override
    protected ListenableFuture<Object> doInvokeFunction(UUID scriptId, String functionName, Object[] args) {
        try {
            Object result;
            if (useJsSandbox()) {
                result = sandbox.getSandboxedInvocable().invokeFunction(functionName, args);
            } else {
                result = ((Invocable) engine).invokeFunction(functionName, args);
            }
            return Futures.immediateFuture(result);
        } catch (Exception e) {
            onScriptExecutionError(scriptId);
            return Futures.immediateFailedFuture(e);
        }
    }

    protected void doRelease(UUID scriptId, String functionName) throws ScriptException {
        if (useJsSandbox()) {
            sandbox.eval(functionName + " = undefined;");
        } else {
            engine.eval(functionName + " = undefined;");
        }
    }

}
