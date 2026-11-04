// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.edge;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.GroupEntity;
import org.thingsboard.server.common.data.HasLabel;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class Edge extends BaseDataWithAdditionalInfo<EdgeId>
        implements HasLabel, GroupEntity<EdgeId>, HasVersion {

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
    @NoXss
    @Length(fieldName = "edgeLicenseKey", max = 10000)
    private String edgeLicenseKey;
    @NoXss
    @Length(fieldName = "cloudEndpoint")
    private String cloudEndpoint;

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
        this.edgeLicenseKey = edge.getEdgeLicenseKey();
        this.cloudEndpoint = edge.getCloudEndpoint();
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
        this.edgeLicenseKey = edge.getEdgeLicenseKey();
        this.cloudEndpoint = edge.getCloudEndpoint();
        this.version = edge.getVersion();
    }

    @Schema(description = "JSON object with the Edge Id. " +
            "Specify this field to update the Edge. " +
            "Referencing non-existing Edge Id will cause error. " +
            "Omit this field to create new Edge.")
    @Override
    public EdgeId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the edge creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "JSON object with Tenant Id. Always set to the tenant of the current user on save; cannot be changed after creation.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return this.tenantId;
    }

    @Schema(description = "JSON object with Customer Id.")
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

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Edge license key obtained from license portal", example = "AgcnI24Z06XC&m6Sxsdgf")
    public String getEdgeLicenseKey() {
        return this.edgeLicenseKey;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Edge uses this cloud URL to activate and periodically check it's license", example = "https://thingsboard.cloud")
    public String getCloudEndpoint() {
        return this.cloudEndpoint;
    }

    @JsonIgnore
    @Override
    public EntityType getEntityType() {
        return EntityType.EDGE;
    }

    @Override
    public EntityId getOwnerId() {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    @Override
    public void setOwnerId(EntityId entityId) {
        if (EntityType.CUSTOMER.equals(entityId.getEntityType())) {
            this.customerId = new CustomerId(entityId.getId());
        } else {
            this.customerId = new CustomerId(CustomerId.NULL_UUID);
        }
    }

    @Schema(description = "Additional parameters of the edge. " +
            "May include: 'description' (string).",
            implementation = com.fasterxml.jackson.databind.JsonNode.class,
            example = "{\"description\":\"Edge at location A\"}")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

}
