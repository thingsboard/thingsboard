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
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.MobileAppSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.AndroidConfig;
import org.thingsboard.server.common.data.mobile.IosConfig;
import org.thingsboard.server.common.data.mobile.MobileAppSettings;
import org.thingsboard.server.common.data.mobile.QRCodeConfig;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = ModelConstants.MOBILE_APP_SETTINGS_TABLE_NAME)
public class MobileAppSettingsEntity extends BaseSqlEntity<MobileAppSettings> {

    @Column(name = ModelConstants.TENANT_ID_COLUMN, columnDefinition = "uuid")
    protected UUID tenantId;

    @Column(name = ModelConstants.MOBILE_APP_SETTINGS_USE_DEFAULT_APP_PROPERTY)
    private boolean useDefaultApp;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.MOBILE_APP_SETTINGS_ANDROID_CONFIG_PROPERTY)
    private JsonNode androidConfig;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.MOBILE_APP_SETTINGS_IOS_CONFIG_PROPERTY)
    private JsonNode iosConfig;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.MOBILE_APP_SETTINGS_QR_CODE_CONFIG_PROPERTY)
    private JsonNode qrCodeConfig;

    public MobileAppSettingsEntity(MobileAppSettings mobileAppSettings) {
        this.setId(mobileAppSettings.getUuidId());
        this.setCreatedTime(mobileAppSettings.getCreatedTime());
        this.tenantId = mobileAppSettings.getTenantId().getId();
        this.useDefaultApp = mobileAppSettings.isUseDefaultApp();
        this.androidConfig = toJson(mobileAppSettings.getAndroidConfig());
        this.iosConfig = toJson(mobileAppSettings.getIosConfig());
        this.qrCodeConfig = toJson(mobileAppSettings.getQrCodeConfig());
    }

    @Override
    public MobileAppSettings toData() {
        MobileAppSettings mobileAppSettings = new MobileAppSettings(new MobileAppSettingsId(getUuid()));
        mobileAppSettings.setCreatedTime(createdTime);
        mobileAppSettings.setTenantId(TenantId.fromUUID(tenantId));
        mobileAppSettings.setUseDefaultApp(useDefaultApp);
        mobileAppSettings.setAndroidConfig(fromJson(androidConfig, AndroidConfig.class));
        mobileAppSettings.setIosConfig(fromJson(iosConfig, IosConfig.class));
        mobileAppSettings.setQrCodeConfig(fromJson(qrCodeConfig, QRCodeConfig.class));
        return mobileAppSettings;
    }

}
