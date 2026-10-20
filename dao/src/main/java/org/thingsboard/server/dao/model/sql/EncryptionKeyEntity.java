// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.encryptionkey.EncryptionKey;
import org.thingsboard.server.common.data.id.EncryptionKeyId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ENCRYPTION_KEY_PASSWORD_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ENCRYPTION_KEY_SALT_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ENCRYPTION_KEY_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ENCRYPTION_KEY_TABLE_NAME)
public class EncryptionKeyEntity extends BaseSqlEntity<EncryptionKey> {

    @Column(name = TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = ENCRYPTION_KEY_PASSWORD_COLUMN)
    private String password;

    @Column(name = ENCRYPTION_KEY_SALT_COLUMN)
    private String salt;

    public EncryptionKeyEntity() {
        super();
    }

    public EncryptionKeyEntity(EncryptionKey encryptionKey) {
        super(encryptionKey);
        this.tenantId = encryptionKey.getTenantId().getId();
        this.password = encryptionKey.getPassword();
        this.salt = encryptionKey.getSalt();
    }

    @Override
    public EncryptionKey toData() {
        EncryptionKey encryptionKey = new EncryptionKey(new EncryptionKeyId(id));
        encryptionKey.setCreatedTime(createdTime);
        encryptionKey.setTenantId(TenantId.fromUUID(tenantId));
        encryptionKey.setPassword(password);
        encryptionKey.setSalt(salt);
        return encryptionKey;
    }

}
