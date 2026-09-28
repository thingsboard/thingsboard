// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.secret.SecretInfo;

import static org.thingsboard.server.dao.model.ModelConstants.SECRET_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = SECRET_TABLE_NAME)
public class SecretInfoEntity extends AbstractSecretInfoEntity<SecretInfo> {

    public SecretInfoEntity() {
        super();
    }

    public SecretInfoEntity(SecretInfo secretInfo) {
        super(secretInfo);
    }

    @Override
    public SecretInfo toData() {
        return super.toSecretInfo();
    }

}
