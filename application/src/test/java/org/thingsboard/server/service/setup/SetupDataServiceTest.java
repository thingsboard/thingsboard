// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.setup;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.DataValidationException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = "transport.http.enabled=true")
public class SetupDataServiceTest extends AbstractControllerTest {

    private static final String SYS_ADMIN_EMAIL = "admin2@thingsboard.org";
    private static final String SYS_ADMIN_PASSWORD = "StrongPass1";
    private static final String REJECTED_SYS_ADMIN_EMAIL = "admin3@thingsboard.org";

    private static final String DEMO_TENANT_TITLE = "Tenant";
    private static final String DEMO_CUSTOMER_EMAIL = "customer@thingsboard.org";
    private static final String DEMO_CUSTOMER_PASSWORD = "customer";
    private static final String THERMOSTAT_PROFILE_NAME = "thermostat";
    private static final String DEMO_DEVICE_NAME = "Test Device A1";
    private static final String DEMO_DEVICE_TOKEN = "A1_TEST_TOKEN";
    private static final String TELEMETRY_KEY = "temperature";
    private static final long TELEMETRY_VALUE = 42L;

    @Autowired
    private SetupDataService setupDataService;
    @Autowired
    private UserService userService;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private DeviceProfileService deviceProfileService;
    @Autowired
    private CalculatedFieldService calculatedFieldService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private TimeseriesService timeseriesService;

    /**
     * The system tenant's users survive {@code AbstractWebTest.teardownWebTest()} - it deletes tenants, tenant
     * profiles and the notification/audit tables, and nothing that touches them. A stray system administrator
     * left behind here would reach every later class sharing this database, and sysadmin existence is exactly
     * what drives {@code SystemSetupState}, so it is the kind of leftover that produces order-dependent
     * failures somewhere far away from this file.
     */
    @After
    public void deleteCreatedSysAdmins() {
        deleteSysAdminIfPresent(SYS_ADMIN_EMAIL);
        deleteSysAdminIfPresent(REJECTED_SYS_ADMIN_EMAIL);
    }

    @Test
    public void testCreateSysAdminCreatesEnabledUserThatCanLogIn() throws Exception {
        setupDataService.createSysAdmin(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);
        login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);

        // A successful login proves only that some enabled user was created: without this the test would stay
        // green if the authority regressed to CUSTOMER_USER.
        User created = doGet("/api/auth/user", User.class);
        assertThat(created.getAuthority()).isEqualTo(Authority.SYS_ADMIN);
    }

    /**
     * The one genuinely new behaviour in this otherwise moved class: the password arrives from an
     * unauthenticated caller through {@code /api/noauth/setup/complete}, so it has to satisfy the same policy
     * as any other password the platform accepts. Deleting that line would otherwise leave the suite green.
     */
    @Test
    public void testCreateSysAdminRejectsAPasswordThatViolatesThePolicy() {
        assertThatThrownBy(() -> setupDataService.createSysAdmin(REJECTED_SYS_ADMIN_EMAIL, "123"))
                .isInstanceOf(DataValidationException.class);

        // The validation runs before the user is created, so a rejected password must leave nothing behind -
        // an existing sysadmin row would report the account step as done and lock the operator out of it.
        assertThat(userService.findUserByEmail(TenantId.SYS_TENANT_ID, REJECTED_SYS_ADMIN_EMAIL)).isNull();
    }

    /**
     * Demo-data creation moved out of install time and into the runtime wizard with this change, which left it
     * covered by nothing at all - the only other reference to it in the codebase is a mocked call. Scoped to a
     * smoke test rather than an asset-by-asset assertion: the demo tenant, a demo user that can actually sign
     * in, and the thermostat profile's alarm calculated fields, which are the newest and most intricate part
     * of what it builds.
     */
    @Test
    public void testLoadDemoDataCreatesTheDemoTenantItsUsersAndTheThermostatAlarmRules() throws Exception {
        setupDataService.loadDemoData();

        Tenant demoTenant = findDemoTenant();
        assertThat(demoTenant).as("demo tenant").isNotNull();

        DeviceProfile thermostatProfile = deviceProfileService.findDeviceProfileByName(
                demoTenant.getId(), THERMOSTAT_PROFILE_NAME);
        assertThat(thermostatProfile).as("thermostat device profile").isNotNull();
        List<CalculatedField> alarmRules = calculatedFieldService.findCalculatedFieldsByEntityId(
                demoTenant.getId(), thermostatProfile.getId());
        assertThat(alarmRules).hasSize(2);
        assertThat(alarmRules).allSatisfy(alarmRule ->
                assertThat(alarmRule.getType()).isEqualTo(CalculatedFieldType.ALARM));
        assertThat(alarmRules).extracting(CalculatedField::getName)
                .containsExactlyInAnyOrder("High Temperature", "Low Humidity");

        // The demo users are only useful if they can actually sign in, which is a second write (the password)
        // on top of the user row.
        login(DEMO_CUSTOMER_EMAIL, DEMO_CUSTOMER_PASSWORD);

        // Removed here rather than left to the suite's stray-tenant sweep, so the cost of the delete is paid
        // by the test that created it.
        loginSysAdmin();
        deleteTenant(demoTenant.getId());
    }

    /**
     * The demo tenant's rule chains must be committed inside its own transaction: the CREATED event fires on that
     * commit and builds the tenant actor, which reads the chains once and never learns of any saved afterwards.
     */
    @Test
    public void testLoadDemoDataLeavesTheDemoTenantAbleToStoreDeviceTelemetry() throws Exception {
        setupDataService.loadDemoData();

        Tenant demoTenant = findDemoTenant();
        assertThat(demoTenant).as("demo tenant").isNotNull();
        Device demoDevice = deviceService.findDeviceByTenantIdAndName(demoTenant.getId(), DEMO_DEVICE_NAME);
        assertThat(demoDevice).as("demo device").isNotNull();

        mockMvc.perform(asyncDispatch(doPost("/api/v1/" + DEMO_DEVICE_TOKEN + "/telemetry",
                        Map.of(TELEMETRY_KEY, TELEMETRY_VALUE), new String[]{}).andReturn()))
                .andExpect(status().isOk());

        Awaitility.await("demo device telemetry is saved by the demo tenant's root rule chain")
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    Optional<TsKvEntry> saved = timeseriesService.findLatest(
                            demoTenant.getId(), demoDevice.getId(), TELEMETRY_KEY).get();
                    assertThat(saved).as("latest '%s' of the demo device", TELEMETRY_KEY).isPresent();
                    assertThat(saved.get().getLongValue()).contains(TELEMETRY_VALUE);
                });

        loginSysAdmin();
        deleteTenant(demoTenant.getId());
    }

    private Tenant findDemoTenant() {
        PageData<Tenant> tenants = tenantService.findTenants(new PageLink(100));
        return tenants.getData().stream()
                .filter(tenant -> DEMO_TENANT_TITLE.equals(tenant.getTitle()))
                .findFirst()
                .orElse(null);
    }

    private void deleteSysAdminIfPresent(String email) {
        User user = userService.findUserByEmail(TenantId.SYS_TENANT_ID, email);
        if (user != null) {
            userService.deleteUser(TenantId.SYS_TENANT_ID, user);
        }
    }

}
