/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ADDITIONAL_INFO_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.DEBUG_MODE;
import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_NODE_CHAIN_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_NODE_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_NODE_CONFIGURATION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_NODE_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_NODE_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

@Table(name = RULE_NODE_COLUMN_FAMILY_NAME)
@EqualsAndHashCode
@ToString
public class RuleNodeEntity implements SearchTextEntity<RuleNode> {

    @PartitionKey
    @Column(name = ID_PROPERTY)
    private UUID id;
    @Column(name = RULE_NODE_CHAIN_ID_PROPERTY)
    private UUID ruleChainId;
    @Column(name = RULE_NODE_TYPE_PROPERTY)
    private String type;
    @Column(name = RULE_NODE_NAME_PROPERTY)
    private String name;
    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;
    @Column(name = RULE_NODE_CONFIGURATION_PROPERTY, codec = JsonCodec.class)
    private JsonNode configuration;
    @Column(name = ADDITIONAL_INFO_PROPERTY, codec = JsonCodec.class)
    private JsonNode additionalInfo;
    @Getter
    @Setter
    @Column(name = DEBUG_MODE)
    private boolean debugMode;

    public RuleNodeEntity() {
    }

    public RuleNodeEntity(RuleNode ruleNode) {
        if (ruleNode.getId() != null) {
            this.id = ruleNode.getUuidId();
        }
        if (ruleNode.getRuleChainId() != null) {
            this.ruleChainId = ruleNode.getRuleChainId().getId();
        }
        this.type = ruleNode.getType();
        this.name = ruleNode.getName();
        this.debugMode = ruleNode.isDebugMode();
        this.searchText = ruleNode.getName();
        this.configuration = ruleNode.getConfiguration();
        this.additionalInfo = ruleNode.getAdditionalInfo();
    }

    @Override
    public String getSearchTextSource() {
        return getSearchText();
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRuleChainId() {
        return ruleChainId;
    }

    public void setRuleChainId(UUID ruleChainId) {
        this.ruleChainId = ruleChainId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSearchText() {
        return searchText;
    }

    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    public JsonNode getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    public RuleNode toData() {
        RuleNode ruleNode = new RuleNode(new RuleNodeId(id));
        ruleNode.setCreatedTime(UUIDs.unixTimestamp(id));
        if (this.ruleChainId != null) {
            ruleNode.setRuleChainId(new RuleChainId(this.ruleChainId));
        }
        ruleNode.setType(this.type);
        ruleNode.setName(this.name);
        ruleNode.setDebugMode(this.debugMode);
        ruleNode.setConfiguration(this.configuration);
        ruleNode.setAdditionalInfo(this.additionalInfo);
        return ruleNode;
    }

}
