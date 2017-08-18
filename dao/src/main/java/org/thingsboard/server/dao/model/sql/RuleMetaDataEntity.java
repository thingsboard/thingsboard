/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.RULE_COLUMN_FAMILY_NAME)
public class RuleMetaDataEntity extends BaseSqlEntity<RuleMetaData> implements SearchTextEntity<RuleMetaData> {

    @Transient
    private static final long serialVersionUID = -1506905644259463884L;

    @Column(name = ModelConstants.RULE_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = ModelConstants.RULE_NAME_PROPERTY)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.RULE_STATE_PROPERTY)
    private ComponentLifecycleState state;

    @Column(name = ModelConstants.RULE_WEIGHT_PROPERTY)
    private int weight;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = ModelConstants.RULE_PLUGIN_TOKEN_PROPERTY)
    private String pluginToken;

    @Type(type = "json")
    @Column(name = ModelConstants.RULE_FILTERS)
    private JsonNode filters;

    @Type(type = "json")
    @Column(name = ModelConstants.RULE_PROCESSOR)
    private JsonNode processor;

    @Type(type = "json")
    @Column(name = ModelConstants.RULE_ACTION)
    private JsonNode action;

    @Type(type = "json")
    @Column(name = ModelConstants.ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public RuleMetaDataEntity() {
    }

    public RuleMetaDataEntity(RuleMetaData rule) {
        if (rule.getId() != null) {
            this.setId(rule.getUuidId());
        }
        this.tenantId = toString(DaoUtil.getId(rule.getTenantId()));
        this.name = rule.getName();
        this.pluginToken = rule.getPluginToken();
        this.state = rule.getState();
        this.weight = rule.getWeight();
        this.searchText = rule.getName();
        this.filters = rule.getFilters();
        this.processor = rule.getProcessor();
        this.action = rule.getAction();
        this.additionalInfo = rule.getAdditionalInfo();
    }

    @Override
    public String getSearchTextSource() {
        return searchText;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public RuleMetaData toData() {
        RuleMetaData rule = new RuleMetaData(new RuleId(getId()));
        rule.setTenantId(new TenantId(toUUID(tenantId)));
        rule.setName(name);
        rule.setState(state);
        rule.setWeight(weight);
        rule.setCreatedTime(UUIDs.unixTimestamp(getId()));
        rule.setPluginToken(pluginToken);
        rule.setFilters(filters);
        rule.setProcessor(processor);
        rule.setAction(action);
        rule.setAdditionalInfo(additionalInfo);
        return rule;
    }
}
