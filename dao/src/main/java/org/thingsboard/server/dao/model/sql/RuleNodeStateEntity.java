/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.RuleNodeStateId;
import org.thingsboard.server.common.data.rule.RuleNodeState;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.RULE_NODE_STATE_TABLE_NAME)
public class RuleNodeStateEntity extends BaseSqlEntity<RuleNodeState> {

    @Column(name = ModelConstants.RULE_NODE_STATE_NODE_ID_PROPERTY)
    private UUID ruleNodeId;

    @Column(name = ModelConstants.RULE_NODE_STATE_ENTITY_TYPE_PROPERTY)
    private String entityType;

    @Column(name = ModelConstants.RULE_NODE_STATE_ENTITY_ID_PROPERTY)
    private UUID entityId;

    @Column(name = ModelConstants.RULE_NODE_STATE_DATA_PROPERTY)
    private String stateData;

    public RuleNodeStateEntity() {
    }

    public RuleNodeStateEntity(RuleNodeState ruleNodeState) {
        if (ruleNodeState.getId() != null) {
            this.setUuid(ruleNodeState.getUuidId());
        }
        this.setCreatedTime(ruleNodeState.getCreatedTime());
        this.ruleNodeId = DaoUtil.getId(ruleNodeState.getRuleNodeId());
        this.entityId = ruleNodeState.getEntityId().getId();
        this.entityType = ruleNodeState.getEntityId().getEntityType().name();
        this.stateData = ruleNodeState.getStateData();
    }

    @Override
    public RuleNodeState toData() {
        RuleNodeState ruleNode = new RuleNodeState(new RuleNodeStateId(this.getUuid()));
        ruleNode.setCreatedTime(createdTime);
        ruleNode.setRuleNodeId(new RuleNodeId(ruleNodeId));
        ruleNode.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        ruleNode.setStateData(stateData);
        return ruleNode;
    }
}
