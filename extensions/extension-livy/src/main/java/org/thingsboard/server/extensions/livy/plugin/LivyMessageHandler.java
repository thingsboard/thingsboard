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
package org.thingsboard.server.extensions.livy.plugin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
import org.thingsboard.server.extensions.livy.action.LivyActionMessage;
import org.thingsboard.server.extensions.livy.action.LivyActionPayload;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
public class LivyMessageHandler implements RuleMsgHandler {

    private static final String SPARK_JOB_KEY = "spark_";
    private final String baseUrl;
    private final HttpHeaders headers;
    private final Object lock = new Object();
    private final AtomicBoolean isSparkJobRunning = new AtomicBoolean(false);


    @Override
    public void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) throws RuleException {
        if (!(msg instanceof LivyActionMessage)) {
            throw new RuleException("Unsupported message type " + msg.getClass().getName() + "!");
        }
        LivyActionPayload payload = ((LivyActionMessage)msg).getPayload();
        final String sparkApplication = SPARK_JOB_KEY + payload.getSparkApplication().toString();
        if(!isSparkJobRunning.get()) {
            synchronized (lock) {
                ctx.loadAttributes(ruleId, DataConstants.SERVER_SCOPE, Collections.singleton(sparkApplication),
                        new PluginCallback<List<AttributeKvEntry>>() {
                            @Override
                            public void onSuccess(PluginContext ctx, List<AttributeKvEntry> values) {
                                setSparkJobRunning(values, sparkApplication);
                                if (!isSparkJobRunning.get()) {
                                    postSparkJob(payload);
                                    updateRuleAttribute(ctx, tenantId, ruleId, sparkApplication);
                                }
                            }

                            @Override
                            public void onFailure(PluginContext ctx, Exception e) {
                                log.error("Failed to fetch application status for tenant. {}", e);
                            }
                        });
            }
        }
    }

    private void updateRuleAttribute(PluginContext ctx, TenantId tenantId, RuleId ruleId, String sparkApplication) {
        long ts = System.currentTimeMillis();
        ctx.saveAttributes(tenantId, ruleId, DataConstants.SERVER_SCOPE,
                Collections.singletonList(new BaseAttributeKvEntry(new BooleanDataEntry(sparkApplication, true), ts)),
                new PluginCallback<Void>() {
                    @Override
                    public void onSuccess(PluginContext ctx, Void value) {
                        isSparkJobRunning.set(true);
                        log.warn("Updated attribute for Spark application {}", sparkApplication);
                    }

                    @Override
                    public void onFailure(PluginContext ctx, Exception e) {
                        log.error("Failed to save attributes {}", e);
                    }
                });
    }

    private void postSparkJob(LivyActionPayload payload) {
        try {
            log.warn("giving a rest call to Livy service");
            new RestTemplate().exchange(
                    baseUrl + payload.getActionPath(),
                    HttpMethod.POST,
                    new HttpEntity<>(payload.getMsgBody(), headers),
                    Void.class);

        } catch (RestClientException e) {
            log.error("Error occurred while rest call", e);
        }
    }

    private void setSparkJobRunning(List<AttributeKvEntry> values, String sparkApplication) {
        for (AttributeKvEntry e : values) {
            if (e.getKey().equalsIgnoreCase(sparkApplication) && e.getBooleanValue().get()) {
                isSparkJobRunning.set(true);
            }
        }
    }
}
