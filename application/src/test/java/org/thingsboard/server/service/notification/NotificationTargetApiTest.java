/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.targets.AllUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.CustomerUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.SingleUserNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.UserListNotificationTargetConfig;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.notification.NotificationTargetDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class NotificationTargetApiTest extends AbstractControllerTest {

    @Autowired
    private NotificationTargetDao notificationTargetDao;

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void givenInvalidNotificationTarget_whenSaving_returnValidationError() throws Exception {
        NotificationTarget notificationTarget = new NotificationTarget();
        notificationTarget.setTenantId(null);
        notificationTarget.setName(null);
        notificationTarget.setConfiguration(null);

        String validationError = saveAndGetError(notificationTarget, status().isBadRequest());
        assertThat(validationError)
                .contains("name must not be")
                .contains("configuration must not be");

        SingleUserNotificationTargetConfig singleUserConfig = new SingleUserNotificationTargetConfig();
        singleUserConfig.setUserId(null);
        notificationTarget.setConfiguration(singleUserConfig);

        validationError = saveAndGetError(notificationTarget, status().isBadRequest());
        assertThat(validationError)
                .contains("userId must not be");

        UserListNotificationTargetConfig userListConfig = new UserListNotificationTargetConfig();
        userListConfig.setUsersIds(Collections.emptyList());
        notificationTarget.setConfiguration(userListConfig);

        validationError = saveAndGetError(notificationTarget, status().isBadRequest());
        assertThat(validationError)
                .contains("usersIds must not be");
    }

    @Test
    public void givenNotificationTargetWithUsersFromDifferentTenant_whenSaving_returnAccessDeniedError() throws Exception {
        loginDifferentTenant();
        NotificationTarget notificationTarget = new NotificationTarget();
        notificationTarget.setTenantId(differentTenantId);
        notificationTarget.setName("Target 1");
        UserListNotificationTargetConfig userListConfig = new UserListNotificationTargetConfig();
        userListConfig.setUsersIds(List.of(customerUserId.getId(), tenantAdminUserId.getId()));
        notificationTarget.setConfiguration(userListConfig);

        saveAndGetError(notificationTarget, status().isForbidden());

        SingleUserNotificationTargetConfig singleUserConfig = new SingleUserNotificationTargetConfig();
        singleUserConfig.setUserId(customerUserId.getId());
        notificationTarget.setConfiguration(singleUserConfig);

        saveAndGetError(notificationTarget, status().isForbidden());

        loginSysAdmin();
        notificationTarget.setTenantId(TenantId.SYS_TENANT_ID);
        notificationTarget.setConfiguration(userListConfig);
        save(notificationTarget, status().isOk());
    }

    @Test
    public void givenNotificationTargetConfig_testGetRecipients() throws Exception {
        NotificationTarget notificationTarget = new NotificationTarget();
        notificationTarget.setTenantId(tenantId);
        notificationTarget.setName("Test target");
        CustomerUsersNotificationTargetConfig customerUsersConfig = new CustomerUsersNotificationTargetConfig();
        customerUsersConfig.setCustomerId(customerId.getId());
        notificationTarget.setConfiguration(customerUsersConfig);

        List<User> recipients = getRecipients(notificationTarget);
        assertThat(recipients).size().isNotZero();
        assertThat(recipients).allSatisfy(recipient -> {
            assertThat(recipient.getCustomerId()).isEqualTo(customerId);
        });

        AllUsersNotificationTargetConfig allUsersConfig = new AllUsersNotificationTargetConfig();
        notificationTarget.setConfiguration(allUsersConfig);
        recipients = getRecipients(notificationTarget);
        assertThat(recipients).size().isGreaterThanOrEqualTo(2);
        assertThat(recipients).allSatisfy(recipient -> {
            assertThat(recipient.getTenantId()).isEqualTo(tenantId);
        });

        createDifferentTenant();
        loginSysAdmin();
        recipients = getRecipients(notificationTarget);
        assertThat(recipients).size().isGreaterThanOrEqualTo(3);
        assertThat(recipients).anySatisfy(recipient -> {
            assertThat(recipient.getTenantId()).isEqualTo(tenantId);
        });
        assertThat(recipients).anySatisfy(recipient -> {
            assertThat(recipient.getTenantId()).isEqualTo(differentTenantId);
        });
    }

    @Test
    public void whenDeletingTenant_thenDeleteNotificationTarget() throws Exception {
        createDifferentTenant();
        NotificationTarget notificationTarget = new NotificationTarget();
        notificationTarget.setName("Test 1");
        notificationTarget.setTenantId(differentTenantId);
        notificationTarget.setConfiguration(new AllUsersNotificationTargetConfig());
        save(notificationTarget, status().isOk());
        assertThat(notificationTargetDao.find(TenantId.SYS_TENANT_ID)).isNotEmpty();

        deleteDifferentTenant();
        assertThat(notificationTargetDao.find(TenantId.SYS_TENANT_ID)).isEmpty();
    }

    private String saveAndGetError(NotificationTarget notificationTarget, ResultMatcher statusMatcher) throws Exception {
        return getErrorMessage(save(notificationTarget, statusMatcher));
    }

    private ResultActions save(NotificationTarget notificationTarget, ResultMatcher statusMatcher) throws Exception {
        return doPost("/api/notification/target", notificationTarget)
                .andExpect(statusMatcher);
    }

    private List<User> getRecipients(NotificationTarget notificationTarget) throws Exception {
        return doPostWithTypedResponse("/api/notification/target/recipients?page=0&pageSize=100", notificationTarget, new TypeReference<PageData<User>>() {}).getData();
    }

}
