// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Immutable;
import org.thingsboard.server.common.data.ai.AiModelInfo;
import org.thingsboard.server.common.data.ai.provider.AiProvider;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

// Leaves the configuration column unmapped, so listing infos never loads provider credentials
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Immutable
@Table(name = ModelConstants.AI_MODEL_TABLE_NAME)
public class AiModelInfoEntity extends BaseVersionedEntity<AiModelInfo> {

    @Column(name = ModelConstants.AI_MODEL_TENANT_ID_COLUMN_NAME, columnDefinition = "UUID")
    private UUID tenantId;

    @Column(name = ModelConstants.AI_MODEL_NAME_COLUMN_NAME)
    private String name;

    @Column(name = ModelConstants.AI_MODEL_CUSTOMER_ACCESS_ENABLED_COLUMN_NAME)
    private boolean customerAccessEnabled;

    @Formula("configuration ->> 'provider'")
    private String provider;

    @Formula("configuration ->> 'modelId'")
    private String modelId;

    public AiModelInfoEntity() {}

    @Override
    public AiModelInfo toData() {
        var modelInfo = new AiModelInfo(new AiModelId(id));
        modelInfo.setCreatedTime(createdTime);
        modelInfo.setVersion(version);
        modelInfo.setTenantId(TenantId.fromUUID(tenantId));
        modelInfo.setName(name);
        modelInfo.setCustomerAccessEnabled(customerAccessEnabled);
        modelInfo.setProvider(provider != null ? AiProvider.valueOf(provider) : null);
        modelInfo.setModelId(modelId);
        return modelInfo;
    }

}
