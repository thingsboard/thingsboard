/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.validator.RuleChainDataValidator;

public class RuleChainDataValidatorTest {

    @Test
    public void testSingletonSupport() {
        String node = "org.thingsboard.rule.engine.mqtt.TbMqttNode";
        RuleNode ruleNode = createRuleNode(node, false);
        RuleChainDataValidator.validateRuleNode(ruleNode);
        ruleNode.setSingletonMode(true);
        RuleChainDataValidator.validateRuleNode(ruleNode);
    }

    @Test
    public void testSingletonNotSupport() {
        String node = "org.thingsboard.rule.engine.flow.TbAckNode";
        RuleNode ruleNode = createRuleNode(node, false);
        RuleChainDataValidator.validateRuleNode(ruleNode);
        ruleNode.setSingletonMode(true);
        Assertions.assertThrows(DataValidationException.class,
                () -> RuleChainDataValidator.validateRuleNode(ruleNode),
                String.format("Singleton mode not supported for [%s].", ruleNode.getType()));
    }

    @Test
    public void testSingletonOnly() {
        String node = "org.thingsboard.rule.engine.mqtt.azure.TbAzureIotHubNode";
        RuleNode ruleNode = createRuleNode(node, true);
        RuleChainDataValidator.validateRuleNode(ruleNode);
        ruleNode.setSingletonMode(false);
        Assertions.assertThrows(DataValidationException.class,
                () -> RuleChainDataValidator.validateRuleNode(ruleNode),
                String.format("Supported only singleton mode for [%s].", ruleNode.getType()));
    }

    private RuleNode createRuleNode(String type, boolean singletonMode) {
        RuleNode ruleNode = new RuleNode();
        ruleNode.setName("test node");
        ruleNode.setType(type);
        ruleNode.setSingletonMode(singletonMode);
        ruleNode.setConfiguration(JacksonUtil.newObjectNode());
        return ruleNode;
    }
}
