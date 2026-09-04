// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.layout.MobileLayoutConfig;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractMobileAppBundleEntity<T extends MobileAppBundle> extends BaseSqlEntity<T> {

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

    public AbstractMobileAppBundleEntity() {
        super();
    }

    public AbstractMobileAppBundleEntity(MobileAppBundleEntity mobileAppBundleEntity) {
        super(mobileAppBundleEntity);
        this.tenantId = mobileAppBundleEntity.getTenantId();
        this.title = mobileAppBundleEntity.getTitle();
        this.description = mobileAppBundleEntity.getDescription();
        this.androidAppId = mobileAppBundleEntity.getAndroidAppId();
        this.iosAppID = mobileAppBundleEntity.getIosAppID();
        this.layoutConfig = mobileAppBundleEntity.getLayoutConfig();
        this.oauth2Enabled = mobileAppBundleEntity.getOauth2Enabled();
    }

    public AbstractMobileAppBundleEntity(T mobileAppBundle) {
        super(mobileAppBundle);
        if (mobileAppBundle.getTenantId() != null) {
            this.tenantId = mobileAppBundle.getTenantId().getId();
        }
        this.title = mobileAppBundle.getTitle();
        this.description = mobileAppBundle.getDescription();
        if (mobileAppBundle.getAndroidAppId() != null) {
            this.androidAppId = mobileAppBundle.getAndroidAppId().getId();
        }
        if (mobileAppBundle.getIosAppId() != null) {
            this.iosAppID = mobileAppBundle.getIosAppId().getId();
        }
        this.layoutConfig = toJson(mobileAppBundle.getLayoutConfig());
        this.oauth2Enabled = mobileAppBundle.getOauth2Enabled();
    }

    protected MobileAppBundle toMobileAppBundle() {
        MobileAppBundle mobileAppBundle = new MobileAppBundle(new MobileAppBundleId(id));
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
