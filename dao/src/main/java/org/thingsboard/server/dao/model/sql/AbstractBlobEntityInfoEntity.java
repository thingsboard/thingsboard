// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_ADDITIONAL_INFO_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_CONTENT_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_TYPE_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractBlobEntityInfoEntity<T extends BlobEntityInfo> extends BaseSqlEntity<T> implements BaseEntity<T> {

    @Column(name = BLOB_ENTITY_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = BLOB_ENTITY_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = BLOB_ENTITY_NAME_PROPERTY)
    private String name;

    @Column(name = BLOB_ENTITY_TYPE_PROPERTY)
    private String type;

    @Column(name = BLOB_ENTITY_CONTENT_TYPE_PROPERTY)
    private String contentType;

    @Convert(converter = JsonConverter.class)
    @Column(name = BLOB_ENTITY_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public AbstractBlobEntityInfoEntity() {
        super();
    }

    public AbstractBlobEntityInfoEntity(BlobEntityInfo blobEntityInfo) {
        this.createdTime = blobEntityInfo.getCreatedTime();
        if (blobEntityInfo.getId() != null) {
            this.setUuid(blobEntityInfo.getId().getId());
        }
        if (blobEntityInfo.getTenantId() != null) {
            this.tenantId = blobEntityInfo.getTenantId().getId();
        }
        if (blobEntityInfo.getCustomerId() != null) {
            this.customerId = blobEntityInfo.getCustomerId().getId();
        }
        this.name = blobEntityInfo.getName();
        this.type = blobEntityInfo.getType();
        this.contentType = blobEntityInfo.getContentType();
        this.additionalInfo = blobEntityInfo.getAdditionalInfo();
    }

    public AbstractBlobEntityInfoEntity(BlobEntityInfoEntity blobEntityInfoEntity) {
        this.setId(blobEntityInfoEntity.getId());
        this.setCreatedTime(blobEntityInfoEntity.getCreatedTime());
        this.tenantId = blobEntityInfoEntity.getTenantId();
        this.customerId = blobEntityInfoEntity.getCustomerId();
        this.type = blobEntityInfoEntity.getType();
        this.name = blobEntityInfoEntity.getName();
        this.contentType = blobEntityInfoEntity.getContentType();
        this.additionalInfo = blobEntityInfoEntity.getAdditionalInfo();
    }

    protected BlobEntityInfo toBlobEntityInfo() {
        BlobEntityInfo blobEntityInfo = new BlobEntityInfo(new BlobEntityId(id));
        blobEntityInfo.setCreatedTime(createdTime);
        if (tenantId != null) {
            blobEntityInfo.setTenantId(new TenantId(tenantId));
        }
        if (customerId != null) {
            blobEntityInfo.setCustomerId(new CustomerId(customerId));
        }
        blobEntityInfo.setName(name);
        blobEntityInfo.setType(type);
        blobEntityInfo.setContentType(contentType);
        blobEntityInfo.setAdditionalInfo(additionalInfo);
        return blobEntityInfo;
    }
}
