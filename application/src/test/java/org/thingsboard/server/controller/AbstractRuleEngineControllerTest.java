/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.dao.rule.RuleChainService;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * Created by ashvayka on 20.03.18.
 */
public class AbstractRuleEngineControllerTest extends AbstractControllerTest {

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

    protected PageData<Event> getDebugEvents(TenantId tenantId, EntityId entityId, int limit) throws Exception {
        TimePageLink pageLink = new TimePageLink(limit);
        return doGetTypedWithTimePageLink("/api/events/{entityType}/{entityId}/{eventType}?tenantId={tenantId}&",
                new TypeReference<PageData<Event>>() {
                }, pageLink, entityId.getEntityType(), entityId.getId(), DataConstants.DEBUG_RULE_NODE, tenantId.getId());
    }

    protected JsonNode getMetadata(Event outEvent) {
        String metaDataStr = outEvent.getBody().get("metadata").asText();
        try {
            return mapper.readTree(metaDataStr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Predicate<Event> filterByCustomEvent() {
        return event -> event.getBody().get("msgType").textValue().equals("CUSTOM");
    }

}
