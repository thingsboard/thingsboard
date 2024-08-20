/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.MobileApp;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.MOBILE_APP_TABLE_NAME)
public class MobileAppEntity extends BaseSqlEntity<MobileApp> {

    @Column(name = TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = ModelConstants.MOBILE_APP_PKG_NAME_PROPERTY)
    private String pkgName;

    @Column(name = ModelConstants.MOBILE_APP_APP_SECRET_PROPERTY)
    private String appSecret;

    @Column(name = ModelConstants.MOBILE_APP_OAUTH2_ENABLED_PROPERTY)
    private Boolean oauth2Enabled;

    public MobileAppEntity() {
        super();
    }

    public MobileAppEntity(MobileApp mobile) {
        super(mobile);
        if (mobile.getTenantId() != null) {
            this.tenantId = mobile.getTenantId().getId();
        }
        this.pkgName = mobile.getPkgName();
        this.appSecret = mobile.getAppSecret();
        this.oauth2Enabled = mobile.isOauth2Enabled();
    }

    @Override
    public MobileApp toData() {
        MobileApp mobile = new MobileApp();
        mobile.setId(new MobileAppId(id));
        if (tenantId != null) {
            mobile.setTenantId(TenantId.fromUUID(tenantId));
        }
        mobile.setCreatedTime(createdTime);
        mobile.setPkgName(pkgName);
        mobile.setAppSecret(appSecret);
        mobile.setOauth2Enabled(oauth2Enabled);
        return mobile;
    }
}
