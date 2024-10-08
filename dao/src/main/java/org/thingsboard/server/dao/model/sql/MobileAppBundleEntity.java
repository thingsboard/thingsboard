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

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.MobileLayoutConfig;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.MOBILE_APP_BUNDLE_TABLE_NAME)
public class MobileAppBundleEntity extends BaseSqlEntity<MobileAppBundle> {

    @Column(name = TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = ModelConstants.MOBILE_APP_BUNDLE_TITLE_PROPERTY)
    private String title;

    @Column(name = ModelConstants.MOBILE_APP_BUNDLE_DESCRIPTION_PROPERTY)
    private String description;

    @Column(name = ModelConstants.MOBILE_APP_BUNDLE_ANDROID_APP_ID_PROPERTY)
    private UUID androidAppId;

    @Column(name = ModelConstants.MOBILE_APP_BUNDLE_IOS_APP_ID_PROPERTY)
    private UUID iosAppID;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.MOBILE_APP_BUNDLE_LAYOUT_CONFIG_PROPERTY)
    private JsonNode layoutConfig;

    @Column(name = ModelConstants.MOBILE_APP_BUNDLE_OAUTH2_ENABLED_PROPERTY)
    private Boolean oauth2Enabled;

    public MobileAppBundleEntity() {
        super();
    }

    public MobileAppBundleEntity(MobileAppBundle mobile) {
        super(mobile);
        if (mobile.getTenantId() != null) {
            this.tenantId = mobile.getTenantId().getId();
        }
        this.title = mobile.getTitle();
        this.description = mobile.getDescription();
        if (mobile.getAndroidAppId() != null) {
            this.androidAppId = mobile.getAndroidAppId().getId();
        }
        if (mobile.getIosAppId() != null) {
            this.iosAppID = mobile.getIosAppId().getId();
        }
        this.layoutConfig =   toJson(mobile.getLayoutConfig());
        this.oauth2Enabled = mobile.getOauth2Enabled();
    }

    @Override
    public MobileAppBundle toData() {
        MobileAppBundle mobileAppBundle = new MobileAppBundle();
        mobileAppBundle.setId(new MobileAppBundleId(id));
        mobileAppBundle.setCreatedTime(createdTime);
        mobileAppBundle.setTitle(title);
        mobileAppBundle.setDescription(description);
        if (tenantId != null) {
            mobileAppBundle.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (androidAppId != null) {
            mobileAppBundle.setAndroidAppId(new MobileAppId(androidAppId));
        }
        if (iosAppID != null) {
            mobileAppBundle.setIosAppId(new MobileAppId(iosAppID));
        }
        mobileAppBundle.setLayoutConfig(fromJson(layoutConfig, MobileLayoutConfig.class));
        mobileAppBundle.setOauth2Enabled(oauth2Enabled);
        return mobileAppBundle;
    }
}
