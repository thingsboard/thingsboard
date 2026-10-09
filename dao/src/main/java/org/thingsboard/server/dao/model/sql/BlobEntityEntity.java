// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_ADDITIONAL_INFO_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_CONTENT_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_DATA_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_TYPE_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = BLOB_ENTITY_TABLE_NAME)
public final class BlobEntityEntity extends BaseSqlEntity<BlobEntity> implements BaseEntity<BlobEntity> {

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

    @Column(name = BLOB_ENTITY_DATA_PROPERTY)
    private String data;

    @Convert(converter = JsonConverter.class)
    @Column(name = BLOB_ENTITY_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public BlobEntityEntity() {
        super();
    }

    public BlobEntityEntity(BlobEntity blobEntity) {
        this.createdTime = blobEntity.getCreatedTime();
        if (blobEntity.getId() != null) {
            this.setUuid(blobEntity.getId().getId());
        }
        if (blobEntity.getTenantId() != null) {
            this.tenantId = blobEntity.getTenantId().getId();
        }
        if (blobEntity.getCustomerId() != null) {
            this.customerId = blobEntity.getCustomerId().getId();
        }
        this.name = blobEntity.getName();
        this.type = blobEntity.getType();
        this.contentType = blobEntity.getContentType();
        this.additionalInfo = blobEntity.getAdditionalInfo();
        this.data = Base64.getEncoder().encodeToString(blobEntity.getData().array());
    }

    @Override
    public BlobEntity toData() {
        BlobEntity blobEntity = new BlobEntity(new BlobEntityId(id));
        blobEntity.setCreatedTime(createdTime);
        if (tenantId != null) {
            blobEntity.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            blobEntity.setCustomerId(new CustomerId(customerId));
        }
        blobEntity.setName(name);
        blobEntity.setType(type);
        blobEntity.setContentType(contentType);
        blobEntity.setAdditionalInfo(additionalInfo);
        blobEntity.setData(ByteBuffer.wrap(Base64.getDecoder().decode(data)));
        return blobEntity;
    }

}
