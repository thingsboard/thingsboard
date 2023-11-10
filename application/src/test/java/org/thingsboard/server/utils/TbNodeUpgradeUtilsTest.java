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
package org.thingsboard.server.utils;

import com.fasterxml.jackson.databind.node.NullNode;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNode;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.util.TbPair;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TbNodeUpgradeUtilsTest {

    @Test
    public void testUpgradeRuleNodeConfigurationWithNullConfig() throws Exception {
        // GIVEN
        var node = mock(RuleNode.class);
        var nodeClass = TbGetAttributesNode.class;
        var nodeConfigClazz = TbGetAttributesNodeConfiguration.class;

        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);

        var defaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(node.getConfiguration()).thenReturn(null);
        when(node.getConfigurationVersion()).thenReturn(0);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);
        // WHEN
        var upgradedConfig = TbNodeUpgradeUtils.upgradeRuleNodeConfiguration(node, annotation, nodeClass);

        // THEN
        Assertions.assertThat(upgradedConfig).isEqualTo(defaultConfig);
    }

    @Test
    public void testUpgradeRuleNodeConfigurationWithNullNodeConfig() throws Exception {
        // GIVEN
        var node = mock(RuleNode.class);
        var nodeClass = TbGetAttributesNode.class;
        var nodeConfigClazz = TbGetAttributesNodeConfiguration.class;

        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);

        var defaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(node.getConfiguration()).thenReturn(NullNode.instance);
        when(node.getConfigurationVersion()).thenReturn(0);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);
        // WHEN
        var upgradedConfig = TbNodeUpgradeUtils.upgradeRuleNodeConfiguration(node, annotation, nodeClass);

        // THEN
        Assertions.assertThat(upgradedConfig).isEqualTo(defaultConfig);
    }

    @Test
    public void testUpgradeRuleNodeConfigurationWithNonNullConfig() throws Exception {
        // GIVEN
        var node = mock(RuleNode.class);
        var nodeClass = TbGetAttributesNode.class;
        var nodeConfigClazz = TbGetAttributesNodeConfiguration.class;

        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);

        String versionZeroDefaultConfigStr = "{\"fetchToData\":false," +
                "\"clientAttributeNames\":[]," +
                "\"sharedAttributeNames\":[]," +
                "\"serverAttributeNames\":[]," +
                "\"latestTsKeyNames\":[]," +
                "\"tellFailureIfAbsent\":true," +
                "\"getLatestValueWithTs\":false}";

        var existingConfig = JacksonUtil.toJsonNode(versionZeroDefaultConfigStr);
        int fromVersion = 0;
        var currentDefaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(node.getConfiguration()).thenReturn(existingConfig);
        when(node.getConfigurationVersion()).thenReturn(fromVersion);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);

        TbNode tbVersionedNodeMock = mock(nodeClass);

        when(tbVersionedNodeMock.upgrade(fromVersion, existingConfig)).thenReturn(new TbPair<>(true, currentDefaultConfig));

        // WHEN
        var upgradedConfig = TbNodeUpgradeUtils.upgradeRuleNodeConfiguration(node, annotation, nodeClass);

        // THEN
        Assertions.assertThat(upgradedConfig).isEqualTo(currentDefaultConfig);
    }

}
