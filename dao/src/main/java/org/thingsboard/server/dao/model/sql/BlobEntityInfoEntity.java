// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;

import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = BLOB_ENTITY_TABLE_NAME)
public final class BlobEntityInfoEntity extends AbstractBlobEntityInfoEntity<BlobEntityInfo> {

    public BlobEntityInfoEntity() {
        super();
    }

    public BlobEntityInfoEntity(BlobEntityInfo blobEntityInfo) {
        super(blobEntityInfo);
    }

    @Override
    public BlobEntityInfo toData() {
        return super.toBlobEntityInfo();
    }

}
