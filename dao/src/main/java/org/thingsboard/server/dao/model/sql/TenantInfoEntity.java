// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.TenantInfo;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class TenantInfoEntity extends AbstractTenantEntity<TenantInfo> {

    public static final Map<String,String> tenantInfoColumnMap = new HashMap<>();
    static {
        tenantInfoColumnMap.put("tenantProfileName", "p.name");
    }

    private String tenantProfileName;

    public TenantInfoEntity() {
        super();
    }

    public TenantInfoEntity(TenantEntity tenantEntity, String tenantProfileName) {
        super(tenantEntity);
        this.tenantProfileName = tenantProfileName;
    }

    @Override
    public TenantInfo toData() {
        return new TenantInfo(super.toTenant(), this.tenantProfileName);
    }
}
