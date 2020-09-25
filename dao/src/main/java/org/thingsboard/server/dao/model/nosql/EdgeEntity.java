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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EDGE_ADDITIONAL_INFO_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_CLOUD_ENDPOINT_KEY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_CONFIGURATION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_LABEL_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_LICENSE_KEY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_ROOT_RULE_CHAIN_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_ROUTING_KEY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_SECRET_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

@Data
@Table(name = EDGE_COLUMN_FAMILY_NAME)
public class EdgeEntity implements SearchTextEntity<Edge> {

    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;

    @PartitionKey(value = 1)
    @Column(name = EDGE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @PartitionKey(value = 2)
    @Column(name = EDGE_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = EDGE_ROOT_RULE_CHAIN_ID_PROPERTY)
    private UUID rootRuleChainId;

    @PartitionKey(value = 3)
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

    @Column(name = EDGE_LICENSE_KEY_PROPERTY)
    private String edgeLicenseKey;

    @Column(name = EDGE_CLOUD_ENDPOINT_KEY_PROPERTY)
    private String cloudEndpoint;

    @Column(name = EDGE_SECRET_PROPERTY)
    private String secret;

    @Column(name = EDGE_CONFIGURATION_PROPERTY, codec = JsonCodec.class)
    private JsonNode configuration;

    @Column(name = EDGE_ADDITIONAL_INFO_PROPERTY, codec = JsonCodec.class)
    private JsonNode additionalInfo;

    public EdgeEntity() {
        super();
    }

    @Override
    public UUID getUuid() {
        return getId();
    }

    @Override
    public void setUuid(UUID id) {
        this.id = id;
    }

    public EdgeEntity(Edge edge) {
        if (edge.getId() != null) {
            this.id = edge.getId().getId();
        }
        if (edge.getTenantId() != null) {
            this.tenantId = edge.getTenantId().getId();
        }
        if (edge.getCustomerId() != null) {
            this.customerId = edge.getCustomerId().getId();
        }
        if (edge.getRootRuleChainId() != null) {
            this.rootRuleChainId = edge.getRootRuleChainId().getId();
        }
        this.type = edge.getType();
        this.name = edge.getName();
        this.label = edge.getLabel();
        this.routingKey = edge.getRoutingKey();
        this.secret = edge.getSecret();
        this.edgeLicenseKey = edge.getEdgeLicenseKey();
        this.cloudEndpoint = edge.getCloudEndpoint();
        this.configuration = edge.getConfiguration();
        this.additionalInfo = edge.getAdditionalInfo();
    }

    @Override
    public String getSearchTextSource() {
        return getName();
    }

    @Override
    public Edge toData() {
        Edge edge = new Edge(new EdgeId(id));
        edge.setCreatedTime(UUIDs.unixTimestamp(id));
        if (tenantId != null) {
            edge.setTenantId(new TenantId(tenantId));
        }
        if (customerId != null) {
            edge.setCustomerId(new CustomerId(customerId));
        }
        if (rootRuleChainId != null) {
            edge.setRootRuleChainId(new RuleChainId(rootRuleChainId));
        }
        edge.setType(type);
        edge.setName(name);
        edge.setLabel(label);
        edge.setRoutingKey(routingKey);
        edge.setSecret(secret);
        edge.setEdgeLicenseKey(edgeLicenseKey);
        edge.setCloudEndpoint(cloudEndpoint);
        edge.setConfiguration(configuration);
        edge.setAdditionalInfo(additionalInfo);
        return edge;
    }
}
