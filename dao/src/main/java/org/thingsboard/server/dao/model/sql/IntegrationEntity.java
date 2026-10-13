// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EXTERNAL_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_ALLOW_CREATE_DEVICES_OR_ASSETS;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_CONVERTER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_DOWNLINK_CONVERTER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_ENABLED_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_IS_REMOTE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_ROUTING_KEY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_SECRET_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_TYPE_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = INTEGRATION_TABLE_NAME)
public class IntegrationEntity extends BaseVersionedEntity<Integration> {

    @Column(name = INTEGRATION_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = INTEGRATION_NAME_PROPERTY)
    private String name;

    @Column(name = INTEGRATION_SECRET_PROPERTY)
    private String secret;

    @Column(name = INTEGRATION_CONVERTER_ID_PROPERTY)
    private UUID converterId;

    @Column(name = INTEGRATION_DOWNLINK_CONVERTER_ID_PROPERTY)
    private UUID downlinkConverterId;

    @Column(name = INTEGRATION_ROUTING_KEY_PROPERTY)
    private String routingKey;

    @Enumerated(EnumType.STRING)
    @Column(name = INTEGRATION_TYPE_PROPERTY)
    private IntegrationType type;

    @Column(name = ModelConstants.DEBUG_SETTINGS)
    private String debugSettings;

    @Column(name = INTEGRATION_ENABLED_PROPERTY)
    private Boolean enabled;

    @Column(name = INTEGRATION_IS_REMOTE_PROPERTY)
    private Boolean isRemote;

    @Column(name = INTEGRATION_ALLOW_CREATE_DEVICES_OR_ASSETS)
    private Boolean allowCreateDevicesOrAssets;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.INTEGRATION_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.INTEGRATION_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Column(name = EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    @Column(name = ModelConstants.INTEGRATION_IS_EDGE_TEMPLATE_MODE_PROPERTY)
    private boolean edgeTemplate;

    public IntegrationEntity() {
        super();
    }

    public IntegrationEntity(Integration integration) {
        super(integration);
        if (integration.getTenantId() != null) {
            this.tenantId = integration.getTenantId().getId();
        }
        if (integration.getDefaultConverterId() != null) {
            this.converterId = integration.getDefaultConverterId().getId();
        }
        if (integration.getDownlinkConverterId() != null) {
            this.downlinkConverterId = integration.getDownlinkConverterId().getId();
        }
        this.name = integration.getName();
        this.routingKey = integration.getRoutingKey();
        this.secret = integration.getSecret();
        this.type = integration.getType();
        this.debugSettings = JacksonUtil.toString(integration.getDebugSettings());
        this.enabled = integration.isEnabled();
        this.isRemote = integration.isRemote();
        this.allowCreateDevicesOrAssets = integration.isAllowCreateDevicesOrAssets();
        this.configuration = integration.getConfiguration();
        this.additionalInfo = integration.getAdditionalInfo();
        if (integration.getExternalId() != null) {
            this.externalId = integration.getExternalId().getId();
        }
        this.edgeTemplate = integration.isEdgeTemplate();
    }

    @Override
    public Integration toData() {
        Integration integration = new Integration(new IntegrationId(id));
        integration.setCreatedTime(this.createdTime);
        integration.setVersion(version);
        if (tenantId != null) {
            integration.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (converterId != null) {
            integration.setDefaultConverterId(new ConverterId(converterId));
        }
        if (downlinkConverterId != null) {
            integration.setDownlinkConverterId(new ConverterId(downlinkConverterId));
        }
        integration.setName(name);
        integration.setRoutingKey(routingKey);
        integration.setSecret(secret);
        integration.setType(type);
        integration.setDebugSettings(JacksonUtil.fromString(debugSettings, DebugSettings.class));
        integration.setEnabled(enabled);
        integration.setRemote(isRemote);
        integration.setAllowCreateDevicesOrAssets(allowCreateDevicesOrAssets);
        integration.setConfiguration(configuration);
        integration.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            integration.setExternalId(new IntegrationId(externalId));
        }
        integration.setEdgeTemplate(edgeTemplate);
        return integration;
    }

}
