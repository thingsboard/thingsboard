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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractEdgeEntity<T extends Edge> extends BaseSqlEntity<T> implements SearchTextEntity<T> {

    @Column(name = EDGE_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = EDGE_CUSTOMER_ID_PROPERTY)
    private String customerId;

    @Column(name = EDGE_ROOT_RULE_CHAIN_ID_PROPERTY)
    private String rootRuleChainId;

    @Column(name = EDGE_TYPE_PROPERTY)
    private String type;

    @Column(name = EDGE_NAME_PROPERTY)
    private String name;

    @Column(name = EDGE_LABEL_PROPERTY)
    private String label;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = EDGE_ROUTING_KEY_PROPERTY)
    private String routingKey;

    @Column(name = EDGE_SECRET_PROPERTY)
    private String secret;

    @Type(type = "json")
    @Column(name = ModelConstants.EDGE_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    @Type(type = "json")
    @Column(name = ModelConstants.EDGE_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public AbstractEdgeEntity() {
        super();
    }

    public AbstractEdgeEntity(Edge edge) {
        if (edge.getId() != null) {
            this.setId(edge.getId().getId());
        }
        if (edge.getTenantId() != null) {
            this.tenantId = UUIDConverter.fromTimeUUID(edge.getTenantId().getId());
        }
        if (edge.getCustomerId() != null) {
            this.customerId = UUIDConverter.fromTimeUUID(edge.getCustomerId().getId());
        }
        if (edge.getRootRuleChainId() != null) {
            this.rootRuleChainId = UUIDConverter.fromTimeUUID(edge.getRootRuleChainId().getId());
        }
        this.type = edge.getType();
        this.name = edge.getName();
        this.label = edge.getLabel();
        this.routingKey = edge.getRoutingKey();
        this.secret = edge.getSecret();
        this.configuration = edge.getConfiguration();
        this.additionalInfo = edge.getAdditionalInfo();
    }

    public AbstractEdgeEntity(EdgeEntity edgeEntity) {
        this.setId(edgeEntity.getId());
        this.tenantId = edgeEntity.getTenantId();
        this.customerId = edgeEntity.getCustomerId();
        this.rootRuleChainId = edgeEntity.getRootRuleChainId();
        this.type = edgeEntity.getType();
        this.name = edgeEntity.getName();
        this.label = edgeEntity.getLabel();
        this.searchText = edgeEntity.getSearchText();
        this.routingKey = edgeEntity.getRoutingKey();
        this.secret = edgeEntity.getSecret();
        this.configuration = edgeEntity.getConfiguration();
        this.additionalInfo = edgeEntity.getAdditionalInfo();
    }

    public String getSearchText() {
        return searchText;
    }

    @Override
    public String getSearchTextSource() {
        return name;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    protected Edge toEdge() {
        Edge edge = new Edge(new EdgeId(UUIDConverter.fromString(id)));
        edge.setCreatedTime(UUIDs.unixTimestamp(UUIDConverter.fromString(id)));
        if (tenantId != null) {
            edge.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
        }
        if (customerId != null) {
            edge.setCustomerId(new CustomerId(UUIDConverter.fromString(customerId)));
        }
        if (rootRuleChainId != null) {
            edge.setRootRuleChainId(new RuleChainId(UUIDConverter.fromString(rootRuleChainId)));
        }
        edge.setType(type);
        edge.setName(name);
        edge.setLabel(label);
        edge.setRoutingKey(routingKey);
        edge.setSecret(secret);
        edge.setConfiguration(configuration);
        edge.setAdditionalInfo(additionalInfo);
        return edge;
    }
}