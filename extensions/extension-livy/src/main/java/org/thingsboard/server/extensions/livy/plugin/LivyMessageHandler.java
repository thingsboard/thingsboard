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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.core.BasicStatusCodeResponse;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.api.plugins.msg.ResponsePluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleException;
import org.thingsboard.server.extensions.livy.action.LivyActionMessage;
import org.thingsboard.server.extensions.livy.action.LivyActionPayload;

@RequiredArgsConstructor
public class LivyMessageHandler implements RuleMsgHandler {

    private final String baseUrl;
    private final HttpHeaders headers;

    @Override
    public void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) throws RuleException {
        if (!(msg instanceof LivyActionMessage)) {
            throw new RuleException("Unsupported message type " + msg.getClass().getName() + "!");
        }
        LivyActionPayload payload = ((LivyActionMessage)msg).getPayload();
        try {
            new RestTemplate().exchange(
                    baseUrl + payload.getActionPath(),
                    HttpMethod.POST, //TODO: Make it Configurable option
                    new HttpEntity<>(payload.getMsgBody(), headers),
                    Void.class);

        } catch (RestClientException e) {
            throw new RuleException(e.getMessage(), e);
        }
    }
}
