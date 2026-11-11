// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.LicenseInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;

@Service
@Slf4j
@Profile({"install", "test"})
public class InstallSubscriptionService implements SubscriptionService {

    @Override
    public void createDeviceAllowed(TenantId tenantId) throws SubscriptionException {

    }

    @Override
    public void createAssetAllowed(TenantId tenantId) throws SubscriptionException {

    }

    @Override
    public void createEdgeAllowed(TenantId tenantId) throws SubscriptionException {

    }

    @Override
    public boolean isCreateEdgeAllowed(TenantId tenantId) {
        return true;
    }

    @Override
    public void createAgentAllowed(TenantId tenantId) throws SubscriptionException {

    }

    @Override
    public void whiteLabelingAllowed(TenantId tenantId) throws SubscriptionException {

    }

    @Override
    public boolean whiteLabelingEnabled(TenantId tenantId) throws SubscriptionException {
        return true;
    }

    @Override
    public boolean edgeEnabled(TenantId tenantId) throws SubscriptionException {
        return false;
    }

    @Override
    public boolean trendzEnabled(TenantId tenantId) throws SubscriptionException {
        return false;
    }

    @Override
    public boolean isFeatureEnabled(TenantId tenantId, PlatformFeature feature) {
        // Must be enabled: this implementation also covers the whole test profile.
        return true;
    }

    @Override
    public void checkFeatureAllowed(TenantId tenantId, PlatformFeature feature) throws SubscriptionException {
    }

    @Override
    public boolean isDevelopment(TenantId tenantId) throws SubscriptionException {
        return false;
    }

    @Override
    public boolean solutionTemplateLevelAllowed(TenantId tenantId, String solutionTemplateLevel) throws SubscriptionException {
        return true;
    }

    @Override
    public LicenseInfo getLicenseInfo() {
        return null;
    }

    @Override
    public int getLicenseVersion() {
        return 1;
    }

    @Override
    public SubscriptionInfo getSubscriptionInfo() {
        return null;
    }

    @Override
    public SubscriptionInfo refreshLicense() {
        return null;
    }

    @Override
    public boolean isLicenseActivated() {
        return true;
    }

    @Override
    public void pickUpStoredLicenseSecret() {
        // no-op: license enforcement is disabled under install/test profiles
    }

    @Override
    public void reconcileLicenseState() {
        // no-op: license enforcement is disabled under install/test profiles
    }

    @Override
    public boolean isNonProductionMode() {
        return false;
    }

    @Override
    public boolean revokeNonProductionEntitlement() {
        // no-op: license enforcement is disabled under install/test profiles
        return false;
    }

    @Override
    public void applyLicenseKey(String secret) {
        // no-op: license enforcement is disabled under install/test profiles
    }

}
