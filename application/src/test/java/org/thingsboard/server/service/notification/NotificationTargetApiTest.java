/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EntityActionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.AllUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.CustomerUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.UserListFilter;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.notification.NotificationTargetDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class NotificationTargetApiTest extends AbstractNotificationApiTest {

    @Autowired
    private NotificationTargetDao notificationTargetDao;

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void givenInvalidNotificationTarget_whenSaving_returnValidationError() throws Exception {
        NotificationTarget target = new NotificationTarget();
        target.setTenantId(null);
        target.setName(null);
        target.setConfiguration(null);

        String validationError = saveAndGetError(target, status().isBadRequest());
        assertThat(validationError)
                .contains("name must not be")
                .contains("configuration must not be");

        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        UserListFilter userListFilter = new UserListFilter();
        userListFilter.setUsersIds(Collections.emptyList());
        targetConfig.setUsersFilter(userListFilter);
        target.setConfiguration(targetConfig);

        validationError = saveAndGetError(target, status().isBadRequest());
        assertThat(validationError)
                .contains("usersIds must not be");
    }

    @Test
    public void givenNotificationTargetWithUsersFromDifferentTenant_whenSaving_returnAccessDeniedError() throws Exception {
        loginDifferentTenant();
        NotificationTarget notificationTarget = new NotificationTarget();
        notificationTarget.setTenantId(differentTenantId);
        notificationTarget.setName("Target 1");

        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        UserListFilter userListFilter = new UserListFilter();
        userListFilter.setUsersIds(List.of(customerUserId.getId(), tenantAdminUserId.getId()));
        targetConfig.setUsersFilter(userListFilter);
        notificationTarget.setConfiguration(targetConfig);

        saveAndGetError(notificationTarget, status().isForbidden());

        loginSysAdmin();
        notificationTarget.setTenantId(TenantId.SYS_TENANT_ID);
        save(notificationTarget, status().isOk());
    }

    @Test
    public void givenNotificationTargetConfig_testGetRecipients() throws Exception {
        NotificationTarget notificationTarget = new NotificationTarget();
        notificationTarget.setTenantId(tenantId);
        notificationTarget.setName("Test target");

        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        CustomerUsersFilter customerUsersFilter = new CustomerUsersFilter();
        customerUsersFilter.setCustomerId(customerId.getId());
        targetConfig.setUsersFilter(customerUsersFilter);
        notificationTarget.setConfiguration(targetConfig);

        List<User> recipients = getRecipients(notificationTarget);
        assertThat(recipients).size().isNotZero();
        assertThat(recipients).allSatisfy(recipient -> {
            assertThat(recipient.getCustomerId()).isEqualTo(customerId);
        });

        AllUsersFilter allUsersFilter = new AllUsersFilter();
        targetConfig.setUsersFilter(allUsersFilter);
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
        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        targetConfig.setUsersFilter(new AllUsersFilter());
        notificationTarget.setConfiguration(targetConfig);
        save(notificationTarget, status().isOk());
        assertThat(notificationTargetDao.findByTenantIdAndPageLink(differentTenantId, new PageLink(10)).getData()).isNotEmpty();
        assertThat(notificationTargetDao.findByTenantIdAndSupportedNotificationTypeAndPageLink(differentTenantId, NotificationType.GENERAL, new PageLink(10)).getData()).isNotEmpty();

        deleteDifferentTenant();
        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(notificationTargetDao.findByTenantIdAndPageLink(differentTenantId, new PageLink(10)).getData()).isEmpty();
        });
    }

    @Test
    public void whenDeletingTargetUsedByRule_thenReturnError() throws Exception {
        NotificationTarget target = createNotificationTarget(tenantAdminUserId);
        createNotificationRule(new EntityActionNotificationRuleTriggerConfig(), "Test", "Test", target.getId());

        String error = getErrorMessage(doDelete("/api/notification/target/" + target.getId())
                .andExpect(status().isBadRequest()));
        assertThat(error).containsIgnoringCase("used in notification rule");
    }

    @Test
    public void whenDeletingTargetUsedByScheduledNotificationRequest_thenReturnError() throws Exception {
        NotificationTarget target = createNotificationTarget(tenantAdminUserId);
        submitNotificationRequest(target.getId(), "Test", 100, NotificationDeliveryMethod.WEB);

        String error = getErrorMessage(doDelete("/api/notification/target/" + target.getId())
                .andExpect(status().isBadRequest()));
        assertThat(error).containsIgnoringCase("referenced by scheduled notification request");
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
