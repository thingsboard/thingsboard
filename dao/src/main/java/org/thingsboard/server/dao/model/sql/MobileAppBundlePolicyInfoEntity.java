// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.selfregistration.MobileSelfRegistrationParams;
import org.thingsboard.server.dao.model.ModelConstants;

import static org.thingsboard.server.dao.model.ModelConstants.MOBILE_APP_BUNDLE_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = MOBILE_APP_BUNDLE_TABLE_NAME)
public final class MobileAppBundlePolicyInfoEntity extends AbstractMobileAppBundleEntity<MobileAppBundle> {

    @Column(name = ModelConstants.MOBILE_APP_BUNDLE_TERMS_OF_USE_PROPERTY)
    private String termsOfUse;

    @Column(name = ModelConstants.MOBILE_APP_BUNDLE_PRIVACY_POLICY_PROPERTY)
    private String privacyPolicy;

    public MobileAppBundlePolicyInfoEntity() {
        super();
    }

    public MobileAppBundlePolicyInfoEntity(MobileAppBundle mobileAppBundle) {
        super(mobileAppBundle);
        MobileSelfRegistrationParams selfRegistrationParams = mobileAppBundle.getSelfRegistrationParams();
        if (selfRegistrationParams != null) {
            this.termsOfUse = selfRegistrationParams.getTermsOfUse();
            this.privacyPolicy = selfRegistrationParams.getPrivacyPolicy();
            selfRegistrationParams.setPrivacyPolicy(null);
            selfRegistrationParams.setTermsOfUse(null);
            this.selfRegistrationConfig = toJson(mobileAppBundle.getSelfRegistrationParams());
        }
    }

    @Override
    public MobileAppBundle toData() {
        MobileAppBundle mobileAppBundle = super.toMobileAppBundle();
        MobileSelfRegistrationParams selfRegistrationParams = mobileAppBundle.getSelfRegistrationParams();
        if (selfRegistrationParams != null) {
            selfRegistrationParams.setPrivacyPolicy(privacyPolicy);
            selfRegistrationParams.setTermsOfUse(termsOfUse);
        }
        return mobileAppBundle;
    }
}
