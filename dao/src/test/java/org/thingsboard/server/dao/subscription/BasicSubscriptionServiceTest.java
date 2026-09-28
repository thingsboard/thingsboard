// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.license.client.NonProductionTbLicenseClient;
import org.thingsboard.license.shared.PlanDataConstants;
import org.thingsboard.license.shared.PlanItem;
import org.thingsboard.license.shared.SubscriptionData;
import org.thingsboard.license.shared.exception.LicenseErrorCode;
import org.thingsboard.license.shared.exception.LicenseException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.subscription.SubscriptionErrorCode;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BasicSubscriptionServiceTest {

    @Test
    public void anInstanceWithNoLicenseClientFailsClosedWithoutNpe() {
        // What every guarded path here answers once the activation service has torn the client down - the
        // state a critical runtime licence error or an exhausted non-production allowance leaves behind.
        // Each of these must refuse rather than dereference the client that is no longer there.
        BasicSubscriptionService subscriptionService = activatedService(null);

        assertThat(subscriptionService.isLicenseActivated()).isFalse();

        // getLicenseVersion must return the mirrored version instead of dereferencing the null client - and
        // that version must read as 0, rather than keep reporting the licence that is gone.
        assertThatCode(subscriptionService::getLicenseVersion).doesNotThrowAnyException();
        assertThat(subscriptionService.getLicenseVersion()).isZero();

        // create*Allowed must fail closed with a SubscriptionException (never an NPE).
        assertThatExceptionOfType(SubscriptionException.class)
                .isThrownBy(() -> subscriptionService.createDeviceAllowed(TenantId.SYS_TENANT_ID));
        assertThatExceptionOfType(SubscriptionException.class)
                .isThrownBy(() -> subscriptionService.createAssetAllowed(TenantId.SYS_TENANT_ID));
        assertThatExceptionOfType(SubscriptionException.class)
                .isThrownBy(() -> subscriptionService.createEdgeAllowed(TenantId.SYS_TENANT_ID));
        assertThatExceptionOfType(SubscriptionException.class)
                .isThrownBy(() -> subscriptionService.whiteLabelingAllowed(TenantId.SYS_TENANT_ID));

        // The three a licence-management screen calls on a locked instance. Unlike the methods above, each of
        // these dereferences the licence client immediately after its guard, so a missing guard is a straight
        // NPE on a request path rather than a wrong answer.
        assertThatExceptionOfType(SubscriptionException.class)
                .isThrownBy(subscriptionService::getLicenseInfo);
        assertThatExceptionOfType(SubscriptionException.class)
                .isThrownBy(subscriptionService::getSubscriptionInfo);
        assertThatExceptionOfType(SubscriptionException.class)
                .isThrownBy(subscriptionService::refreshLicense);

        // Boolean feature getters must report the feature as unavailable rather than NPE.
        assertThat(subscriptionService.isCreateEdgeAllowed(TenantId.SYS_TENANT_ID)).isFalse();
        assertThat(subscriptionService.whiteLabelingEnabled(TenantId.SYS_TENANT_ID)).isFalse();
        assertThat(subscriptionService.edgeEnabled(TenantId.SYS_TENANT_ID)).isFalse();
        assertThat(subscriptionService.trendzEnabled(TenantId.SYS_TENANT_ID)).isFalse();
        // An instance with no licence client cannot know it is licensed for production, and answers the
        // conservative way round rather than dropping the non-production notice.
        assertThat(subscriptionService.isDevelopment(TenantId.SYS_TENANT_ID)).isTrue();
    }

    @Test
    public void isFeatureEnabledDelegatesToTheClientUnderTheFeaturesOwnPlanDataKey() {
        AbstractTbLicenseClient tbLicenseClient = mock(AbstractTbLicenseClient.class);
        // Only the integrations key is stubbed, so this passes only if the lookup uses that exact key.
        // How the client resolves an absent key is its own contract, pinned by
        // PlatformFeatureContractTest rather than here.
        when(tbLicenseClient.isPlanFeatureEnabled(PlanDataConstants.INTEGRATIONS_KEY)).thenReturn(true);
        BasicSubscriptionService subscriptionService = activatedService(tbLicenseClient);

        assertThat(subscriptionService.isFeatureEnabled(TenantId.SYS_TENANT_ID, PlatformFeature.INTEGRATIONS)).isTrue();
    }

    @Test
    public void isFeatureEnabledIsFalseWhenTheClientSaysTheFeatureIsWithheld() {
        AbstractTbLicenseClient tbLicenseClient = mock(AbstractTbLicenseClient.class);
        when(tbLicenseClient.isPlanFeatureEnabled(PlanDataConstants.SCHEDULER_KEY)).thenReturn(false);
        BasicSubscriptionService subscriptionService = activatedService(tbLicenseClient);

        assertThat(subscriptionService.isFeatureEnabled(TenantId.SYS_TENANT_ID, PlatformFeature.SCHEDULER)).isFalse();
    }

    @Test
    public void aDisabledFeatureIsRefusedWithFeatureDisabled() {
        AbstractTbLicenseClient tbLicenseClient = mock(AbstractTbLicenseClient.class);
        when(tbLicenseClient.isPlanFeatureEnabled(PlanDataConstants.SCHEDULER_KEY)).thenReturn(false);
        BasicSubscriptionService subscriptionService = activatedService(tbLicenseClient);

        assertThatThrownBy(() -> subscriptionService.checkFeatureAllowed(TenantId.SYS_TENANT_ID, PlatformFeature.SCHEDULER))
                .isInstanceOf(SubscriptionException.class)
                .hasMessageContaining("Scheduler")
                .extracting(exception -> ((SubscriptionException) exception).getErrorCode())
                .isEqualTo(SubscriptionErrorCode.FEATURE_DISABLED);
    }

    @Test
    public void anEnabledFeatureIsAllowed() {
        AbstractTbLicenseClient tbLicenseClient = mock(AbstractTbLicenseClient.class);
        when(tbLicenseClient.isPlanFeatureEnabled(PlanDataConstants.REPORTING_KEY)).thenReturn(true);
        BasicSubscriptionService subscriptionService = activatedService(tbLicenseClient);

        assertThatNoException().isThrownBy(() -> subscriptionService.checkFeatureAllowed(TenantId.SYS_TENANT_ID, PlatformFeature.REPORTING));
    }

    @Test
    public void anUnactivatedInstanceReportsEveryFeatureDisabled() {
        // An absent licence client is not an absent plan-data key. The absent key means a licence answered
        // and said nothing about the feature, which grants it; no client means no licence answered at all.
        // Granting there would make losing a licence grant strictly more than holding one - a plan that
        // withholds reporting would resume allowing reports the moment any critical runtime error nulled
        // the client - so this fails closed.
        BasicSubscriptionService subscriptionService = activatedService(null);

        for (PlatformFeature feature : PlatformFeature.values()) {
            assertThat(subscriptionService.isFeatureEnabled(TenantId.SYS_TENANT_ID, feature)).isFalse();
        }
    }

    @Test
    public void aKeylessInstanceKeepsEveryFeatureAndStaysNonProduction() {
        // The other side of failing closed: the deployment that legitimately has no licence secret still
        // holds a real client, and that client's own plan data - not a mock's, which would only agree with
        // whatever this test assumed - is what keeps all three features granted. Pins that the fail-closed
        // rule above cannot be read as "keyless deployments lose their features".
        NonProductionTbLicenseClient keylessClient = NonProductionTbLicenseClient.builder()
                .releaseDate(System.currentTimeMillis())
                .listener(licenseException -> {})
                .build();
        BasicSubscriptionService subscriptionService = activatedService(keylessClient);

        for (PlatformFeature feature : PlatformFeature.values()) {
            assertThat(subscriptionService.isFeatureEnabled(TenantId.SYS_TENANT_ID, feature))
                    .as("%s must stay granted for a keyless deployment", feature)
                    .isTrue();
        }
        assertThat(subscriptionService.isNonProductionMode()).isTrue();
        assertThat(subscriptionService.resolveDevelopment()).isTrue();
    }

    @Test
    public void anInstanceWithNoLicenseClientStillReportsItselfAsNonProduction() {
        // A keyless deployment that exhausts its allowance has its client torn down by the enforcer. If
        // isDevelopment answered false there, the non-production notice would drop out of its scheduled
        // reports exactly when the client is torn down - the watermark would disappear once the limit is
        // passed.
        BasicSubscriptionService subscriptionService = activatedService(null);

        assertThat(subscriptionService.resolveDevelopment()).isTrue();
    }

    @Test
    public void aLicenseThatCannotAnswerTheDevelopmentQuestionIsTreatedAsNonProduction() {
        // "I do not know" must not be reported as "licensed for production": the client refuses to guess
        // when the plan data cannot answer, and that refusal has to stay conservative on this side too.
        AbstractTbLicenseClient tbLicenseClient = mock(AbstractTbLicenseClient.class);
        when(tbLicenseClient.getPlanBooleanValue(PlanDataConstants.DEVELOPMENT_KEY))
                .thenThrow(new LicenseException("plan data unavailable", LicenseErrorCode.GENERAL_ERROR));
        BasicSubscriptionService subscriptionService = activatedService(tbLicenseClient);

        assertThat(subscriptionService.resolveDevelopment()).isTrue();
    }

    @Test
    public void aKeylessInstanceReportsACoherentSubscriptionRatherThanJavaDefaults() {
        // NonProductionTbLicenseClient hands back a blank SubscriptionData - correctly, since there is no
        // subscription to fill it from - so every subscription-shaped field arrives as a Java default. Copied
        // out unchanged that yields perpetual = false together with null period timestamps, a combination the
        // client contract says cannot exist: only a subscription with no billing period has null period
        // fields. Any consumer branching on perpetual then takes the periodic branch and renders the period,
        // the due date and the next charge out of nulls, which is how the literal "from {{startDate}} to
        // {{endDate}}" reached the licence-management screen. Built on the real keyless client rather than a
        // mock, so this pins the shape actually produced instead of the one this test assumed.
        BasicSubscriptionService subscriptionService = subscriptionInfoService(NonProductionTbLicenseClient.builder()
                .releaseDate(System.currentTimeMillis())
                .listener(licenseException -> {})
                .build());

        SubscriptionInfo subscriptionInfo = subscriptionService.getSubscriptionInfo();

        assertThat(subscriptionInfo.isNonProduction()).isTrue();
        assertThat(subscriptionInfo.isDevelopment()).isTrue();
        // No billing period, no due date and no next charge - and reported as period-less, rather than as a
        // periodic subscription that happens to be missing its dates.
        assertThat(subscriptionInfo.isPerpetual()).isTrue();
        assertThat(subscriptionInfo.getCurrentPeriodStartTs()).isNull();
        assertThat(subscriptionInfo.getCurrentPeriodEndTs()).isNull();
        assertThat(subscriptionInfo.getEndTs()).isNull();
        assertThat(subscriptionInfo.getUpcomingInvoiceDate()).isNull();
        assertThat(subscriptionInfo.getUpcomingInvoiceAmountDue()).isNull();
        // A name for the row that would otherwise render blank.
        assertThat(subscriptionInfo.getSubscriptionPlanName()).isEqualTo(BasicSubscriptionService.NON_PRODUCTION_PLAN_NAME);
        // No subscription was ever bought, so there is no id to manage one by; whoever displays this drops
        // the actions that need it rather than building a portal URL out of a null.
        assertThat(subscriptionInfo.getSubscriptionId()).isNull();
        // No licence server was ever contacted, so consumers drop the actions that need one.
        assertThat(subscriptionInfo.getLicenseServerEndpoint()).isNull();
        // Unlimited quotas, reported as the sentinel rather than as the 0 that reads as "not included".
        assertThat(subscriptionInfo.getMaxDevices()).isEqualTo(PlanDataConstants.UNLIMITED_QUOTA);
        assertThat(subscriptionInfo.getMaxAssets()).isEqualTo(PlanDataConstants.UNLIMITED_QUOTA);
        assertThat(subscriptionInfo.getMaxEdges()).isEqualTo(PlanDataConstants.UNLIMITED_QUOTA);
        // Edge is granted even though no plan addon says so, which is why the screen cannot decide whether to
        // show the edge rows from planEdgeEnabled alone.
        assertThat(subscriptionInfo.isEdgeEnabled()).isTrue();
        assertThat(subscriptionInfo.isPlanEdgeEnabled()).isFalse();
        assertThat(subscriptionInfo.isWhiteLabelingEnabled()).isFalse();
    }

    @Test
    public void aLicensedDevelopmentSubscriptionKeepsItsOwnPeriodAndEndpoint() {
        // The keyless shaping is gated on the licence client, not on the development flag. A real licence on
        // the portal's Development plan is development mode too, but it has a genuine subscription behind it -
        // a plan name, period timestamps and a licence-server endpoint that the manage-subscription action
        // works against - and none of that may be overwritten with the keyless answer.
        AbstractTbLicenseClient tbLicenseClient = mock(AbstractTbLicenseClient.class);
        SubscriptionData subscriptionData = new SubscriptionData();
        subscriptionData.setSubscriptionId("subscription-id");
        subscriptionData.setSubscriptionPlanName("Development");
        subscriptionData.setCurrentPeriodStartTs(1000L);
        subscriptionData.setCurrentPeriodEndTs(2000L);
        subscriptionData.setEndTs(2000L);
        when(tbLicenseClient.getSubscriptionData()).thenReturn(subscriptionData);
        when(tbLicenseClient.getPlanBooleanValue(PlanDataConstants.DEVELOPMENT_KEY)).thenReturn(true);
        when(tbLicenseClient.getLicenseServerEndpoint()).thenReturn("https://license.example.test");
        BasicSubscriptionService subscriptionService = subscriptionInfoService(tbLicenseClient);

        SubscriptionInfo subscriptionInfo = subscriptionService.getSubscriptionInfo();

        assertThat(subscriptionInfo.isNonProduction()).isFalse();
        assertThat(subscriptionInfo.isDevelopment()).isTrue();
        assertThat(subscriptionInfo.isPerpetual()).isFalse();
        assertThat(subscriptionInfo.getSubscriptionId()).isEqualTo("subscription-id");
        assertThat(subscriptionInfo.getSubscriptionPlanName()).isEqualTo("Development");
        assertThat(subscriptionInfo.getCurrentPeriodStartTs()).isEqualTo(1000L);
        assertThat(subscriptionInfo.getCurrentPeriodEndTs()).isEqualTo(2000L);
        assertThat(subscriptionInfo.getEndTs()).isEqualTo(2000L);
        assertThat(subscriptionInfo.getLicenseServerEndpoint()).isEqualTo("https://license.example.test");
    }

    @Test
    public void anUnlimitedSoftwareUpdateHorizonIsReportedAsNoHorizonAtAll() {
        // The sentinel is not a date and must never be rendered as one - left untranslated it reaches the
        // licence screen as a year-thousands date. Null is what this field already means "no horizon" with,
        // and the billing period beside it is a real date that must survive the translation untouched.
        AbstractTbLicenseClient tbLicenseClient = mock(AbstractTbLicenseClient.class);
        SubscriptionData subscriptionData = new SubscriptionData();
        subscriptionData.setCurrentPeriodStartTs(1000L);
        subscriptionData.setCurrentPeriodEndTs(2000L);
        subscriptionData.setEndTs(PlanDataConstants.PERPETUAL_END_TS);
        when(tbLicenseClient.getSubscriptionData()).thenReturn(subscriptionData);
        BasicSubscriptionService subscriptionService = subscriptionInfoService(tbLicenseClient);

        SubscriptionInfo subscriptionInfo = subscriptionService.getSubscriptionInfo();

        assertThat(subscriptionInfo.getEndTs()).isNull();
        assertThat(subscriptionInfo.getCurrentPeriodStartTs()).isEqualTo(1000L);
        assertThat(subscriptionInfo.getCurrentPeriodEndTs()).isEqualTo(2000L);
    }

    @Test
    public void isDevelopmentFollowsAPlanChangedUnderARunningClient() {
        // The licence client updates its plan in place when the portal answers - it swaps no client and raises
        // no callback - so anything cached here would hold the plan this node activated on until it restarts,
        // and in a cluster the nodes would disagree about watermarking. Guards against caching this again.
        Map<String, PlanItem> plan = new HashMap<>(Map.of(PlanDataConstants.DEVELOPMENT_KEY, new PlanItem(true)));
        BasicSubscriptionService subscriptionService = activatedService(new FixedPlanLicenseClient(plan));
        assertThat(subscriptionService.isDevelopment(TenantId.SYS_TENANT_ID)).isTrue();

        plan.put(PlanDataConstants.DEVELOPMENT_KEY, new PlanItem(false));

        assertThat(subscriptionService.isDevelopment(TenantId.SYS_TENANT_ID)).isFalse();
    }

    /**
     * The service reading a published licence client. The client itself belongs to
     * {@link LicenseActivationService} now, so it is handed over through a stub of that interface rather than
     * set on a field here - which is also what makes a {@code null} client stand for a locked instance.
     */
    private BasicSubscriptionService activatedService(AbstractTbLicenseClient tbLicenseClient) {
        return activatedService(tbLicenseClient, 0);
    }

    private BasicSubscriptionService activatedService(AbstractTbLicenseClient tbLicenseClient, int licenseVersion) {
        BasicSubscriptionService subscriptionService = new BasicSubscriptionService();
        LicenseActivationStub.wire(subscriptionService, tbLicenseClient, licenseVersion);
        return subscriptionService;
    }

    /**
     * An activated service that can answer getSubscriptionInfo: that method counts devices, assets, edges and
     * agents, and the service never goes through Spring, so those four collaborators have to be wired by hand
     * or the counts fail before any assertion is reached.
     */
    private BasicSubscriptionService subscriptionInfoService(AbstractTbLicenseClient tbLicenseClient) {
        BasicSubscriptionService subscriptionService = activatedService(tbLicenseClient);
        ReflectionTestUtils.setField(subscriptionService, "deviceService", mock(DeviceService.class));
        ReflectionTestUtils.setField(subscriptionService, "assetService", mock(AssetService.class));
        ReflectionTestUtils.setField(subscriptionService, "edgeService", mock(EdgeService.class));
        ReflectionTestUtils.setField(subscriptionService, "agentService", mock(AgentService.class));
        return subscriptionService;
    }

}
