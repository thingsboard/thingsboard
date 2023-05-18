/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.info.EntityActionNotificationInfo;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.template.EmailDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class NotificationTemplateApiTest extends AbstractNotificationApiTest {

    @Autowired
    private NotificationCenter notificationCenter;
    @Autowired
    private NotificationService notificationService;

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void givenInvalidNotificationTemplate_whenSaving_returnValidationError() throws Exception {
        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setTenantId(tenantId);
        notificationTemplate.setName(null);
        notificationTemplate.setNotificationType(null);
        notificationTemplate.setConfiguration(null);

        String validationError = saveAndGetError(notificationTemplate, status().isBadRequest());
        assertThat(validationError)
                .contains("name must not be")
                .contains("notificationType must not be")
                .contains("configuration must not be");

        NotificationTemplateConfig config = new NotificationTemplateConfig();
        notificationTemplate.setConfiguration(config);
        EmailDeliveryMethodNotificationTemplate emailTemplate = new EmailDeliveryMethodNotificationTemplate();
        emailTemplate.setEnabled(true);
        emailTemplate.setBody(null);
        emailTemplate.setSubject(null);
        config.setDeliveryMethodsTemplates(Map.of(
                NotificationDeliveryMethod.EMAIL, emailTemplate
        ));
        notificationTemplate.setName("<script/>");

        validationError = saveAndGetError(notificationTemplate, status().isBadRequest());
        assertThat(validationError)
                .contains("subject must not be")
                .contains("body must not be")
                .contains("name is malformed");
    }

    @Test
    public void testTemplatesProcessing_tbelScripts() throws Exception {
        User firstRecipient = new User();
        firstRecipient.setEmail("recipient1@tb.org");
        firstRecipient.setAuthority(Authority.TENANT_ADMIN);
        firstRecipient.setFirstName("Aba");
        firstRecipient.setLastName("Duba");
        firstRecipient = createUser(firstRecipient, "123456");
        User secondRecipient = new User();
        secondRecipient.setEmail("recipient2@tb.org");
        secondRecipient.setAuthority(Authority.TENANT_ADMIN);
        secondRecipient = createUser(secondRecipient, "123456");
        NotificationTargetId target = createNotificationTarget(firstRecipient.getId(), secondRecipient.getId()).getId();

        String template = "Hi, #{ recipientFirstName.isEmpty() ? recipientEmail : (recipientFirstName + ' ' + recipientLastName) } #{ '!!!' }";
        process(template, NotificationType.GENERAL, null, target);
        assertThat(getLastNotification(firstRecipient).getText()).isEqualTo("Hi, Aba Duba !!!");
        assertThat(getLastNotification(secondRecipient).getText()).isEqualTo("Hi, recipient2@tb.org !!!");

        NotificationInfo info = EntityActionNotificationInfo.builder()
                .entityId(new DeviceId(UUID.randomUUID()))
                .entityName("device A")
                .actionType(ActionType.DELETED)
                .userId(tenantAdminUserId.getId())
                .build();
        template = "Hi, #{ recipientTitle } (${recipientEmail}). Someone ${actionType} your ${entityName}" +
                "#{ actionType == 'deleted' ? '!!!' : '.' } #{ ':(' }";
        process(template, NotificationType.ENTITY_ACTION, info, target);
        assertThat(getLastNotification(firstRecipient).getText()).isEqualTo("Hi, Aba Duba (recipient1@tb.org). " +
                "Someone deleted your device A!!! :(");
     assertThat(getLastNotification(secondRecipient).getText()).isEqualTo("Hi, recipient2@tb.org (recipient2@tb.org). " +
                "Someone deleted your device A!!! :(");
    }

    private void process(String text, NotificationType notificationType, NotificationInfo info, NotificationTargetId target) {
        NotificationTemplate template = setUpNotificationTemplate(notificationType, "Test", text, NotificationDeliveryMethod.WEB);
        NotificationRequest request = NotificationRequest.builder()
                .tenantId(tenantId)
                .targets(List.of(target.getId()))
                .template(template)
                .info(info)
                .originatorEntityId(tenantAdminUserId)
                .build();
        notificationCenter.processNotificationRequest(tenantId, request, null);
    }

    private Notification getLastNotification(User recipient) {
        return await().atMost(30, TimeUnit.SECONDS)
                .until(() -> notificationService.findLatestUnreadNotificationsByRecipientId(tenantId, recipient.getId(), 1).getData(),
                        notifications -> !notifications.isEmpty())
                .get(0);
    }

    @Test
    public void testTemplatesSearch() throws Exception {
        NotificationTemplate alarmNotificationTemplate = createNotificationTemplate(NotificationType.ALARM, "Alarm", "Alarm", NotificationDeliveryMethod.WEB);
        NotificationTemplate generalNotificationTemplate = createNotificationTemplate(NotificationType.GENERAL, "General", "General", NotificationDeliveryMethod.WEB);
        NotificationTemplate entityActionNotificationTemplate = createNotificationTemplate(NotificationType.ENTITY_ACTION, "Entity action", "Entity action", NotificationDeliveryMethod.WEB);

        assertThat(findTemplates(NotificationType.ALARM)).extracting(IdBased::getId)
                .containsOnly(alarmNotificationTemplate.getId());
        assertThat(findTemplates(NotificationType.ENTITY_ACTION)).extracting(IdBased::getId)
                .containsOnly(entityActionNotificationTemplate.getId());
        assertThat(findTemplates(NotificationType.GENERAL)).extracting(IdBased::getId)
                .containsOnly(generalNotificationTemplate.getId());

        assertThat(findTemplates(NotificationType.GENERAL, NotificationType.ALARM)).extracting(IdBased::getId)
                .containsOnly(generalNotificationTemplate.getId(), alarmNotificationTemplate.getId());
        assertThat(findTemplates(NotificationType.GENERAL, NotificationType.ENTITY_ACTION)).extracting(IdBased::getId)
                .containsOnly(generalNotificationTemplate.getId(), entityActionNotificationTemplate.getId());

        assertThat(findTemplates()).extracting(IdBased::getId)
                .containsOnly(generalNotificationTemplate.getId(), alarmNotificationTemplate.getId(), entityActionNotificationTemplate.getId());
    }

    private String saveAndGetError(NotificationTemplate notificationTemplate, ResultMatcher statusMatcher) throws Exception {
        return getErrorMessage(save(notificationTemplate, statusMatcher));
    }

    private ResultActions save(NotificationTemplate notificationTemplate, ResultMatcher statusMatcher) throws Exception {
        return doPost("/api/notification/template", notificationTemplate)
                .andExpect(statusMatcher);
    }

    private List<NotificationTemplate> findTemplates(NotificationType... notificationTypes) throws Exception {
        PageLink pageLink = new PageLink(100, 0);
        return doGetTypedWithPageLink("/api/notification/templates?notificationTypes=" + StringUtils.join(notificationTypes, ",") + "&",
                new TypeReference<PageData<NotificationTemplate>>() {}, pageLink).getData();
    }

}
