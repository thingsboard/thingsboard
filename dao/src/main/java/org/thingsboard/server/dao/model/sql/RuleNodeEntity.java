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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.RULE_NODE_TABLE_NAME)
public class RuleNodeEntity extends BaseSqlEntity<RuleNode> {

    @Column(name = ModelConstants.RULE_NODE_CHAIN_ID_PROPERTY)
    private UUID ruleChainId;

    @Column(name = ModelConstants.RULE_NODE_TYPE_PROPERTY)
    private String type;

    @Column(name = ModelConstants.RULE_NODE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.RULE_NODE_VERSION_PROPERTY)
    private int configurationVersion;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.RULE_NODE_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Column(name = ModelConstants.DEBUG_SETTINGS)
    private String debugSettings;

    @Column(name = ModelConstants.SINGLETON_MODE)
    private boolean singletonMode;

    @Column(name = ModelConstants.QUEUE_NAME)
    private String queueName;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public RuleNodeEntity() {
    }

    public RuleNodeEntity(RuleNode ruleNode) {
        if (ruleNode.getId() != null) {
            this.setUuid(ruleNode.getUuidId());
        }
        this.setCreatedTime(ruleNode.getCreatedTime());
        if (ruleNode.getRuleChainId() != null) {
            this.ruleChainId = DaoUtil.getId(ruleNode.getRuleChainId());
        }
        this.type = ruleNode.getType();
        this.name = ruleNode.getName();
        this.debugSettings = JacksonUtil.toString(ruleNode.getDebugSettings());
        this.singletonMode = ruleNode.isSingletonMode();
        this.queueName = ruleNode.getQueueName();
        this.configurationVersion = ruleNode.getConfigurationVersion();
        this.configuration = ruleNode.getConfiguration();
        this.additionalInfo = ruleNode.getAdditionalInfo();
        if (ruleNode.getExternalId() != null) {
            this.externalId = ruleNode.getExternalId().getId();
        }
    }

    @Override
    public RuleNode toData() {
        RuleNode ruleNode = new RuleNode(new RuleNodeId(this.getUuid()));
        ruleNode.setCreatedTime(createdTime);
        if (ruleChainId != null) {
            ruleNode.setRuleChainId(new RuleChainId(ruleChainId));
        }
        ruleNode.setType(type);
        ruleNode.setName(name);
        ruleNode.setDebugSettings(JacksonUtil.fromString(debugSettings, DebugSettings.class));
        ruleNode.setSingletonMode(singletonMode);
        ruleNode.setQueueName(queueName);
        ruleNode.setConfigurationVersion(configurationVersion);
        ruleNode.setConfiguration(configuration);
        ruleNode.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            ruleNode.setExternalId(new RuleNodeId(externalId));
        }
        return ruleNode;
    }
}
