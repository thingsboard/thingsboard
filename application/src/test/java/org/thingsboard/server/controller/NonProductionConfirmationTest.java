// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.setup.SystemSetupState;
import org.thingsboard.server.config.SystemSetupFilter;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.dao.subscription.TbClusterStore;
import org.thingsboard.server.exception.SystemSetupIncompleteException;
import org.thingsboard.server.service.license.NonProductionConfirmationService;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A lapsed non-production confirmation locks the management plane (423, {@link SystemSetupIncompleteException}) the
 * same way an incomplete first-time setup does, while device telemetry - the data plane - keeps flowing, and a
 * system administrator can still sign in and clear the lock. See {@link NonProductionConfirmationService} for the
 * confirmation state itself and {@link SystemSetupFilter} for the gate that enforces the lock.
 */
@DaoSqlTest
// The test profile disables the HTTP device transport (application-test.properties), so DeviceApiController is
// never registered unless a test opts back in - without this, the telemetry POST below would 404 rather than
// exercise the setup lock at all.
@TestPropertySource(properties = {"transport.http.enabled=true"})
public class NonProductionConfirmationTest extends AbstractControllerTest {

    @MockitoSpyBean
    private SubscriptionService subscriptionService;

    @Autowired
    private TbClusterStore tbClusterStore;

    @Autowired
    private NonProductionConfirmationService nonProductionConfirmationService;

    private DeviceCredentials deviceCredentials;

    private boolean confirmationSeeded;

    private Long previousConfirmedTs;

    @Test
    public void managementApiIsLockedWhenTheConfirmationHasLapsed() throws Exception {
        givenLapsedNonProductionConfirmation();
        loginTenantAdmin();

        doLockAwareGet("/api/tenant/devices?pageSize=10&page=0")
                .andExpect(status().isLocked());
    }

    @Test
    public void theLockedResponseCarriesTheFixedConfirmationPromptAndTheNonProductionHeader() throws Exception {
        // The dialog a tenant admin or customer user sees while locked reads its copy from this same response,
        // and the header that marks every response as non-production must not be swallowed by the lock either.
        givenLapsedNonProductionConfirmation();
        loginTenantAdmin();

        doLockAwareGet("/api/tenant/devices?pageSize=10&page=0")
                .andExpect(status().isLocked())
                .andExpect(header().string("X-ThingsBoard-Non-Production", "true"))
                .andExpect(jsonPath("$.message", is(DataConstants.NON_PRODUCTION_CONFIRMATION_PROMPT)))
                .andExpect(jsonPath("$.setupState", is(SystemSetupState.NON_PRODUCTION_CONFIRMATION_REQUIRED.name())));
    }

    @Test
    public void deviceTelemetryKeepsFlowingWhileTheManagementPlaneIsLocked() throws Exception {
        // Provisioned before the lock: once locked, the management API that creates a device is exactly what
        // is blocked.
        givenDevice();

        // Block management, keep running. What this asserts is end-to-end: with the instance locked, a device
        // that was already provisioned still gets a 200 for its telemetry. It does NOT pin the allowlist entry
        // that makes that true in production, and would stay green with the device API removed from
        // SETUP_ALLOWED_ENTRY_POINTS - /api/v1/** is matched by its own higher-precedence SecurityFilterChain,
        // so the FilterChainProxy this harness installs never routes it through SystemSetupFilter at all. The
        // exemption itself is pinned by SystemSetupFilterTest, which exercises the filter directly.
        givenLapsedNonProductionConfirmation();

        JsonNode payload = JacksonUtil.newObjectNode().put("temperature", 42);
        doLockAwareAsyncPost("/api/v1/" + deviceCredentials.getCredentialsId() + "/telemetry", payload)
                .andExpect(status().isOk());
    }

    @Test
    public void aSysAdminCanSignInAndReachTheConfirmationEndpointWhileLocked() throws Exception {
        // Without this the instance locks itself out of its own remedy: login, token refresh, current-user
        // and 2FA are already exempt, and the confirm endpoint has to join them.
        givenLapsedNonProductionConfirmation();
        loginTenantAdmin();
        // Prove the lock is genuinely engaged before showing the sysadmin can still reach the remedy through it.
        doLockAwareGet("/api/tenant/devices?pageSize=10&page=0").andExpect(status().isLocked());

        loginSysAdmin();
        doLockAwarePost("/api/admin/nonProduction/confirm").andExpect(status().isOk());
    }

    @Test
    public void confirmingRestoresTheReadyState() throws Exception {
        givenLapsedNonProductionConfirmation();
        loginTenantAdmin();
        doLockAwareGet("/api/tenant/devices?pageSize=10&page=0").andExpect(status().isLocked());

        loginSysAdmin();
        doLockAwarePost("/api/admin/nonProduction/confirm").andExpect(status().isOk());

        loginTenantAdmin();
        doLockAwareGet("/api/tenant/devices?pageSize=10&page=0").andExpect(status().isOk());
    }

    @Test
    public void aTenantAdminCannotConfirmOnTheSysAdminsBehalf() throws Exception {
        // The declaration is the system administrator's to make. A tenant admin gets the dialog with a
        // "log in as system administrator" button, not the ability to clear the lock.
        givenLapsedNonProductionConfirmation();
        loginTenantAdmin();

        // Lock-aware on purpose: the point is that exempting the confirm endpoint from the setup lock did not
        // also exempt it from the role check, which only a request the lock actually sees can show.
        doLockAwarePost("/api/admin/nonProduction/confirm").andExpect(status().isForbidden());
    }

    @Test
    public void anUnauthenticatedCallerCannotConfirm() throws Exception {
        givenLapsedNonProductionConfirmation();

        doLockAwarePost("/api/admin/nonProduction/confirm").andExpect(status().isUnauthorized());
    }

    private void givenDevice() throws Exception {
        loginTenantAdmin();
        Device device = new Device();
        device.setName("Non-production confirmation test device");
        device.setType("default");
        device = doPost("/api/device", device, Device.class);

        deviceCredentials =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);
    }

    private void givenLapsedNonProductionConfirmation() {
        // Armed before anything below can fail, so the @After always undoes the stubbing and the seeded row.
        confirmationSeeded = true;
        when(subscriptionService.isDevelopment(any())).thenReturn(true);
        previousConfirmedTs = tbClusterStore.getNonProductionConfirmedTs().orElse(null);
        tbClusterStore.saveNonProductionConfirmedTs(
                System.currentTimeMillis() - NonProductionConfirmationService.CONFIRMATION_PERIOD_MS - 1);
        // The confirmation service is a singleton bean shared across this test class - and, through Spring's
        // test context cache, potentially across others with the same bean overrides - so its in-memory cache
        // may already hold a non-lapsed timestamp from earlier activity. Clearing it forces the next isLapsed()
        // check to re-read the row just seeded above instead of answering from stale state.
        ReflectionTestUtils.setField(nonProductionConfirmationService, "confirmedTs", 0L);
    }

    /**
     * Puts the deployment back into the ready state before {@code AbstractWebTest.teardownWebTest} - which runs
     * after this method, per JUnit's subclass-first @After ordering - deletes the test tenant over the very
     * management API the lock blocks.
     * <p>
     * Both halves of the lapsed state have to be undone here by hand. The spy override is only reset once the
     * whole test method has finished, teardown included, so a stubbed {@code isDevelopment} would otherwise keep
     * the confirmation applicable throughout teardown. And the seeded timestamp lives in a cluster-wide row
     * shared by every test class running against this database, so leaving it lapsed would hand the next class
     * a deployment that locks itself the moment anything reports a development licence.
     */
    @After
    public void restorePreviousNonProductionConfirmation() {
        if (!confirmationSeeded) {
            return;
        }
        confirmationSeeded = false;
        reset(subscriptionService);
        if (previousConfirmedTs != null) {
            tbClusterStore.saveNonProductionConfirmedTs(previousConfirmedTs);
        } else {
            // The store reads the timestamp as an optional but writes a primitive, so an absent one cannot be
            // put back through it: a zero would read back as a real confirmation made at the epoch rather than
            // as no confirmation at all, which is what the column held before this class ran.
            jdbcTemplate.update("UPDATE tb_cluster SET non_production_confirmed_ts = NULL");
        }
        ReflectionTestUtils.setField(nonProductionConfirmationService, "confirmedTs", 0L);
        previousConfirmedTs = null;
    }

}
