// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.utils;

import com.fasterxml.jackson.databind.node.NullNode;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNode;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.rule.engine.metadata.TbGetCustomerAttributeNode;
import org.thingsboard.rule.engine.metadata.TbGetEntityDataNodeConfiguration;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.service.component.RuleNodeClassInfo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TbNodeUpgradeUtilsTest {

    @Test
    public void testUpgradeRuleNodeConfigurationWithNullConfig() throws Exception {
        // GIVEN
        var node = new RuleNode();
        var nodeInfo = mock(RuleNodeClassInfo.class);
        var nodeConfigClazz = TbGetAttributesNodeConfiguration.class;
        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);
        var defaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(nodeInfo.getClazz()).thenReturn((Class) TbGetAttributesNode.class);
        when(nodeInfo.getCurrentVersion()).thenReturn(1);
        when(nodeInfo.getAnnotation()).thenReturn(annotation);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);

        // WHEN
        TbNodeUpgradeUtils.upgradeConfigurationAndVersion(node, nodeInfo);
        // THEN
        Assertions.assertThat(node.getConfiguration()).isEqualTo(defaultConfig);
        Assertions.assertThat(node.getConfigurationVersion()).isEqualTo(1);
    }

    @Test
    public void testUpgradeRuleNodeConfigurationWithNullNodeConfig() throws Exception {
        // GIVEN
        var node = new RuleNode();
        node.setConfiguration(NullNode.instance);
        var nodeInfo = mock(RuleNodeClassInfo.class);
        var nodeConfigClazz = TbGetAttributesNodeConfiguration.class;
        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);
        var defaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(nodeInfo.getClazz()).thenReturn((Class) TbGetAttributesNode.class);
        when(nodeInfo.getCurrentVersion()).thenReturn(1);
        when(nodeInfo.getAnnotation()).thenReturn(annotation);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);

        // WHEN
        TbNodeUpgradeUtils.upgradeConfigurationAndVersion(node, nodeInfo);
        // THEN
        Assertions.assertThat(node.getConfiguration()).isEqualTo(defaultConfig);
        Assertions.assertThat(node.getConfigurationVersion()).isEqualTo(1);
    }

    @Test
    public void testUpgradeRuleNodeConfigurationWithNonNullConfig() throws Exception {
        // GIVEN
        var node = new RuleNode();
        var nodeInfo = mock(RuleNodeClassInfo.class);
        var nodeConfigClazz = TbGetAttributesNodeConfiguration.class;
        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);
        var defaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(nodeInfo.getClazz()).thenReturn((Class) TbGetAttributesNode.class);
        when(nodeInfo.getCurrentVersion()).thenReturn(1);
        when(nodeInfo.getAnnotation()).thenReturn(annotation);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);

        String versionZeroDefaultConfigStr = "{\"fetchToData\":false," +
                "\"clientAttributeNames\":[]," +
                "\"sharedAttributeNames\":[]," +
                "\"serverAttributeNames\":[]," +
                "\"latestTsKeyNames\":[]," +
                "\"tellFailureIfAbsent\":true," +
                "\"getLatestValueWithTs\":false}";
        node.setConfiguration(JacksonUtil.toJsonNode(versionZeroDefaultConfigStr));
        // WHEN
        TbNodeUpgradeUtils.upgradeConfigurationAndVersion(node, nodeInfo);
        // THEN
        Assertions.assertThat(node.getConfiguration()).isEqualTo(defaultConfig);
        Assertions.assertThat(node.getConfigurationVersion()).isEqualTo(1);

    }

    @Test
    public void testUpgradeRuleNodeConfigurationWithNewConfigAndOldConfigVersion() throws Exception {
        // GIVEN
        var node = new RuleNode();
        var nodeInfo = mock(RuleNodeClassInfo.class);
        var nodeConfigClazz = TbGetEntityDataNodeConfiguration.class;
        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);
        var defaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(nodeInfo.getClazz()).thenReturn((Class) TbGetCustomerAttributeNode.class);
        when(nodeInfo.getCurrentVersion()).thenReturn(1);
        when(nodeInfo.getAnnotation()).thenReturn(annotation);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);

        String versionOneDefaultConfig = "{\"fetchTo\":\"METADATA\"," +
                "\"dataMapping\":{\"alarmThreshold\":\"threshold\"}," +
                "\"dataToFetch\":\"ATTRIBUTES\"}";
        node.setConfiguration(JacksonUtil.toJsonNode(versionOneDefaultConfig));
        // WHEN
        TbNodeUpgradeUtils.upgradeConfigurationAndVersion(node, nodeInfo);
        // THEN
        Assertions.assertThat(node.getConfiguration()).isEqualTo(defaultConfig);
        Assertions.assertThat(node.getConfigurationVersion()).isEqualTo(1);

    }

    @Test
    public void testUpgradeRuleNodeConfigurationWithInvalidConfigAndOldConfigVersion() throws Exception {
        // GIVEN
        var node = new RuleNode();
        var nodeInfo = mock(RuleNodeClassInfo.class);
        var nodeConfigClazz = TbGetEntityDataNodeConfiguration.class;
        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);
        var defaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(nodeInfo.getClazz()).thenReturn((Class) TbGetCustomerAttributeNode.class);
        when(nodeInfo.getCurrentVersion()).thenReturn(1);
        when(nodeInfo.getAnnotation()).thenReturn(annotation);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);

        // missing telemetry field
        String oldConfig = "{\"attrMapping\":{\"alarmThreshold\":\"threshold\"}}";;
        node.setConfiguration(JacksonUtil.toJsonNode(oldConfig));
        // WHEN
        TbNodeUpgradeUtils.upgradeConfigurationAndVersion(node, nodeInfo);
        // THEN
        Assertions.assertThat(node.getConfiguration()).isEqualTo(defaultConfig);
        Assertions.assertThat(node.getConfigurationVersion()).isEqualTo(1);

    }

}
