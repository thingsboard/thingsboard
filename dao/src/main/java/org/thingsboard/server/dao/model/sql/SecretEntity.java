// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.secret.Secret;

import static org.thingsboard.server.dao.model.ModelConstants.SECRET_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.SECRET_VALUE_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = SECRET_TABLE_NAME)
public class SecretEntity extends AbstractSecretInfoEntity<Secret> {

    @Column(name = SECRET_VALUE_COLUMN)
    private byte[] value;

    public SecretEntity() {
        super();
    }

    public SecretEntity(Secret secret) {
        super(secret);
        this.value = secret.getEncryptedValue();
    }

    @Override
    public Secret toData() {
        return new Secret(super.toSecretInfo(), value);
    }

}
