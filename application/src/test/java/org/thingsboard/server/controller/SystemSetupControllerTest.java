// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.thingsboard.server.common.data.setup.LicenseClaimStatus;
import org.thingsboard.server.common.data.setup.LicenseKeyRequest;
import org.thingsboard.server.common.data.setup.SystemSetupRequest;
import org.thingsboard.server.common.data.setup.SystemSetupState;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class SystemSetupControllerTest extends AbstractControllerTest {

    // The sysadmin is created by the test bootstrap, and the test profile subscription service always reports the
    // license as activated, so the real setup state in this harness is READY. The locked states are simply not
    // exercised by this class - they are reachable through the real filter and security chain by overriding the
    // beans the state is computed from, as NonProductionConfirmationTest does, at the cost of a separate Spring
    // test context. The filter's own behaviour while locked is covered by SystemSetupFilterTest, and the
    // controller's behaviour in the incomplete states by SystemSetupControllerUnitTest.

    @Test
    public void getStateIsReachableAnonymouslyAndReportsReady() throws Exception {
        resetTokens();

        doGet("/api/noauth/setup/state")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(SystemSetupState.READY.name())));
    }

    @Test
    public void setupEndpointsRejectAnonymousCallsOnceSetupIsComplete() throws Exception {
        resetTokens();

        String expectedMessage = "This setup step is not applicable in the " + SystemSetupState.READY + " state";

        LicenseKeyRequest licenseKeyRequest = new LicenseKeyRequest();
        licenseKeyRequest.setSecret("SECRET");
        doPost("/api/noauth/setup/license", licenseKeyRequest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(expectedMessage)));

        doPost("/api/noauth/setup/claim")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(expectedMessage)));

        SystemSetupRequest systemSetupRequest = new SystemSetupRequest();
        systemSetupRequest.setEmail("takeover@thingsboard.org");
        systemSetupRequest.setPassword("StrongPass1");
        doPost("/api/noauth/setup/complete", systemSetupRequest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(expectedMessage)));
    }

    @Test
    public void getClaimIsReachableAnonymouslyOnAnActivatedInstance() throws Exception {
        resetTokens();

        // Unlike the other setup endpoints this one answers rather than 400s once setup is complete. The
        // answer comes from the already-activated short-circuit, which returns before the claim token is
        // ever read: the license is in place, so it reports ACTIVATED and makes no outbound call. A wizard
        // that missed the ACTIVATED of the claiming poll therefore still learns it can move on, instead of
        // being sent back to the manual key form.
        doGet("/api/noauth/setup/claim")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(LicenseClaimStatus.ACTIVATED.name())));

        // Named token or not, the short-circuit answers first: whichever claim delivered the licence, a
        // session still holding a superseded token must be told to move on rather than that its link is dead.
        doGet("/api/noauth/setup/claim?claimToken=a-token-no-longer-stored")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(LicenseClaimStatus.ACTIVATED.name())));
    }

    @Test
    public void clearLicenseIsSysAdminOnly() throws Exception {
        resetTokens();
        doPost("/api/admin/license/clear").andExpect(status().isUnauthorized());

        loginTenantAdmin();
        doPost("/api/admin/license/clear").andExpect(status().isForbidden());
    }

    @Test
    public void clearLicenseAnswersWithTheResultingSetupState() throws Exception {
        // The one round trip a system administrator recovering an instance gets: whether the deployment is
        // ready is read off this body rather than from a follow-up call. The test profile's subscription
        // service keeps reporting the license as activated, so the state after clearing is still READY - what
        // this pins is the endpoint's contract, not the state transition.
        loginSysAdmin();

        doPost("/api/admin/license/clear")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(SystemSetupState.READY.name())));
    }

    @Test
    public void changeLicenseKeyIsSysAdminOnly() throws Exception {
        LicenseKeyRequest licenseKeyRequest = new LicenseKeyRequest();
        licenseKeyRequest.setSecret("SECRET");

        resetTokens();
        doPost("/api/admin/license/key", licenseKeyRequest).andExpect(status().isUnauthorized());

        loginTenantAdmin();
        doPost("/api/admin/license/key", licenseKeyRequest).andExpect(status().isForbidden());

        loginSysAdmin();
        doPost("/api/admin/license/key", licenseKeyRequest).andExpect(status().isOk());
    }

    @Test
    public void changeLicenseKeyAnswersWithTheResultingSetupState() throws Exception {
        // The round trip the licence-management page relies on: the resulting state comes back with the key,
        // so a system administrator who has just unlocked an instance is not sent to make a second request
        // the instance might still have been refusing a moment earlier. The test profile's subscription
        // service reports no subscription, so what this pins is the body's shape, not the licence in it.
        LicenseKeyRequest licenseKeyRequest = new LicenseKeyRequest();
        licenseKeyRequest.setSecret("SECRET");
        loginSysAdmin();

        doPost("/api/admin/license/key", licenseKeyRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(SystemSetupState.READY.name())));
    }

    @Test
    public void previewLicenseKeyIsSysAdminOnly() throws Exception {
        // A preview reads a licence the deployment does not hold, so it is guarded exactly like the key
        // itself - and, like the key, it stays reachable while the instance is locked, which is the state an
        // administrator is in when they need it.
        LicenseKeyRequest licenseKeyRequest = new LicenseKeyRequest();
        licenseKeyRequest.setSecret("SECRET");

        resetTokens();
        doPost("/api/admin/license/preview", licenseKeyRequest).andExpect(status().isUnauthorized());

        loginTenantAdmin();
        doPost("/api/admin/license/preview", licenseKeyRequest).andExpect(status().isForbidden());
    }

}
