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
package org.thingsboard.script.api.mvel;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.ExecutionContext;
import org.mvel2.MVEL;
import org.mvel2.SandboxedParserConfiguration;
import org.mvel2.SandboxedParserContext;
import org.mvel2.ScriptMemoryOverflowException;
import org.mvel2.optimizers.OptimizerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.script.api.AbstractScriptInvokeService;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.script.api.TbScriptException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

@Slf4j
@ConditionalOnProperty(prefix = "mvel", value = "enabled", havingValue = "true", matchIfMissing = true)
@Service
public class DefaultMvelInvokeService extends AbstractScriptInvokeService implements MvelInvokeService {

    protected final Map<UUID, String> scriptIdToHash = new ConcurrentHashMap<>();
    protected final Map<String, MvelScript> scriptMap = new ConcurrentHashMap<>();
    protected Cache<String, Serializable> compiledScriptsCache;

    private SandboxedParserConfiguration parserConfig;

    private static final Pattern NEW_KEYWORD_PATTERN = Pattern.compile("new\\s");

    @Getter
    @Value("${mvel.max_total_args_size:100000}")
    private long maxTotalArgsSize;
    @Getter
    @Value("${mvel.max_result_size:300000}")
    private long maxResultSize;
    @Getter
    @Value("${mvel.max_script_body_size:50000}")
    private long maxScriptBodySize;

    @Getter
    @Value("${mvel.max_errors:3}")
    private int maxErrors;

    @Getter
    @Value("${mvel.max_black_list_duration_sec:60}")
    private int maxBlackListDurationSec;

    @Getter
    @Value("${mvel.max_requests_timeout:0}")
    private long maxInvokeRequestsTimeout;

    @Getter
    @Value("${mvel.stats.enabled:false}")
    private boolean statsEnabled;

    @Value("${mvel.thread_pool_size:50}")
    private int threadPoolSize;

    @Value("${mvel.max_memory_limit_mb:8}")
    private long maxMemoryLimitMb;

    @Value("${mvel.compiled_scripts_cache_size:1000}")
    private int compiledScriptsCacheSize;

    private ListeningExecutorService executor;

    private final Lock lock = new ReentrantLock();

    protected DefaultMvelInvokeService(Optional<TbApiUsageStateClient> apiUsageStateClient, Optional<TbApiUsageReportClient> apiUsageReportClient) {
        super(apiUsageStateClient, apiUsageReportClient);
    }

    @Scheduled(fixedDelayString = "${mvel.stats.print_interval_ms:10000}")
    public void printStats() {
        super.printStats();
    }

    @SneakyThrows
    @PostConstruct
    public void init() {
        super.init();
        OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);
        parserConfig = new SandboxedParserConfiguration();
        parserConfig.addImport("JSON", TbJson.class);
        parserConfig.registerDataType("Date", TbDate.class, date -> 8L);
        TbUtils.register(parserConfig);
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(threadPoolSize, "mvel-executor"));
        try {
            // Special command to warm up MVEL engine
            Serializable script = compileScript("var warmUp = {}; warmUp");
            MVEL.executeTbExpression(script, new ExecutionContext(parserConfig), Collections.emptyMap());
        } catch (Exception e) {
            // do nothing
        }
        compiledScriptsCache = Caffeine.newBuilder()
                .maximumSize(compiledScriptsCacheSize)
                .build();
    }

    @PreDestroy
    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    protected String getStatsName() {
        return "MVEL Scripts Stats";
    }

    @Override
    protected Executor getCallbackExecutor() {
        return MoreExecutors.directExecutor();
    }

    @Override
    protected boolean isScriptPresent(UUID scriptId) {
        return scriptIdToHash.containsKey(scriptId);
    }

    @Override
    protected ListenableFuture<UUID> doEvalScript(TenantId tenantId, ScriptType scriptType, String scriptBody, UUID scriptId, String[] argNames) {
        return executor.submit(() -> {
            try {
                String scriptHash = hash(scriptBody, argNames);
                compiledScriptsCache.get(scriptHash, k -> {
                    return compileScript(scriptBody);
                });
                lock.lock();
                try {
                    scriptIdToHash.put(scriptId, scriptHash);
                    scriptMap.computeIfAbsent(scriptHash, k -> {
                        return new MvelScript(scriptBody, argNames);
                    });
                } finally {
                    lock.unlock();
                }
                return scriptId;
            } catch (Exception e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.COMPILATION, scriptBody, e);
            }
        });
    }

    @Override
    protected MvelScriptExecutionTask doInvokeFunction(UUID scriptId, Object[] args) {
        ExecutionContext executionContext = new ExecutionContext(this.parserConfig, maxMemoryLimitMb * 1024 * 1024);
        return new MvelScriptExecutionTask(executionContext, executor.submit(() -> {
            String scriptHash = scriptIdToHash.get(scriptId);
            if (scriptHash == null) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, null, new RuntimeException("Script not found!"));
            }
            MvelScript script = scriptMap.get(scriptHash);
            Serializable compiledScript = compiledScriptsCache.get(scriptHash, k -> {
                return compileScript(script.getScriptBody());
            });
            try {
                return MVEL.executeTbExpression(compiledScript, executionContext, script.createVars(args));
            } catch (ScriptMemoryOverflowException e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, script.getScriptBody(), new RuntimeException("Script memory overflow!"));
            } catch (Exception e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.RUNTIME, script.getScriptBody(), e);
            }
        }));
    }

    @Override
    protected void doRelease(UUID scriptId) throws Exception {
        String scriptHash = scriptIdToHash.remove(scriptId);
        if (scriptHash != null) {
            lock.lock();
            try {
                if (!scriptIdToHash.containsValue(scriptHash)) {
                    scriptMap.remove(scriptHash);
                    compiledScriptsCache.invalidate(scriptHash);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private Serializable compileScript(String scriptBody) {
        return MVEL.compileExpression(scriptBody, new SandboxedParserContext(parserConfig));
    }

    @SuppressWarnings("UnstableApiUsage")
    protected String hash(String scriptBody, String[] argNames) {
        Hasher hasher = Hashing.murmur3_128().newHasher();
        hasher.putUnencodedChars(scriptBody);
        for (String argName : argNames) {
            hasher.putString(argName, StandardCharsets.UTF_8);
        }
        return hasher.hash().toString();
    }

}
