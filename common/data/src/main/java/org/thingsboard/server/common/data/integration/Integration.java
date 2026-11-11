// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.integration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.UUID;

@ToString(callSuper = true)
@Schema
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Integration extends AbstractIntegration implements ExportableEntity<IntegrationId> {

    private static final long serialVersionUID = 4934987577236873728L;

    private ConverterId defaultConverterId;
    private ConverterId downlinkConverterId;
    @NoXss
    @Length(fieldName = "routingKey")
    private String routingKey;

    @NoXss
    @Length(fieldName = "secret")
    private String secret;
    private JsonNode configuration;
    @NoXss
    private JsonNode additionalInfo;

    @Getter
    @Setter
    private IntegrationId externalId;

    public Integration() {
        super();
    }

    public Integration(IntegrationId id) {
        super(id);
    }

    /**
     * Partial-hydration constructor that exists SOLELY for the JPQL constructor projection in
     * {@code IntegrationRepository#findCoreIntegrations} (also reached via {@code findAllCoreIntegrations}).
     * It sets only the base fields consumed by that server-only active-integration-list path
     * ({@code createIntegrations}/{@code isMine} and the {@code IntegrationInfoProto} mapping), deliberately
     * skipping the heavy JSON columns: the resulting instance has {@code configuration},
     * {@code defaultConverterId} and {@code additionalInfo} left null. Do NOT construct instances this way for,
     * or hand the result to, consumers that expect a fully loaded Integration.
     */
    public Integration(UUID id, UUID tenantId, String name, IntegrationType type,
                       Boolean enabled, Boolean isRemote, Boolean allowCreateDevicesOrAssets) {
        super(new IntegrationId(id));
        setTenantId(TenantId.fromUUID(tenantId));
        setName(name);
        setType(type);
        setEnabled(enabled);
        setRemote(isRemote);
        setAllowCreateDevicesOrAssets(allowCreateDevicesOrAssets);
    }

    public Integration(Integration integration) {
        super(integration);
        this.defaultConverterId = integration.getDefaultConverterId();
        this.downlinkConverterId = integration.getDownlinkConverterId();
        this.routingKey = integration.getRoutingKey();
        this.secret = integration.getSecret();
        this.configuration = integration.getConfiguration() != null ? integration.getConfiguration().deepCopy() : null;
        this.additionalInfo = integration.getAdditionalInfo();
        this.externalId = integration.getExternalId();
    }

    @Schema(description = "JSON object with the Integration Id. " +
                          "Specify this field to update the Integration. " +
                          "Referencing non-existing Integration Id will cause error. " +
                          "Omit this field to create new Integration.")
    @Override
    public IntegrationId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the integration creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "JSON object with the Uplink Converter Id", requiredMode = Schema.RequiredMode.REQUIRED)
    public ConverterId getDefaultConverterId() {
        return defaultConverterId;
    }

    public void setDefaultConverterId(ConverterId defaultConverterId) {
        this.defaultConverterId = defaultConverterId;
    }

    @Schema(description = "JSON object with the Downlink Converter Id")
    public ConverterId getDownlinkConverterId() {
        return downlinkConverterId;
    }

    public void setDownlinkConverterId(ConverterId downlinkConverterId) {
        this.downlinkConverterId = downlinkConverterId;
    }

    @Schema(description = "String value used by HTTP based integrations for the base URL construction and by the remote integrations. " +
                          "Remote integration uses this value along with the 'secret' for kind of security and validation to be able to connect to the platform using Grpc",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "ca1a01b6-4ca1-3da5-54e4-a07090b65644")
    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    @Schema(description = "String value used by the remote integrations. " +
                          "Remote integration uses this value along with the 'routingKey' for kind of security and validation to be able to connect to the platform using Grpc", example = "nl83m1ktpwpwwmww29sm")
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Schema(description = "JSON object representing integration configuration. Each integration type has specific configuration with the connectivity parameters " +
                          "(like 'host' and 'port' for MQTT type or 'baseUrl' for HTTP based type, etc.) " +
                          "and other important parameters dependent on the integration type", requiredMode = Schema.RequiredMode.REQUIRED)
    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    @Schema(description = "Additional parameters of the integration", implementation = com.fasterxml.jackson.databind.JsonNode.class)
    public JsonNode getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.INTEGRATION;
    }

    @Builder
    public Integration(TenantId tenantId, String name, IntegrationType type,
                       Boolean enabled, Boolean isRemote, Boolean allowCreateDevicesOrAssets,
                       boolean isEdgeTemplate, ConverterId defaultConverterId, ConverterId downlinkConverterId,
                       String routingKey, DebugSettings debugSettings, String secret,
                       JsonNode configuration, JsonNode additionalInfo, IntegrationId externalId, Long version) {
        super(tenantId, name, type, false, debugSettings, enabled, isRemote, allowCreateDevicesOrAssets, isEdgeTemplate, version);
        this.defaultConverterId = defaultConverterId;
        this.downlinkConverterId = downlinkConverterId;
        this.routingKey = routingKey;
        this.secret = secret;
        this.configuration = configuration;
        this.additionalInfo = additionalInfo;
        this.externalId = externalId;
    }

}
