// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CalculatedFieldLinkId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_LINK_CALCULATED_FIELD_ID;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_LINK_ENTITY_ID;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_LINK_ENTITY_TYPE;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_LINK_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_LINK_TENANT_ID_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = CALCULATED_FIELD_LINK_TABLE_NAME)
public class CalculatedFieldLinkEntity extends BaseSqlEntity<CalculatedFieldLink> implements BaseEntity<CalculatedFieldLink> {

    @Column(name = CALCULATED_FIELD_LINK_TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = CALCULATED_FIELD_LINK_ENTITY_TYPE)
    private String entityType;

    @Column(name = CALCULATED_FIELD_LINK_ENTITY_ID)
    private UUID entityId;

    @Column(name = CALCULATED_FIELD_LINK_CALCULATED_FIELD_ID)
    private UUID calculatedFieldId;

    public CalculatedFieldLinkEntity() {
        super();
    }

    public CalculatedFieldLinkEntity(CalculatedFieldLink calculatedFieldLink) {
        super(calculatedFieldLink);
        this.tenantId = calculatedFieldLink.getTenantId().getId();
        this.entityType = calculatedFieldLink.getEntityId().getEntityType().name();
        this.entityId = calculatedFieldLink.getEntityId().getId();
        this.calculatedFieldId = calculatedFieldLink.getCalculatedFieldId().getId();
    }

    @Override
    public CalculatedFieldLink toData() {
        CalculatedFieldLink calculatedFieldLink = new CalculatedFieldLink(new CalculatedFieldLinkId(id));
        calculatedFieldLink.setCreatedTime(createdTime);
        calculatedFieldLink.setTenantId(TenantId.fromUUID(tenantId));
        calculatedFieldLink.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        calculatedFieldLink.setCalculatedFieldId(new CalculatedFieldId(calculatedFieldId));
        return calculatedFieldLink;
    }

}
