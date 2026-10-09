// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.license.shared.PlanData;
import org.thingsboard.license.shared.PlanDataConstants;
import org.thingsboard.license.shared.PlanItem;
import org.thingsboard.license.shared.SubscriptionData;
import org.thingsboard.server.common.data.LicenseInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins that a negative device, asset or edge quota means unlimited on a v2 licence, and how each of the two
 * reporting paths spells that out: {@link BasicSubscriptionService#getLicenseInfo()} keeps the 0 its consumer
 * renders as the infinity glyph, while {@link BasicSubscriptionService#getSubscriptionInfo()} reports the
 * sentinel, so a licence granting none stays distinguishable from one granting no limit.
 */
public class UnlimitedQuotaTest {

    private BasicSubscriptionService subscriptionService;
    private LicenseActivationService licenseActivationService;
    private AbstractTbLicenseClient tbLicenseClient;
    private EdgeService edgeService;
    private PlanData planData;

    @BeforeEach
    void setUp() {
        subscriptionService = new BasicSubscriptionService();
        tbLicenseClient = mock(AbstractTbLicenseClient.class);
        DeviceService deviceService = mock(DeviceService.class);
        AssetService assetService = mock(AssetService.class);
        edgeService = mock(EdgeService.class);
        AgentService agentService = mock(AgentService.class);
        licenseActivationService = LicenseActivationStub.wire(subscriptionService, tbLicenseClient, 0);
        ReflectionTestUtils.setField(subscriptionService, "deviceService", deviceService);
        ReflectionTestUtils.setField(subscriptionService, "assetService", assetService);
        ReflectionTestUtils.setField(subscriptionService, "edgeService", edgeService);
        ReflectionTestUtils.setField(subscriptionService, "agentService", agentService);
        // getSubscriptionInfo dereferences the subscription data field by field; an empty stub keeps the
        // fields irrelevant to these tests from causing an unrelated NPE.
        when(tbLicenseClient.getSubscriptionData()).thenReturn(new SubscriptionData());
        planData = new PlanData();
        when(tbLicenseClient.getPlanData()).thenReturn(planData);
    }

    private void givenLicenseVersion(int version) {
        when(licenseActivationService.getLicenseVersion()).thenReturn(version);
    }

    /**
     * Written to both readings of the plan, because the two reporting paths do not share one: the enforcement
     * and {@code getLicenseInfo} sites read the client's typed getter, while {@code getSubscriptionInfo} goes
     * through {@link SubscriptionInfoMapper} and reads the plan data itself. Stubbing only the getter left the
     * subscription assertions passing against an unstubbed - and therefore empty - plan.
     */
    private void givenPlanLongValue(String key, long value) {
        when(tbLicenseClient.getPlanLongValue(key)).thenReturn(value);
        planData.put(key, new PlanItem(value));
    }

    private void givenActualEdgeCount(long count) {
        when(edgeService.countEdges()).thenReturn(count);
    }

    @Test
    public void aNegativeDeviceQuotaMeansUnlimitedOnAV2Licence() {
        givenLicenseVersion(2);
        givenPlanLongValue(PlanDataConstants.MAX_DEVICES_KEY, -1L);

        assertThatCode(() -> subscriptionService.createDeviceAllowed(TenantId.SYS_TENANT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    public void aNegativeAssetQuotaMeansUnlimitedOnAV2Licence() {
        // The asset path carries its own copy of the unlimited guard, so it has to be pinned separately: a
        // guard added to devices alone would leave asset creation refused on the very plans that grant it
        // without limit.
        givenLicenseVersion(2);
        givenPlanLongValue(PlanDataConstants.MAX_ASSETS_KEY, -1L);

        assertThatCode(() -> subscriptionService.createAssetAllowed(TenantId.SYS_TENANT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    public void aNegativeEdgeQuotaMeansUnlimitedOnAV2Licence() {
        // A negative edge quota has to pass the isUnlimited short-circuit in isCreateEdgeAllowed, which
        // createEdgeAllowed delegates to, so edge creation is never blocked on a plan that grants edges
        // without limit.
        givenLicenseVersion(2);
        givenPlanLongValue(PlanDataConstants.MAX_EDGES_KEY, -1L);

        assertThatCode(() -> subscriptionService.createEdgeAllowed(TenantId.SYS_TENANT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    public void anUnlimitedQuotaIsReportedAsZeroRatherThanMinusOne() {
        // getLicenseInfo keeps the 0 convention: its only consumer is the home-page widget, whose plural rule
        // renders `=0` as the infinity glyph and would print "/ -1" for anything else. On all three quotas,
        // since each goes through the same helper.
        givenLicenseVersion(2);
        givenPlanLongValue(PlanDataConstants.MAX_DEVICES_KEY, -1L);
        givenPlanLongValue(PlanDataConstants.MAX_ASSETS_KEY, -1L);
        givenPlanLongValue(PlanDataConstants.MAX_EDGES_KEY, -1L);

        LicenseInfo licenseInfo = subscriptionService.getLicenseInfo();

        assertThat(licenseInfo.getMaxDevices()).isZero();
        assertThat(licenseInfo.getMaxAssets()).isZero();
        assertThat(licenseInfo.getMaxEdges()).isZero();
    }

    @Test
    public void theSubscriptionApiReportsUnlimitedAsTheSentinel() {
        // The licence page reads this one, and it renders 0 as "not included". Collapsing unlimited to 0 there
        // advertised a Development licence - which grants devices and assets without limit - as granting none.
        givenLicenseVersion(2);
        givenPlanLongValue(PlanDataConstants.MAX_DEVICES_KEY, -1L);
        givenPlanLongValue(PlanDataConstants.MAX_ASSETS_KEY, -1L);
        givenPlanLongValue(PlanDataConstants.MAX_EDGES_KEY, -1L);

        SubscriptionInfo subscriptionInfo = subscriptionService.getSubscriptionInfo();

        assertThat(subscriptionInfo.getMaxDevices()).isEqualTo(PlanDataConstants.UNLIMITED_QUOTA);
        assertThat(subscriptionInfo.getMaxAssets()).isEqualTo(PlanDataConstants.UNLIMITED_QUOTA);
        assertThat(subscriptionInfo.getMaxEdges()).isEqualTo(PlanDataConstants.UNLIMITED_QUOTA);
    }

    @Test
    public void theSubscriptionApiKeepsAV2ZeroQuotaAsZero() {
        // The other half of the same distinction: a v2 licence granting no devices reports 0, which the
        // licence page renders as "not included". Unlimited and none must not arrive as the same number.
        givenLicenseVersion(2);
        givenPlanLongValue(PlanDataConstants.MAX_DEVICES_KEY, 0L);

        assertThat(subscriptionService.getSubscriptionInfo().getMaxDevices()).isZero();
    }

    @Test
    public void aV1AbsentQuotaReachesTheSubscriptionApiAsUnlimited() {
        // A v1 licence predates the quota keys, so its absent-or-zero quota is unlimited. It has to be spelled
        // as the sentinel here too, or the licence page reports every v1 licence as granting no devices.
        givenLicenseVersion(1);
        givenPlanLongValue(PlanDataConstants.MAX_DEVICES_KEY, 0L);

        assertThat(subscriptionService.getSubscriptionInfo().getMaxDevices())
                .isEqualTo(PlanDataConstants.UNLIMITED_QUOTA);
    }

    @Test
    public void theEnforcementReadersStillReportUnlimitedAsZero() {
        // getLicensedDeviceLimit and getLicensedAssetLimit are read by LicenseCapacityStartupGate, which
        // treats a non-positive limit as "no cap" and already answers 0 for a node with no licence client at
        // all. Routing them through the reporting convention would only make the sentinel mean the same thing
        // by accident; they keep 0 deliberately.
        givenLicenseVersion(2);
        givenPlanLongValue(PlanDataConstants.MAX_DEVICES_KEY, -1L);
        givenPlanLongValue(PlanDataConstants.MAX_ASSETS_KEY, -1L);

        assertThat(subscriptionService.getLicensedDeviceLimit()).isZero();
        assertThat(subscriptionService.getLicensedAssetLimit()).isZero();
    }

    @Test
    public void aQuotaOfZeroStillBlocksOnAV2Licence() {
        // Unchanged behaviour, pinned so the fix cannot widen into "anything <= 0 means unlimited" on v2.
        // Only an explicitly stored negative value grants unlimited, so no existing plan changes behaviour.
        givenLicenseVersion(2);
        givenPlanLongValue(PlanDataConstants.MAX_DEVICES_KEY, 0L);

        assertThatThrownBy(() -> subscriptionService.createDeviceAllowed(TenantId.SYS_TENANT_ID))
                .isInstanceOf(SubscriptionException.class);
    }

    @Test
    public void aQuotaOfZeroStillMeansUnlimitedOnAV1Licence() {
        // The v1 convention is untouched: absent or zero means unlimited there.
        givenLicenseVersion(1);
        givenPlanLongValue(PlanDataConstants.MAX_DEVICES_KEY, 0L);

        assertThatCode(() -> subscriptionService.createDeviceAllowed(TenantId.SYS_TENANT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    public void aPositiveQuotaIsStillEnforced() {
        givenLicenseVersion(2);
        givenPlanLongValue(PlanDataConstants.MAX_EDGES_KEY, 2L);
        givenActualEdgeCount(2);

        assertThatThrownBy(() -> subscriptionService.createEdgeAllowed(TenantId.SYS_TENANT_ID))
                .isInstanceOf(SubscriptionException.class);
    }

}
