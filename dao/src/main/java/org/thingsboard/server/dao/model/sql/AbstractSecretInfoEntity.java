// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SECRET_DESCRIPTION_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.SECRET_NAME_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractSecretInfoEntity<T extends SecretInfo> extends BaseSqlEntity<T> implements BaseEntity<T> {

    @Column(name = TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = SECRET_NAME_COLUMN)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.SECRET_TYPE_COLUMN)
    private SecretType type;

    @Column(name = SECRET_DESCRIPTION_COLUMN)
    private String description;

    public AbstractSecretInfoEntity() {
        super();
    }

    public AbstractSecretInfoEntity(SecretInfo secretInfo) {
        super(secretInfo);
        this.tenantId = secretInfo.getTenantId().getId();
        this.name = secretInfo.getName();
        this.type = secretInfo.getType();
        this.description = secretInfo.getDescription();
    }

    protected SecretInfo toSecretInfo() {
        SecretInfo secretInfo = new SecretInfo(new SecretId(getUuid()));
        secretInfo.setCreatedTime(createdTime);
        secretInfo.setTenantId(TenantId.fromUUID(tenantId));
        secretInfo.setName(name);
        secretInfo.setType(type);
        secretInfo.setDescription(description);
        return secretInfo;
    }

}
