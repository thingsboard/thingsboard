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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.MobileAppSettings;
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

    @Column(name = ModelConstants.MOBILE_APP_SETTINGS_APP_PACKAGE_PROPERTY)
    private String appPackage;

    @Column(name = ModelConstants.MOBILE_APP_SETTINGS_SHA256_CERT_FINGERPRINTS_PROPERTY)
    private String sha256CertFingerprints;

    @Column(name = ModelConstants.MOBILE_APP_APP_ID_PROPERTY)
    private String appId;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.MOBILE_APP_SETTINGS_PROPERTY)
    private JsonNode settings;

    public MobileAppSettingsEntity(MobileAppSettings mobileAppSettings) {
        this.tenantId = mobileAppSettings.getTenantId().getId();
        if (mobileAppSettings.getAppPackage() != null) {
            this.appPackage = mobileAppSettings.getAppPackage();
        }
        if (mobileAppSettings.getSha256CertFingerprints() != null) {
            this.sha256CertFingerprints = mobileAppSettings.getSha256CertFingerprints();
        }
        if (mobileAppSettings.getAppId() != null) {
            this.appId = mobileAppSettings.getAppId();
        }
        if (mobileAppSettings.getSettings() != null) {
            this.settings = mobileAppSettings.getSettings();
        }
   }

    @Override
    public MobileAppSettings toData() {
        MobileAppSettings mobileAppSettings = new MobileAppSettings();
        mobileAppSettings.setTenantId(TenantId.fromUUID(tenantId));
        if (appPackage != null) {
            mobileAppSettings.setAppPackage(appPackage);
        }
        if (sha256CertFingerprints != null) {
            mobileAppSettings.setSha256CertFingerprints(sha256CertFingerprints);
        }
        if (appId != null) {
            mobileAppSettings.setAppId(appId);
        }
        if (settings != null) {
            mobileAppSettings.setSettings(settings);
        }
        return mobileAppSettings;
    }
}
