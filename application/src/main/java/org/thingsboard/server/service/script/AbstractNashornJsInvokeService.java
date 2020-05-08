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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import delight.nashornsandbox.NashornSandbox;
import delight.nashornsandbox.NashornSandboxes;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class AbstractNashornJsInvokeService extends AbstractJsInvokeService {

    private NashornSandbox sandbox;
    private ScriptEngine engine;
    private ExecutorService monitorExecutorService;

    private final AtomicInteger jsPushedMsgs = new AtomicInteger(0);
    private final AtomicInteger jsInvokeMsgs = new AtomicInteger(0);
    private final AtomicInteger jsEvalMsgs = new AtomicInteger(0);
    private final AtomicInteger jsFailedMsgs = new AtomicInteger(0);
    private final AtomicInteger jsTimeoutMsgs = new AtomicInteger(0);
    private final FutureCallback<UUID> evalCallback = new JsStatCallback<>(jsEvalMsgs, jsTimeoutMsgs, jsFailedMsgs);
    private final FutureCallback<Object> invokeCallback = new JsStatCallback<>(jsInvokeMsgs, jsTimeoutMsgs, jsFailedMsgs);

    @Autowired
    @Getter
    private JsExecutorService jsExecutor;

    @Value("${js.local.max_requests_timeout:0}")
    private long maxRequestsTimeout;

    @Value("${js.local.stats.enabled:false}")
    private boolean statsEnabled;

    @Scheduled(fixedDelayString = "${js.local.stats.print_interval_ms:10000}")
    public void printStats() {
        if (statsEnabled) {
            int pushedMsgs = jsPushedMsgs.getAndSet(0);
            int invokeMsgs = jsInvokeMsgs.getAndSet(0);
            int evalMsgs = jsEvalMsgs.getAndSet(0);
            int failed = jsFailedMsgs.getAndSet(0);
            int timedOut = jsTimeoutMsgs.getAndSet(0);
            if (pushedMsgs > 0 || invokeMsgs > 0 || evalMsgs > 0 || failed > 0 || timedOut > 0) {
                log.info("Nashorn JS Invoke Stats: pushed [{}] received [{}] invoke [{}] eval [{}] failed [{}] timedOut [{}]",
                        pushedMsgs, invokeMsgs + evalMsgs, invokeMsgs, evalMsgs, failed, timedOut);
            }
        }
    }

    @PostConstruct
    public void init() {
        super.init(maxRequestsTimeout);
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
        super.stop();
        if (monitorExecutorService != null) {
            monitorExecutorService.shutdownNow();
        }
    }

    protected abstract boolean useJsSandbox();

    protected abstract int getMonitorThreadPoolSize();

    protected abstract long getMaxCpuTime();

    @Override
    protected ListenableFuture<UUID> doEval(UUID scriptId, String functionName, String jsScript) {
        jsPushedMsgs.incrementAndGet();
        ListenableFuture<UUID> result = jsExecutor.executeAsync(() -> {
            try {
                if (useJsSandbox()) {
                    sandbox.eval(jsScript);
                } else {
                    engine.eval(jsScript);
                }
                scriptIdToNameMap.put(scriptId, functionName);
                return scriptId;
            } catch (Exception e) {
                log.warn("Failed to compile JS script: {}", e.getMessage(), e);
                throw new ExecutionException(e);
            }
        });
        if (maxRequestsTimeout > 0) {
            result = Futures.withTimeout(result, maxRequestsTimeout, TimeUnit.MILLISECONDS, timeoutExecutorService);
        }
        Futures.addCallback(result, evalCallback, MoreExecutors.directExecutor());
        return result;
    }

    @Override
    protected ListenableFuture<Object> doInvokeFunction(UUID scriptId, String functionName, Object[] args) {
        jsPushedMsgs.incrementAndGet();
        ListenableFuture<Object> result = jsExecutor.executeAsync(() -> {
            try {
                if (useJsSandbox()) {
                    return sandbox.getSandboxedInvocable().invokeFunction(functionName, args);
                } else {
                    return ((Invocable) engine).invokeFunction(functionName, args);
                }
            } catch (Exception e) {
                onScriptExecutionError(scriptId);
                throw new ExecutionException(e);
            }
        });

        if (maxRequestsTimeout > 0) {
            result = Futures.withTimeout(result, maxRequestsTimeout, TimeUnit.MILLISECONDS, timeoutExecutorService);
        }
        Futures.addCallback(result, invokeCallback, MoreExecutors.directExecutor());
        return result;
    }

    protected void doRelease(UUID scriptId, String functionName) throws ScriptException {
        if (useJsSandbox()) {
            sandbox.eval(functionName + " = undefined;");
        } else {
            engine.eval(functionName + " = undefined;");
        }
    }

}
