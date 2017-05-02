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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Data
@Entity
@Table(name = ModelConstants.RULE_COLUMN_FAMILY_NAME)
public class RuleMetaDataEntity implements SearchTextEntity<RuleMetaData> {

    @Transient
    private static final long serialVersionUID = -1506905644259463884L;
    @Id
    @Column(name = ModelConstants.ID_PROPERTY, columnDefinition = "BINARY(16)")
    private UUID id;
    @Column(name = ModelConstants.RULE_TENANT_ID_PROPERTY, columnDefinition = "BINARY(16)")
    private UUID tenantId;
    @Column(name = ModelConstants.RULE_NAME_PROPERTY)
    private String name;
    @Column(name = ModelConstants.RULE_STATE_PROPERTY)
    private ComponentLifecycleState state;
    @Column(name = ModelConstants.RULE_WEIGHT_PROPERTY)
    private int weight;
    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;
    @Column(name = ModelConstants.RULE_PLUGIN_TOKEN_PROPERTY)
    private String pluginToken;
    @Column(name = ModelConstants.RULE_FILTERS)
    private String filters;
    @Column(name = ModelConstants.RULE_PROCESSOR)
    private String processor;
    @Column(name = ModelConstants.RULE_ACTION)
    private String action;
    @Column(name = ModelConstants.ADDITIONAL_INFO_PROPERTY)
    private String additionalInfo;

    public RuleMetaDataEntity() {
    }

    public RuleMetaDataEntity(RuleMetaData rule) {
        if (rule.getId() != null) {
            this.id = rule.getUuidId();
        }
        this.tenantId = DaoUtil.getId(rule.getTenantId());
        this.name = rule.getName();
        this.pluginToken = rule.getPluginToken();
        this.state = rule.getState();
        this.weight = rule.getWeight();
        this.searchText = rule.getName();
        if (rule.getFilters() != null) {
            this.filters = rule.getFilters().toString();
        }
        if (rule.getProcessor() != null) {
            this.processor = rule.getProcessor().toString();
        }
        if (rule.getAction() != null) {
            this.action = rule.getAction().toString();
        }
        if (rule.getAdditionalInfo() != null) {
            this.additionalInfo = rule.getAdditionalInfo().toString();
        }
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
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public RuleMetaData toData() {
        RuleMetaData rule = new RuleMetaData(new RuleId(id));
        rule.setTenantId(new TenantId(tenantId));
        rule.setName(name);
        rule.setState(state);
        rule.setWeight(weight);
        rule.setCreatedTime(UUIDs.unixTimestamp(id));
        rule.setPluginToken(pluginToken);
        ObjectMapper mapper = new ObjectMapper();
        if (filters != null) {
            try {
                JsonNode jsonNode = mapper.readTree(filters);
                rule.setFilters(jsonNode);
            } catch (IOException e) {
                log.warn(String.format("Error parsing JsonNode: %s. Reason: %s ", filters, e.getMessage()), e);
            }
        }
        if (processor != null) {
            try {
                JsonNode jsonNode = mapper.readTree(processor);
                rule.setProcessor(jsonNode);
            } catch (IOException e) {
                log.warn(String.format("Error parsing JsonNode: %s. Reason: %s ", processor, e.getMessage()), e);
            }
        }
        if (action != null) {
            try {
                JsonNode jsonNode = mapper.readTree(action);
                rule.setAction(jsonNode);
            } catch (IOException e) {
                log.warn(String.format("Error parsing JsonNode: %s. Reason: %s ", action, e.getMessage()), e);
            }
        }
        if (additionalInfo != null) {
            try {
                JsonNode jsonNode = mapper.readTree(additionalInfo);
                rule.setAdditionalInfo(jsonNode);
            } catch (IOException e) {
                log.warn(String.format("Error parsing JsonNode: %s. Reason: %s ", additionalInfo, e.getMessage()), e);
            }
        }
        return rule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleMetaDataEntity that = (RuleMetaDataEntity) o;
        return weight == that.weight &&
                Objects.equals(id, that.id) &&
                Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(name, that.name) &&
                Objects.equals(pluginToken, that.pluginToken) &&
                Objects.equals(state, that.state) &&
                Objects.equals(searchText, that.searchText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tenantId, name, pluginToken, state, weight, searchText);
    }

    @Override
    public String toString() {
        return "RuleMetaDataEntity{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", name='" + name + '\'' +
                ", pluginToken='" + pluginToken + '\'' +
                ", state='" + state + '\'' +
                ", weight=" + weight +
                ", searchText='" + searchText + '\'' +
                '}';
    }
}
