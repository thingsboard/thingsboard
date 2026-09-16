// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.dao.model.ModelConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.TENANT_TABLE_NAME)
public final class TenantEntity extends AbstractTenantEntity<Tenant> {

    public TenantEntity() {
        super();
    }

    public TenantEntity(Tenant tenant) {
        super(tenant);
    }

    @Override
    public Tenant toData() {
        return super.toTenant();
    }
}
