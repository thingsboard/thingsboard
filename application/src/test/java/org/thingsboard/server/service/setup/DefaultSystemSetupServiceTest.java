// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.setup;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.license.client.TbLicenseCtx;
import org.thingsboard.license.shared.FreeLicenseClaimResponse;
import org.thingsboard.license.shared.exception.LicenseErrorCode;
import org.thingsboard.license.shared.exception.LicenseException;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.setup.LicenseChangeResult;
import org.thingsboard.server.common.data.setup.LicenseClaimMode;
import org.thingsboard.server.common.data.setup.LicenseClaimResult;
import org.thingsboard.server.common.data.setup.LicenseClaimStatus;
import org.thingsboard.server.common.data.setup.SystemSetupState;
import org.thingsboard.server.common.data.subscription.SubscriptionErrorCode;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.dao.subscription.TbClusterStore;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.service.license.NonProductionConfirmationService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.throwable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultSystemSetupServiceTest {

    // Deliberately left on Mockito's default strict stubs: an unused stub here almost always means the
    // test is not exercising the branch it claims to.

    private static final UUID CLUSTER_ID = UUID.randomUUID();

    /** How the license client resolves its endpoint when no explicit one is passed - see DefaultLicensePortalClient. */
    private static final String LICENSE_SERVER_PROPERTY = "tb.license.server";

    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private UserService userService;
    @Mock
    private SetupDataService setupDataService;
    @Mock
    private LicensePortalClient licensePortalClient;
    @Mock
    private TbClusterStore tbClusterStore;
    @Mock
    private NonProductionConfirmationService nonProductionConfirmationService;
    @Mock
    private TbLicenseCtx licenseCtx;

    private DefaultSystemSetupService systemSetupService;

    @BeforeEach
    public void setUp() {
        systemSetupService = new DefaultSystemSetupService(subscriptionService, userService, setupDataService,
                licensePortalClient, tbClusterStore, nonProductionConfirmationService, licenseCtx);
    }

    @Test
    public void stateReflectsLicenseAndSysadmin() {
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        assertThat(systemSetupService.getState()).isEqualTo(SystemSetupState.LICENSE_REQUIRED);

        when(subscriptionService.isLicenseActivated()).thenReturn(true);
        when(userService.existsByAuthority(Authority.SYS_ADMIN)).thenReturn(false);
        assertThat(systemSetupService.getState()).isEqualTo(SystemSetupState.ACCOUNT_REQUIRED);

        when(userService.existsByAuthority(Authority.SYS_ADMIN)).thenReturn(true);
        assertThat(systemSetupService.getState()).isEqualTo(SystemSetupState.READY);
    }

    @Test
    public void stateLocksForNonProductionConfirmationOnlyAfterLicenseAndSysadmin() {
        // Ordered last: a half-configured instance finishes the license and account steps before it is ever
        // asked to reconfirm, rather than being presented with a lock it cannot yet act on.
        when(subscriptionService.isLicenseActivated()).thenReturn(true);
        when(userService.existsByAuthority(Authority.SYS_ADMIN)).thenReturn(true);
        when(nonProductionConfirmationService.isLapsed()).thenReturn(true);

        assertThat(systemSetupService.getState()).isEqualTo(SystemSetupState.NON_PRODUCTION_CONFIRMATION_REQUIRED);

        when(nonProductionConfirmationService.isLapsed()).thenReturn(false);
        assertThat(systemSetupService.getState()).isEqualTo(SystemSetupState.READY);
    }

    @Test
    public void runtimeReLockReportsLicenseRequiredEvenWhenSysadminAlreadyExists() throws Exception {
        // Complete the setup so the sysAdminExists latch is set to true.
        when(subscriptionService.isLicenseActivated()).thenReturn(true);
        systemSetupService.completeSetup("sysadmin@thingsboard.org", "password", false);
        assertThat(systemSetupService.getState()).isEqualTo(SystemSetupState.READY);
        verify(setupDataService, never()).loadDemoData();

        // The license is revoked at runtime. Activation is checked before the latch, so the state locks again to
        // LICENSE_REQUIRED even though the sysadmin still exists (management plane goes 423-locked once more).
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        assertThat(systemSetupService.getState()).isEqualTo(SystemSetupState.LICENSE_REQUIRED);
    }

    @Test
    public void completeSetupLoadsTheDemoDataWhenAsked() throws Exception {
        systemSetupService.completeSetup("sysadmin@thingsboard.org", "password", true);

        verify(setupDataService).createSysAdmin("sysadmin@thingsboard.org", "password");
        verify(setupDataService).loadDemoData();
    }

    @Test
    public void aFailureToLoadTheDemoDataDoesNotFailACompletedSetup() throws Exception {
        doThrow(new RuntimeException("the demo tenant could not be created"))
                .when(setupDataService).loadDemoData();

        // Deliberate, not incidental: the system administrator exists by the time the demo data is loaded, so
        // a failure that propagated would leave the caller unable to retry a setup that has already succeeded.
        assertThatCode(() -> systemSetupService.completeSetup("sysadmin@thingsboard.org", "password", true))
                .doesNotThrowAnyException();
        verify(setupDataService).createSysAdmin("sysadmin@thingsboard.org", "password");
    }

    /**
     * The account step is first-caller-wins, and the re-check inside the lock is the only thing standing
     * between two concurrent anonymous calls and two system administrators. Driven sequentially, because the
     * latch the first call sets is what the second one has to lose on.
     */
    @Test
    public void aSecondCompleteSetupIsRefusedRatherThanCreatingASecondSysAdmin() {
        systemSetupService.completeSetup("sysadmin@thingsboard.org", "password", false);

        assertThatThrownBy(() -> systemSetupService.completeSetup("other@thingsboard.org", "password", false))
                .asInstanceOf(throwable(ThingsboardRuntimeException.class))
                .extracting(ThingsboardRuntimeException::getErrorCode)
                .isEqualTo(ThingsboardErrorCode.BAD_REQUEST_PARAMS);

        verify(setupDataService, times(1)).createSysAdmin(anyString(), anyString());
    }

    /**
     * The same refusal on a node that never ran the first call itself: the sysadmin was created on a peer, so
     * the latch is down and the database is what answers.
     */
    @Test
    public void aCompleteSetupOnANodeThatMissedTheFirstOneIsRefusedToo() {
        when(userService.existsByAuthority(Authority.SYS_ADMIN)).thenReturn(true);

        assertThatThrownBy(() -> systemSetupService.completeSetup("sysadmin@thingsboard.org", "password", false))
                .asInstanceOf(throwable(ThingsboardRuntimeException.class))
                .extracting(ThingsboardRuntimeException::getErrorCode)
                .isEqualTo(ThingsboardErrorCode.BAD_REQUEST_PARAMS);

        verifyNoInteractions(setupDataService);
    }

    @Test
    public void requestClaimStoresTheTokenBeforeCallingOut() throws Exception {
        when(licensePortalClient.isPortalReachable()).thenReturn(true);
        when(licensePortalClient.getPortalBaseUrl()).thenReturn("https://license.thingsboard.io");
        when(licenseCtx.getClusterId()).thenReturn(CLUSTER_ID);

        LicenseClaimResult result = systemSetupService.requestClaim();

        // Write first, call second: a token the portal has seen and this instance has not is unrecoverable,
        // while one the portal never saw is superseded by the next request.
        InOrder inOrder = inOrder(tbClusterStore, licensePortalClient);
        inOrder.verify(tbClusterStore).saveLicenseClaimToken(anyString());
        inOrder.verify(licensePortalClient).isPortalReachable();

        assertThat(result.getMode()).isEqualTo(LicenseClaimMode.ONLINE);
        assertThat(result.getSignUpUrl())
                .startsWith("https://license.thingsboard.io/declareLicense?")
                // The inverse of the assertion this replaces: the parameter is gone because the path now tells
                // activation from Community Grant enrolment, and nothing should quietly put it back.
                .doesNotContain("flow=")
                .contains("clusterId=" + CLUSTER_ID)
                .contains("claimToken=")
                .doesNotContain("offline");
    }

    @Test
    public void anUnreachablePortalHandsOutTheOfflineHint() throws Exception {
        when(licensePortalClient.isPortalReachable()).thenReturn(false);
        when(licensePortalClient.getPortalBaseUrl()).thenReturn("https://license.thingsboard.io");
        when(licenseCtx.getClusterId()).thenReturn(CLUSTER_ID);

        LicenseClaimResult result = systemSetupService.requestClaim();

        // The URL is still rendered in full: an air-gapped server cannot reach the portal, but the
        // operator's workstation frequently can.
        assertThat(result.getMode()).isEqualTo(LicenseClaimMode.OFFLINE);
        assertThat(result.getSignUpUrl()).contains("offline=true");
    }

    @Test
    public void aFailureToStoreTheTokenHandsOutNoUrl() {
        doThrow(new RuntimeException("the database is unreachable"))
                .when(tbClusterStore).saveLicenseClaimToken(anyString());

        assertThatThrownBy(() -> systemSetupService.requestClaim())
                .isInstanceOf(ThingsboardException.class)
                // Rendered, and actionable: nothing was requested of the portal, so retrying is the whole
                // remedy and the message has to say so. Without this pin the assertion below is satisfied by
                // any message at all, including an empty one.
                .hasMessageContaining("could not record the activation request")
                .hasMessageContaining("Please try again")
                // The raw failure may carry the statement and the database's own error text, and this body is
                // reachable anonymously.
                .hasMessageNotContaining("the database is unreachable");
        verifyNoInteractions(licensePortalClient);
    }

    /**
     * The sibling of the test above, and the worse half of the pair: the claim token is durably stored by the
     * time this fails, so the operator is left with a request the cluster has recorded and no link to complete
     * it with. A rendered message is the only thing standing between them and a raw 500.
     */
    @Test
    public void aMissingClusterRowIsNotReportedAsAMisconfiguredPortalAddress() {
        when(licenseCtx.getClusterId()).thenThrow(new IllegalStateException("Row is missing in tb_cluster table!"));

        assertThatThrownBy(() -> systemSetupService.requestClaim())
                .isInstanceOf(ThingsboardException.class)
                // What actually failed, and a way forward, because the wizard leaves the operator on this
                // screen. Not the portal address: an interrupted install sent to check tb.license.server
                // verifies a setting that is correct while the real cause sits in a stack trace nobody was
                // pointed at.
                .hasMessageContaining("could not read the cluster id")
                .hasMessageContaining("enter it below")
                .hasMessageNotContaining("address")
                // This body is reachable anonymously, and the cause names the table it failed to read.
                .hasMessageNotContaining("tb_cluster");

        // The token is stored first on purpose, so this failure leaves it behind: the assertion is here to
        // record that the state is deliberate, and that the message therefore has to carry the operator.
        verify(tbClusterStore).saveLicenseClaimToken(anyString());
    }

    @Test
    public void aPortalAddressThatCannotBeParsedHandsOutARenderedMessage() {
        // A typo in tb.license.server. Deliberately one that the URI parser actually rejects: a merely odd
        // address ("not a url") is parsed as a relative URI and yields a nonsense link rather than a failure,
        // so it would exercise nothing here.
        when(licensePortalClient.getPortalBaseUrl()).thenReturn("https://license.thingsboard.io:not-a-port");
        when(licenseCtx.getClusterId()).thenReturn(CLUSTER_ID);
        when(licensePortalClient.isPortalReachable()).thenReturn(true);

        assertThatThrownBy(() -> systemSetupService.requestClaim())
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("No sign-up link could be built")
                .hasMessageContaining("enter it below")
                // The cause is not this instance's cluster row, so the message must not send the operator
                // looking there either.
                .hasMessageNotContaining("cluster id")
                // Anonymously reachable body: the parser's own message is a diagnosis for the log, not for a
                // response.
                .hasMessageNotContaining("Bad authority");

        verify(tbClusterStore).saveLicenseClaimToken(anyString());
    }

    @Test
    public void aTrailingSlashOnTheConfiguredAddressDoesNotDoubleTheSlash() throws Exception {
        when(licensePortalClient.isPortalReachable()).thenReturn(true);
        when(licensePortalClient.getPortalBaseUrl()).thenReturn("https://license.thingsboard.io/");
        when(licenseCtx.getClusterId()).thenReturn(CLUSTER_ID);

        // An endpoint is just as likely to be configured with a trailing slash as without one, and the two have
        // to produce the same link. Pinned because the builder is what guarantees it - path() is joined against
        // the parsed path rather than concatenated - so nothing here trims the endpoint by hand.
        assertThat(systemSetupService.requestClaim().getSignUpUrl())
                .startsWith("https://license.thingsboard.io/declareLicense?");
    }

    @Test
    public void requestClaimMintsAFreshTokenEveryTime() throws Exception {
        when(licensePortalClient.isPortalReachable()).thenReturn(true);
        when(licensePortalClient.getPortalBaseUrl()).thenReturn("https://license.thingsboard.io");
        when(licenseCtx.getClusterId()).thenReturn(CLUSTER_ID);

        systemSetupService.requestClaim();
        // Elapses the mint throttle instead of sleeping through it, so this test stays about the token and the
        // suite does not pay MIN_CLAIM_REQUEST_INTERVAL_MS. The throttle itself is pinned by the test below.
        ReflectionTestUtils.setField(systemSetupService, "lastClaimRequestTs", 0L);
        systemSetupService.requestClaim();

        ArgumentCaptor<String> claimTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(tbClusterStore, times(2)).saveLicenseClaimToken(claimTokenCaptor.capture());
        assertThat(claimTokenCaptor.getAllValues().get(0)).isNotEqualTo(claimTokenCaptor.getAllValues().get(1));
        // The token is a bearer credential for the subscription secret, so its shape is pinned: 32 random
        // bytes Base64url-encoded without padding is exactly 43 URL-safe characters. A UUID would pass an
        // isNotBlank() check while carrying far less entropy.
        assertThat(claimTokenCaptor.getAllValues())
                .allSatisfy(claimToken -> assertThat(claimToken).hasSize(43).matches("[A-Za-z0-9_-]+"));
    }

    @Test
    public void aSecondMintRequestInsideTheWindowIsRefusedRatherThanSupersedingTheFirst() throws Exception {
        // Each mint replaces the stored claim token and the poll always claims with whatever is stored, so an
        // unthrottled mint endpoint - which is unauthenticated - lets anyone who can reach an unlicensed
        // instance keep superseding the operator's in-flight claim so that it can never complete.
        when(licensePortalClient.isPortalReachable()).thenReturn(true);
        when(licensePortalClient.getPortalBaseUrl()).thenReturn("https://license.thingsboard.io");
        when(licenseCtx.getClusterId()).thenReturn(CLUSTER_ID);

        systemSetupService.requestClaim();

        assertThatThrownBy(() -> systemSetupService.requestClaim())
                .isInstanceOf(ThingsboardException.class)
                // The wizard branches on the status code, so a refusal that arrived as anything else would be
                // read as the end of the automatic path rather than as "wait a moment".
                .asInstanceOf(throwable(ThingsboardException.class))
                .extracting(ThingsboardException::getErrorCode)
                .isEqualTo(ThingsboardErrorCode.TOO_MANY_REQUESTS);

        // Refused before anything was written: the first request's token is still the stored one, so the claim
        // the operator is already carrying to the portal stays the one this instance will recognise.
        verify(tbClusterStore, times(1)).saveLicenseClaimToken(anyString());
    }

    @Test
    public void aPortalMessageThatIsNotASentenceIsEndedBeforeTheManualFallbackIsAppended() throws Exception {
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("claim-token"));
        // What a portal failure actually reads like: a fragment, no full stop. Appending straight onto one of
        // those produces "... 502 Bad Gateway You can also enter..." - one run-on line the user reads as a
        // single garbled sentence.
        when(licensePortalClient.claimFreeLicense("claim-token"))
                .thenThrow(new LicenseException("Unexpected error from ThingsBoard License Server: 502 Bad Gateway",
                        LicenseErrorCode.GENERAL_SERVER_ERROR));

        assertThatThrownBy(() -> systemSetupService.pollClaim(null))
                .isInstanceOf(ThingsboardException.class)
                .hasMessage("Unexpected error from ThingsBoard License Server: 502 Bad Gateway. "
                        + "You can also enter the license key manually.");
    }

    @Test
    public void activationIsNotRequestedAndMakesNoOutboundCallWhenNoTokenIsStored() throws Exception {
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.empty());
        // Nothing is stored either: this is the genuine "nobody ever requested a claim" case, not a peer node
        // catching up on a race.
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());

        // The one state NOT_REQUESTED is reserved for: not activated, and no claim outstanding. This is the
        // state in which the wizard is right to settle on the manual key form.
        assertThat(systemSetupService.pollClaim(null).getStatus()).isEqualTo(LicenseClaimStatus.NOT_REQUESTED);

        // An unauthenticated endpoint must not be usable to generate portal traffic from an instance that
        // never requested a claim.
        verify(licensePortalClient, never()).claimFreeLicense(any());
    }

    @Test
    public void anUnreadableClusterRowStillAnswersTheWizardInsteadOfFailingTheRequest() {
        // A half-installed deployment is exactly the one running the wizard, and an unreadable tb_cluster row
        // is one of the states it gets there in. A 500 leaves the wizard with nothing to act on, so both
        // reads degrade and the poll settles on the manual key form.
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken())
                .thenThrow(new IllegalStateException("Failed to read the license_claim_token: table tb_cluster is empty"));
        when(tbClusterStore.getLicenseSecret())
                .thenThrow(new IllegalStateException("Failed to read the license_secret: table tb_cluster is empty"));

        assertThatCode(() -> assertThat(systemSetupService.pollClaim(null).getStatus())
                .isEqualTo(LicenseClaimStatus.NOT_REQUESTED)).doesNotThrowAnyException();
    }

    @Test
    public void activationReportsActivatedWhenASecretIsStoredEvenThoughThisNodeNeverConvergesOnIt() throws Exception {
        // Regression pin: the terminal NOT_REQUESTED must come from the durable cluster fact, never from
        // whether this node itself managed to activate. isLicenseActivated() can be a throttled stale "not
        // yet" on a peer node right after another node activated and cleared the claim token, and the node's
        // own on-demand attempt can fail on a genuinely activated cluster - a concurrent attempt holds the
        // single-flight lock, or this node already failed once on this very secret and short-circuits from
        // then on. Here that attempt is left doing nothing at all, which is exactly that case. Answering
        // NOT_REQUESTED off it would end the wizard's poll loop for good, since the wizard treats
        // NOT_REQUESTED as terminal.
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.empty());
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("stored-by-another-node"));

        assertThat(systemSetupService.pollClaim(null).getStatus()).isEqualTo(LicenseClaimStatus.ACTIVATED);

        // Still converges this node, so that the setup state request the wizard makes next - served from this
        // node's own activation state - does not contradict the ACTIVATED it was just handed.
        verify(subscriptionService).pickUpStoredLicenseSecret();
    }

    @Test
    public void onceAStoredSecretIsSeenItIsNeverReadAgain() throws Exception {
        // The other half of the pair below, and the one the javadoc calls the load-bearing invariant: the
        // latch turns an activated cluster into a memory read forever, with no window and no lock. Nothing
        // else would notice a change that put a per-request SELECT back on an activated instance.
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.empty());
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("stored-by-another-node"));

        for (int i = 0; i < 10; i++) {
            assertThat(systemSetupService.pollClaim(null).getStatus()).isEqualTo(LicenseClaimStatus.ACTIVATED);
        }

        // Exactly once, not atMost: the latch removes the time window entirely, so no amount of elapsed time
        // between the ten polls can earn a legitimate second read.
        verify(tbClusterStore, times(1)).getLicenseSecret();
    }

    /**
     * The other side of that latch: it must not outlive the licence it describes. Without the reset, a node
     * whose licence has just been cleared keeps answering ACTIVATED from memory while its setup state answers
     * LICENSE_REQUIRED, and the wizard settles on a screen that offers nothing to do.
     */
    @Test
    public void clearingTheLicenseMakesTheStoredSecretReadableAgain() throws Exception {
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.empty());
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("stored-by-another-node"));
        assertThat(systemSetupService.pollClaim(null).getStatus()).isEqualTo(LicenseClaimStatus.ACTIVATED);

        systemSetupService.clearLicense();

        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        assertThat(systemSetupService.pollClaim(null).getStatus()).isEqualTo(LicenseClaimStatus.NOT_REQUESTED);
    }

    /**
     * The same reset through the listener, which is the path the subscription service actually drives: the
     * clearing node calls {@code clearLicense()}, every other node in the cluster learns of it here.
     */
    @Test
    public void aReconciledLicenseStateMakesTheStoredSecretReadableAgain() throws Exception {
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.empty());
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("stored-by-another-node"));
        assertThat(systemSetupService.pollClaim(null).getStatus()).isEqualTo(LicenseClaimStatus.ACTIVATED);

        systemSetupService.onLicenseStateReconciled();

        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());
        assertThat(systemSetupService.pollClaim(null).getStatus()).isEqualTo(LicenseClaimStatus.NOT_REQUESTED);
    }

    @Test
    public void theTerminalAnswerReadsTheStoredSecretAtMostOncePerWindow() throws Exception {
        // The activation endpoint is unauthenticated and this branch is the permanent steady state of an
        // instance that is locked and never re-activates on its own, so an unthrottled read here would be one
        // SELECT per anonymous request forever.
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.empty());
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.empty());

        for (int i = 0; i < 10; i++) {
            assertThat(systemSetupService.pollClaim(null).getStatus()).isEqualTo(LicenseClaimStatus.NOT_REQUESTED);
        }

        // atMost rather than exactly once: ten unthrottled polls would be ten reads, so this still fails if
        // the window is gone, but it does not also fail when a loaded machine drags the ten iterations across
        // a window boundary and earns a legitimate second read.
        verify(tbClusterStore, atMost(2)).getLicenseSecret();
        // And the convergence attempt stays behind that gate rather than in front of it: it reads the
        // database itself, unthrottled, so calling it on this path would defeat the whole window.
        verify(subscriptionService, never()).pickUpStoredLicenseSecret();
    }

    @Test
    public void activationReportsActivatedOnAnAlreadyActivatedInstance() throws Exception {
        when(subscriptionService.isLicenseActivated()).thenReturn(true);

        // ACTIVATED and not NOT_REQUESTED: a wizard that missed the ACTIVATED of the claiming poll - a
        // refresh, a backgrounded tab, a dropped connection - must be told the license is in place, not sent
        // back to the manual key form of an instance that needs no key.
        assertThat(systemSetupService.pollClaim(null).getStatus()).isEqualTo(LicenseClaimStatus.ACTIVATED);

        // Short-circuits before even reading the token: a poll still in flight on another node when the
        // claiming node finishes would otherwise be handed the secret again (the claim is idempotent by
        // design) and pointlessly re-validate it and rewrite license_secret.
        verify(tbClusterStore, never()).getLicenseClaimToken();
        verify(licensePortalClient, never()).claimFreeLicense(any());
    }

    @Test
    public void activationIsPendingWhileThePortalReportsNotActivated() throws Exception {
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("claim-token"));
        when(licensePortalClient.claimFreeLicense("claim-token")).thenReturn(FreeLicenseClaimResponse.pending());

        assertThat(systemSetupService.pollClaim(null).getStatus()).isEqualTo(LicenseClaimStatus.PENDING);
        verify(subscriptionService, never()).applyLicenseKey(any());
    }

    @Test
    public void activationAppliesTheClaimedSecretAndReportsActivated() throws Exception {
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("claim-token"));
        when(licensePortalClient.claimFreeLicense("claim-token"))
                .thenReturn(FreeLicenseClaimResponse.activated("THE-SECRET"));

        assertThat(systemSetupService.pollClaim(null).getStatus()).isEqualTo(LicenseClaimStatus.ACTIVATED);
        verify(subscriptionService).applyLicenseKey("THE-SECRET");
    }

    @Test
    public void aRejectedClaimIsReportedAsExpiredNotThrown() throws Exception {
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("claim-token"));
        when(licensePortalClient.claimFreeLicense("claim-token"))
                .thenThrow(new LicenseException("rejected", LicenseErrorCode.CLAIM_LICENSE_REJECTED));

        // The one failure we understand: one of the portal's windows on this token has closed. Driven here as a
        // stubbed CLAIM_LICENSE_REJECTED because that is the portal's own terminal answer, and it stays a real
        // one - the portal rejects only a token it holds and has since timed out, never a token it has simply
        // not been told about, which is answered pending and reaches the PENDING test above. A status the
        // wizard can render specifically, rather than an error indistinguishable from "something broke".
        assertThat(systemSetupService.pollClaim(null).getStatus()).isEqualTo(LicenseClaimStatus.EXPIRED);

        // Terminal all the same, so the token is still retired and the next poll settles on the manual form.
        // Compare-and-clear, naming the token this claim actually used: the operator may have started a fresh
        // claim request while this poll was out, and an unconditional clear would then wipe that fresh token
        // and report "nothing pending" for a claim that is about to succeed.
        verify(tbClusterStore).clearLicenseClaimToken("claim-token");
        verify(tbClusterStore, never()).forceClearLicenseClaimToken();
    }

    /**
     * The regression this whole branch exists for: a portal that answers 404 must NOT retire the claim token.
     * <p>
     * Driven end to end through the real {@link DefaultLicensePortalClient} against a local server, not through
     * the mock every other test here uses, because the bug lived in the status-to-error-code mapping inside the
     * license client - a stubbed exception would assert only the half of the contract that was already right.
     * A 404 is what a portal and a ThingsBoard built from different revisions produce once the claim endpoint's
     * path has moved, and mapped as a rejection it retired the token on the first poll and told the operator
     * their sign-up link had expired, permanently, for a mismatch that a redeploy would have fixed.
     * <p>
     * Non-terminal is the whole assertion: the token stays stored, so the next poll retries, and the message
     * points at the mismatch rather than at an expired link - while still naming the manual key fallback,
     * because the wizard leaves the operator on this screen.
     */
    @Test
    public void aPortalAnsweringNotFoundLeavesTheClaimTokenInPlace() throws Exception {
        HttpServer portal = startPortalAnswering(404);
        String previousEndpoint = System.setProperty(LICENSE_SERVER_PROPERTY, portalBaseUrl(portal));
        try {
            DefaultSystemSetupService service = new DefaultSystemSetupService(subscriptionService, userService,
                    setupDataService, new DefaultLicensePortalClient(), tbClusterStore,
                    nonProductionConfirmationService, licenseCtx);
            when(subscriptionService.isLicenseActivated()).thenReturn(false);
            when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("claim-token"));

            assertThatThrownBy(() -> service.pollClaim(null))
                    .isInstanceOf(ThingsboardException.class)
                    .hasMessageContaining("404")
                    .hasMessageContaining("version mismatch")
                    .hasMessageContaining("enter the license key manually")
                    // Non-terminal on the wire as well as in the database. BAD_REQUEST_PARAMS is the one code
                    // the error handler turns into a 400, and a 400 is what a caller reads as the end of the
                    // automatic path - so a 404 that arrives as one would strand the operator even with the
                    // token still stored.
                    .asInstanceOf(throwable(ThingsboardException.class))
                    .extracting(ThingsboardException::getErrorCode)
                    .isEqualTo(ThingsboardErrorCode.GENERAL);

            // Neither form of the clear: the token this poll used is still the stored one, so the next poll
            // retries the same claim instead of settling on the manual form for good.
            verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
            verify(tbClusterStore, never()).forceClearLicenseClaimToken();
        } finally {
            if (previousEndpoint == null) {
                System.clearProperty(LICENSE_SERVER_PROPERTY);
            } else {
                System.setProperty(LICENSE_SERVER_PROPERTY, previousEndpoint);
            }
            portal.stop(0);
        }
    }

    @Test
    public void anUnreachablePortalSurfacesTheEndpointAndTheManualFallback() throws Exception {
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("claim-token"));
        when(licensePortalClient.claimFreeLicense("claim-token")).thenThrow(new LicenseException(
                "Could not reach the ThingsBoard license server at https://license.thingsboard.io. "
                        + "Check that this instance has outbound internet access, or configure a proxy.",
                LicenseErrorCode.CONNECTION_ERROR));

        assertThatThrownBy(() -> systemSetupService.pollClaim(null))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("https://license.thingsboard.io")
                .hasMessageContaining("configure a proxy. You can also enter the license key manually.");

        // Not terminal for the claim itself - connectivity comes back - so the token stays.
        verify(tbClusterStore, never()).forceClearLicenseClaimToken();
        verify(tbClusterStore, never()).clearLicenseClaimToken(any());
    }

    @Test
    public void aRateLimitedClaimIsReportedAsTooManyRequestsInTheProductsOwnWords() throws Exception {
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("claim-token"));
        when(licensePortalClient.claimFreeLicense("claim-token")).thenThrow(new LicenseException(
                "Rate limit exceeded: 6 requests in 60s from 203.0.113.7", LicenseErrorCode.RATE_LIMITED));

        assertThatThrownBy(() -> systemSetupService.pollClaim(null))
                .isInstanceOf(ThingsboardException.class)
                // The only arm that replaces the upstream text rather than passing it through, and the
                // upstream text is what a rate limiter says about other callers - not for an anonymous body.
                .hasMessage("Too many attempts. Please try again in a few minutes. "
                        + "You can also enter the license key manually.")
                .hasMessageNotContaining("203.0.113.7")
                // And the only arm that produces something other than GENERAL. The wizard's retry behaviour is
                // keyed off the status code, so a switch edit that collapsed this into the default arm would
                // turn a throttled claim into a 500 with nothing going red.
                .asInstanceOf(throwable(ThingsboardException.class))
                .extracting(ThingsboardException::getErrorCode)
                .isEqualTo(ThingsboardErrorCode.TOO_MANY_REQUESTS);
    }


    @Test
    public void previewIsHandedStraightToTheLicenceLayer() {
        // This layer adds nothing to a preview: the instances the deployment runs are filled in by the
        // controller, alongside the other figures that come from outside the licence.
        SubscriptionInfo fromLicenseLayer = new SubscriptionInfo();
        when(subscriptionService.previewLicenseKey("A-KEY")).thenReturn(fromLicenseLayer);

        assertThat(systemSetupService.previewLicenseKey("A-KEY")).isSameAs(fromLicenseLayer);
    }

    @Test
    public void applyingAKeyReportsTheStateAndTheLicenceItLeftInForce() {
        // One round trip: the page that applied the key learns both whether the lock lifted and what the
        // deployment now runs on, rather than making a second request a locked instance might refuse.
        SubscriptionInfo subscriptionInfo = new SubscriptionInfo();
        subscriptionInfo.setSubscriptionPlanName("Enterprise");
        when(subscriptionService.isLicenseActivated()).thenReturn(true);
        when(userService.existsByAuthority(Authority.SYS_ADMIN)).thenReturn(true);
        when(nonProductionConfirmationService.isLapsed()).thenReturn(false);
        when(subscriptionService.getSubscriptionInfo()).thenReturn(subscriptionInfo);

        LicenseChangeResult result = systemSetupService.applyLicenseKeyAndReport("A-KEY");

        verify(subscriptionService).applyLicenseKey("A-KEY");
        assertThat(result.getStatus()).isEqualTo(SystemSetupState.READY);
        assertThat(result.getSubscription()).isSameAs(subscriptionInfo);
    }

    @Test
    public void aKeyThatTookEffectIsNotReportedAsARefusalWhenTheLicenceCannotBeReadBack() {
        // getSubscriptionInfo refuses whenever this node holds no client, and a critical error or a peer's
        // clear can take the client away between the apply and the read. Letting that refusal out would tell
        // the operator that a key which is applied, persisted and broadcast was rejected.
        when(subscriptionService.isLicenseActivated()).thenReturn(true);
        when(userService.existsByAuthority(Authority.SYS_ADMIN)).thenReturn(true);
        when(nonProductionConfirmationService.isLapsed()).thenReturn(false);
        doThrow(new SubscriptionException("the instance is not activated", SubscriptionErrorCode.FEATURE_DISABLED))
                .when(subscriptionService).getSubscriptionInfo();

        LicenseChangeResult result = systemSetupService.applyLicenseKeyAndReport("A-KEY");

        verify(subscriptionService).applyLicenseKey("A-KEY");
        assertThat(result.getStatus()).isEqualTo(SystemSetupState.READY);
        assertThat(result.getSubscription()).isNull();
    }

    @Test
    public void aFailingApplyLicenseKeyLeavesTheTokenInPlaceForTheNextPoll() throws Exception {
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("claim-token"));
        when(licensePortalClient.claimFreeLicense("claim-token"))
                .thenReturn(FreeLicenseClaimResponse.activated("THE-SECRET"));
        // The real type and one of the messages applyLicenseKey throws on a key the portal will not validate.
        doThrow(new SubscriptionException("That key wasn't recognized. Copy it from the ThingsBoard portal " +
                "again, end to end.", SubscriptionErrorCode.FEATURE_DISABLED))
                .when(subscriptionService).applyLicenseKey("THE-SECRET");

        assertThatThrownBy(() -> systemSetupService.pollClaim(null))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("could not be applied")
                // Fixed message: this body is reachable anonymously, and the underlying failure may be a raw
                // database error carrying the UPDATE statement and the PostgreSQL error text.
                .hasMessageNotContaining("That key wasn't recognized");

        // The claim is idempotent within its window, so the next poll retries instead of permanently
        // killing the automatic path.
        verify(tbClusterStore, never()).forceClearLicenseClaimToken();
        verify(tbClusterStore, never()).clearLicenseClaimToken(any());
    }

    @Test
    public void aTerminalVerdictOnARetiredTokenDoesNotSupersedeAFreshClaim() throws Exception {
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("claim-token"));
        when(licensePortalClient.isPortalReachable()).thenReturn(true);
        when(licensePortalClient.getPortalBaseUrl()).thenReturn("https://license.thingsboard.io");
        when(licenseCtx.getClusterId()).thenReturn(CLUSTER_ID);
        when(licensePortalClient.claimFreeLicense("claim-token")).thenAnswer(invocation -> {
            // The operator starts a fresh claim while this poll is out - the reason the poll's verdict below
            // describes a token nobody is waiting on any more.
            systemSetupService.requestClaim();
            throw new LicenseException("rejected", LicenseErrorCode.CLAIM_LICENSE_REJECTED);
        });

        // EXPIRED is terminal and the wizard stops polling on it, so publishing it here would abandon the
        // claim the operator has just started.
        assertThat(systemSetupService.pollClaim(null).getStatus()).isEqualTo(LicenseClaimStatus.PENDING);
    }

    @Test
    public void aPollOnASupersededTokenIsExpiredWithoutTouchingTheLiveClaim() throws Exception {
        // Two browser sessions started the activation, and the second mint replaced the first session's token.
        // The first session is watching a link nobody can activate any more; naming its own token is the only
        // way this endpoint can tell it so, since the stored claim it would otherwise report on is alive.
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("second-session-token"));
        // Inside the throttle window, whose cached status describes the surviving session's claim - so a
        // mismatch decided after it would answer for the wrong session.
        ReflectionTestUtils.setField(systemSetupService, "lastClaimAttemptTs", System.currentTimeMillis());

        assertThat(systemSetupService.pollClaim("first-session-token").getStatus())
                .isEqualTo(LicenseClaimStatus.EXPIRED);

        // Decided locally: the portal has nothing to say about a token this instance no longer holds.
        verify(licensePortalClient, never()).claimFreeLicense(any());
        // The stored token is the surviving session's and is alive. Retiring it here would let any caller
        // holding a stale token kill the one claim that can still complete.
        verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
        verify(tbClusterStore, never()).forceClearLicenseClaimToken();
    }

    @Test
    public void theSurvivingSessionKeepsPollingAfterAnotherSessionIsToldItsLinkIsDead() throws Exception {
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("second-session-token"));
        when(licensePortalClient.claimFreeLicense("second-session-token"))
                .thenReturn(FreeLicenseClaimResponse.pending());

        assertThat(systemSetupService.pollClaim("first-session-token").getStatus())
                .isEqualTo(LicenseClaimStatus.EXPIRED);

        assertThat(systemSetupService.pollClaim("second-session-token").getStatus())
                .isEqualTo(LicenseClaimStatus.PENDING);
        verify(licensePortalClient).claimFreeLicense("second-session-token");
    }

    @Test
    public void aPollNamingTheStoredTokenClaimsItJustAsAPollNamingNoneDoes() throws Exception {
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("claim-token"));
        when(licensePortalClient.claimFreeLicense("claim-token"))
                .thenReturn(FreeLicenseClaimResponse.activated("THE-SECRET"));

        assertThat(systemSetupService.pollClaim("claim-token").getStatus()).isEqualTo(LicenseClaimStatus.ACTIVATED);
        verify(subscriptionService).applyLicenseKey("THE-SECRET");
    }

    @Test
    public void anEmptyClaimTokenIsReadAsNoneRatherThanAsOneThatMatchesNothing() throws Exception {
        // What a client sends when it holds no token yet. Compared as a token it would match nothing, and the
        // automatic path would end on the very first poll.
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("claim-token"));
        when(licensePortalClient.claimFreeLicense("claim-token")).thenReturn(FreeLicenseClaimResponse.pending());

        assertThat(systemSetupService.pollClaim("").getStatus()).isEqualTo(LicenseClaimStatus.PENDING);
    }

    @Test
    public void anActivatedInstanceReportsActivatedEvenToASessionWhoseTokenWasSuperseded() throws Exception {
        // The licence most likely landed through the very token that replaced this caller's. EXPIRED here
        // would send a session back to the manual key form of an instance that needs no key.
        when(subscriptionService.isLicenseActivated()).thenReturn(true);

        assertThat(systemSetupService.pollClaim("first-session-token").getStatus())
                .isEqualTo(LicenseClaimStatus.ACTIVATED);
        verify(tbClusterStore, never()).getLicenseClaimToken();
    }

    @Test
    public void theThrottleCollapsesRapidPollsIntoOneOutboundCall() throws Exception {
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("claim-token"));
        when(licensePortalClient.claimFreeLicense("claim-token")).thenReturn(FreeLicenseClaimResponse.pending());

        for (int i = 0; i < 10; i++) {
            assertThat(systemSetupService.pollClaim(null).getStatus()).isEqualTo(LicenseClaimStatus.PENDING);
        }

        // Without this the endpoint is a request amplifier: it is unauthenticated, and every call would
        // become an outbound portal call.
        verify(licensePortalClient, times(1)).claimFreeLicense("claim-token");
    }

    @Test
    public void aPollAfterTheThrottleWindowIssuesAFreshOutboundCall() throws Exception {
        // The other direction of the throttle, and the one that actually strands the wizard if it breaks: a
        // throttle that latched permanently would collapse every poll for good and leave the operator on a
        // stale PENDING, which the test above would not notice.
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("claim-token"));
        when(licensePortalClient.claimFreeLicense("claim-token")).thenReturn(FreeLicenseClaimResponse.pending());

        assertThat(systemSetupService.pollClaim(null).getStatus()).isEqualTo(LicenseClaimStatus.PENDING);
        verify(licensePortalClient, times(1)).claimFreeLicense("claim-token");

        // Elapses the window instead of sleeping through it, so the suite does not pay MIN_CLAIM_INTERVAL_MS.
        ReflectionTestUtils.setField(systemSetupService, "lastClaimAttemptTs", 0L);

        assertThat(systemSetupService.pollClaim(null).getStatus()).isEqualTo(LicenseClaimStatus.PENDING);
        verify(licensePortalClient, times(2)).claimFreeLicense("claim-token");
    }

    @Test
    public void concurrentPollsSingleFlightOntoOneOutboundCallWhileAClaimIsInFlight() throws Exception {
        when(subscriptionService.isLicenseActivated()).thenReturn(false);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("claim-token"));

        CountDownLatch portalCallEntered = new CountDownLatch(1);
        CountDownLatch portalCallReleased = new CountDownLatch(1);
        when(licensePortalClient.claimFreeLicense("claim-token")).thenAnswer(invocation -> {
            // The throttle has two independent halves, and this test is about the single-flight lock only.
            // Reopening the time window from inside the in-flight call is what makes that so: otherwise the
            // 2-second window alone would keep the other pollers out and the test would still pass with the
            // lock deleted. A real in-flight claim outlives the window just as easily - the portal client's
            // read timeout is several times longer than it.
            ReflectionTestUtils.setField(systemSetupService, "lastClaimAttemptTs", 0L);
            portalCallEntered.countDown();
            assertThat(portalCallReleased.await(30, TimeUnit.SECONDS)).isTrue();
            return FreeLicenseClaimResponse.pending();
        });

        // Seeded with something other than the field's initial value, which is PENDING: without this the shed
        // polls below cannot tell "answered from the last known status" from "answered from a field nobody has
        // written yet", and the assertion on them could not fail.
        ReflectionTestUtils.setField(systemSetupService, "lastClaimStatus", LicenseClaimStatus.EXPIRED);

        int concurrentPollers = 8;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentPollers);
        try {
            Future<LicenseClaimStatus> inFlightPoll = executor.submit(() -> systemSetupService.pollClaim(null).getStatus());
            assertThat(portalCallEntered.await(30, TimeUnit.SECONDS)).isTrue();

            List<Future<LicenseClaimStatus>> concurrentPolls = new ArrayList<>();
            for (int i = 0; i < concurrentPollers - 1; i++) {
                concurrentPolls.add(executor.submit(() -> systemSetupService.pollClaim(null).getStatus()));
            }
            for (Future<LicenseClaimStatus> concurrentPoll : concurrentPolls) {
                // Answered from the last known status - the seeded one, not the default - instead of queueing
                // behind the in-flight claim.
                assertThat(concurrentPoll.get(30, TimeUnit.SECONDS)).isEqualTo(LicenseClaimStatus.EXPIRED);
            }
            // The defence the unauthenticated endpoint rests on: N concurrent callers, one outbound call.
            verify(licensePortalClient, times(1)).claimFreeLicense("claim-token");

            portalCallReleased.countDown();
            assertThat(inFlightPoll.get(30, TimeUnit.SECONDS)).isEqualTo(LicenseClaimStatus.PENDING);
            verify(licensePortalClient, times(1)).claimFreeLicense("claim-token");
        } finally {
            portalCallReleased.countDown();
            executor.shutdownNow();
        }
    }


    /** A stand-in portal that answers every path with one status, so the client's mapping of it is what is tested. */
    private static HttpServer startPortalAnswering(int responseStatus) throws IOException {
        HttpServer portal = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        portal.createContext("/", exchange -> {
            exchange.sendResponseHeaders(responseStatus, -1);
            exchange.close();
        });
        portal.start();
        return portal;
    }

    private static String portalBaseUrl(HttpServer portal) {
        return "http://localhost:" + portal.getAddress().getPort();
    }

}
