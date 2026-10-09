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
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_INTEGRATION_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_IS_EDGE_TEMPLATE_MODE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_VERSION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EXTERNAL_ID_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = CONVERTER_TABLE_NAME)
public final class ConverterEntity extends BaseVersionedEntity<Converter> {

    @Column(name = CONVERTER_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = CONVERTER_NAME_PROPERTY)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = CONVERTER_TYPE_PROPERTY)
    private ConverterType type;

    @Enumerated(EnumType.STRING)
    @Column(name = CONVERTER_INTEGRATION_TYPE_PROPERTY)
    private IntegrationType integrationType;

    @Column(name = ModelConstants.DEBUG_SETTINGS)
    private String debugSettings;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.CONVERTER_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.CONVERTER_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Column(name = EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    @Column(name = CONVERTER_IS_EDGE_TEMPLATE_MODE_PROPERTY)
    private boolean edgeTemplate;

    @Column(name = CONVERTER_VERSION_PROPERTY)
    private Integer converterVersion;

    public ConverterEntity() {
        super();
    }

    public ConverterEntity(Converter converter) {
        super(converter);
        if (converter.getTenantId() != null) {
            this.tenantId = converter.getTenantId().getId();
        }
        this.name = converter.getName();
        this.type = converter.getType();
        this.integrationType = converter.getIntegrationType();
        this.debugSettings = JacksonUtil.toString(converter.getDebugSettings());
        this.configuration = converter.getConfiguration();
        this.additionalInfo = converter.getAdditionalInfo();
        if (converter.getExternalId() != null) {
            this.externalId = converter.getExternalId().getId();
        }
        this.edgeTemplate = converter.isEdgeTemplate();
        this.converterVersion = converter.getConverterVersion();
    }

    @Override
    public Converter toData() {
        Converter converter = new Converter(new ConverterId(id));
        converter.setCreatedTime(createdTime);
        converter.setVersion(version);
        if (tenantId != null) {
            converter.setTenantId(TenantId.fromUUID(tenantId));
        }
        converter.setName(name);
        converter.setType(type);
        converter.setIntegrationType(integrationType);
        converter.setDebugSettings(JacksonUtil.fromString(debugSettings, DebugSettings.class));
        converter.setConfiguration(configuration);
        converter.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            converter.setExternalId(new ConverterId(externalId));
        }
        converter.setEdgeTemplate(edgeTemplate);
        converter.setConverterVersion(converterVersion);
        return converter;
    }

}
