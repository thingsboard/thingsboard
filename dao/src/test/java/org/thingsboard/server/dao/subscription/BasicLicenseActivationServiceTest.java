// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.license.client.NonProductionTbLicenseClient;
import org.thingsboard.license.client.OfflineTbLicenseClient;
import org.thingsboard.license.client.TbLicenseClient;
import org.thingsboard.license.client.TbLicenseClientListener;
import org.thingsboard.license.client.TbLicenseCtx;
import org.thingsboard.license.shared.PlanData;
import org.thingsboard.license.shared.PlanDataConstants;
import org.thingsboard.license.shared.PlanItem;
import org.thingsboard.license.shared.SubscriptionData;
import org.thingsboard.license.shared.SubscriptionPreviewResponse;
import org.thingsboard.license.shared.exception.LicenseErrorCode;
import org.thingsboard.license.shared.exception.LicenseException;
import org.thingsboard.server.common.data.subscription.SubscriptionErrorCode;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.subscription.LicenseCapacity.CappedEntity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BasicLicenseActivationServiceTest {

    @Test
    public void aCriticalRuntimeErrorTearsTheLicenceDownAndClearsWhatDescribesIt() {
        // The instance must be genuinely activated first, or lock() returns at its own first line and
        // onError() is a no-op that proves nothing about what a re-locked instance does.
        BasicLicenseActivationService licenseActivationService = new BasicLicenseActivationService();
        AbstractTbLicenseClient tbLicenseClient = mock(AbstractTbLicenseClient.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", tbLicenseClient);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        ReflectionTestUtils.setField(licenseActivationService, "licenseVersion", 2);
        // No secret is stored, so the on-demand re-activation attempt finds nothing to activate with.
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);

        // Simulate a critical runtime license error: clears the activated state and nulls the client.
        LicenseException criticalError = new LicenseException("Simulated critical error", LicenseErrorCode.GENERAL_ERROR);
        criticalError.setIsCritical(true);
        licenseActivationService.onError(criticalError);

        verify(tbLicenseClient).stop();
        assertThat(licenseActivationService.isLicenseActivated()).isFalse();
        // No licence field may outlive the client it describes: the client is gone and so is everything that
        // was reported about it, rather than the version of a licence that is no longer in force.
        assertThat(licenseActivationService.getClient()).isNull();
        assertThatCode(licenseActivationService::getLicenseVersion).doesNotThrowAnyException();
        assertThat(licenseActivationService.getLicenseVersion()).isZero();
        assertThat(licenseActivationService.isOfflineLicense()).isFalse();
    }

    @Test
    public void aNodeLockedByATransientLicenseErrorRecoversFromTheStoredSecretWithoutARestart() throws Exception {
        // The licence client treats everything except an in-grace connection error as critical, so a plain
        // server error, a rate limit or an instance-capacity rejection from its periodic instance check locks
        // the node - none of which say anything about this deployment's entitlement. Recovery used to come
        // from the process exiting and being restarted; with an in-process lock instead, retrying the stored
        // secret is the only thing left that can recover a fully valid licence.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        AbstractTbLicenseClient tbLicenseClient = mock(AbstractTbLicenseClient.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", tbLicenseClient);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        // The state boot really leaves behind: this node activated from exactly the secret that is stored, so
        // the stored secret is also the last one it attempted - and recordActivationAttempt always writes the
        // repeat-backoff deadline together with that secret, never one without the other. Setting only the
        // secret would leave the deadline at 0 and make the recovery below look instant, which is not the
        // latency a real node in this state experiences. The deadline's length is private to the service, so
        // any future instant stands in for "inside the window".
        ReflectionTestUtils.setField(licenseActivationService, "lastAttemptedSecret", "a-real-secret");
        ReflectionTestUtils.setField(licenseActivationService, "nextRepeatedSecretAttemptAtMs",
                System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("a-real-secret"));
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);

        LicenseException serverError = new LicenseException("License server unavailable", LicenseErrorCode.GENERAL_SERVER_ERROR);
        serverError.setIsCritical(true);
        licenseActivationService.onError(serverError);
        verify(tbLicenseClient).stop();

        // The licence server recovers seconds later, exactly as an outage does.
        AbstractTbLicenseClient recoveredClient = mock(AbstractTbLicenseClient.class);
        doReturn(recoveredClient).when(licenseActivationService).createLicenseClient("a-real-secret");

        // Still inside the backoff window: the stored secret is the one this node just attempted, so it is
        // not revalidated yet and the node stays locked. That is the recovery latency the interval buys.
        assertThat(licenseActivationService.isLicenseActivated()).isFalse();
        verify(licenseActivationService, never()).createLicenseClient("a-real-secret");

        // The backoff window elapses. The database throttle is cleared alongside it because the check above
        // armed it; on a real node the two simply expire on their own schedules.
        ReflectionTestUtils.setField(licenseActivationService, "nextRepeatedSecretAttemptAtMs", 0L);
        ReflectionTestUtils.setField(licenseActivationService, "nextReactivationAttemptAtMs", 0L);

        assertThat(licenseActivationService.isLicenseActivated()).isTrue();
    }

    @Test
    public void aRepeatedSecretIsRevalidatedAtMostOncePerBackoffWindow() throws Exception {
        // The other half of the retry: it must stay rate-limited. pickUpStoredLicenseSecret() skips the
        // database throttle and is reachable from an unauthenticated request path, so without a separate
        // ceiling on repeating an attempt, anonymous traffic would turn into one outbound licence-server
        // validation per request.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        ReflectionTestUtils.setField(licenseActivationService, "lastAttemptedSecret", "a-real-secret");
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("a-real-secret"));
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        doThrow(new LicenseException("still unavailable", LicenseErrorCode.GENERAL_SERVER_ERROR))
                .when(licenseActivationService).createLicenseClient("a-real-secret");

        assertThat(licenseActivationService.isLicenseActivated()).isFalse();
        licenseActivationService.pickUpStoredLicenseSecret();
        licenseActivationService.pickUpStoredLicenseSecret();

        verify(licenseActivationService, times(1)).createLicenseClient("a-real-secret");
    }

    @Test
    public void aKeylessNodeConvergesOntoASecretAPeerStoredLater() throws Exception {
        // A keyless node is activated, so nothing about the activation state alone makes it look for a
        // stored secret - but it is activated off the synthesized non-production client, which withholds
        // white-labeling, watermarks the deployment and is eventually revoked by the enforcer. On a cluster
        // where an administrator entered a licence key that the load balancer routed to another node, this
        // one has to converge on its own rather than wait to be restarted by hand.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        NonProductionTbLicenseClient keylessClient = mock(NonProductionTbLicenseClient.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", keylessClient);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("a-peer-secret"));
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        AbstractTbLicenseClient realClient = mock(AbstractTbLicenseClient.class);
        doReturn(realClient).when(licenseActivationService).createLicenseClient("a-peer-secret");
        assertThat(licenseActivationService.isNonProductionMode()).isTrue();

        // The act, not a check: an ordinary activation check is what discovers the peer's secret.
        boolean activated = licenseActivationService.isLicenseActivated();

        assertThat(activated).isTrue();
        assertThat(licenseActivationService.isNonProductionMode()).isFalse();
        verify(keylessClient).stop();
    }

    @Test
    public void revokingTheNonProductionEntitlementLocksAnActivatedInstance() {
        // Built on the keyless client rather than a plain licence client, because that is the only instance
        // the enforcer ever revokes: it is the exhausted non-production allowance being taken away, not a
        // licence.
        BasicLicenseActivationService licenseActivationService = new BasicLicenseActivationService();
        NonProductionTbLicenseClient keylessClient = mock(NonProductionTbLicenseClient.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", keylessClient);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);

        licenseActivationService.revokeNonProductionEntitlement();

        assertThat(licenseActivationService.isLicenseActivated()).isFalse();
        verify(keylessClient).stop();
    }

    @Test
    public void revokingTheNonProductionEntitlementTwiceStopsTheClientOnlyOnce() {
        // Idempotency matters here specifically because the scheduler that calls this retries every 15
        // minutes: without the guard, an already-locked instance would re-tear-down (and re-log) on every
        // single tick for as long as it stays up.
        BasicLicenseActivationService licenseActivationService = new BasicLicenseActivationService();
        NonProductionTbLicenseClient keylessClient = mock(NonProductionTbLicenseClient.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", keylessClient);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);

        licenseActivationService.revokeNonProductionEntitlement();
        licenseActivationService.revokeNonProductionEntitlement();

        verify(keylessClient, times(1)).stop();
    }

    @Test
    public void revokingTheNonProductionEntitlementOnAnUnactivatedInstanceIsANoOp() {
        // Reachable in principle if the scheduler ever raced ahead of boot; must not NPE on a client that
        // was never set.
        BasicLicenseActivationService licenseActivationService = new BasicLicenseActivationService();

        assertThatCode(licenseActivationService::revokeNonProductionEntitlement).doesNotThrowAnyException();
    }

    @Test
    public void revokingTheNonProductionEntitlementDoesNotLockALicensedInstance() {
        // The enforcer decides to revoke in three steps - it reads the mode, makes a database round trip for
        // the accrued uptime, and only then calls this - so its verdict can be stale by the time it lands. A
        // node that acquired a real licence inside that window (an administrator applying a key, or a peer's
        // secret picked up by the retry) must not be locked over an allowance that no longer governs it: the
        // administrator would watch the key they just entered be accepted and then apparently ignored, with
        // nothing but another activation able to undo it.
        BasicLicenseActivationService licenseActivationService = new BasicLicenseActivationService();
        AbstractTbLicenseClient licensedClient = mock(AbstractTbLicenseClient.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", licensedClient);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        // Wired so that a regression here fails as a plain assertion rather than as an NPE from the
        // re-activation attempt a wrongly locked instance would make on the check below.
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);

        licenseActivationService.revokeNonProductionEntitlement();

        assertThat(licenseActivationService.isLicenseActivated()).isTrue();
        verify(licensedClient, never()).stop();
    }

    @Test
    public void applyingALicenseKeyReactivatesAnInstanceLockedByAnExhaustedNonProductionAllowance() throws Exception {
        // Pins the headline "locked, not dead" property: recovery from an exhausted non-production allowance
        // must not require a restart, exactly like recovery from a critical runtime licence error already
        // does - applyLicenseKey does not know or care which locked it.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        // The keyless client, because that is the only one an exhausted allowance can be revoked from -
        // revokeNonProductionEntitlement re-checks the mode and declines to lock anything else.
        NonProductionTbLicenseClient exhaustedClient = mock(NonProductionTbLicenseClient.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", exhaustedClient);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        licenseActivationService.revokeNonProductionEntitlement();
        assertThat(licenseActivationService.isLicenseActivated()).isFalse();

        AbstractTbLicenseClient newClient = mock(AbstractTbLicenseClient.class);
        doReturn(newClient).when(licenseActivationService).createLicenseClient("a-real-secret");
        ReflectionTestUtils.setField(licenseActivationService, "eventPublisher", mock(ApplicationEventPublisher.class));
        // applyLicenseKey now measures the incoming licence against the live fleet before persisting it.
        wireCounts(licenseActivationService, 0L, 0L);

        licenseActivationService.applyLicenseKey("a-real-secret");

        assertThat(licenseActivationService.isLicenseActivated()).isTrue();
        verify(tbClusterStore).saveLicenseSecret("a-real-secret");
    }

    @Test
    public void repeatedActivationChecksDoNotHitTheDatabaseOnEveryCall() {
        // Once a keyless instance is locked by an exhausted allowance, isLicenseActivated() stays false
        // forever and is on the hot path of every management request - so without a throttle, this becomes
        // one SELECT per request forever rather than a rare occurrence.
        BasicLicenseActivationService licenseActivationService = new BasicLicenseActivationService();
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);

        for (int i = 0; i < 5; i++) {
            assertThat(licenseActivationService.isLicenseActivated()).isFalse();
        }

        verify(tbClusterStore, times(1)).getLicenseSecret();
    }

    @Test
    public void theActivationCheckRetriesTheDatabaseAgainAfterTheThrottleWindowElapses() {
        // The throttle must not turn into "disable the retry": a secret written to tb_cluster by another
        // node still has to be picked up, just not on every single request.
        BasicLicenseActivationService licenseActivationService = new BasicLicenseActivationService();
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        armThrottleWindow(licenseActivationService);

        // Simulates the throttle window having elapsed without sleeping in the test.
        ReflectionTestUtils.setField(licenseActivationService, "nextReactivationAttemptAtMs", 0L);
        assertThat(licenseActivationService.isLicenseActivated()).isFalse();

        verify(tbClusterStore, times(2)).getLicenseSecret();
    }

    @Test
    public void pickingUpAStoredSecretBypassesAnActiveThrottleWindow() {
        // pickUpStoredLicenseSecret() exists precisely for callers that cannot leave this node sitting on a
        // stale throttled answer, such as DefaultSystemSetupService.pollClaim() once it has already
        // established from the stored secret that the cluster is activated.
        BasicLicenseActivationService licenseActivationService = new BasicLicenseActivationService();
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        armThrottleWindow(licenseActivationService);

        licenseActivationService.pickUpStoredLicenseSecret();

        // One read to arm the throttle, and a second one that ignored it.
        verify(tbClusterStore, times(2)).getLicenseSecret();
    }

    @Test
    public void pickingUpAStoredSecretFindsASecretWrittenDuringTheThrottleWindow() throws Exception {
        // Pins the convergence property the throttle could otherwise defer for a whole window: an ordinary
        // check answers a stale false for a secret another node wrote in between, while this picks it up.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        armThrottleWindow(licenseActivationService);

        // Simulates another node writing a secret to tb_cluster within the throttle window.
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("a-real-secret"));
        doReturn(mock(AbstractTbLicenseClient.class)).when(licenseActivationService).createLicenseClient("a-real-secret");

        assertThat(licenseActivationService.isLicenseActivated()).isFalse();

        licenseActivationService.pickUpStoredLicenseSecret();

        assertThat(licenseActivationService.isLicenseActivated()).isTrue();
    }

    @Test
    public void anEnvironmentSecretThatFailsToValidateIsNeverPersisted() throws Exception {
        // The stored secret is durable shared state that nothing ever clears, and it is read elsewhere as
        // proof that the cluster holds a licence. Persisting one before it validates would let a typo'd,
        // expired or revoked environment value claim a licence for every node, permanently.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        ReflectionTestUtils.setField(licenseActivationService, "licenseSecret", "a-typo-secret");
        doThrow(new LicenseException("rejected", LicenseErrorCode.GENERAL_ERROR))
                .when(licenseActivationService).createLicenseClient("a-typo-secret");

        licenseActivationService.init();

        verify(tbClusterStore, never()).saveLicenseSecret(any());
        assertThat(licenseActivationService.isLicenseActivated()).isFalse();
    }

    @Test
    public void anEnvironmentSecretIsPersistedOnlyOnceItHasValidated() throws Exception {
        // The other side of the same property: seeding still happens, so peer nodes and later restarts pick
        // the secret up without the environment variable - just never before the client it produced is live.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        ReflectionTestUtils.setField(licenseActivationService, "licenseSecret", "a-real-secret");
        doReturn(mock(AbstractTbLicenseClient.class)).when(licenseActivationService).createLicenseClient("a-real-secret");

        licenseActivationService.init();

        assertThat(licenseActivationService.isLicenseActivated()).isTrue();
        var order = inOrder(licenseActivationService, tbClusterStore);
        order.verify(licenseActivationService).createLicenseClient("a-real-secret");
        order.verify(tbClusterStore).saveLicenseSecret("a-real-secret");
    }

    @Test
    public void aCriticalErrorFromASupersededClientIsIgnored() throws Exception {
        // A stopped client can still deliver a critical error from an instance check that was already in
        // flight when activate() swapped clients. Acting on it would tear down the licence the operator has
        // just applied - which is the whole reason each client gets a listener scoped to itself. Driven
        // through the listener rather than through onError(), because the guard lives only in the listener.
        BasicLicenseActivationService licenseActivationService = new BasicLicenseActivationService();
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getNonProductionUptimeMs()).thenReturn(0L);
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);

        AbstractTbLicenseClient supersededClient = licenseActivationService.createNonProductionClient();
        TbLicenseClientListener supersededListener =
                (TbLicenseClientListener) ReflectionTestUtils.getField(supersededClient, "listener");

        // The client the operator's key produced, published after the one above was built.
        AbstractTbLicenseClient currentClient = mock(AbstractTbLicenseClient.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", currentClient);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);

        LicenseException criticalError = new LicenseException("Simulated critical error", LicenseErrorCode.GENERAL_ERROR);
        criticalError.setIsCritical(true);
        supersededListener.onError(criticalError);

        assertThat(licenseActivationService.isLicenseActivated()).isTrue();
        assertThat(ReflectionTestUtils.getField(licenseActivationService, "tbLicenseClient")).isSameAs(currentClient);
        verify(currentClient, never()).stop();
    }

    @Test
    public void anEmptyLicenseKeyIsRejectedBeforeAnythingIsPersisted() {
        BasicLicenseActivationService licenseActivationService = new BasicLicenseActivationService();
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> licenseActivationService.applyLicenseKey(""));

        verify(tbClusterStore, never()).saveLicenseSecret(any());
    }

    @Test
    public void aWrongLicenseKeyCannotTakeDownAnAlreadyActivatedInstance() throws Exception {
        // The property applyLicenseKey validates before touching anything for: an administrator who pastes
        // the wrong key into a running, licensed instance gets an error, not an outage. Nothing else here
        // covers the invalid-key branch at all.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        AbstractTbLicenseClient runningClient = mock(AbstractTbLicenseClient.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", runningClient);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        doThrow(new LicenseException("rejected", LicenseErrorCode.GENERAL_ERROR))
                .when(licenseActivationService).createLicenseClient("a-wrong-secret");

        assertThatExceptionOfType(SubscriptionException.class)
                .isThrownBy(() -> licenseActivationService.applyLicenseKey("a-wrong-secret"))
                .satisfies(e -> assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.FEATURE_DISABLED));

        assertThat(licenseActivationService.isLicenseActivated()).isTrue();
        assertThat(ReflectionTestUtils.getField(licenseActivationService, "tbLicenseClient")).isSameAs(runningClient);
        verify(runningClient, never()).stop();
        verify(tbClusterStore, never()).saveLicenseSecret(any());
    }

    @Test
    public void aValidatedClientIsNotLeakedWhenTheSecretCannotBePersisted() throws Exception {
        // The key validated, so a fully initialized client - with its own polling loop - already exists, but
        // it is never published because the write that has to precede publication failed. Without the
        // stopClient on that path it would keep running with nothing referencing it, which is the kind of
        // leak nothing else would ever notice.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        AbstractTbLicenseClient runningClient = mock(AbstractTbLicenseClient.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", runningClient);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        AbstractTbLicenseClient validatedClient = mock(AbstractTbLicenseClient.class);
        doReturn(validatedClient).when(licenseActivationService).createLicenseClient("a-real-secret");
        doThrow(new DataAccessResourceFailureException("the database is unreachable"))
                .when(tbClusterStore).saveLicenseSecret("a-real-secret");
        // applyLicenseKey now measures the incoming licence against the live fleet before persisting it.
        wireCounts(licenseActivationService, 0L, 0L);

        assertThatThrownBy(() -> licenseActivationService.applyLicenseKey("a-real-secret"))
                .isInstanceOf(DataAccessResourceFailureException.class);

        verify(validatedClient).stop();
        // And the instance is left exactly as it was, on the client it was already running.
        assertThat(ReflectionTestUtils.getField(licenseActivationService, "tbLicenseClient")).isSameAs(runningClient);
        verify(runningClient, never()).stop();
    }

    @Test
    public void aKeylessActivationLeavesAPendingClaimTokenInPlace() throws Exception {
        // A keyless client is not a licence that makes a pending claim moot: the operator is mid-way through
        // using that token, and clearing it shows them "link expired" on their next poll.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        ReflectionTestUtils.setField(licenseActivationService, "nonProductionUse", true);
        ReflectionTestUtils.setField(licenseActivationService, "eventPublisher", mock(ApplicationEventPublisher.class));
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        doReturn(mock(NonProductionTbLicenseClient.class)).when(licenseActivationService).createNonProductionClient();

        licenseActivationService.init();

        verify(tbClusterStore, never()).forceClearLicenseClaimToken();
    }

    @Test
    public void aLicensedActivationStillRetiresTheClaimToken() throws Exception {
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        ReflectionTestUtils.setField(licenseActivationService, "eventPublisher", mock(ApplicationEventPublisher.class));
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        // applyLicenseKey now measures the incoming licence against the live fleet before persisting it.
        wireCounts(licenseActivationService, 0L, 0L);
        doReturn(mock(AbstractTbLicenseClient.class)).when(licenseActivationService).createLicenseClient("secret-1");

        licenseActivationService.applyLicenseKey("secret-1");

        verify(tbClusterStore).forceClearLicenseClaimToken();
    }

    @Test
    public void aStoredSecretWinsOverTheEnvironmentVariableTheyDisagreeOn() throws Exception {
        // A node that still carries an old TB_LICENSE_SECRET in its compose file must activate off the
        // secret the rest of the cluster is already using, not off the stale variable - otherwise one node
        // in a cluster silently runs on a different licence from every other.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("a-stored-secret"));
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        ReflectionTestUtils.setField(licenseActivationService, "licenseSecret", "an-env-secret");
        doReturn(mock(AbstractTbLicenseClient.class)).when(licenseActivationService).createLicenseClient("a-stored-secret");

        licenseActivationService.init();

        assertThat(licenseActivationService.isLicenseActivated()).isTrue();
        verify(licenseActivationService).createLicenseClient("a-stored-secret");
        verify(licenseActivationService, never()).createLicenseClient("an-env-secret");
    }

    @Test
    public void aSecretAlreadyInTheDatabaseIsNotSeededAgain() throws Exception {
        // Seeding exists to publish a secret nothing has stored yet. Re-writing one that is already there on
        // every boot would be a pointless write, and on a node whose environment variable disagrees it would
        // be the stale value overwriting the cluster's real one.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("a-stored-secret"));
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        ReflectionTestUtils.setField(licenseActivationService, "licenseSecret", "an-env-secret");
        doReturn(mock(AbstractTbLicenseClient.class)).when(licenseActivationService).createLicenseClient("a-stored-secret");

        licenseActivationService.init();

        verify(tbClusterStore, never()).saveLicenseSecret(any());
    }

    @Test
    public void aDatabaseThatCannotAnswerLeavesTheInstanceLockedRatherThanFailingTheBoot() {
        // A broken or outdated schema has to leave a diagnosable instance behind, not a context that refuses
        // to start: an operator can read the log and re-enter a key on the former, and has nothing at all on
        // the latter.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret())
                .thenThrow(new DataAccessResourceFailureException("the license_secret column does not exist"));
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);

        assertThatCode(licenseActivationService::init).doesNotThrowAnyException();

        assertThat(licenseActivationService.isLicenseActivated()).isFalse();
    }

    @Test
    public void aBootThatCannotReadTheClusterRowStillActivatesFromTheEnvironmentSecret() throws Exception {
        // A missing tb_cluster row - a restored dump, a half-run install - must not cost a deployment that
        // carries TB_LICENSE_SECRET its licence: boot can only acquire one, never give one up, so an
        // unreadable row degrades to "nothing stored" and the environment fallback still applies. Refusing
        // here would leave the whole cluster unactivated with no way back short of repairing the row.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret())
                .thenThrow(new IllegalStateException("Failed to read the license_secret: table tb_cluster is empty"));
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        ReflectionTestUtils.setField(licenseActivationService, "licenseSecret", "an-env-secret");
        doReturn(mock(AbstractTbLicenseClient.class)).when(licenseActivationService).createLicenseClient("an-env-secret");

        licenseActivationService.init();

        assertThat(licenseActivationService.isLicenseActivated()).isTrue();
        verify(licenseActivationService).createLicenseClient("an-env-secret");
        // Activated, but nothing written back: a read that failed is no evidence the cluster holds nothing,
        // and seeding off it would let a node with a stale environment secret republish it cluster-wide.
        verify(tbClusterStore, never()).saveLicenseSecret(any());
    }

    @Test
    public void aRetryThatCannotReadTheClusterRowStillActivatesFromTheEnvironmentSecret() throws Exception {
        // The same for the five-minute retry: a node that boots before the row is readable would otherwise
        // bail at the read on every tick and never converge.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret())
                .thenThrow(new IllegalStateException("Failed to read the license_secret: table tb_cluster is empty"));
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        ReflectionTestUtils.setField(licenseActivationService, "licenseSecret", "an-env-secret");
        doReturn(mock(AbstractTbLicenseClient.class)).when(licenseActivationService).createLicenseClient("an-env-secret");

        licenseActivationService.retryActivation();

        assertThat(licenseActivationService.isLicenseActivated()).isTrue();
        // And the same suppression as on boot, which matters more here: this runs every five minutes on a
        // live cluster whose licence secret is already in place.
        verify(tbClusterStore, never()).saveLicenseSecret(any());
    }

    @Test
    public void anActivatedNodeAdoptsAReplacedLicenseKeyOnTheRetryTick() throws Exception {
        // An activated node never re-reads the stored secret on any request path, so a key replaced on
        // another node would only reach it on a restart without this.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        AbstractTbLicenseClient currentClient = mock(AbstractTbLicenseClient.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", currentClient);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        ReflectionTestUtils.setField(licenseActivationService, "lastAttemptedSecret", "the-old-secret");
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("the-new-secret"));
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        AbstractTbLicenseClient newClient = mock(AbstractTbLicenseClient.class);
        doReturn(newClient).when(licenseActivationService).createLicenseClient("the-new-secret");

        licenseActivationService.retryActivation();

        assertThat(ReflectionTestUtils.getField(licenseActivationService, "tbLicenseClient")).isSameAs(newClient);
        assertThat(licenseActivationService.getClient()).isSameAs(newClient);
        verify(currentClient).stop();
    }

    @Test
    public void anActivatedNodeWhoseStoredSecretIsUnchangedDoesNoWorkOnTheTick() throws Exception {
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        AbstractTbLicenseClient currentClient = mock(AbstractTbLicenseClient.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", currentClient);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        ReflectionTestUtils.setField(licenseActivationService, "lastAttemptedSecret", "the-secret");
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("the-secret"));
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);

        // The database read belongs to the tick alone: a request on an activated node must not pay for it.
        licenseActivationService.isLicenseActivated();
        verify(tbClusterStore, never()).getLicenseSecret();

        licenseActivationService.retryActivation();

        verify(tbClusterStore).getLicenseSecret();
        verify(licenseActivationService, never()).createLicenseClient(any());
        verify(currentClient, never()).stop();
        assertThat(ReflectionTestUtils.getField(licenseActivationService, "tbLicenseClient")).isSameAs(currentClient);
    }

    @Test
    public void aReplacedSecretThatFailsValidationLeavesTheRunningClientInPlace() throws Exception {
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        AbstractTbLicenseClient currentClient = mock(AbstractTbLicenseClient.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", currentClient);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        ReflectionTestUtils.setField(licenseActivationService, "lastAttemptedSecret", "the-old-secret");
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("the-new-secret"));
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        doThrow(new LicenseException("rejected", LicenseErrorCode.GENERAL_ERROR))
                .when(licenseActivationService).createLicenseClient("the-new-secret");

        licenseActivationService.retryActivation();

        assertThat(ReflectionTestUtils.getField(licenseActivationService, "tbLicenseClient")).isSameAs(currentClient);
        assertThat(licenseActivationService.isLicenseActivated()).isTrue();
        verify(currentClient, never()).stop();
    }

    @Test
    public void reconcileDropsTheLicenceWhenThePeerClearedTheStoredSecret() {
        // A cleared secret used to reach nobody: the replaced-secret-only predecessor returned early on an
        // empty one, so the node that was told to clear kept serving the licence it had until it restarted.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        AbstractTbLicenseClient tbLicenseClient = mock(AbstractTbLicenseClient.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", tbLicenseClient);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        ReflectionTestUtils.setField(licenseActivationService, "lastAttemptedSecret", "secret-1");
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        wireReconciliationListener(licenseActivationService);

        licenseActivationService.reconcileLicenseState();

        verify(tbLicenseClient).stop();
        assertThat(ReflectionTestUtils.getField(licenseActivationService, "tbLicenseClient")).isNull();
        assertThat(ReflectionTestUtils.getField(licenseActivationService, "licenseActivated")).isEqualTo(false);
    }

    @Test
    public void reconcileNeverResurrectsAClearedSecretFromTheEnvironment() throws Exception {
        // resolveEffectiveSecret() falls back to TB_LICENSE_SECRET. Reconverging through it would make a peer
        // with that variable set answer a clear broadcast by re-activating on the old env key AND re-seeding it
        // into tb_cluster, undoing the clear for the whole cluster.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        ReflectionTestUtils.setField(licenseActivationService, "licenseSecret", "env-secret");
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", mock(AbstractTbLicenseClient.class));
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        ReflectionTestUtils.setField(licenseActivationService, "lastAttemptedSecret", "env-secret");
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        wireReconciliationListener(licenseActivationService);

        licenseActivationService.reconcileLicenseState();

        verify(tbClusterStore, never()).saveLicenseSecret(anyString());
        verify(licenseActivationService, never()).createLicenseClient(anyString());
        assertThat(ReflectionTestUtils.getField(licenseActivationService, "licenseActivated")).isEqualTo(false);
    }

    @Test
    public void reconcileAdoptsASecretAPeerReplaced() throws Exception {
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", mock(AbstractTbLicenseClient.class));
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        ReflectionTestUtils.setField(licenseActivationService, "lastAttemptedSecret", "secret-1");
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("secret-2"));
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        AbstractTbLicenseClient replacement = mock(AbstractTbLicenseClient.class);
        doReturn(replacement).when(licenseActivationService).createLicenseClient("secret-2");

        licenseActivationService.reconcileLicenseState();

        assertThat(ReflectionTestUtils.getField(licenseActivationService, "tbLicenseClient")).isSameAs(replacement);
        assertThat(ReflectionTestUtils.getField(licenseActivationService, "lastAttemptedSecret")).isEqualTo("secret-2");
        // A reader, never a writer: the node that changed the secret already stored it.
        verify(tbClusterStore, never()).saveLicenseSecret(anyString());
    }

    @Test
    public void reconcileRetriesTheSameSecretOnALockedNodeDespiteTheBackoff() throws Exception {
        // The per-secret backoff caps outbound validations from the unauthenticated request path. A
        // reconvergence is an operator-initiated event and must not be held back by it - a locked node that
        // would otherwise wait out REPEATED_SECRET_RETRY_INTERVAL_MS is the whole failure this closes.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", false);
        ReflectionTestUtils.setField(licenseActivationService, "lastAttemptedSecret", "secret-1");
        ReflectionTestUtils.setField(licenseActivationService, "nextRepeatedSecretAttemptAtMs",
                System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("secret-1"));
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        doReturn(mock(AbstractTbLicenseClient.class)).when(licenseActivationService).createLicenseClient("secret-1");

        licenseActivationService.reconcileLicenseState();

        assertThat(ReflectionTestUtils.getField(licenseActivationService, "licenseActivated")).isEqualTo(true);
    }

    @Test
    public void reconcileLeavesAKeylessNodeKeylessWhenNothingIsStored() {
        // NON_PRODUCTION_USE=true with no stored secret is a supported steady state, not a licence that went
        // missing, so reconvergence must not tear it down.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        ReflectionTestUtils.setField(licenseActivationService, "nonProductionUse", true);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", mock(NonProductionTbLicenseClient.class));
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        wireReconciliationListener(licenseActivationService);

        licenseActivationService.reconcileLicenseState();

        assertThat(licenseActivationService.isNonProductionMode()).isTrue();
        assertThat(ReflectionTestUtils.getField(licenseActivationService, "licenseActivated")).isEqualTo(true);
    }

    @Test
    public void aKeylessConfiguredNodeThatCameUpUnactivatedSettlesOnItsKeylessClient() {
        // NON_PRODUCTION_USE is set but boot found no secret and could not activate - a licence server that
        // was unreachable, or a tb_cluster row that was not readable yet. Nothing else grants the keyless
        // client afterwards, so without this the node stays locked until it is restarted by hand.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        ReflectionTestUtils.setField(licenseActivationService, "nonProductionUse", true);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", null);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", false);
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        when(tbClusterStore.getNonProductionUptimeMs()).thenReturn(0L);
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        wireReconciliationListener(licenseActivationService);

        licenseActivationService.retryActivation();

        assertThat(licenseActivationService.isNonProductionMode()).isTrue();
        assertThat(ReflectionTestUtils.getField(licenseActivationService, "licenseActivated")).isEqualTo(true);
    }

    @Test
    public void reconcileTellsTheSetupServiceToDropItsLatchesWhenTheLicenceGoesAway() {
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", mock(AbstractTbLicenseClient.class));
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        LicenseStateReconciliationListener listener = wireReconciliationListener(licenseActivationService);

        licenseActivationService.reconcileLicenseState();

        verify(listener).onLicenseStateReconciled();
    }

    @Test
    public void reconcileTellsTheSetupServiceEvenWhenThereWasNoLicenceToGiveUp() {
        // An unactivated node is precisely where the setup latches go stale: licenseSecretStored is set from
        // pollClaim()'s not-activated branch, so a node that is locked or whose boot activation failed carries
        // it. Skipping the reset there leaves getState() answering LICENSE_REQUIRED while pollClaim() answers
        // ACTIVATED, and the wizard never offers the activation flow again until the node restarts.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", false);
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        LicenseStateReconciliationListener listener = wireReconciliationListener(licenseActivationService);

        licenseActivationService.reconcileLicenseState();

        verify(listener).onLicenseStateReconciled();
    }

    @Test
    public void anUnreadableClusterRowLeavesTheNodeOnTheLicenceItHolds() {
        // An absent tb_cluster row is not a cleared secret, and the store refuses to report it as one. The
        // tick must treat that refusal as no evidence at all: a stale replica after a failover, a restored
        // dump or a half-run install would otherwise drop the licence on every node of the cluster at once.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        AbstractTbLicenseClient tbLicenseClient = mock(AbstractTbLicenseClient.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", tbLicenseClient);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        ReflectionTestUtils.setField(licenseActivationService, "lastAttemptedSecret", "secret-1");
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        when(tbClusterStore.getLicenseSecret()).thenThrow(new IllegalStateException("table tb_cluster is empty"));
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        LicenseStateReconciliationListener listener = wireReconciliationListener(licenseActivationService);

        licenseActivationService.reconcileLicenseState();

        assertThat(ReflectionTestUtils.getField(licenseActivationService, "tbLicenseClient")).isSameAs(tbLicenseClient);
        assertThat(ReflectionTestUtils.getField(licenseActivationService, "licenseActivated")).isEqualTo(true);
        verify(tbLicenseClient, never()).stop();
        // Nor are the setup latches reset: nothing was learned about the cluster's licence.
        verify(listener, never()).onLicenseStateReconciled();
    }

    @Test
    public void aLicenceClearedWhileTheReconvergenceWasValidatingIsNotPublished() throws Exception {
        // Validating a secret is a 20s HTTP call made outside every lock, so a peer's clear can land while it
        // is out. activate()'s superseded check cannot catch that one - it answers false for an empty stored
        // secret - so this node would publish the secret it adopted and keep serving a revoked licence.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        AbstractTbLicenseClient currentClient = mock(AbstractTbLicenseClient.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", currentClient);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        ReflectionTestUtils.setField(licenseActivationService, "lastAttemptedSecret", "secret-1");
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        // The clear lands between the two reads: the first still sees the peer's replacement secret.
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("secret-2"), Optional.empty());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        AbstractTbLicenseClient adoptedClient = mock(AbstractTbLicenseClient.class);
        doReturn(adoptedClient).when(licenseActivationService).createLicenseClient("secret-2");
        LicenseStateReconciliationListener listener = wireReconciliationListener(licenseActivationService);

        licenseActivationService.reconcileLicenseState();

        verify(adoptedClient).stop();
        // And the clear is honoured rather than only skipped: the licence this node was serving is gone too.
        verify(currentClient).stop();
        assertThat(ReflectionTestUtils.getField(licenseActivationService, "tbLicenseClient")).isNull();
        assertThat(ReflectionTestUtils.getField(licenseActivationService, "licenseActivated")).isEqualTo(false);
        verify(listener).onLicenseStateReconciled();
    }

    @Test
    public void applyingALicenseKeyTellsTheClusterToReconverge() throws Exception {
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        TbClusterStore tbClusterStore = mock(TbClusterStore.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ReflectionTestUtils.setField(licenseActivationService, "eventPublisher", eventPublisher);
        // applyLicenseKey now measures the incoming licence against the live fleet before persisting it.
        wireCounts(licenseActivationService, 0L, 0L);
        doReturn(mock(AbstractTbLicenseClient.class)).when(licenseActivationService).createLicenseClient("secret-1");

        licenseActivationService.applyLicenseKey("secret-1");

        verify(eventPublisher).publishEvent(any(LicenseStateChangedEvent.class));
    }

    @Test
    public void clearingTheLicenseTellsTheClusterToReconverge() {
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", mock(TbClusterStore.class));
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ReflectionTestUtils.setField(licenseActivationService, "eventPublisher", eventPublisher);
        ReflectionTestUtils.setField(licenseActivationService, "instanceDataFilePath", "target/no-such-instance-license.data");

        licenseActivationService.clearLicense();

        verify(eventPublisher).publishEvent(any(LicenseStateChangedEvent.class));
    }

    @Test
    public void lockingDoesNotTellTheClusterAnything() {
        // lock() writes nothing to tb_cluster, so a healthy peer would reconverge onto the same secret and
        // return - while an unhealthy one would revalidate against a licence server that is very likely the
        // reason this node locked. Correlated locking across N nodes would make that N squared.
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", mock(AbstractTbLicenseClient.class));
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ReflectionTestUtils.setField(licenseActivationService, "eventPublisher", eventPublisher);

        LicenseException criticalError = new LicenseException("boom", LicenseErrorCode.GENERAL_SERVER_ERROR);
        criticalError.setIsCritical(true);
        licenseActivationService.onError(criticalError);

        verify(eventPublisher, never()).publishEvent(any(LicenseStateChangedEvent.class));
    }


    private static final String ONLINE_SECRET = "AAAA-BBBB-CCCC-DDDD";
    private static final String OFFLINE_BLOB = "an-offline-license-blob";
    private static final String NEW_SECRET = "the-new-secret";

    @Test
    public void previewRefusesAKeyThatCoversFewerDevicesThanTheDeploymentHolds() {
        // Refused rather than flagged, so an administrator is stopped before the swap instead of after it.
        // The sentence is the one the boot gate and the upgrade pre-flight use; the condition is this site's
        // own - see verifyFleetFitsLicense.
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(4812L, 0L);

        assertThatThrownBy(() -> previewOnline(licenseActivationService,
                planWith(PlanDataConstants.MAX_DEVICES_KEY, 100L)))
                .isInstanceOf(SubscriptionException.class)
                .hasMessage(LicenseCapacity.capExceededMessage(CappedEntity.DEVICE, 4812L, 100L));
    }

    @Test
    public void previewRefusesAKeyThatCoversFewerAssetsThanTheDeploymentHolds() {
        // The asset cap is checked separately from the device one, so a key that fits the devices and not the
        // assets is still refused.
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(0L, 900L);

        assertThatThrownBy(() -> previewOnline(licenseActivationService,
                planWith(PlanDataConstants.MAX_ASSETS_KEY, 100L)))
                .isInstanceOf(SubscriptionException.class)
                .hasMessage(LicenseCapacity.capExceededMessage(CappedEntity.ASSET, 900L, 100L));
    }

    @Test
    public void previewRefusesAV2KeyThatGrantsNoDevicesAtAllToANonEmptyFleet() {
        // The strictest key there is, and the one the check is blindest to if it reads the reported quota:
        // that answers 0 both for "unlimited" and for "none", so a v2 zero has to be read off the plan.
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(1L, 0L);

        assertThatThrownBy(() -> previewOnline(licenseActivationService,
                planWith(PlanDataConstants.MAX_DEVICES_KEY, 0L, 2)))
                .isInstanceOf(SubscriptionException.class)
                .hasMessage(LicenseCapacity.capExceededMessage(CappedEntity.DEVICE, 1L, 0L));
    }

    @Test
    public void documentsThatADeploymentHoldingNoneFitsAV2KeyThatGrantsNone() {
        // Documentation, not regression cover: it passes on the pre-Ruling-17 code too, since a deployment
        // holding nothing is refused by no rule. What it pins is the boundary inside exceedsCap - holding
        // exactly what the licence covers is compliant, zero included - so a > that became a >= fails here.
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(0L, 0L);

        SubscriptionInfo preview = previewOnline(licenseActivationService,
                planWith(PlanDataConstants.MAX_DEVICES_KEY, 0L, 2));

        assertThat(preview.getMaxDevices()).isZero();
    }

    @Test
    public void documentsThatAV1ZeroDeviceQuotaStillMeansUnlimited() {
        // Documentation, not regression cover: the pre-Ruling-17 rule accepted this key too, for its own
        // reason. What it guards against is a careless fix - one that reads the v2 rule into a v1 licence,
        // which predates the quota keys, and so refuses every v1 licence on every non-empty deployment.
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(4812L, 0L);

        SubscriptionInfo preview = previewOnline(licenseActivationService,
                planWith(PlanDataConstants.MAX_DEVICES_KEY, 0L, 1));

        assertThat(preview.getMaxDevices()).isEqualTo(PlanDataConstants.UNLIMITED_QUOTA);
    }

    @Test
    public void previewDescribesTheIncomingKeyAndNothingElse() {
        // Two licences must not be mixed into one body. What only the running deployment can answer stays
        // unset: the entity counts are the caller's own, and the AI credits already spent were spent against
        // the licence being replaced.
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(12L, 3L);

        SubscriptionInfo preview = previewOnline(licenseActivationService,
                planWith(PlanDataConstants.MAX_DEVICES_KEY, 100L));

        assertThat(preview.getMaxDevices()).isEqualTo(100L);
        assertThat(preview.getDevicesCount()).isZero();
        assertThat(preview.getAssetsCount()).isZero();
        assertThat(preview.getEdgesCount()).isZero();
        assertThat(preview.getUsedAiCredits()).isZero();
    }

    @Test
    public void previewReportsThePreviewedKeysAiCredits() {
        // The AI grant is the previewed key's own, read off its plan data - so the preview answers "what this
        // key gives you" rather than leaving the figure at zero. Reported in credits, not in the 1M-credit
        // packs the plan counts, because used credits are reported raw.
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(0L, 0L);

        SubscriptionInfo preview = previewOnline(licenseActivationService,
                planWith(PlanDataConstants.MAX_AI_CREDITS_KEY, 3L));

        assertThat(preview.getMaxAiCredits()).isEqualTo(3L * PlanDataConstants.AI_CREDITS_UNIT);
    }

    @Test
    public void previewClampsTheAiCreditGrantToWhatTheMintedTokenAllows() {
        // The licence server clamps the AI token's limit to Integer.MAX_VALUE, so a pack count above that
        // reported verbatim would promise credits the token refuses to spend.
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(0L, 0L);

        SubscriptionInfo preview = previewOnline(licenseActivationService,
                planWith(PlanDataConstants.MAX_AI_CREDITS_KEY, 100_000L));

        assertThat(preview.getMaxAiCredits()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void previewReportsNoAiCreditsForAPlanThatMintsNoToken() {
        // A non-positive pack count is no AI at all - the licence server mints no token for it - rather than
        // unlimited AI, so it must not be reported as the unlimited sentinel or as a negative credit grant.
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(0L, 0L);

        SubscriptionInfo preview = previewOnline(licenseActivationService,
                planWith(PlanDataConstants.MAX_AI_CREDITS_KEY, -1L));

        assertThat(preview.getMaxAiCredits()).isZero();
    }

    @Test
    public void theEdgeQuotaCrossesIntoThePreviewAndDecidesNothing() {
        // The edge quota is a soft, creation-time cap under every licence - the same asymmetry the boot gate
        // and the upgrade pre-flight record - so it is reported and never enforced here.
        //
        // Named for what it pins rather than for an over-quota edge count, because no such fixture can be
        // built: the service holds no edge collaborator at all, so there is no edge count for a check to read.
        // That absence is the exemption, and it is structural rather than conditional - which is also why this
        // test cannot fail on an edge check the way the device tests fail on a device one.
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(0L, 0L);

        SubscriptionInfo preview = previewOnline(licenseActivationService,
                planWith(PlanDataConstants.MAX_EDGES_KEY, 5L, 2));

        assertThat(preview.getMaxEdges()).isEqualTo(5L);
    }

    @Test
    public void previewReportsAnUnlimitedInstanceQuotaAsTheSentinel() {
        // Passed through verbatim, so the licence page can tell an unlimited instance quota from a plan that
        // grants no production instance at all - a development plan ships exactly that, as maxinstances = 0.
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(0L, 0L);

        SubscriptionInfo preview = previewOnline(licenseActivationService,
                planWith(PlanDataConstants.MAX_INSTANCES_KEY, PlanDataConstants.UNLIMITED_INSTANCES));

        assertThat(preview.getMaxInstances()).isEqualTo(PlanDataConstants.UNLIMITED_INSTANCES);
    }

    @Test
    public void previewReportsAnInstanceQuotaOfNoneAsZero() {
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(0L, 0L);

        SubscriptionInfo preview = previewOnline(licenseActivationService,
                planWith(PlanDataConstants.MAX_INSTANCES_KEY, 0L));

        assertThat(preview.getMaxInstances()).isZero();
    }

    @Test
    public void previewReportsAFiniteInstanceQuotaAsItStands() {
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(0L, 0L);

        SubscriptionInfo preview = previewOnline(licenseActivationService,
                planWith(PlanDataConstants.MAX_INSTANCES_KEY, 1L));

        assertThat(preview.getMaxInstances()).isEqualTo(1L);
    }

    @Test
    public void previewReportsAClusterMismatchWithoutRetryingOnline() {
        // An offline licence that names another cluster is a verdict, not a hint that this might be an online
        // key: retrying it against the portal would answer INVALID_LICENSE_SECRET and hide the real reason.
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(0L, 0L);
        try (MockedStatic<OfflineTbLicenseClient> offlineClient = mockStatic(OfflineTbLicenseClient.class);
             MockedStatic<TbLicenseClient> portal = mockStatic(TbLicenseClient.class)) {
            offlineClient.when(() -> OfflineTbLicenseClient.preview(eq(OFFLINE_BLOB), any(), anyLong()))
                    .thenThrow(new LicenseException("Unknown cluster Id!", LicenseErrorCode.INVALID_CLUSTER_ID_CHECK));

            assertThatThrownBy(() -> licenseActivationService.previewLicenseKey(OFFLINE_BLOB))
                    .isInstanceOf(SubscriptionException.class)
                    .hasMessage(LicenseKeyRejections.BOUND_ELSEWHERE);

            portal.verifyNoInteractions();
        }
    }

    @Test
    public void anOfflineBlobIsPreviewedWithoutCallingThePortal() {
        // An offline licence is read locally by design: the deployment using one may have no outbound access
        // at all, and the portal has no verdict to give on a key it never opened a session for.
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(0L, 0L);
        try (MockedStatic<OfflineTbLicenseClient> offlineClient = mockStatic(OfflineTbLicenseClient.class);
             MockedStatic<TbLicenseClient> portal = mockStatic(TbLicenseClient.class)) {
            offlineClient.when(() -> OfflineTbLicenseClient.preview(eq(OFFLINE_BLOB), any(), anyLong()))
                    .thenReturn(new SubscriptionPreviewResponse(planWith(PlanDataConstants.MAX_DEVICES_KEY, 100L),
                            new SubscriptionData()));

            SubscriptionInfo preview = licenseActivationService.previewLicenseKey(OFFLINE_BLOB);

            assertThat(preview.isOffline()).isTrue();
            assertThat(preview.getMaxDevices()).isEqualTo(100L);
            portal.verifyNoInteractions();
        }
    }

    @Test
    public void previewDoesNotPublishOrReplaceTheRunningClient() {
        AbstractTbLicenseClient running = mock(AbstractTbLicenseClient.class);
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(0L, 0L);
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", running);
        TbClusterStore tbClusterStore =
                (TbClusterStore) ReflectionTestUtils.getField(licenseActivationService, "tbClusterStore");

        previewOnline(licenseActivationService, planWith(PlanDataConstants.MAX_DEVICES_KEY, 100L));

        assertThat(licenseActivationService.getClient()).isSameAs(running);
        verify(tbClusterStore, never()).saveLicenseSecret(anyString());
    }

    @Test
    public void previewingTheKeyInForceDescribesItRatherThanSeekingASlotForIt() {
        // The slot a full preview looks for is the one this very node already holds, so a full licence would
        // come back refused for the key the node is running on.
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(0L, 0L);
        markRunningOn(licenseActivationService, mock(AbstractTbLicenseClient.class), ONLINE_SECRET);
        try (MockedStatic<TbLicenseClient> portal = mockStatic(TbLicenseClient.class)) {
            portal.when(() -> TbLicenseClient.previewSubscription(isNull(), eq(ONLINE_SECRET), anyLong(), any(), eq(true)))
                    .thenReturn(new SubscriptionPreviewResponse(planWith(PlanDataConstants.MAX_DEVICES_KEY, 100L),
                            new SubscriptionData()));

            SubscriptionInfo preview = licenseActivationService.previewLicenseKey(ONLINE_SECRET);

            assertThat(preview.getMaxDevices()).isEqualTo(100L);
            portal.verify(() -> TbLicenseClient.previewSubscription(isNull(), eq(ONLINE_SECRET), anyLong(), any()),
                    never());
        }
    }

    @Test
    public void previewingTheKeyInForceDoesNotMeasureTheFleetAgainstIt() {
        // A plan downgraded in the portal and picked up on the next check-in leaves the fleet over the quota
        // of the very licence this node is running. Refusing it here would refuse what Replace short-circuits
        // into a no-op, which is the disagreement between the two this whole path exists to prevent.
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(4812L, 0L);
        markRunningOn(licenseActivationService, mock(AbstractTbLicenseClient.class), ONLINE_SECRET);
        try (MockedStatic<TbLicenseClient> portal = mockStatic(TbLicenseClient.class)) {
            portal.when(() -> TbLicenseClient.previewSubscription(isNull(), eq(ONLINE_SECRET), anyLong(), any(), eq(true)))
                    .thenReturn(new SubscriptionPreviewResponse(planWith(PlanDataConstants.MAX_DEVICES_KEY, 100L),
                            new SubscriptionData()));

            SubscriptionInfo preview = licenseActivationService.previewLicenseKey(ONLINE_SECRET);

            assertThat(preview.getMaxDevices()).isEqualTo(100L);
        }
    }

    @Test
    public void previewingAKeyThisNodeIsNotRunningStillSeeksASlotForIt() {
        // Both skips are scoped by the secret, not by there being a licence at all: an activated node asked
        // about any other key gets the preview every unactivated node gets.
        BasicLicenseActivationService licenseActivationService = serviceWithCounts(0L, 0L);
        markRunningOn(licenseActivationService, mock(AbstractTbLicenseClient.class), "the-key-this-node-runs");
        try (MockedStatic<TbLicenseClient> portal = mockStatic(TbLicenseClient.class)) {
            portal.when(() -> TbLicenseClient.previewSubscription(isNull(), eq(ONLINE_SECRET), anyLong(), any()))
                    .thenReturn(new SubscriptionPreviewResponse(planWith(PlanDataConstants.MAX_DEVICES_KEY, 100L),
                            new SubscriptionData()));

            SubscriptionInfo preview = licenseActivationService.previewLicenseKey(ONLINE_SECRET);

            assertThat(preview.getMaxDevices()).isEqualTo(100L);
            portal.verify(() -> TbLicenseClient.previewSubscription(isNull(), eq(ONLINE_SECRET), anyLong(), any(), eq(true)),
                    never());
        }
    }

    @Test
    public void applyReportsWhyTheKeyWasRefused() throws Exception {
        BasicLicenseActivationService licenseActivationService = spy(serviceWithCounts(0L, 0L));
        TbClusterStore tbClusterStore =
                (TbClusterStore) ReflectionTestUtils.getField(licenseActivationService, "tbClusterStore");
        doThrow(new LicenseException("raw", LicenseErrorCode.ACTIVE_INSTANCES_CAPACITY_EXCEEDED))
                .when(licenseActivationService).createLicenseClient(anyString());

        assertThatThrownBy(() -> licenseActivationService.applyLicenseKey(ONLINE_SECRET))
                .isInstanceOf(SubscriptionException.class)
                .hasMessage(LicenseKeyRejections.NO_AVAILABLE_INSTANCE);
        verify(tbClusterStore, never()).saveLicenseSecret(anyString());
    }

    /**
     * A service that can answer a preview: the device and asset counts the cap check reads, and the cluster id
     * the portal call needs. It never goes through Spring, so each has to be wired by hand.
     */
    private BasicLicenseActivationService serviceWithCounts(long devices, long assets) {
        BasicLicenseActivationService licenseActivationService = new BasicLicenseActivationService();
        DeviceService deviceService = mock(DeviceService.class);
        when(deviceService.countDevices()).thenReturn(devices);
        AssetService assetService = mock(AssetService.class);
        when(assetService.countAssets()).thenReturn(assets);
        TbLicenseCtx licenseCtx = mock(TbLicenseCtx.class);
        when(licenseCtx.getClusterId()).thenReturn(UUID.randomUUID());
        ReflectionTestUtils.setField(licenseActivationService, "deviceService", deviceService);
        ReflectionTestUtils.setField(licenseActivationService, "assetService", assetService);
        ReflectionTestUtils.setField(licenseActivationService, "licenseCtx", licenseCtx);
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", mock(TbClusterStore.class));
        return licenseActivationService;
    }

    /**
     * Previews {@link #ONLINE_SECRET} with the portal answering {@code planData}. The portal call is static,
     * so the stub has to be scoped rather than set up and left behind.
     */
    private SubscriptionInfo previewOnline(BasicLicenseActivationService licenseActivationService, PlanData planData) {
        try (MockedStatic<TbLicenseClient> portal = mockStatic(TbLicenseClient.class)) {
            portal.when(() -> TbLicenseClient.previewSubscription(isNull(), eq(ONLINE_SECRET), anyLong(), any()))
                    .thenReturn(new SubscriptionPreviewResponse(planData, new SubscriptionData()));
            return licenseActivationService.previewLicenseKey(ONLINE_SECRET);
        }
    }

    private static PlanData planWith(String key, long value) {
        PlanData planData = new PlanData();
        planData.put(key, new PlanItem(value));
        return planData;
    }

    /** The same, on a licence of a stated version - v1 and v2 disagree about what a zero quota means. */
    private static PlanData planWith(String key, long value, int licenseVersion) {
        PlanData planData = planWith(key, value);
        planData.put(PlanDataConstants.LICENSE_VERSION_KEY, new PlanItem(licenseVersion));
        return planData;
    }


    @Test
    public void applyReleasesTheOutgoingInstance() throws Exception {
        // A licence binds to the deployment that activates it, so a deployment moving to another licence must
        // give the old one's slot back - otherwise a customer who swaps keys twice runs out of instances on a
        // licence nothing is using any more.
        AbstractTbLicenseClient outgoing = mock(AbstractTbLicenseClient.class);
        AbstractTbLicenseClient incoming = mock(AbstractTbLicenseClient.class);
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        doReturn(incoming).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        licenseActivationService.applyLicenseKey(NEW_SECRET);

        verify(outgoing).releaseInstance();
        verify(incoming, never()).releaseInstance();
    }

    @Test
    public void aFailedReleaseDoesNotFailTheSwap() throws Exception {
        // The new licence is already in force by the time the release runs; a portal that cannot be reached
        // must not undo a swap that has taken.
        AbstractTbLicenseClient outgoing = mock(AbstractTbLicenseClient.class);
        AbstractTbLicenseClient incoming = mock(AbstractTbLicenseClient.class);
        doThrow(new LicenseException("portal down", LicenseErrorCode.CONNECTION_ERROR))
                .when(outgoing).releaseInstance();
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        doReturn(incoming).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        assertThatNoException().isThrownBy(() -> licenseActivationService.applyLicenseKey(NEW_SECRET));
        assertThat(licenseActivationService.getClient()).isSameAs(incoming);
    }

    @Test
    public void reconvergenceReleasesThisNodesOwnInstance() throws Exception {
        // A peer node never sees the REST call, so if the release did not happen here an N-node cluster would
        // leave N-1 orphans on the displaced licence.
        AbstractTbLicenseClient outgoing = mock(AbstractTbLicenseClient.class);
        AbstractTbLicenseClient incoming = mock(AbstractTbLicenseClient.class);
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        TbClusterStore tbClusterStore =
                (TbClusterStore) ReflectionTestUtils.getField(licenseActivationService, "tbClusterStore");
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of(NEW_SECRET));
        doReturn(incoming).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        licenseActivationService.reconcileLicenseState();

        verify(outgoing).releaseInstance();
    }

    @Test
    public void reApplyingTheSecretAlreadyInForceReleasesNothing() throws Exception {
        // Not a swap: the rebuilt client found instance-license.data still describing this secret and reused
        // it, so it holds the very instance the outgoing one did. Releasing it would leave this node running
        // against an instance the portal has released, and it would fail on its next check-in.
        AbstractTbLicenseClient outgoing = clientHolding("instance-1");
        AbstractTbLicenseClient incoming = clientHolding("instance-1");
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        doReturn(incoming).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        licenseActivationService.applyLicenseKey(NEW_SECRET);

        verify(outgoing, never()).releaseInstance();
        assertThat(licenseActivationService.getClient()).isSameAs(incoming);
    }

    @Test
    public void applyingTheKeyThisNodeRunsBuildsNoClientAndStillStoresIt() throws Exception {
        // Building a client to say "nothing changed" would re-check the instance this node runs on, rotating
        // its credential on the portal and leaving the live client's heartbeat stale. Storing it is what the
        // call is still good for: the seed after an env-var activation can fail, and re-entering the key is
        // what an operator does about that.
        AbstractTbLicenseClient running = clientHolding("instance-1");
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(running, NEW_SECRET);
        TbClusterStore tbClusterStore =
                (TbClusterStore) ReflectionTestUtils.getField(licenseActivationService, "tbClusterStore");
        doReturn(mock(AbstractTbLicenseClient.class)).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        licenseActivationService.applyLicenseKey(NEW_SECRET);

        verify(licenseActivationService, never()).createLicenseClient(anyString());
        verify(running, never()).releaseInstance();
        verify(running, never()).stop();
        assertThat(licenseActivationService.getClient()).isSameAs(running);
        verify(tbClusterStore).saveLicenseSecret(NEW_SECRET);
    }

    @Test
    public void reconvergenceOnTheSecretAlreadyInForceReleasesNothing() throws Exception {
        // The second release site, and the one no test reached before. A node running secret A that has since
        // rejected a key B carries B in lastAttemptedSecret, so the "already on it" early return does not fire
        // and the client is rebuilt on A. activate() publishes it - the same secret is never superseded - and
        // here the rebuilt client did find instance-license.data still describing A, so the instance it holds
        // is the one the outgoing client held. Releasing it would kill the instance the live client runs on.
        AbstractTbLicenseClient outgoing = clientHolding("instance-1");
        AbstractTbLicenseClient incoming = clientHolding("instance-1");
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        ReflectionTestUtils.setField(licenseActivationService, "lastAttemptedSecret", "a-key-that-was-rejected");
        TbClusterStore tbClusterStore =
                (TbClusterStore) ReflectionTestUtils.getField(licenseActivationService, "tbClusterStore");
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of(NEW_SECRET));
        doReturn(incoming).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        licenseActivationService.reconcileLicenseState();

        verify(outgoing, never()).releaseInstance();
        assertThat(licenseActivationService.getClient()).isSameAs(incoming);
    }

    @Test
    public void reconvergingAfterARefusedKeyReleasesTheInstanceTheRefusalStranded() throws Exception {
        // The same code path as the test above, on the history that makes its premise false. The refused key B
        // was activated before it was refused, and activating it deleted the instance-license.data describing
        // A - so rebuilding the client on A cannot reuse the file and activates a second instance instead.
        // Both clients were built from the same secret, so only their instance ids tell them apart.
        AbstractTbLicenseClient outgoing = clientHolding("instance-before-the-refusal");
        AbstractTbLicenseClient incoming = clientHolding("instance-activated-afresh");
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        ReflectionTestUtils.setField(licenseActivationService, "lastAttemptedSecret", "a-key-that-was-refused");
        TbClusterStore tbClusterStore =
                (TbClusterStore) ReflectionTestUtils.getField(licenseActivationService, "tbClusterStore");
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of(NEW_SECRET));
        doReturn(incoming).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        licenseActivationService.reconcileLicenseState();

        verify(outgoing, times(1)).releaseInstance();
        verify(incoming, never()).releaseInstance();
        assertThat(licenseActivationService.getClient()).isSameAs(incoming);
        // The pin, and the reason this test exists: the decision is taken on what the two clients hold, not on
        // the secrets they were built from. A guard that went back to comparing secrets would find these two
        // equal, skip the release, and orphan the first instance forever - and would fail here first.
        verify(outgoing, atLeastOnce()).getInstanceId();
        verify(incoming, atLeastOnce()).getInstanceId();
    }

    @Test
    public void aRefusedKeyPutsTheRunningLicencesInstanceDataFileBack() throws Exception {
        // Building the client for the refused key deleted the file describing the licence this node is running
        // on. Left deleted, a restart re-activates a second instance - and on a single-instance licence cannot
        // activate at all, because this node's own live client holds the only slot.
        AbstractTbLicenseClient outgoing = clientHolding("instance-of-the-running-licence");
        AbstractTbLicenseClient incoming = clientWithPlan(planWith(PlanDataConstants.MAX_DEVICES_KEY, 100L));
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        wireCounts(licenseActivationService, 4812L, 0L);
        doReturn(incoming).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        assertThatThrownBy(() -> licenseActivationService.applyLicenseKey(NEW_SECRET))
                .isInstanceOf(SubscriptionException.class);

        verify(outgoing).persistInstanceData();
    }

    @Test
    public void aKeyThatCannotBeStoredPutsTheRunningLicencesInstanceDataFileBack() throws Exception {
        // The other discard inside applyLicenseKey, and the same trace to clean up: a refused key must leave
        // none, and the file describing a key this node never ran is one.
        AbstractTbLicenseClient outgoing = clientHolding("instance-of-the-running-licence");
        AbstractTbLicenseClient incoming = mock(AbstractTbLicenseClient.class);
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        TbClusterStore tbClusterStore =
                (TbClusterStore) ReflectionTestUtils.getField(licenseActivationService, "tbClusterStore");
        doThrow(new DataAccessResourceFailureException("tb_cluster is unreachable"))
                .when(tbClusterStore).saveLicenseSecret(NEW_SECRET);
        doReturn(incoming).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        assertThatThrownBy(() -> licenseActivationService.applyLicenseKey(NEW_SECRET))
                .isInstanceOf(DataAccessResourceFailureException.class);

        verify(outgoing).persistInstanceData();
    }

    @Test
    public void aRefusedKeyOnAnUnactivatedNodeDestroysTheFileDescribingTheInstanceItGaveBack() throws Exception {
        // Nothing is running, so nothing rewrites instance-license.data - and what the refused key's client
        // left there describes an instance that has just been handed back. Kept, it makes every later attempt
        // at that same key a stale activation, across restarts.
        AbstractTbLicenseClient refused = clientHolding("instance-the-refused-key-took");
        when(refused.getPlanData()).thenReturn(planWith(PlanDataConstants.MAX_DEVICES_KEY, 100L));
        BasicLicenseActivationService licenseActivationService = spy(serviceWithCounts(4812L, 0L));
        Path dataFile = instanceDataFileOf(licenseActivationService);
        doReturn(refused).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        assertThatThrownBy(() -> licenseActivationService.applyLicenseKey(NEW_SECRET))
                .isInstanceOf(SubscriptionException.class);

        verify(refused).releaseInstance();
        assertThat(dataFile).doesNotExist();
    }

    @Test
    public void aReleaseThePortalRefusedKeepsTheFileThatCouldStillReadoptTheInstance() throws Exception {
        // The release is best effort, so a portal that could not be reached leaves the instance active. The
        // file is the only local record of it: deleted, nothing can ever re-adopt it and the slot is orphaned
        // until someone deactivates it by hand.
        AbstractTbLicenseClient refused = clientHolding("instance-the-refused-key-took");
        when(refused.getPlanData()).thenReturn(planWith(PlanDataConstants.MAX_DEVICES_KEY, 100L));
        doThrow(new IllegalStateException("the portal is unreachable")).when(refused).releaseInstance();
        BasicLicenseActivationService licenseActivationService = spy(serviceWithCounts(4812L, 0L));
        Path dataFile = instanceDataFileOf(licenseActivationService);
        doReturn(refused).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        assertThatThrownBy(() -> licenseActivationService.applyLicenseKey(NEW_SECRET))
                .isInstanceOf(SubscriptionException.class);

        assertThat(dataFile).exists();
    }

    @Test
    public void clearingTheLicenseGivesThisNodesInstanceBackAndDestroysTheRecordOfIt() {
        // Clearing is how an operator moves a licence to another deployment: a slot not handed back here is one
        // nothing can ever hand back, and the file describing it has to go with it - see the peer test below
        // for what keeping one costs.
        AbstractTbLicenseClient running = mock(AbstractTbLicenseClient.class);
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(running);
        Path dataFile = instanceDataFileOf(licenseActivationService);

        licenseActivationService.clearLicense();

        verify(running).releaseInstance();
        verify(running).stop();
        assertThat(dataFile).doesNotExist();
        assertThat(licenseActivationService.getClient()).isNull();
    }

    @Test
    public void aPeerClearingTheLicenseGivesThisNodesInstanceBackAndDestroysTheRecordOfItToo() throws Exception {
        // The path every node except the one the operator used takes. Without the release an N-node cluster
        // gives up one slot and leaks the other N-1; without the delete it is worse than the leak, because the
        // file then describes an instance the portal has deactivated - re-entering the same key re-checks that
        // instance, which is a critical error, and nothing on that path deletes the file. The node could never
        // activate again, across restarts.
        AbstractTbLicenseClient running = mock(AbstractTbLicenseClient.class);
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(running);
        Path dataFile = instanceDataFileOf(licenseActivationService);
        TbClusterStore tbClusterStore =
                (TbClusterStore) ReflectionTestUtils.getField(licenseActivationService, "tbClusterStore");
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());

        licenseActivationService.reconcileLicenseState();

        verify(running).releaseInstance();
        verify(running).stop();
        assertThat(dataFile).doesNotExist();
        assertThat(licenseActivationService.getClient()).isNull();
    }

    @Test
    public void aPortalThatCannotBeReachedDoesNotBlockAClear() {
        // Best effort, like every other release: an operator clearing a licence must not be left activated
        // because the portal is down.
        AbstractTbLicenseClient running = mock(AbstractTbLicenseClient.class);
        doThrow(new LicenseException("portal down", LicenseErrorCode.CONNECTION_ERROR)).when(running).releaseInstance();
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(running);
        Path dataFile = instanceDataFileOf(licenseActivationService);

        assertThatNoException().isThrownBy(licenseActivationService::clearLicense);

        verify(running).stop();
        // And the rest of the clear still happens: a portal that could not be reached must not be what leaves
        // this node holding a file it can never activate from again.
        assertThat(dataFile).doesNotExist();
        assertThat(licenseActivationService.isLicenseActivated()).isFalse();
    }

    @Test
    public void aFleetCountThatCannotBeReadGivesTheKeysInstanceBack() throws Exception {
        // The fleet check reads two counts off the database, inside the same attempt that holds the new
        // client. A database that cannot answer is no verdict on the key, but the client is just as
        // unpublishable as a refused one - so it costs the same nothing.
        AbstractTbLicenseClient outgoing = clientHolding("instance-of-the-running-licence");
        AbstractTbLicenseClient incoming = mock(AbstractTbLicenseClient.class);
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        DeviceService deviceService = mock(DeviceService.class);
        when(deviceService.countDevices()).thenThrow(new DataAccessResourceFailureException("the database is unreachable"));
        ReflectionTestUtils.setField(licenseActivationService, "deviceService", deviceService);
        TbClusterStore tbClusterStore =
                (TbClusterStore) ReflectionTestUtils.getField(licenseActivationService, "tbClusterStore");
        doReturn(incoming).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        assertThatThrownBy(() -> licenseActivationService.applyLicenseKey(NEW_SECRET))
                .isInstanceOf(DataAccessResourceFailureException.class);

        verify(incoming).releaseInstance();
        verify(incoming).stop();
        verify(outgoing).persistInstanceData();
        verify(tbClusterStore, never()).saveLicenseSecret(anyString());
        assertThat(licenseActivationService.getClient()).isSameAs(outgoing);
    }

    @Test
    public void aKeyThatCannotBeStoredGivesItsInstanceBack() throws Exception {
        // The client took a slot on the portal during createLicenseClient and is now thrown away without ever
        // having run: the cluster keeps the secret it had and this node the licence it was on. The same
        // invariant the refusal path honours - a key that does not take must cost no slot.
        AbstractTbLicenseClient outgoing = mock(AbstractTbLicenseClient.class);
        AbstractTbLicenseClient incoming = mock(AbstractTbLicenseClient.class);
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        TbClusterStore tbClusterStore =
                (TbClusterStore) ReflectionTestUtils.getField(licenseActivationService, "tbClusterStore");
        doThrow(new DataAccessResourceFailureException("tb_cluster is unreachable"))
                .when(tbClusterStore).saveLicenseSecret(NEW_SECRET);
        doReturn(incoming).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        assertThatThrownBy(() -> licenseActivationService.applyLicenseKey(NEW_SECRET))
                .isInstanceOf(DataAccessResourceFailureException.class);

        verify(incoming).releaseInstance();
        verify(incoming).stop();
        assertThat(licenseActivationService.getClient()).isSameAs(outgoing);
        verify(outgoing, never()).releaseInstance();
    }

    @Test
    public void aFailedStoreOfTheSecretAlreadyInForceReleasesNothing() throws Exception {
        // Re-applying the key already in force is not a swap even when the write fails: the rebuilt client
        // reused instance-license.data, so the slot it appears to hold is the running client's.
        AbstractTbLicenseClient outgoing = clientHolding("instance-1");
        AbstractTbLicenseClient incoming = clientHolding("instance-1");
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        TbClusterStore tbClusterStore =
                (TbClusterStore) ReflectionTestUtils.getField(licenseActivationService, "tbClusterStore");
        doThrow(new DataAccessResourceFailureException("tb_cluster is unreachable"))
                .when(tbClusterStore).saveLicenseSecret(NEW_SECRET);
        doReturn(incoming).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        assertThatThrownBy(() -> licenseActivationService.applyLicenseKey(NEW_SECRET))
                .isInstanceOf(DataAccessResourceFailureException.class);

        verify(incoming, never()).releaseInstance();
        verify(outgoing, never()).releaseInstance();
    }

    @Test
    public void aSupersededActivationGivesItsInstanceBack() throws Exception {
        // Another node stored a different key while this one was validating, so the client it built is dropped
        // unpublished. It still took a slot on the portal, and nothing will ever use it.
        AbstractTbLicenseClient outgoing = clientHolding("instance-of-the-running-licence");
        AbstractTbLicenseClient incoming = mock(AbstractTbLicenseClient.class);
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        TbClusterStore tbClusterStore =
                (TbClusterStore) ReflectionTestUtils.getField(licenseActivationService, "tbClusterStore");
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("the-key-another-node-stored"));
        doReturn(incoming).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        assertThatThrownBy(() -> licenseActivationService.applyLicenseKey(NEW_SECRET))
                .isInstanceOf(SubscriptionException.class)
                .hasMessageContaining("superseded");

        verify(incoming).releaseInstance();
        verify(incoming).stop();
        // This node stays on the licence it was running, whose slot it still needs.
        assertThat(licenseActivationService.getClient()).isSameAs(outgoing);
        verify(outgoing, never()).releaseInstance();
        // And on the file that licence is described by, which building the superseded client deleted.
        verify(outgoing).persistInstanceData();
    }

    @Test
    public void aKeyThePortalRefusesPutsTheRunningLicencesInstanceDataFileBack() throws Exception {
        // The likeliest way a deployment loses that file: the client deletes it the moment it is handed another
        // secret, before it asks the portal for an instance - so an operator pasting a key the portal refuses
        // leaves this node running a licence whose file is gone, without any client ever having been built.
        AbstractTbLicenseClient outgoing = clientHolding("instance-of-the-running-licence");
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        doThrow(new LicenseException("no instances left", LicenseErrorCode.ACTIVE_INSTANCES_CAPACITY_EXCEEDED))
                .when(licenseActivationService).createLicenseClient(NEW_SECRET);

        assertThatThrownBy(() -> licenseActivationService.applyLicenseKey(NEW_SECRET))
                .isInstanceOf(SubscriptionException.class)
                .hasMessage(LicenseKeyRejections.NO_AVAILABLE_INSTANCE);

        verify(outgoing).persistInstanceData();
        assertThat(licenseActivationService.getClient()).isSameAs(outgoing);
    }

    @Test
    public void aReconvergenceThatCannotBuildItsClientPutsTheRunningLicencesInstanceDataFileBack() throws Exception {
        // The same failure on the path no operator is watching. A peer stored a key this node cannot validate,
        // so this node keeps the licence it has - and has to keep the file describing it too.
        AbstractTbLicenseClient outgoing = clientHolding("instance-of-the-running-licence");
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        TbClusterStore tbClusterStore =
                (TbClusterStore) ReflectionTestUtils.getField(licenseActivationService, "tbClusterStore");
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of(NEW_SECRET));
        doThrow(new LicenseException("no instances left", LicenseErrorCode.ACTIVE_INSTANCES_CAPACITY_EXCEEDED))
                .when(licenseActivationService).createLicenseClient(NEW_SECRET);

        licenseActivationService.reconcileLicenseState();

        verify(outgoing).persistInstanceData();
        assertThat(licenseActivationService.getClient()).isSameAs(outgoing);
    }

    @Test
    public void aClientBuiltOnASecretTheClusterClearedGivesItsInstanceBack() throws Exception {
        // The reconvergence twin of the superseded path: the secret was cleared, not replaced, while this node
        // was validating it. The client is dropped unpublished and its slot goes back with it.
        AbstractTbLicenseClient outgoing = mock(AbstractTbLicenseClient.class);
        AbstractTbLicenseClient incoming = mock(AbstractTbLicenseClient.class);
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        TbClusterStore tbClusterStore =
                (TbClusterStore) ReflectionTestUtils.getField(licenseActivationService, "tbClusterStore");
        // Read twice: once to find the secret to reconverge on, once again after the validation to see whether
        // it survived it.
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of(NEW_SECRET), Optional.empty());
        doReturn(incoming).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        licenseActivationService.reconcileLicenseState();

        verify(incoming).releaseInstance();
        verify(incoming).stop();
        assertThat(licenseActivationService.getClient()).isNull();
    }

    @Test
    public void applyRefusesAKeyThatCoversFewerDevicesThanTheDeploymentHolds() throws Exception {
        // The same refusal the preview reports, at the site that would otherwise take the key: an operator who
        // skipped the dialog must not be able to shrink the licence out from under a running fleet.
        AbstractTbLicenseClient outgoing = mock(AbstractTbLicenseClient.class);
        AbstractTbLicenseClient incoming = clientWithPlan(planWith(PlanDataConstants.MAX_DEVICES_KEY, 100L));
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        wireCounts(licenseActivationService, 4812L, 0L);
        TbClusterStore tbClusterStore =
                (TbClusterStore) ReflectionTestUtils.getField(licenseActivationService, "tbClusterStore");
        doReturn(incoming).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        assertThatThrownBy(() -> licenseActivationService.applyLicenseKey(NEW_SECRET))
                .isInstanceOf(SubscriptionException.class)
                .hasMessage(LicenseCapacity.capExceededMessage(CappedEntity.DEVICE, 4812L, 100L));

        // The running licence is untouched, the cluster keeps the secret it had, and the instance the refused
        // key took on the portal is given back - a refusal must cost no slot.
        assertThat(licenseActivationService.getClient()).isSameAs(outgoing);
        verify(outgoing, never()).stop();
        verify(tbClusterStore, never()).saveLicenseSecret(anyString());
        verify(incoming).releaseInstance();
        verify(incoming).stop();
    }

    @Test
    public void applyRefusesAV2KeyThatGrantsNoDevicesAtAllToANonEmptyFleet() throws Exception {
        // The apply site reads the plan raw too, so the strictest key is refused there and not only in the
        // dialog - an operator who skipped the preview gets the same answer.
        AbstractTbLicenseClient outgoing = mock(AbstractTbLicenseClient.class);
        AbstractTbLicenseClient incoming = clientWithPlan(planWith(PlanDataConstants.MAX_DEVICES_KEY, 0L, 2));
        when(incoming.getLicenseVersion()).thenReturn(2);
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        wireCounts(licenseActivationService, 1L, 0L);
        doReturn(incoming).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        assertThatThrownBy(() -> licenseActivationService.applyLicenseKey(NEW_SECRET))
                .isInstanceOf(SubscriptionException.class)
                .hasMessage(LicenseCapacity.capExceededMessage(CappedEntity.DEVICE, 1L, 0L));

        assertThat(licenseActivationService.getClient()).isSameAs(outgoing);
    }

    @Test
    public void theEdgeQuotaCrossesIntoAnApplyAndDecidesNothing() throws Exception {
        // The apply site is as blind to the edge quota as the preview is, and for the same reason: no edge
        // count reaches it. Same caveat about what this can and cannot fail on.
        AbstractTbLicenseClient outgoing = mock(AbstractTbLicenseClient.class);
        AbstractTbLicenseClient incoming = clientWithPlan(planWith(PlanDataConstants.MAX_EDGES_KEY, 5L, 2));
        when(incoming.getLicenseVersion()).thenReturn(2);
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(outgoing);
        wireCounts(licenseActivationService, 0L, 0L);
        doReturn(incoming).when(licenseActivationService).createLicenseClient(NEW_SECRET);

        licenseActivationService.applyLicenseKey(NEW_SECRET);

        assertThat(licenseActivationService.getClient()).isSameAs(incoming);
    }

    /**
     * Points the service at a real, existing {@code instance-license.data} and hands the path back, so a test
     * can assert on the file itself rather than on a call. Deleted on exit if the test left it behind.
     */
    private static Path instanceDataFileOf(BasicLicenseActivationService licenseActivationService) {
        try {
            Path dataFile = Files.createTempFile("instance-license", ".data");
            dataFile.toFile().deleteOnExit();
            ReflectionTestUtils.setField(licenseActivationService, "instanceDataFilePath", dataFile.toString());
            return dataFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A client holding the named portal instance. Instance identity, not the secret, is what the release
     * guards compare: all clients of a node share one instance-license.data, and building one for a different
     * secret deletes it, so two clients built on the same secret need not hold the same instance.
     */
    private static AbstractTbLicenseClient clientHolding(String instanceId) {
        AbstractTbLicenseClient client = mock(AbstractTbLicenseClient.class);
        when(client.getInstanceId()).thenReturn(instanceId);
        return client;
    }

    /** A client that answers the plan data the cap check reads, and nothing else. */
    private static AbstractTbLicenseClient clientWithPlan(PlanData planData) {
        AbstractTbLicenseClient client = mock(AbstractTbLicenseClient.class);
        when(client.getPlanData()).thenReturn(planData);
        return client;
    }

    /**
     * The two counters the cap check reads. Edges are absent on purpose: their quota is never checked, so the
     * service holds no edge collaborator at all and a test that wired one would be documenting a query that
     * does not happen.
     */
    private static void wireCounts(BasicLicenseActivationService licenseActivationService,
                                   long devices, long assets) {
        DeviceService deviceService = mock(DeviceService.class);
        when(deviceService.countDevices()).thenReturn(devices);
        AssetService assetService = mock(AssetService.class);
        when(assetService.countAssets()).thenReturn(assets);
        ReflectionTestUtils.setField(licenseActivationService, "deviceService", deviceService);
        ReflectionTestUtils.setField(licenseActivationService, "assetService", assetService);
    }

    /**
     * A spied, activated service running on {@code running}, with the collaborators a licence swap touches -
     * the cluster store it persists through and the publisher it broadcasts on - stubbed.
     */
    private BasicLicenseActivationService serviceRunningOn(AbstractTbLicenseClient running) {
        BasicLicenseActivationService licenseActivationService = spy(new BasicLicenseActivationService());
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", running);
        ReflectionTestUtils.setField(licenseActivationService, "licenseActivated", true);
        ReflectionTestUtils.setField(licenseActivationService, "lastAttemptedSecret", "the-old-secret");
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", mock(TbClusterStore.class));
        ReflectionTestUtils.setField(licenseActivationService, "eventPublisher", mock(ApplicationEventPublisher.class));
        // applyLicenseKey measures the incoming licence against the live fleet, so the three counters have to
        // answer; an empty deployment fits every licence and keeps these tests about the swap itself.
        wireCounts(licenseActivationService, 0L, 0L);
        wireReconciliationListener(licenseActivationService);
        return licenseActivationService;
    }

    /** The same, on a node whose running client was built from {@code publishedSecret}. */
    private BasicLicenseActivationService serviceRunningOn(AbstractTbLicenseClient running, String publishedSecret) {
        BasicLicenseActivationService licenseActivationService = serviceRunningOn(running);
        markRunningOn(licenseActivationService, running, publishedSecret);
        return licenseActivationService;
    }

    /**
     * The two fields that together make a key the one already in force: the published client and the secret it
     * was built from. Neither alone says which key a node is running.
     */
    private static void markRunningOn(BasicLicenseActivationService licenseActivationService,
                                      AbstractTbLicenseClient running, String publishedSecret) {
        ReflectionTestUtils.setField(licenseActivationService, "tbLicenseClient", running);
        ReflectionTestUtils.setField(licenseActivationService, "publishedClientSecret", publishedSecret);
    }

    /**
     * Wires the optional reconciliation listener the way Spring does, as the provider the service resolves on
     * use rather than as the bean itself.
     */
    @SuppressWarnings("unchecked")
    private LicenseStateReconciliationListener wireReconciliationListener(BasicLicenseActivationService licenseActivationService) {
        LicenseStateReconciliationListener listener = mock(LicenseStateReconciliationListener.class);
        ObjectProvider<LicenseStateReconciliationListener> provider = mock(ObjectProvider.class);
        when(provider.getIfUnique()).thenReturn(listener);
        ReflectionTestUtils.setField(licenseActivationService, "licenseStateReconciliationListener", provider);
        return listener;
    }

    /**
     * Arms the database throttle window the way a prior request would have, by making the very activation
     * check the throttle guards. Written as its own step rather than as an assertion because it is an act:
     * the tests that use it depend on the call having happened, not on what it answered, and an assertion
     * that a later reader is free to reorder or drop would silently change what they exercise.
     */
    private void armThrottleWindow(BasicLicenseActivationService licenseActivationService) {
        licenseActivationService.isLicenseActivated();
    }

}
