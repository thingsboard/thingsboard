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
package org.thingsboard.server.extensions.rest.plugin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
import org.thingsboard.server.extensions.rest.action.RestApiCallActionMsg;
import org.thingsboard.server.extensions.rest.action.RestApiCallActionPayload;

@RequiredArgsConstructor
public class RestApiCallMsgHandler implements RuleMsgHandler {

    private final String baseUrl;
    private final HttpHeaders headers;

    @Override
    public void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) throws RuleException {
        if (!(msg instanceof RestApiCallActionMsg)) {
            throw new RuleException("Unsupported message type " + msg.getClass().getName() + "!");
        }
        RestApiCallActionPayload payload = ((RestApiCallActionMsg)msg).getPayload();
        try {
            ResponseEntity<String> exchangeResponse = new RestTemplate().exchange(
                    baseUrl + payload.getActionPath(),
                    payload.getHttpMethod(),
                    new HttpEntity<>(payload.getMsgBody(), headers),
                    String.class);
            if (exchangeResponse.getStatusCode().equals(payload.getExpectedResultCode()) && payload.isSync()) {
                ctx.reply(new ResponsePluginToRuleMsg(msg.getUid(), tenantId, ruleId,
                        BasicStatusCodeResponse.onSuccess(payload.getMsgType(), payload.getRequestId())));
            } else if(!exchangeResponse.getStatusCode().equals(payload.getExpectedResultCode())) {
                throw new RuntimeException("Response Status Code '"
                        + exchangeResponse.getStatusCode()
                        + "' doesn't equals to Expected Status Code '"
                        + payload.getExpectedResultCode() + "'");
            }

        } catch (RestClientException e) {
            throw new RuleException(e.getMessage(), e);
        }
    }
}
