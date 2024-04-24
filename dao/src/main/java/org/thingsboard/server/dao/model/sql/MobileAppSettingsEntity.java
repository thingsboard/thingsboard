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
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.AndroidConfig;
import org.thingsboard.server.common.data.mobile.IosConfig;
import org.thingsboard.server.common.data.mobile.MobileAppSettings;
import org.thingsboard.server.common.data.mobile.QRCodeConfig;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = ModelConstants.MOBILE_APP_SETTINGS_TABLE_NAME)
public class MobileAppSettingsEntity implements ToData<MobileAppSettings>, Serializable {

    @Id
    @Column(name = ModelConstants.TENANT_ID_COLUMN, columnDefinition = "uuid")
    protected UUID tenantId;

    @Column(name = ModelConstants.MOBILE_APP_SETTINGS_USE_DEFAULT_PROPERTY)
    private boolean useDefault;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.MOBILE_APP_SETTINGS_ANDROID_CONFIG_PROPERTY)
    private JsonNode androidConfig;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.MOBILE_APP_IOS_CONFIG_PROPERTY)
    private JsonNode iosConfig;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.MOBILE_APP_QR_CODE_CONFIG_PROPERTY)
    private JsonNode qrCodeConfig;

    public MobileAppSettingsEntity(MobileAppSettings mobileAppSettings) {
        this.tenantId = mobileAppSettings.getTenantId().getId();
        this.useDefault = mobileAppSettings.isUseDefault();
        if (mobileAppSettings.getAndroidConfig() != null) {
            this.androidConfig = JacksonUtil.valueToTree(mobileAppSettings.getAndroidConfig());
        }
        if (mobileAppSettings.getIosConfig() != null) {
            this.iosConfig = JacksonUtil.valueToTree(mobileAppSettings.getIosConfig());
        }
        if (mobileAppSettings.getQrCodeConfig() != null) {
            this.qrCodeConfig = JacksonUtil.valueToTree(mobileAppSettings.getQrCodeConfig());
        }
   }

    @Override
    public MobileAppSettings toData() {
        MobileAppSettings mobileAppSettings = new MobileAppSettings();
        mobileAppSettings.setTenantId(TenantId.fromUUID(tenantId));
        mobileAppSettings.setUseDefault(useDefault);
        if (qrCodeConfig != null) {
            mobileAppSettings.setAndroidConfig(JacksonUtil.convertValue(androidConfig, AndroidConfig.class));
        }
        if (qrCodeConfig != null) {
            mobileAppSettings.setIosConfig(JacksonUtil.convertValue(iosConfig, IosConfig.class));
        }
        if (qrCodeConfig != null) {
            mobileAppSettings.setQrCodeConfig(JacksonUtil.convertValue(qrCodeConfig, QRCodeConfig.class));
        }
        return mobileAppSettings;
    }
}
