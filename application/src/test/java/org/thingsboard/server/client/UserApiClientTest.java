// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.AddEntitiesToEntityGroupArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteUserArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteUserSettingsByTypeArgs;
import org.thingsboard.client.api.ThingsboardApi.GetActivationLinkArgs;
import org.thingsboard.client.api.ThingsboardApi.GetActivationLinkInfoArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAllUserInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomerUserInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomerUsersArgs;
import org.thingsboard.client.api.ThingsboardApi.GetMobileSessionArgs;
import org.thingsboard.client.api.ThingsboardApi.GetUserByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetUserSettingsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetUserTokenArgs;
import org.thingsboard.client.api.ThingsboardApi.GetUserUsersArgs;
import org.thingsboard.client.api.ThingsboardApi.GetUsersByEntityGroupIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetUsersByIdsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetUsersForAssignArgs;
import org.thingsboard.client.api.ThingsboardApi.PutGeneralUserSettingsArgs;
import org.thingsboard.client.api.ThingsboardApi.PutUserSettingsArgs;
import org.thingsboard.client.api.ThingsboardApi.RemoveMobileSessionArgs;
import org.thingsboard.client.api.ThingsboardApi.ReportUserDashboardActionArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveAlarmArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveCustomerArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDashboardArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDeviceArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveEntityGroupArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveMobileSessionArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveUserArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveUserSettingsArgs;
import org.thingsboard.client.api.ThingsboardApi.SetUserCredentialsEnabledArgs;
import org.thingsboard.client.model.Alarm;
import org.thingsboard.client.model.AlarmSeverity;
import org.thingsboard.client.model.Authority;
import org.thingsboard.client.model.Customer;
import org.thingsboard.client.model.Dashboard;
import org.thingsboard.client.model.Device;
import org.thingsboard.client.model.EntityGroup;
import org.thingsboard.client.model.EntityGroupInfo;
import org.thingsboard.client.model.EntityId;
import org.thingsboard.client.model.EntityType;
import org.thingsboard.client.model.JwtPair;
import org.thingsboard.client.model.MobileSessionInfo;
import org.thingsboard.client.model.PageDataUser;
import org.thingsboard.client.model.PageDataUserEmailInfo;
import org.thingsboard.client.model.PageDataUserInfo;
import org.thingsboard.client.model.User;
import org.thingsboard.client.model.UserActivationLink;
import org.thingsboard.client.model.UserDashboardsInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class UserApiClientTest extends AbstractApiClientTest {

    @Test
    public void testUserLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<User> createdUsers = new ArrayList<>();

        // create 20 tenant admin users
        for (int i = 0; i < 20; i++) {
            User user = new User();
            String email = ((i % 2 == 0) ? TEST_PREFIX : TEST_PREFIX_2) + timestamp + "_" + i + "@test.com";
            user.setEmail(email);
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(savedClientTenant.getId());
            user.setFirstName("First" + i);
            user.setLastName("Last" + i);

            User createdUser = client.saveUser(SaveUserArgs.builder()
                    .user(user)
                    .sendActivationMail("false")
                    .build());
            assertNotNull(createdUser);
            assertNotNull(createdUser.getId());
            assertEquals(email, createdUser.getEmail());
            assertEquals(Authority.TENANT_ADMIN, createdUser.getAuthority());

            createdUsers.add(createdUser);
        }

        // find all tenant admins, check count (20 created + 3 from setup: clientTenantAdmin + savedClientCustomerUser + savedClientSubCustomerUser)
        PageDataUser allUsers = client.getUserUsers(GetUserUsersArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(allUsers);
        assertNotNull(allUsers.getData());
        int initialSize = allUsers.getData().size();
        assertEquals("Expected 23 users (20 created + 3 from setup), but got " + initialSize, 23, initialSize);

        // find with search text, check count
        PageDataUser filteredUsers = client.getUserUsers(GetUserUsersArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX_2)
                .build());
        assertEquals("Expected exactly 10 users matching prefix", 10, filteredUsers.getData().size());

        // find by id
        User searchUser = createdUsers.get(10);
        User fetchedUser = client.getUserById(GetUserByIdArgs.builder()
                .userId(searchUser.getId().getId().toString())
                .build());
        assertEquals(searchUser.getEmail(), fetchedUser.getEmail());
        assertEquals(searchUser.getFirstName(), fetchedUser.getFirstName());

        // update user
        fetchedUser.setFirstName("UpdatedFirst");
        fetchedUser.setLastName("UpdatedLast");
        User updatedUser = client.saveUser(SaveUserArgs.builder()
                .user(fetchedUser)
                .sendActivationMail("false")
                .build());
        assertEquals("UpdatedFirst", updatedUser.getFirstName());
        assertEquals("UpdatedLast", updatedUser.getLastName());

        // activate user and get token
        activateUser(createdUsers.get(0).getId(), "password123", false);
        JwtPair userToken = client.getUserToken(GetUserTokenArgs.builder()
                .userId(createdUsers.get(0).getId().getId().toString())
                .build());
        assertNotNull(userToken);
        assertNotNull(userToken.getToken());

        // disable user credentials
        client.setUserCredentialsEnabled(SetUserCredentialsEnabledArgs.builder()
                .userId(createdUsers.get(0).getId().getId().toString())
                .userCredentialsEnabled("false")
                .build());

        // re-enable user credentials
        client.setUserCredentialsEnabled(SetUserCredentialsEnabledArgs.builder()
                .userId(createdUsers.get(0).getId().getId().toString())
                .userCredentialsEnabled("true")
                .build());

        // create customer users and verify listing
        Customer customer2 = new Customer();
        customer2.setTitle("User test customer " + timestamp);
        customer2.setEmail("usertest_" + timestamp + "@test.com");
        Customer savedCustomer2 = client.saveCustomer(SaveCustomerArgs.builder()
                .customer(customer2)
                .build());

        List<User> customerUsers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            User customerUser = new User();
            customerUser.setEmail("custuser_" + timestamp + "_" + i + "@test.com");
            customerUser.setAuthority(Authority.CUSTOMER_USER);
            customerUser.setTenantId(savedClientTenant.getId());
            customerUser.setCustomerId(savedCustomer2.getId());
            customerUser.setFirstName("CustFirst" + i);
            customerUser.setLastName("CustLast" + i);

            User created = client.saveUser(SaveUserArgs.builder()
                    .user(customerUser)
                    .sendActivationMail("false")
                    .build());
            assertNotNull(created);
            customerUsers.add(created);
        }

        // list customer users
        PageDataUser customerUserPage = client.getCustomerUsers(GetCustomerUsersArgs.builder()
                .customerId(savedCustomer2.getId().getId().toString())
                .pageSize(100)
                .page(0)
                .build());
        assertEquals("Expected 5 customer users", 5, customerUserPage.getData().size());

        // delete user
        UUID userToDeleteId = createdUsers.get(0).getId().getId();
        client.deleteUser(DeleteUserArgs.builder()
                .userId(userToDeleteId.toString())
                .build());

        // verify deletion
        PageDataUser usersAfterDelete = client.getUserUsers(GetUserUsersArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(initialSize + 5 - 1, usersAfterDelete.getData().size());

        assertReturns404(() ->
                client.getUserById(GetUserByIdArgs.builder()
                        .userId(userToDeleteId.toString())
                        .build())
        );
    }

    @Test
    public void testIsUserTokenAccessEnabled() throws Exception {
        Boolean enabled = client.isUserTokenAccessEnabled();
        assertTrue(enabled);
    }

    @Test
    public void testGetActivationLink() throws Exception {
        long ts = System.currentTimeMillis();

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(savedClientTenant.getId());
        user.setEmail("activation_" + ts + "@test.com");
        User created = client.saveUser(SaveUserArgs.builder()
                .user(user)
                .sendActivationMail("false")
                .build());
        String userId = created.getId().getId().toString();

        // getActivationLink returns the activation URL string
        String link = client.getActivationLink(GetActivationLinkArgs.builder()
                .userId(userId)
                .build());
        assertTrue(link.contains("/api/noauth/activate?activateToken="));

        // getActivationLinkInfo returns the link with TTL metadata
        UserActivationLink linkInfo = client.getActivationLinkInfo(GetActivationLinkInfoArgs.builder()
                .userId(userId)
                .build());
        assertTrue(linkInfo.getValue().contains("/api/noauth/activate?activateToken="));
        assertNotNull(linkInfo.getValue());
    }

    @Test
    public void testGetAllUserInfos() throws Exception {
        // setUp creates: clientTenantAdmin + savedClientCustomerUser + savedClientSubCustomerUser = 3
        PageDataUserInfo allIncludingCustomers = client.getAllUserInfos(GetAllUserInfosArgs.builder()
                .pageSize(100)
                .page(0)
                .includeCustomers(true)
                .build());
        assertNotNull(allIncludingCustomers);
        assertEquals(3, allIncludingCustomers.getTotalElements().intValue());

        // without customer users, only tenant admins should be returned
        PageDataUserInfo tenantAdminsOnly = client.getAllUserInfos(GetAllUserInfosArgs.builder()
                .pageSize(100)
                .page(0)
                .includeCustomers(false)
                .build());
        assertNotNull(tenantAdminsOnly);
        assertEquals(1, tenantAdminsOnly.getTotalElements().intValue());
    }

    @Test
    public void testGetCustomerUserInfos() throws Exception {
        // savedClientCustomer has one direct user: savedClientCustomerUser
        PageDataUserInfo result = client.getCustomerUserInfos(GetCustomerUserInfosArgs.builder()
                .customerId(savedClientCustomer.getId().getId().toString())
                .pageSize(100)
                .page(0)
                .includeCustomers(false)
                .build());
        assertNotNull(result);
        assertEquals(1, result.getTotalElements().intValue());

        // with includeCustomers=true the sub-customer user is also included
        PageDataUserInfo withSubcustomers = client.getCustomerUserInfos(GetCustomerUserInfosArgs.builder()
                .customerId(savedClientCustomer.getId().getId().toString())
                .pageSize(100)
                .page(0)
                .includeCustomers(true)
                .build());
        assertNotNull(withSubcustomers);
        assertEquals(2, withSubcustomers.getTotalElements().intValue());
    }

    @Test
    public void testGetUsersByIdsV2() throws Exception {
        long ts = System.currentTimeMillis();

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(savedClientTenant.getId());
        user.setEmail("byids_" + ts + "@test.com");
        User created = client.saveUser(SaveUserArgs.builder()
                .user(user)
                .sendActivationMail("false")
                .build());

        List<User> result = client.getUsersByIds(GetUsersByIdsArgs.builder()
                .userIds(List.of(created.getId().getId().toString()))
                .build());
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(created.getId().getId(), result.get(0).getId().getId());
    }

    @Test
    public void testGetUsersForAssign() throws Exception {
        long ts = System.currentTimeMillis();

        Device device = client.saveDevice(SaveDeviceArgs.builder()
                .device(new Device().name("alarmDev_" + ts).type("default"))
                .build());

        Alarm alarm = client.saveAlarm(SaveAlarmArgs.builder()
                .alarm(new Alarm()
                        .type("TestAlarm")
                        .originator(device.getId())
                        .severity(AlarmSeverity.WARNING)
                        .acknowledged(false)
                        .cleared(false))
                .build());

        PageDataUserEmailInfo result = client.getUsersForAssign(GetUsersForAssignArgs.builder()
                .alarmId(alarm.getId().getId().toString())
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(result);
        assertNotNull(result.getData());
    }

    @Test
    public void testGetUsersByEntityGroupId() throws Exception {
        long ts = System.currentTimeMillis();

        EntityGroup group = new EntityGroup();
        group.setType(EntityGroup.TypeEnum.USER);
        group.setName("Test User Group " + ts);
        EntityGroupInfo savedGroup = client.saveEntityGroup(SaveEntityGroupArgs.builder()
                .entityGroup(group)
                .build());

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(savedClientTenant.getId());
        user.setEmail("ugtest_" + ts + "@test.com");
        User created = client.saveUser(SaveUserArgs.builder()
                .user(user)
                .sendActivationMail("false")
                .build());
        client.addEntitiesToEntityGroup(AddEntitiesToEntityGroupArgs.builder()
                .entityGroupId(savedGroup.getId().getId().toString())
                .requestBody(List.of(created.getId().getId().toString()))
                .build());

        PageDataUser result = client.getUsersByEntityGroupId(GetUsersByEntityGroupIdArgs.builder()
                .entityGroupId(savedGroup.getId().getId().toString())
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals(created.getId().getId(), result.getData().get(0).getId().getId());
    }

    @Test
    public void testUserSettings() throws Exception {
        // saveUserSettings persists general settings and returns the saved map
        JsonNode saved = client.saveUserSettings(SaveUserSettingsArgs.builder()
                .body(Map.<String, Object>of("settingKey", "settingValue"))
                .build());
        assertNotNull(saved);

        // getGeneralUserSettings returns the previously saved general settings
        JsonNode general = client.getGeneralUserSettings();
        assertNotNull(general);
        assertEquals("settingValue", general.get("settingKey").asText());

        // putGeneralUserSettings merges additional keys into general settings
        client.putGeneralUserSettings(PutGeneralUserSettingsArgs.builder()
                .body(Map.<String, Object>of("extraKey", "extraValue"))
                .build());
        JsonNode updatedGeneral = client.getGeneralUserSettings();
        assertNotNull(updatedGeneral);
        assertEquals("extraValue", updatedGeneral.get("extraKey").asText());

        // putUserSettings + getUserSettings: typed settings
        String settingsType = "GETTING_STARTED";
        client.putUserSettings(PutUserSettingsArgs.builder()
                .type(settingsType)
                .body(Map.<String, Object>of("x", "1", "y", "2"))
                .build());

        JsonNode typed = client.getUserSettings(GetUserSettingsArgs.builder()
                .type(settingsType)
                .build());
        assertNotNull(typed);
        assertEquals("1", typed.get("x").asText());
        assertEquals("2", typed.get("y").asText());

        // deleteUserSettings removes a specific key from a typed settings section
        client.deleteUserSettingsByType(DeleteUserSettingsByTypeArgs.builder()
                .paths("x")
                .type(settingsType)
                .build());
        JsonNode afterDelete = client.getUserSettings(GetUserSettingsArgs.builder()
                .type(settingsType)
                .build());
        assertNotNull(afterDelete);
        assertFalse(afterDelete.has("x"));
        assertTrue(afterDelete.has("y"));
    }

    @Test
    public void testDashboardTracking() throws Exception {
        long ts = System.currentTimeMillis();

        Dashboard dashboard = client.saveDashboard(SaveDashboardArgs.builder()
                .dashboard(new Dashboard().title("Track_" + ts))
                .build());

        // reportUserDashboardAction "visit" records the visit and returns current state
        UserDashboardsInfo afterVisit = client.reportUserDashboardAction(ReportUserDashboardActionArgs.builder()
                .dashboardId(dashboard.getId().getId().toString())
                .action("visit")
                .build());
        assertNotNull(afterVisit);
        assertNotNull(afterVisit.getLast());
        assertFalse(afterVisit.getLast().isEmpty());

        // getLastVisitedDashboards returns the same recently visited dashboards
        UserDashboardsInfo lastVisited = client.getLastVisitedDashboards();
        assertNotNull(lastVisited);
        assertFalse(lastVisited.getLast().isEmpty());

        // "star" marks the dashboard as starred
        UserDashboardsInfo afterStar = client.reportUserDashboardAction(ReportUserDashboardActionArgs.builder()
                .dashboardId(dashboard.getId().getId().toString())
                .action("star")
                .build());
        assertNotNull(afterStar);
        assertNotNull(afterStar.getStarred());
        assertFalse(afterStar.getStarred().isEmpty());

        // "unstar" removes the dashboard from starred list
        UserDashboardsInfo afterUnstar = client.reportUserDashboardAction(ReportUserDashboardActionArgs.builder()
                .dashboardId(dashboard.getId().getId().toString())
                .action("unstar")
                .build());
        assertNotNull(afterUnstar);
        assertTrue(afterUnstar.getStarred().isEmpty());
    }

    @Test
    public void testMobileSession() throws Exception {
        long ts = System.currentTimeMillis();
        String mobileToken = "test-mobile-" + ts;
        MobileSessionInfo session = new MobileSessionInfo().fcmTokenTimestamp(ts);

        // saveMobileSession stores a session keyed by the mobile token header
        client.saveMobileSession(SaveMobileSessionArgs.builder()
                .xMobileToken(mobileToken)
                .mobileSessionInfo(session)
                .build());

        // getMobileSession retrieves the stored session
        MobileSessionInfo retrieved = client.getMobileSession(GetMobileSessionArgs.builder()
                .xMobileToken(mobileToken)
                .build());
        assertNotNull(retrieved);
        assertEquals(ts, retrieved.getFcmTokenTimestamp().longValue());

        // removeMobileSession deletes the session
        client.removeMobileSession(RemoveMobileSessionArgs.builder()
                .xMobileToken(mobileToken)
                .build());
        MobileSessionInfo mobileSession = client.getMobileSession(GetMobileSessionArgs.builder()
                .xMobileToken(mobileToken)
                .build());
        assertNull(mobileSession);
    }

}
