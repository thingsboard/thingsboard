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
package org.thingsboard.script.api.js;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import delight.nashornsandbox.NashornSandbox;
import delight.nashornsandbox.NashornSandboxes;
import delight.nashornsandbox.exceptions.ScriptCPUAbuseException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.script.api.TbScriptException;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@ConditionalOnProperty(prefix = "js", value = "evaluator", havingValue = "local", matchIfMissing = true)
@Service
public class NashornJsInvokeService extends AbstractJsInvokeService {

    private NashornSandbox sandbox;
    private ScriptEngine engine;
    private ExecutorService monitorExecutorService;
    private ListeningExecutorService jsExecutor;

    private final ReentrantLock evalLock = new ReentrantLock();

    @Value("${js.local.use_js_sandbox}")
    private boolean useJsSandbox;

    @Value("${js.local.monitor_thread_pool_size}")
    private int monitorThreadPoolSize;

    @Value("${js.local.max_cpu_time:8000}") // 8 seconds
    private long maxCpuTime;

    @Value("${js.local.max_memory:104857600}") // 100 MiB
    private long maxMemory;

    @Getter
    @Value("${js.local.max_errors}")
    private int maxErrors;

    @Getter
    @Value("${js.local.max_black_list_duration_sec:60}")
    private int maxBlackListDurationSec;

    @Getter
    @Value("${js.local.max_requests_timeout:0}")
    private long maxInvokeRequestsTimeout;

    @Getter
    @Value("${js.local.stats.enabled:false}")
    private boolean statsEnabled;

    @Value("${js.local.js_thread_pool_size:50}")
    private int jsExecutorThreadPoolSize;

    public NashornJsInvokeService(Optional<TbApiUsageStateClient> apiUsageStateClient, Optional<TbApiUsageReportClient> apiUsageReportClient) {
        super(apiUsageStateClient, apiUsageReportClient);
    }

    @Override
    protected String getStatsName() {
        return "Nashorn JS Invoke Stats";
    }

    @Override
    protected Executor getCallbackExecutor() {
        return MoreExecutors.directExecutor();
    }

    @Scheduled(fixedDelayString = "${js.local.stats.print_interval_ms:10000}")
    public void printStats() {
        super.printStats();
    }

    @PostConstruct
    @Override
    public void init() {
        super.init();
        jsExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(jsExecutorThreadPoolSize, "nashorn-js-executor"));
        if (useJsSandbox) {
            sandbox = NashornSandboxes.create();
            monitorExecutorService = ThingsBoardExecutors.newWorkStealingPool(monitorThreadPoolSize, "nashorn-js-monitor");
            sandbox.setExecutor(monitorExecutorService);
            sandbox.setMaxCPUTime(maxCpuTime);
            sandbox.setMaxMemory(maxMemory);
            sandbox.allowNoBraces(false);
            sandbox.allowLoadFunctions(true);
            sandbox.setMaxPreparedStatements(30);
        } else {
            ScriptEngineManager factory = new ScriptEngineManager();
            engine = factory.getEngineByName("nashorn");
        }
    }

    @PreDestroy
    @Override
    public void stop() {
        super.stop();
        if (monitorExecutorService != null) {
            monitorExecutorService.shutdownNow();
        }
        if (jsExecutor != null) {
            jsExecutor.shutdownNow();
        }
    }

    @Override
    protected ListenableFuture<UUID> doEval(UUID scriptId, JsScriptInfo scriptInfo, String jsScript) {
        return jsExecutor.submit(() -> {
            try {
                evalLock.lock();
                try {
                    if (useJsSandbox) {
                        sandbox.eval(jsScript);
                    } else {
                        engine.eval(jsScript);
                    }
                } finally {
                    evalLock.unlock();
                }
                scriptInfoMap.put(scriptId, scriptInfo);
                return scriptId;
            } catch (ScriptException e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.COMPILATION, jsScript, e);
            } catch (ScriptCPUAbuseException e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.TIMEOUT, jsScript, e);
            } catch (Exception e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, jsScript, e);
            }
        });
    }

    @Override
    protected ListenableFuture<Object> doInvokeFunction(UUID scriptId, JsScriptInfo scriptInfo, Object[] args) {
        return jsExecutor.submit(() -> {
            try {
                if (useJsSandbox) {
                    return sandbox.getSandboxedInvocable().invokeFunction(scriptInfo.getFunctionName(), args);
                } else {
                    return ((Invocable) engine).invokeFunction(scriptInfo.getFunctionName(), args);
                }
            } catch (ScriptException e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.RUNTIME, null, e);
            } catch (Exception e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, null, e);
            }
        });
    }

    protected void doRelease(UUID scriptId, JsScriptInfo scriptInfo) throws ScriptException {
        if (useJsSandbox) {
            sandbox.eval(scriptInfo.getFunctionName() + " = undefined;");
        } else {
            engine.eval(scriptInfo.getFunctionName() + " = undefined;");
        }
    }

}
