// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainDetails;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.RULE_CHAIN_TABLE_NAME)
public class RuleChainDetailsEntity extends AbstractRuleChainEntity<RuleChainDetails> {

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.RULE_CHAIN_NOTES_PROPERTY)
    private JsonNode notes;

    public RuleChainDetailsEntity() {
        super();
    }

    public RuleChainDetailsEntity(RuleChainDetails ruleChainDetails) {
        super(ruleChainDetails);
        if (ruleChainDetails.getNotes() != null) {
            this.notes = JacksonUtil.valueToTree(ruleChainDetails.getNotes());
        }
    }

    @Override
    public RuleChainDetails toData() {
        RuleChain ruleChain = super.toRuleChain();
        RuleChainDetails details = new RuleChainDetails(ruleChain);
        if (notes != null && notes.isArray()) {
            details.setNotes(JacksonUtil.treeToValue(notes, new TypeReference<>() {}));
        }
        return details;
    }

}
