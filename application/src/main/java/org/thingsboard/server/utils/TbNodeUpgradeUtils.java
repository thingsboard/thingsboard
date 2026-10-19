// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.utils;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.service.component.RuleNodeClassInfo;

import static org.thingsboard.server.common.data.DataConstants.QUEUE_NAME;

@Slf4j
public class TbNodeUpgradeUtils {

    public static void upgradeConfigurationAndVersion(RuleNode node, RuleNodeClassInfo nodeInfo) {
        JsonNode oldConfiguration = node.getConfiguration();
        int configurationVersion = node.getConfigurationVersion();

        int currentVersion = nodeInfo.getCurrentVersion();
        var configClass = nodeInfo.getAnnotation().configClazz();

        if (oldConfiguration == null || !oldConfiguration.isObject()) {
            log.warn("Failed to upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {}. " +
                            "Current configuration is null or not a json object. " +
                            "Going to set default configuration ... ",
                    node.getId(), node.getType(), configurationVersion, currentVersion);
            node.setConfiguration(getDefaultConfig(configClass));
        } else {
            var tbVersionedNode = getTbVersionedNode(nodeInfo);
            try {
                JsonNode queueName = oldConfiguration.get(QUEUE_NAME);
                TbPair<Boolean, JsonNode> upgradeResult = tbVersionedNode.upgrade(configurationVersion, oldConfiguration);
                if (upgradeResult.getFirst()) {
                    node.setConfiguration(upgradeResult.getSecond());
                    if (nodeInfo.getAnnotation().hasQueueName() && queueName != null && queueName.isTextual()) {
                        node.setQueueName(queueName.asText());
                    }
                }
            } catch (Exception e) {
                try {
                    JacksonUtil.treeToValue(oldConfiguration, configClass);
                } catch (Exception ex) {
                    log.warn("Failed to upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {}. " +
                                    "Going to set default configuration ... ",
                            node.getId(), node.getType(), configurationVersion, currentVersion, e);
                    node.setConfiguration(getDefaultConfig(configClass));
                }
            }
        }
        node.setConfigurationVersion(currentVersion);
    }

    @SneakyThrows
    private static TbNode getTbVersionedNode(RuleNodeClassInfo nodeInfo) {
        return (TbNode) nodeInfo.getClazz().getDeclaredConstructor().newInstance();
    }

    @SneakyThrows
    private static JsonNode getDefaultConfig(Class<? extends NodeConfiguration> configClass) {
        return JacksonUtil.valueToTree(configClass.getDeclaredConstructor().newInstance().defaultConfiguration());
    }

}
