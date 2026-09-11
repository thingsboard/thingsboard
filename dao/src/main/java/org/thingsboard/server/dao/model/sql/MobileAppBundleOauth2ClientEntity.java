// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleOauth2Client;
import org.thingsboard.server.dao.model.ToData;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.MOBILE_APP_BUNDLE_OAUTH2_CLIENT_CLIENT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.MOBILE_APP_BUNDLE_OAUTH2_CLIENT_MOBILE_APP_BUNDLE_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.MOBILE_APP_BUNDLE_OAUTH2_CLIENT_TABLE_NAME;


@Data
@Entity
@Table(name = MOBILE_APP_BUNDLE_OAUTH2_CLIENT_TABLE_NAME)
@IdClass(MobileAppOauth2ClientCompositeKey.class)
public final class MobileAppBundleOauth2ClientEntity implements ToData<MobileAppBundleOauth2Client> {

    @Id
    @Column(name = MOBILE_APP_BUNDLE_OAUTH2_CLIENT_MOBILE_APP_BUNDLE_ID_PROPERTY, columnDefinition = "uuid")
    private UUID mobileAppBundleId;

    @Id
    @Column(name = MOBILE_APP_BUNDLE_OAUTH2_CLIENT_CLIENT_ID_PROPERTY, columnDefinition = "uuid")
    private UUID oauth2ClientId;

    public MobileAppBundleOauth2ClientEntity() {
        super();
    }

    public MobileAppBundleOauth2ClientEntity(MobileAppBundleOauth2Client mobileAppBundleOauth2Client) {
        mobileAppBundleId = mobileAppBundleOauth2Client.getMobileAppBundleId().getId();
        oauth2ClientId = mobileAppBundleOauth2Client.getOAuth2ClientId().getId();
    }

    @Override
    public MobileAppBundleOauth2Client toData() {
        MobileAppBundleOauth2Client result = new MobileAppBundleOauth2Client();
        result.setMobileAppBundleId(new MobileAppBundleId(mobileAppBundleId));
        result.setOAuth2ClientId(new OAuth2ClientId(oauth2ClientId));
        return result;
    }
}
