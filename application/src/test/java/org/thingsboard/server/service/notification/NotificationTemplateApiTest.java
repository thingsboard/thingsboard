// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.rule.DefaultNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EntityActionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.template.EmailDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class NotificationTemplateApiTest extends AbstractNotificationApiTest {

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void givenInvalidNotificationTemplate_whenSaving_returnValidationError() throws Exception {
        NotificationTemplate notificationTemplate = new NotificationTemplate();
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

    @Test
    public void testSaveTemplateWithReportRequiresPermissionOverReportUser() throws Exception {
        UserId otherTenantUserId = getDifferentTenantUserId();

        NotificationTemplate notificationTemplate = buildTemplateWithReport(otherTenantUserId);
        save(notificationTemplate, status().isForbidden());

        notificationTemplate.getConfiguration().setUserId(tenantAdminUserId);
        save(notificationTemplate, status().isOk());
    }

    @Test
    public void testNotificationRequestWithReportRequiresPermissionOverReportUser() throws Exception {
        UserId otherTenantUserId = getDifferentTenantUserId();
        NotificationTarget target = createNotificationTarget(tenantAdminUserId);

        NotificationRequest inlineTemplateRequest = NotificationRequest.builder()
                .targets(List.of(target.getUuidId()))
                .template(buildTemplateWithReport(otherTenantUserId))
                .additionalConfig(new NotificationRequestConfig())
                .build();
        doPost("/api/notification/request", inlineTemplateRequest).andExpect(status().isForbidden());

        NotificationTemplate storedTemplate = saveTemplateWithReportUser(otherTenantUserId);
        NotificationRequest storedTemplateRequest = NotificationRequest.builder()
                .targets(List.of(target.getUuidId()))
                .templateId(storedTemplate.getId())
                .additionalConfig(new NotificationRequestConfig())
                .build();
        doPost("/api/notification/request", storedTemplateRequest).andExpect(status().isForbidden());

        assertThat(findNotificationRequests().getData()).isEmpty();
    }

    @Test
    public void testSaveRuleWithReportTemplateRequiresPermissionOverReportUser() throws Exception {
        UserId otherTenantUserId = getDifferentTenantUserId();
        NotificationTemplate storedTemplate = saveTemplateWithReportUser(otherTenantUserId);

        EntityActionNotificationRuleTriggerConfig triggerConfig = new EntityActionNotificationRuleTriggerConfig();
        triggerConfig.setEntityTypes(Set.of(EntityType.DEVICE));
        triggerConfig.setCreated(true);
        NotificationRule rule = new NotificationRule();
        rule.setName("Device created");
        rule.setEnabled(true);
        rule.setTemplateId(storedTemplate.getId());
        rule.setTriggerType(triggerConfig.getTriggerType());
        rule.setTriggerConfig(triggerConfig);
        DefaultNotificationRuleRecipientsConfig recipientsConfig = DefaultNotificationRuleRecipientsConfig.forTriggerType(triggerConfig.getTriggerType());
        recipientsConfig.setTargets(List.of(createNotificationTarget(tenantAdminUserId).getUuidId()));
        rule.setRecipientsConfig(recipientsConfig);

        doPost("/api/notification/rule", rule).andExpect(status().isForbidden());
        assertThat(findNotificationRules().getData()).extracting(NotificationRuleInfo::getName).doesNotContain(rule.getName());
    }

    private UserId getDifferentTenantUserId() throws Exception {
        loginDifferentTenant();
        UserId userId = savedDifferentTenantUser.getId();
        loginTenantAdmin();
        return userId;
    }

    private NotificationTemplate buildTemplateWithReport(UserId userId) {
        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setName("Template with report");
        notificationTemplate.setNotificationType(NotificationType.GENERAL);
        NotificationTemplateConfig config = new NotificationTemplateConfig();
        EmailDeliveryMethodNotificationTemplate emailTemplate = new EmailDeliveryMethodNotificationTemplate();
        emailTemplate.setEnabled(true);
        emailTemplate.setSubject("Report");
        emailTemplate.setBody("Report");
        config.setDeliveryMethodsTemplates(Map.of(NotificationDeliveryMethod.EMAIL, emailTemplate));
        config.setAttachReport(true);
        config.setUserId(userId);
        notificationTemplate.setConfiguration(config);
        return notificationTemplate;
    }

    private NotificationTemplate saveTemplateWithReportUser(UserId userId) {
        NotificationTemplate notificationTemplate = buildTemplateWithReport(userId);
        notificationTemplate.setTenantId(tenantId);
        return notificationTemplateService.saveNotificationTemplate(tenantId, notificationTemplate);
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
