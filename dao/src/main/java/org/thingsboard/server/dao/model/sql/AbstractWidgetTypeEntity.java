// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.BaseWidgetType;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractWidgetTypeEntity<T extends BaseWidgetType> extends BaseVersionedEntity<T> {

    @Column(name = ModelConstants.WIDGET_TYPE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.WIDGET_TYPE_FQN_PROPERTY)
    private String fqn;

    @Column(name = ModelConstants.WIDGET_TYPE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.WIDGET_TYPE_DEPRECATED_PROPERTY)
    private boolean deprecated;

    @Column(name = ModelConstants.WIDGET_TYPE_SCADA_PROPERTY)
    private boolean scada;

    public AbstractWidgetTypeEntity() {
        super();
    }

    public AbstractWidgetTypeEntity(T widgetType) {
        super(widgetType);
        if (widgetType.getTenantId() != null) {
            this.tenantId = widgetType.getTenantId().getId();
        }
        this.fqn = widgetType.getFqn();
        this.name = widgetType.getName();
        this.deprecated = widgetType.isDeprecated();
        this.scada = widgetType.isScada();
    }

    public AbstractWidgetTypeEntity(AbstractWidgetTypeEntity widgetTypeEntity) {
        super(widgetTypeEntity);
        this.tenantId = widgetTypeEntity.getTenantId();
        this.fqn = widgetTypeEntity.getFqn();
        this.name = widgetTypeEntity.getName();
        this.deprecated = widgetTypeEntity.isDeprecated();
        this.scada = widgetTypeEntity.isScada();
    }

    protected BaseWidgetType toBaseWidgetType() {
        BaseWidgetType widgetType = new BaseWidgetType(new WidgetTypeId(getUuid()));
        widgetType.setCreatedTime(createdTime);
        widgetType.setVersion(version);
        if (tenantId != null) {
            widgetType.setTenantId(TenantId.fromUUID(tenantId));
        }
        widgetType.setFqn(fqn);
        widgetType.setName(name);
        widgetType.setDeprecated(deprecated);
        widgetType.setScada(scada);
        return widgetType;
    }

}
