// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasVersion;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class BaseVersionedEntity<D extends BaseData & HasVersion> extends BaseSqlEntity<D> implements HasVersion {

    @Getter @Setter
    @Version
    @Column(name = ModelConstants.VERSION_PROPERTY)
    protected Long version;

    public BaseVersionedEntity() {
        super();
    }

    public BaseVersionedEntity(D domain) {
        super(domain);
        this.version = domain.getVersion();
    }

    public BaseVersionedEntity(BaseVersionedEntity<?> entity) {
        super(entity);
        this.version = entity.version;
    }

    @Override
    public String toString() {
        return "BaseVersionedEntity{" +
                "id=" + id +
                ", createdTime=" + createdTime +
                ", version=" + version +
                '}';
    }

}
