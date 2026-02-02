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

import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.script.api.AbstractScriptInvokeService;
import org.thingsboard.script.api.RuleNodeScriptFactory;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * Created by ashvayka on 26.09.18.
 */
@Slf4j
public abstract class AbstractJsInvokeService extends AbstractScriptInvokeService implements JsInvokeService {

    protected final Map<UUID, JsScriptInfo> scriptInfoMap = new ConcurrentHashMap<>();
    private final Optional<TbApiUsageStateClient> apiUsageStateClient;
    private final Optional<TbApiUsageReportClient> apiUsageReportClient;

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
        this.apiUsageStateClient = apiUsageStateClient;
        this.apiUsageReportClient = apiUsageReportClient;
    }

    @Override
    protected boolean isScriptPresent(UUID scriptId) {
        return scriptInfoMap.containsKey(scriptId);
    }

    @Override
    protected boolean isExecEnabled(TenantId tenantId) {
        return !apiUsageStateClient.isPresent() || apiUsageStateClient.get().getApiUsageState(tenantId).isJsExecEnabled();
    }

    @Override
    protected void reportExecution(TenantId tenantId, CustomerId customerId) {
        apiUsageReportClient.ifPresent(client -> client.report(tenantId, customerId, ApiUsageRecordKey.JS_EXEC_COUNT, 1));
    }

    @Override
    protected JsScriptExecutionTask doInvokeFunction(UUID scriptId, Object[] args) {
        return new JsScriptExecutionTask(doInvokeFunction(scriptId, scriptInfoMap.get(scriptId), args));
    }

    @Override
    protected ListenableFuture<UUID> doEvalScript(TenantId tenantId, ScriptType scriptType, String scriptBody, UUID scriptId, String[] argNames) {
        String scriptHash = hash(tenantId, scriptBody);
        String functionName = constructFunctionName(scriptId, scriptHash);
        String jsScript = generateJsScript(scriptType, functionName, scriptBody, argNames);
        return doEval(scriptId, new JsScriptInfo(scriptHash, functionName), jsScript);
    }

    @Override
    protected void doRelease(UUID scriptId) throws Exception {
        doRelease(scriptId, scriptInfoMap.remove(scriptId));
    }

    @Override
    public String validate(TenantId tenantId, String scriptBody) {
        String errorMessage = super.validate(tenantId, scriptBody);
        if (errorMessage == null) {
            return JsValidator.validate(scriptBody);
        }
        return errorMessage;
    }

    protected abstract ListenableFuture<UUID> doEval(UUID scriptId, JsScriptInfo jsInfo, String scriptBody);

    protected abstract ListenableFuture<Object> doInvokeFunction(UUID scriptId, JsScriptInfo jsInfo, Object[] args);

    protected abstract void doRelease(UUID scriptId, JsScriptInfo scriptInfo) throws Exception;

    private String generateJsScript(ScriptType scriptType, String functionName, String scriptBody, String... argNames) {
        if (scriptType == ScriptType.RULE_NODE_SCRIPT) {
            return RuleNodeScriptFactory.generateRuleNodeScript(functionName, scriptBody, argNames);
        }
        throw new RuntimeException("No script factory implemented for scriptType: " + scriptType);
    }

    protected String constructFunctionName(UUID scriptId, String scriptHash) {
        return "invokeInternal_" + scriptId.toString().replace('-', '_');
    }

    protected String hash(TenantId tenantId, String scriptBody) {
        return Hashing.murmur3_128().newHasher()
                .putLong(tenantId.getId().getMostSignificantBits())
                .putLong(tenantId.getId().getLeastSignificantBits())
                .putUnencodedChars(scriptBody)
                .hash().toString();
    }

    @Override
    protected StatsType getStatsType() {
        return StatsType.JS_INVOKE;
    }
}
