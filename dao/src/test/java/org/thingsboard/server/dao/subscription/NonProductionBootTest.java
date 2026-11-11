// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.license.client.NonProductionTbLicenseClient;
import org.thingsboard.license.client.TbLicenseClient;
import org.thingsboard.license.shared.exception.LicenseErrorCode;
import org.thingsboard.license.shared.exception.LicenseException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NonProductionBootTest {

    private BasicLicenseActivationService licenseActivationService;
    private TbClusterStore tbClusterStore;
    private AbstractTbLicenseClient client;
    private AbstractTbLicenseClient secretBranchClient;

    @BeforeEach
    public void setUp() throws Exception {
        licenseActivationService = spy(new BasicLicenseActivationService());
        // Stubbed rather than exercised for real: a real createLicenseClient would reach out to the license
        // portal and touch an instance-data file on disk. What these tests pin is routing - whether the
        // secret or the non-production branch is taken - not the secret-branch client's own behaviour,
        // which belongs to createLicenseClient's own tests.
        secretBranchClient = mock(TbLicenseClient.class);
        doReturn(secretBranchClient).when(licenseActivationService).createLicenseClient(any());
        // spy(new BasicLicenseActivationService()) never goes through Spring, so the @Autowired field stays
        // unset unless wired here by hand.
        tbClusterStore = mock(TbClusterStore.class);
        ReflectionTestUtils.setField(licenseActivationService, "tbClusterStore", tbClusterStore);
    }

    @AfterEach
    public void tearDown() {
        if (client != null) {
            client.stop();
        }
    }

    private void givenNonProductionUse(boolean nonProductionUse) {
        ReflectionTestUtils.setField(licenseActivationService, "nonProductionUse", nonProductionUse);
    }

    @Test
    public void aLicenceSecretWinsOverTheNonProductionEnvironmentVariable() throws Exception {
        // A licensed deployment that ships NON_PRODUCTION_USE=true in a compose file must keep its licence,
        // its white-labeling and its uptime, rather than being downgraded by a stale variable.
        givenNonProductionUse(true);

        client = licenseActivationService.selectLicenseClient("a-real-secret");

        // The client the secret branch produced, not merely "something that is not the keyless one": the
        // stub is a TbLicenseClient, so an isNotInstanceOf check against the keyless class could never fail.
        assertThat(client).isSameAs(secretBranchClient);
        verify(licenseActivationService, never()).createNonProductionClient();
    }

    @Test
    public void theEnvironmentVariableIsUsedOnlyWhenThereIsNoSecret() throws Exception {
        givenNonProductionUse(true);

        client = licenseActivationService.selectLicenseClient(null);

        assertThat(client).isInstanceOf(NonProductionTbLicenseClient.class);
    }

    @Test
    public void withoutSecretAndWithoutTheVariableTheInstanceStaysUnactivated() throws Exception {
        givenNonProductionUse(false);

        assertThat(licenseActivationService.selectLicenseClient(null)).isNull();
    }

    @Test
    public void noClientIsGrantedOnceTheNonProductionLimitIsExhausted() {
        // givenNonProductionUse(true) is load-bearing: without the flag selectLicenseClient(null) returns
        // null from its third branch and never consults the uptime at all, so the test would pass for the
        // wrong reason - or rather fail, since nothing is thrown.
        givenNonProductionUse(true);
        when(tbClusterStore.getNonProductionUptimeMs())
                .thenReturn(TbClusterStore.NON_PRODUCTION_UPTIME_LIMIT_MS + 1);

        assertThatThrownBy(() -> licenseActivationService.selectLicenseClient(null))
                .isInstanceOf(NonProductionAllowanceExhaustedException.class)
                .hasMessageContaining("non-production")
                .hasMessageContaining("TB_LICENSE_SECRET");
    }

    @Test
    public void theLimitItselfAlreadyCountsAsExhausted() {
        // Pins the ">=" in the enforcement check: stubbing the exact limit (rather than limit + 1, as every
        // other test here does) is what would catch a ">" typo letting one extra tick through.
        givenNonProductionUse(true);
        when(tbClusterStore.getNonProductionUptimeMs())
                .thenReturn(TbClusterStore.NON_PRODUCTION_UPTIME_LIMIT_MS);

        assertThatThrownBy(() -> licenseActivationService.selectLicenseClient(null))
                .isInstanceOf(NonProductionAllowanceExhaustedException.class);
    }

    @Test
    public void oneMillisecondBelowTheLimitAClientIsStillGranted() throws Exception {
        // The other side of the boundary: covered so far only incidentally by the mock's default 0. Without
        // this, flipping ">=" to "always throw" would still pass every other test in this class.
        givenNonProductionUse(true);
        when(tbClusterStore.getNonProductionUptimeMs())
                .thenReturn(TbClusterStore.NON_PRODUCTION_UPTIME_LIMIT_MS - 1);

        client = licenseActivationService.selectLicenseClient(null);

        assertThat(client).isInstanceOf(NonProductionTbLicenseClient.class);
    }

    @Test
    public void aKeylessBootWithTheAllowanceSpentFailsStartup() {
        // Booting on regardless would bring the node straight back up serving the data plane with the
        // allowance already spent, which is what makes the cap toothless.
        when(tbClusterStore.getNonProductionUptimeMs())
                .thenReturn(TbClusterStore.NON_PRODUCTION_UPTIME_LIMIT_MS + 1);
        givenNonProductionUse(true);

        assertThatThrownBy(() -> licenseActivationService.init())
                .isInstanceOf(NonProductionAllowanceExhaustedException.class);
    }

    @Test
    public void anActivationFailureOnARealSecretStillBootsIntoTheLockedState() throws Exception {
        // Only the keyless path fails startup. Every other failure has to keep booting into LICENSE_REQUIRED,
        // because that is the state the activation UI is reachable in.
        givenNonProductionUse(false);
        when(tbClusterStore.getLicenseSecret()).thenReturn(Optional.of("a-real-secret"));
        doThrow(new LicenseException("rejected", LicenseErrorCode.GENERAL_ERROR))
                .when(licenseActivationService).createLicenseClient(any());

        assertThatCode(() -> licenseActivationService.init()).doesNotThrowAnyException();
        assertThat(licenseActivationService.isLicenseActivated()).isFalse();
    }

    @Test
    public void aKeylessInstanceBootsActivatedAndInNonProductionMode() {
        // The success end state of the same boot path, which every other test here approaches only through
        // selectLicenseClient: init() has to publish the keyless client and mark the instance activated, so
        // that a deployment with no key is usable rather than merely not refused. Without this, an init()
        // that built the client and then dropped it would be caught by nothing.
        givenNonProductionUse(true);
        // The uptime is left at the mock's default 0 - below the limit - and no secret is stored, which is
        // exactly the state a first keyless boot starts from.

        licenseActivationService.init();
        client = (AbstractTbLicenseClient) ReflectionTestUtils.getField(licenseActivationService, "tbLicenseClient");

        assertThat(licenseActivationService.isLicenseActivated()).isTrue();
        assertThat(licenseActivationService.isNonProductionMode()).isTrue();
        assertThat(client).isInstanceOf(NonProductionTbLicenseClient.class);
    }

    @Test
    public void anExhaustedTimerDoesNotAffectALicensedInstance() {
        // The timer is only ever read on the keyless path, so a licensed deployment never consults it. The
        // flag is set precisely to prove the secret still wins with the timer exhausted.
        givenNonProductionUse(true);
        when(tbClusterStore.getNonProductionUptimeMs())
                .thenReturn(TbClusterStore.NON_PRODUCTION_UPTIME_LIMIT_MS + 1);

        assertThatCode(() -> licenseActivationService.selectLicenseClient("a-real-secret"))
                .doesNotThrowAnyException();
        verify(tbClusterStore, never()).getNonProductionUptimeMs();
    }

}
