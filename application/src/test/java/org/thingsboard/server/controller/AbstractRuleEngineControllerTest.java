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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.dao.rule.RuleChainService;

import java.util.function.Predicate;

/**
 * Created by ashvayka on 20.03.18.
 */
@TestPropertySource(properties = {
        "js.evaluator=mock",
})
public abstract class AbstractRuleEngineControllerTest extends AbstractControllerTest {

    @Autowired
    protected RuleChainService ruleChainService;

    protected RuleChain saveRuleChain(RuleChain ruleChain) throws Exception {
        return doPost("/api/ruleChain", ruleChain, RuleChain.class);
    }

    protected RuleChain getRuleChain(RuleChainId ruleChainId) throws Exception {
        return doGet("/api/ruleChain/" + ruleChainId.getId().toString(), RuleChain.class);
    }

    protected RuleChainMetaData saveRuleChainMetaData(RuleChainMetaData ruleChainMD) throws Exception {
        return doPost("/api/ruleChain/metadata", ruleChainMD, RuleChainMetaData.class);
    }

    protected RuleChainMetaData getRuleChainMetaData(RuleChainId ruleChainId) throws Exception {
        return doGet("/api/ruleChain/metadata/" + ruleChainId.getId().toString(), RuleChainMetaData.class);
    }

    protected JsonNode getMetadata(EventInfo outEvent) {
        String metaDataStr = outEvent.getBody().get("metadata").asText();
        try {
            return JacksonUtil.toJsonNode(metaDataStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    protected Predicate<EventInfo> filterByPostTelemetryEventType() {
        return event -> event.getBody().get("msgType").textValue().equals(TbMsgType.POST_TELEMETRY_REQUEST.name());
    }

}
