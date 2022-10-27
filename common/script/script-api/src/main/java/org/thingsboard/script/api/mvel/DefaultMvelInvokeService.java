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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.ExecutionContext;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.SandboxedParserContext;
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
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

@Slf4j
@ConditionalOnProperty(prefix = "mvel", value = "enabled", havingValue = "true", matchIfMissing = true)
@Service
public class DefaultMvelInvokeService extends AbstractScriptInvokeService implements MvelInvokeService {

    protected Map<UUID, MvelScript> scriptMap = new ConcurrentHashMap<>();
    private ParserContext parserContext;

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

    private ListeningExecutorService executor;

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
        parserContext = new SandboxedParserContext();
        parserContext.addImport("JSON", TbJson.class);
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(2, "mvel-executor"));
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
        return scriptMap.containsKey(scriptId);
    }

    @Override
    protected ListenableFuture<UUID> doEvalScript(TenantId tenantId, ScriptType scriptType, String scriptBody, UUID scriptId, String[] argNames) {
        if (NEW_KEYWORD_PATTERN.matcher(scriptBody).matches()) {
            //TODO: output line number and char pos.
            return Futures.immediateFailedFuture(new TbScriptException(scriptId, TbScriptException.ErrorCode.COMPILATION, scriptBody,
                    new IllegalArgumentException("Keyword 'new' is forbidden!")));
        }
        return executor.submit(() -> {
            try {
                Serializable compiledScript = MVEL.compileExpression(scriptBody, parserContext);
                MvelScript script = new MvelScript(compiledScript, scriptBody, argNames);
                scriptMap.put(scriptId, script);
                return scriptId;
            } catch (Exception e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.COMPILATION, scriptBody, e);
            }
        });
    }

    @Override
    protected ListenableFuture<Object> doInvokeFunction(UUID scriptId, Object[] args) {
        return executor.submit(() -> {
            MvelScript script = scriptMap.get(scriptId);
            if (script == null) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, null, new RuntimeException("Script not found!"));
            }
            try {
                ExecutionContext executionContext = new ExecutionContext();
                // TODO:
                return MVEL.executeExpression(script.getCompiledScript(), executionContext, script.createVars(args));
            } catch (OutOfMemoryError e) {
                Runtime.getRuntime().gc();
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, script.getScriptBody(), new RuntimeException("Memory error!"));
            } catch (Exception e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.RUNTIME, script.getScriptBody(), e);
            }
        });
    }

    @Override
    protected void doRelease(UUID scriptId) throws Exception {
        scriptMap.remove(scriptId);
    }
}
