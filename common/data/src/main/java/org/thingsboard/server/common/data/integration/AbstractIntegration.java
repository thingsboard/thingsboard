// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.integration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasDebugSettings;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@ToString(callSuper = true)
public abstract class AbstractIntegration extends BaseData<IntegrationId> implements HasName, TenantEntity, HasVersion, HasDebugSettings {

    private static final long serialVersionUID = 1934983577296873728L;

    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "name")
    @TemplateField(key = "integrationName", label = "Integration name", type = FormFieldType.STRING, required = true)
    private String name;
    private IntegrationType type;
    @Deprecated
    private boolean debugMode;
    private DebugSettings debugSettings;
    private Boolean enabled;
    @JsonProperty("remote")
    private Boolean isRemote;
    private Boolean allowCreateDevicesOrAssets;
    @JsonProperty("edgeTemplate")
    private boolean isEdgeTemplate;

    @Getter @Setter
    private Long version;

    public AbstractIntegration() {
        super();
    }

    public AbstractIntegration(IntegrationId id) {
        super(id);
    }

    public AbstractIntegration(AbstractIntegration integration) {
        super(integration);
        this.tenantId = integration.getTenantId();
        this.name = integration.getName();
        this.type = integration.getType();
        this.debugSettings = integration.getDebugSettings();
        this.enabled = integration.isEnabled();
        this.isRemote = integration.isRemote();
        this.allowCreateDevicesOrAssets = integration.isAllowCreateDevicesOrAssets();
        this.isEdgeTemplate = integration.isEdgeTemplate();
        this.version = integration.getVersion();
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

    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The type of the integration")
    public IntegrationType getType() {
        return type;
    }

    public void setType(IntegrationType type) {
        this.type = type;
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

    @Schema(description = "Boolean flag to enable/disable the integration")
    public Boolean isEnabled() {
        return !(enabled == null) && enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Schema(description = "Boolean flag to enable/disable the integration to be executed remotely. Remote integration is launched in a separate microservice. " +
            "Local integration is executed by the platform core")
    public Boolean isRemote() {
        return !(isRemote == null) && isRemote;
    }

    public void setRemote(Boolean remote) {
        isRemote = remote;
    }

    @Schema(description = "Boolean flag to allow/disallow the integration to create devices or assets that send message and do not exist in the system yet")
    public Boolean isAllowCreateDevicesOrAssets() {
        return !(allowCreateDevicesOrAssets == null) && allowCreateDevicesOrAssets;
    }

    public void setAllowCreateDevicesOrAssets(Boolean allow) {
        allowCreateDevicesOrAssets = allow;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Integration Name", example = "Http Integration")
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Schema(description = "Boolean flag that specifies that is regular or edge template integration")
    public boolean isEdgeTemplate() {
        return isEdgeTemplate;
    }

    public void setEdgeTemplate(boolean edgeTemplate) {
        this.isEdgeTemplate = edgeTemplate;
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.INTEGRATION;
    }

}
