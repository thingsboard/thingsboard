/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.extensions.spark.computation.plugin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.extensions.api.plugins.PluginCallback;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleException;
import org.thingsboard.server.extensions.spark.computation.action.SparkComputationActionMessage;
import org.thingsboard.server.extensions.spark.computation.action.SparkComputationActionPayload;
import org.thingsboard.server.extensions.spark.computation.model.Batch;
import org.thingsboard.server.extensions.spark.computation.model.SparkComputationStatus;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class SparkComputationMessageHandler implements RuleMsgHandler {

    private static final String SPARK_COMPUTATION = "COMPUTATION_%s_%s";
    private static final String batchStateURI = "/batches/%d";
    private final String baseUrl;
    private final HttpHeaders headers;
    private volatile Map<String, String> sparkAppsForTenant = new ConcurrentHashMap<>();

    @Override
    public void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) throws RuleException {
        if (!(msg instanceof SparkComputationActionMessage)) {
            throw new RuleException("Unsupported message type " + msg.getClass().getName() + "!");
        }
        SparkComputationActionPayload payload = ((SparkComputationActionMessage)msg).getPayload();
        PluginCallback<List<AttributeKvEntry>> callback = pluginCallback(tenantId, ruleId, payload);
        if(!isSparkAppRunning(sparkApplicationKey(payload, ruleId.getId()))) {
            ctx.loadAttributes(ruleId, DataConstants.SERVER_SCOPE, Collections.singleton(sparkApplicationKey(payload, ruleId.getId())), callback);
        }
    }

    private PluginCallback<List<AttributeKvEntry>> pluginCallback(TenantId tenantId, RuleId ruleId, SparkComputationActionPayload payload) {
        return new PluginCallback<List<AttributeKvEntry>>() {
            @Override
            public void onSuccess(PluginContext ctx, List<AttributeKvEntry> values) {
                final String sparkApplication = sparkApplicationKey(payload, ruleId.getId());
                if (!isSparkAppRunning(sparkApplication)) {
                    onPluginCallbackSuccess(ctx, values, payload, tenantId, ruleId);
                }
            }

            @Override
            public void onFailure(PluginContext ctx, Exception e) {
                log.error("Failed to fetch application status for tenant.", e);
            }
        };
    }

    private String sparkApplicationKey(SparkComputationActionPayload payload, UUID ruleId) {
        return String.format(SPARK_COMPUTATION, payload.getSparkApplication(), ruleId.toString());
    }

    private boolean isSparkAppRunning(String sparkApplication){
        return !StringUtils.isEmpty(sparkAppsForTenant.get(sparkApplication));
    }

    private void onPluginCallbackSuccess(PluginContext ctx, List<AttributeKvEntry> values, SparkComputationActionPayload payload, TenantId tenantId, RuleId ruleId) {
        final String sparkApplication = sparkApplicationKey(payload, ruleId.getId());
        setSparkJobStatusFromAttributes(values, sparkApplication);
        Batch batch = postSparkApplication(payload, sparkApplication);
        if (batch != null) {
            updateRuleAttribute(ctx, tenantId, ruleId, sparkApplication, batch.getId());
        }
    }

    private void setSparkJobStatusFromAttributes(List<AttributeKvEntry> values, String sparkApplication) {
        for (AttributeKvEntry e : values) {
            if (e.getKey().equalsIgnoreCase(sparkApplication) &&
                    !StringUtils.isEmpty(e.getValueAsString())) {
                SparkComputationStatus status = fetchSparkJobStatus(Integer.parseInt(e.getValueAsString()));
                if(status == SparkComputationStatus.RUNNING || status == SparkComputationStatus.STARTING) {
                    sparkAppsForTenant.putIfAbsent(sparkApplication, e.getValueAsString());
                }
            }
        }
    }

    private SparkComputationStatus fetchSparkJobStatus(int batchId){
        String url = String.format(this.baseUrl + batchStateURI, batchId);
        try {
            ResponseEntity<Batch> response = new RestTemplate().exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Batch.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return SparkComputationStatus.RUNNING;
            }
        }catch (RestClientException e){
            return SparkComputationStatus.NOT_FOUND;
        }
        return SparkComputationStatus.UNKNOWN; //TODO: Throw an exception in this case
    }

    private synchronized Batch postSparkApplication(SparkComputationActionPayload payload, String sparkApplication) {
        if (!isSparkAppRunning(sparkApplication)) {
            try {
                ResponseEntity<Batch> response = new RestTemplate().exchange(
                        baseUrl + payload.getActionPath(),
                        HttpMethod.POST,
                        new HttpEntity<>(payload.getMsgBody(), headers),
                        Batch.class);
                if (response.getStatusCode() == HttpStatus.CREATED) {
                    Batch res =  response.getBody();
                    sparkAppsForTenant.put(sparkApplication, String.valueOf(res.getId()));
                    return res;
                }
            } catch (RestClientException e) {
                log.error("Error occurred while rest call", e);
            }
        }
        return null;
    }

    private void updateRuleAttribute(PluginContext ctx, TenantId tenantId, RuleId ruleId, String sparkApplication, int batchId) {
        long ts = System.currentTimeMillis();
        ctx.saveAttributes(tenantId, ruleId, DataConstants.SERVER_SCOPE,
                Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry(sparkApplication, String.valueOf(batchId)), ts)),
                new PluginCallback<Void>() {
                    @Override
                    public void onSuccess(PluginContext ctx, Void value) {
                        log.warn("Updated attribute for Spark application {} with {}", sparkApplication, batchId);
                    }

                    @Override
                    public void onFailure(PluginContext ctx, Exception e) {
                        log.error("Failed to save attributes {}", e);
                    }
                });
    }
}
