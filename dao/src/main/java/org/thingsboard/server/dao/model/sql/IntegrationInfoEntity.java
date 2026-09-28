// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Immutable;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_ALLOW_CREATE_DEVICES_OR_ASSETS;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_ENABLED_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_IS_REMOTE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_VIEW_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Immutable
@Table(name = INTEGRATION_VIEW_NAME)
public class IntegrationInfoEntity extends BaseSqlEntity<IntegrationInfo> implements BaseEntity<IntegrationInfo> {

    @Column(name = INTEGRATION_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = INTEGRATION_NAME_PROPERTY)
    private String name;

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

    @Column(name = ModelConstants.INTEGRATION_IS_EDGE_TEMPLATE_MODE_PROPERTY)
    private Boolean edgeTemplate;

    @Column(name = ModelConstants.INTEGRATION_VIEW_STATUS_PROPERTY)
    private String status;

    public IntegrationInfoEntity() {
        super();
    }

    public IntegrationInfoEntity(UUID id, Long createdTime, UUID tenantId, String name,
                                 String type, DebugSettings debugSettings, Boolean enabled, Boolean isRemote,
                                 Boolean allowCreateDevicesOrAssets, Boolean edgeTemplate, String stats, String status) {
        this.id = id;
        this.createdTime = createdTime;
        this.tenantId = tenantId;
        this.name = name;
        this.type = IntegrationType.valueOf(type);
        this.debugSettings = JacksonUtil.toString(debugSettings);
        this.enabled = enabled;
        this.isRemote = isRemote;
        this.allowCreateDevicesOrAssets = allowCreateDevicesOrAssets;
        this.edgeTemplate = edgeTemplate;
        this.status = status;
    }

    public IntegrationInfoEntity(Integration integration) {
        this.createdTime = integration.getCreatedTime();
        if (integration.getId() != null) {
            this.setUuid(integration.getId().getId());
        }
        if (integration.getTenantId() != null) {
            this.tenantId = integration.getTenantId().getId();
        }
        this.name = integration.getName();
        this.type = integration.getType();
        this.debugSettings = JacksonUtil.toString(debugSettings);
        this.enabled = integration.isEnabled();
        this.isRemote = integration.isRemote();
        this.allowCreateDevicesOrAssets = integration.isAllowCreateDevicesOrAssets();
        this.edgeTemplate = integration.isEdgeTemplate();
    }

    @Override
    public IntegrationInfo toData() {
        IntegrationInfo integration = new IntegrationInfo(new IntegrationId(id));
        integration.setCreatedTime(this.createdTime);
        if (tenantId != null) {
            integration.setTenantId(TenantId.fromUUID(tenantId));
        }
        integration.setName(name);
        integration.setType(type);
        integration.setDebugSettings(JacksonUtil.fromString(debugSettings, DebugSettings.class));
        integration.setEnabled(enabled);
        integration.setRemote(isRemote);
        integration.setAllowCreateDevicesOrAssets(allowCreateDevicesOrAssets);
        integration.setEdgeTemplate(edgeTemplate);

        if (StringUtils.isNotEmpty(status)) {
            integration.setStatus(JacksonUtil.fromString(status, ObjectNode.class));
        }

        return integration;
    }
}
