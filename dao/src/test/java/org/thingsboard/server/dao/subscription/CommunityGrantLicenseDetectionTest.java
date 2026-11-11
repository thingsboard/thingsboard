// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.jupiter.api.Test;
import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.license.shared.PlanDataConstants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommunityGrantLicenseDetectionTest {

    @Test
    public void aLicenseCarryingTheGrantKeyIsDetected() {
        AbstractTbLicenseClient client = mock(AbstractTbLicenseClient.class);
        when(client.getPlanBooleanValue(CommunityGrantPlan.COMMUNITY_GRANT_KEY)).thenReturn(true);
        assertThat(serviceWith(client).isCommunityGrantLicense()).isTrue();
    }

    @Test
    public void anOrdinaryLicenseIsNotDetected() {
        // An ordinary plan carries no such key at all, which the client resolves to false.
        AbstractTbLicenseClient client = mock(AbstractTbLicenseClient.class);
        when(client.getPlanBooleanValue(CommunityGrantPlan.COMMUNITY_GRANT_KEY)).thenReturn(false);
        assertThat(serviceWith(client).isCommunityGrantLicense()).isFalse();
    }

    @Test
    public void anInstanceWithNoLicenseClientIsNotDetected() {
        // Never activated, or locked by a critical runtime error. Must fail closed: no client means no
        // verdict, and a verdict this returns true for would gate an instance nothing licensed at all.
        BasicSubscriptionService subscriptionService = serviceWith(null);
        assertThat(subscriptionService.isCommunityGrantLicense()).isFalse();
        assertThat(subscriptionService.getLicensedDeviceLimit()).isZero();
    }

    @Test
    public void aClientThatThrowsForTheKeyIsNotDetected() {
        AbstractTbLicenseClient client = mock(AbstractTbLicenseClient.class);
        when(client.getPlanBooleanValue(CommunityGrantPlan.COMMUNITY_GRANT_KEY))
                .thenThrow(new IllegalStateException("plan data unavailable"));
        assertThat(serviceWith(client).isCommunityGrantLicense()).isFalse();
    }

    @Test
    public void aClientThatThrowsForTheDeviceQuotaIsReportedAsZero() {
        AbstractTbLicenseClient client = mock(AbstractTbLicenseClient.class);
        when(client.getPlanLongValue(PlanDataConstants.MAX_DEVICES_KEY))
                .thenThrow(new IllegalStateException("plan data unavailable"));
        assertThat(serviceWith(client).getLicensedDeviceLimit()).isZero();
    }

    @Test
    public void theDeviceLimitIsReportedAsZeroWhenTheQuotaIsUnlimited() {
        AbstractTbLicenseClient client = mock(AbstractTbLicenseClient.class);
        when(client.getPlanLongValue(PlanDataConstants.MAX_DEVICES_KEY)).thenReturn(-1L);
        assertThat(serviceWith(client).getLicensedDeviceLimit()).isZero();
    }

    @Test
    public void theDeviceLimitIsTheRealQuotaWhenTheQuotaIsFinite() {
        AbstractTbLicenseClient client = mock(AbstractTbLicenseClient.class);
        when(client.getPlanLongValue(PlanDataConstants.MAX_DEVICES_KEY)).thenReturn(1000L);
        assertThat(serviceWith(client).getLicensedDeviceLimit()).isEqualTo(1000L);
    }

    /**
     * The licence version is irrelevant to everything this class asserts - both the grant-key lookup and the
     * device-quota read consult the client alone - so it is fixed at the current version rather than varied.
     */
    private BasicSubscriptionService serviceWith(AbstractTbLicenseClient client) {
        BasicSubscriptionService subscriptionService = new BasicSubscriptionService();
        LicenseActivationStub.wire(subscriptionService, client, 2);
        return subscriptionService;
    }
}
