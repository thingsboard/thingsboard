/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.QrCodeSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QrCodeSettings;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QRCodeConfig;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = ModelConstants.QR_CODE_SETTINGS_TABLE_NAME)
public class QrCodeSettingsEntity extends BaseSqlEntity<QrCodeSettings> {

    @Column(name = ModelConstants.TENANT_ID_COLUMN, columnDefinition = "uuid")
    protected UUID tenantId;

    @Column(name = ModelConstants.QR_CODE_SETTINGS_USE_DEFAULT_APP_PROPERTY)
    private boolean useDefaultApp;

    @Column(name = ModelConstants.QR_CODE_SETTINGS_ANDROID_ENABLED_PROPERTY)
    private boolean androidEnabled;

    @Column(name = ModelConstants.QR_CODE_SETTINGS_IOS_ENABLED_PROPERTY)
    private boolean iosEnabled;

    @Column(name = ModelConstants.QR_CODE_SETTINGS_BUNDLE_ID_PROPERTY)
    private UUID mobileAppBundleId;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.QR_CODE_SETTINGS_CONFIG_PROPERTY)
    private JsonNode qrCodeConfig;

    public QrCodeSettingsEntity(QrCodeSettings qrCodeSettings) {
        this.setId(qrCodeSettings.getUuidId());
        this.setCreatedTime(qrCodeSettings.getCreatedTime());
        this.tenantId = qrCodeSettings.getTenantId().getId();
        this.useDefaultApp = qrCodeSettings.isUseDefaultApp();
        this.androidEnabled = qrCodeSettings.isAndroidEnabled();
        this.iosEnabled = qrCodeSettings.isIosEnabled();
        if (qrCodeSettings.getMobileAppBundleId() != null) {
            this.mobileAppBundleId = qrCodeSettings.getMobileAppBundleId().getId();
        }
        this.qrCodeConfig = toJson(qrCodeSettings.getQrCodeConfig());
    }

    @Override
    public QrCodeSettings toData() {
        QrCodeSettings qrCodeSettings = new QrCodeSettings(new QrCodeSettingsId(getUuid()));
        qrCodeSettings.setCreatedTime(createdTime);
        qrCodeSettings.setTenantId(TenantId.fromUUID(tenantId));
        qrCodeSettings.setUseDefaultApp(useDefaultApp);
        qrCodeSettings.setAndroidEnabled(androidEnabled);
        qrCodeSettings.setIosEnabled(iosEnabled);
        if (mobileAppBundleId != null) {
            qrCodeSettings.setMobileAppBundleId(new MobileAppBundleId(mobileAppBundleId));
        }
        qrCodeSettings.setQrCodeConfig(fromJson(qrCodeConfig, QRCodeConfig.class));
        return qrCodeSettings;
    }

}
