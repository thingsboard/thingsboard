// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.converter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasDebugSettings;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Converter extends BaseData<ConverterId> implements HasName, TenantEntity, ExportableEntity<ConverterId>, HasVersion, HasDebugSettings {

    private static final long serialVersionUID = -1541581333235769915L;

    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "name")
    private String name;
    private ConverterType type;
    @Setter
    private IntegrationType integrationType;
    @Deprecated
    private boolean debugMode;
    private DebugSettings debugSettings;
    private JsonNode configuration;
    @NoXss
    private JsonNode additionalInfo;
    private boolean edgeTemplate;

    @Getter
    @Setter
    private Integer converterVersion;

    @Getter
    @Setter
    private ConverterId externalId;
    @Getter
    @Setter
    private Long version;

    public Converter() {
        super();
    }

    public Converter(ConverterId id) {
        super(id);
    }

    public Converter(Converter converter) {
        super(converter);
        this.tenantId = converter.getTenantId();
        this.name = converter.getName();
        this.type = converter.getType();
        this.integrationType = converter.getIntegrationType();
        this.debugSettings = converter.getDebugSettings();
        this.configuration = converter.getConfiguration();
        this.additionalInfo = converter.getAdditionalInfo();
        this.edgeTemplate = converter.isEdgeTemplate();
        this.externalId = converter.getExternalId();
        this.version = converter.getVersion();
    }

    @Schema(description = "JSON object with the Converter Id. " +
            "Specify this field to update the Converter. " +
            "Referencing non-existing Converter Id will cause error. " +
            "Omit this field to create new Converter.")
    @Override
    public ConverterId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the converter creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Unique Converter Name in scope of Tenant", example = "Http Converter")
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The type of the converter to process incoming or outgoing messages")
    public ConverterType getType() {
        return type;
    }

    public void setType(ConverterType type) {
        this.type = type;
    }

    @Schema(description = "The type of the integration to which the converter is dedicated")
    public IntegrationType getIntegrationType() {
        return integrationType;
    }

    @Schema(description = "Enable/disable debug. ", example = "false", deprecated = true)
    @Override
    public boolean isDebugMode() {
        return debugMode;
    }

    @Deprecated
    @Override
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    @Schema(description = "Debug settings object.")
    @Override
    public DebugSettings getDebugSettings() {
        return debugSettings;
    }

    @Override
    public void setDebugSettings(DebugSettings debugSettings) {
        this.debugSettings = debugSettings;
    }

    @Schema(description = "JSON object representing converter configuration. It should contain one of two possible fields: 'decoder' or 'encoder'. " +
            "The former is used when the converter has UPLINK type, the latter is used - when DOWNLINK type. " +
            "It can contain both 'decoder' and 'encoder' fields, when the correct one is specified for the appropriate converter type, another one can be set to 'null'")
    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    @Schema(description = "Additional parameters of the converter", implementation = com.fasterxml.jackson.databind.JsonNode.class)
    public JsonNode getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Schema(description = "Boolean flag that specifies that is regular or edge template converter")
    public boolean isEdgeTemplate() {
        return edgeTemplate;
    }

    public void setEdgeTemplate(boolean edgeTemplate) {
        this.edgeTemplate = edgeTemplate;
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.CONVERTER;
    }

    @JsonIgnore
    public boolean isDedicated() {
        return integrationType != null && converterVersion != null && converterVersion == 2;
    }

}
