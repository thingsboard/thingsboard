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
package org.thingsboard.server.common.data.edge;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasLabel;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class Edge extends BaseDataWithAdditionalInfo<EdgeId> implements HasLabel, HasTenantId, HasCustomerId, HasVersion {

    private static final long serialVersionUID = 4934987555236873728L;

    private TenantId tenantId;
    private CustomerId customerId;
    private RuleChainId rootRuleChainId;
    @NoXss
    @Length(fieldName = "name")
    private String name;
    @NoXss
    @Length(fieldName = "type")
    private String type;
    @NoXss
    @Length(fieldName = "label")
    private String label;
    @NoXss
    @Length(fieldName = "routingKey")
    private String routingKey;
    @NoXss
    @Length(fieldName = "secret")
    private String secret;

    @Getter
    private Long version;

    public Edge() {
        super();
    }

    public Edge(EdgeId id) {
        super(id);
    }

    public Edge(Edge edge) {
        super(edge);
        this.tenantId = edge.getTenantId();
        this.customerId = edge.getCustomerId();
        this.rootRuleChainId = edge.getRootRuleChainId();
        this.name = edge.getName();
        this.type = edge.getType();
        this.label = edge.getLabel();
        this.routingKey = edge.getRoutingKey();
        this.secret = edge.getSecret();
        this.version = edge.getVersion();
    }

    public void update(Edge edge) {
        this.tenantId = edge.getTenantId();
        this.customerId = edge.getCustomerId();
        this.rootRuleChainId = edge.getRootRuleChainId();
        this.name = edge.getName();
        this.type = edge.getType();
        this.label = edge.getLabel();
        this.routingKey = edge.getRoutingKey();
        this.secret = edge.getSecret();
        this.version = edge.getVersion();
    }

    @Schema(description = "JSON object with the Edge Id. " +
            "Specify this field to update the Edge. " +
            "Referencing non-existing Edge Id will cause error. " +
            "Omit this field to create new Edge." )
    @Override
    public EdgeId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the edge creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "JSON object with Tenant Id. Use 'assignDeviceToTenant' to change the Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return this.tenantId;
    }

    @Schema(description = "JSON object with Customer Id. Use 'assignEdgeToCustomer' to change the Customer Id.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public CustomerId getCustomerId() {
        return this.customerId;
    }

    @Schema(description = "JSON object with Root Rule Chain Id. Use 'setEdgeRootRuleChain' to change the Root Rule Chain Id.", accessMode = Schema.AccessMode.READ_ONLY)
    public RuleChainId getRootRuleChainId() {
        return this.rootRuleChainId;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Unique Edge Name in scope of Tenant", example = "Silo_A_Edge")
    @Override
    public String getName() {
        return this.name;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Edge type", example = "Silos")
    public String getType() {
        return this.type;
    }

    @Schema(description = "Label that may be used in widgets", example = "Silo Edge on far field")
    public String getLabel() {
        return this.label;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Edge routing key ('username') to authorize on cloud")
    public String getRoutingKey() {
        return this.routingKey;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Edge secret ('password') to authorize on cloud")
    public String getSecret() {
        return this.secret;
    }

}
