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
package org.thingsboard.script.api.js;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.script.api.AbstractScriptInvokeService;
import org.thingsboard.script.api.RuleNodeScriptFactory;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ashvayka on 26.09.18.
 */
@Slf4j
public abstract class AbstractJsInvokeService extends AbstractScriptInvokeService {

    protected Map<UUID, String> scriptIdToNameMap = new ConcurrentHashMap<>();

    @Getter
    @Value("${js.max_total_args_size:100000}")
    private long maxTotalArgsSize;
    @Getter
    @Value("${js.max_result_size:300000}")
    private long maxResultSize;
    @Getter
    @Value("${js.max_script_body_size:50000}")
    private long maxScriptBodySize;

    protected AbstractJsInvokeService(Optional<TbApiUsageStateClient> apiUsageStateClient, Optional<TbApiUsageReportClient> apiUsageReportClient) {
        super(apiUsageStateClient, apiUsageReportClient);
    }

    @Override
    protected boolean isScriptPresent(UUID scriptId) {
        return scriptIdToNameMap.containsKey(scriptId);
    }

    @Override
    public ListenableFuture<Void> release(UUID scriptId) {
        String functionName = scriptIdToNameMap.get(scriptId);
        if (functionName != null) {
            try {
                scriptIdToNameMap.remove(scriptId);
                disabledScripts.remove(scriptId);
                doRelease(scriptId, functionName);
            } catch (Exception e) {
                return Futures.immediateFailedFuture(e);
            }
        }
        return Futures.immediateFuture(null);
    }

    @Override
    protected ListenableFuture<Object> doInvokeFunction(UUID scriptId, Object[] args) {
        return doInvokeFunction(scriptId, scriptIdToNameMap.get(scriptId), args);
    }

    @Override
    protected ListenableFuture<UUID> doEvalScript(ScriptType scriptType, String scriptBody, UUID scriptId, String[] argNames) {
        String functionName = "invokeInternal_" + scriptId.toString().replace('-', '_');
        String jsScript = generateJsScript(scriptType, functionName, scriptBody, argNames);
        return doEval(scriptId, functionName, jsScript);
    }

    @Override
    protected void doRelease(UUID scriptId) throws Exception {
        String functionName = scriptIdToNameMap.remove(scriptId);
        doRelease(scriptId, functionName);
    }

    protected abstract ListenableFuture<UUID> doEval(UUID scriptId, String functionName, String scriptBody);

    protected abstract ListenableFuture<Object> doInvokeFunction(UUID scriptId, String functionName, Object[] args);

    protected abstract void doRelease(UUID scriptId, String functionName) throws Exception;

    private String generateJsScript(ScriptType scriptType, String functionName, String scriptBody, String... argNames) {
        if (scriptType == ScriptType.RULE_NODE_SCRIPT) {
            return RuleNodeScriptFactory.generateRuleNodeScript(functionName, scriptBody, argNames);
        }
        throw new RuntimeException("No script factory implemented for scriptType: " + scriptType);
    }

}
