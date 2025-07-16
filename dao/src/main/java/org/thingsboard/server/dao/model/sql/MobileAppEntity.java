/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileAppStatus;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.app.MobileAppVersionInfo;
import org.thingsboard.server.common.data.mobile.app.StoreInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.MOBILE_APP_STORE_INFO_EMPTY_OBJECT;
import static org.thingsboard.server.dao.model.ModelConstants.MOBILE_APP_VERSION_INFO_EMPTY_OBJECT;
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

    @Column(name = ModelConstants.MOBILE_APP_TITLE_PROPERTY)
    private String title;

    @Column(name = ModelConstants.MOBILE_APP_APP_SECRET_PROPERTY)
    private String appSecret;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.MOBILE_APP_PLATFORM_TYPE_PROPERTY)
    private PlatformType platformType;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.MOBILE_APP_STATUS_PROPERTY)
    private MobileAppStatus status;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.MOBILE_APP_VERSION_INFO_PROPERTY)
    private JsonNode versionInfo;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.MOBILE_APP_STORE_INFO_PROPERTY)
    private JsonNode storeInfo;

    public MobileAppEntity() {
        super();
    }

    public MobileAppEntity(MobileApp mobile) {
        super(mobile);
        if (mobile.getTenantId() != null) {
            this.tenantId = mobile.getTenantId().getId();
        }
        this.pkgName = mobile.getPkgName();
        this.title = mobile.getTitle();
        this.appSecret = mobile.getAppSecret();
        this.platformType = mobile.getPlatformType();
        this.status = mobile.getStatus();
        this.versionInfo = toJson(mobile.getVersionInfo());
        this.storeInfo = toJson(mobile.getStoreInfo());
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
        mobile.setTitle(title);
        mobile.setAppSecret(appSecret);
        mobile.setPlatformType(platformType);
        mobile.setStatus(status);
        mobile.setVersionInfo(versionInfo != null ? fromJson(versionInfo, MobileAppVersionInfo.class) : MOBILE_APP_VERSION_INFO_EMPTY_OBJECT);
        mobile.setStoreInfo(storeInfo != null ? fromJson(storeInfo, StoreInfo.class) : MOBILE_APP_STORE_INFO_EMPTY_OBJECT);
        return mobile;
    }
}
