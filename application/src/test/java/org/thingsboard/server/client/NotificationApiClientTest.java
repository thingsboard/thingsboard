// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.CreateNotificationRequestArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteNotificationRequestArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteNotificationRuleArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteNotificationTargetByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteNotificationTemplateByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetNotificationRequestByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetNotificationRequestsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetNotificationRuleByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetNotificationRulesArgs;
import org.thingsboard.client.api.ThingsboardApi.GetNotificationTargetByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetNotificationTargetsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetNotificationTemplateByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetNotificationTemplatesArgs;
import org.thingsboard.client.api.ThingsboardApi.GetNotificationsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetUnreadNotificationsCountArgs;
import org.thingsboard.client.api.ThingsboardApi.MarkAllNotificationsAsReadArgs;
import org.thingsboard.client.api.ThingsboardApi.MarkNotificationAsReadArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveNotificationRuleArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveNotificationTargetArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveNotificationTemplateArgs;
import org.thingsboard.client.model.EntityActionNotificationRuleTriggerConfig;
import org.thingsboard.client.model.EntityActionRecipientsConfig;
import org.thingsboard.client.model.EntityType;
import org.thingsboard.client.model.NotificationDeliveryMethod;
import org.thingsboard.client.model.NotificationRequest;
import org.thingsboard.client.model.NotificationRequestInfo;
import org.thingsboard.client.model.NotificationRule;
import org.thingsboard.client.model.NotificationRuleInfo;
import org.thingsboard.client.model.NotificationRuleTriggerType;
import org.thingsboard.client.model.NotificationSettings;
import org.thingsboard.client.model.NotificationTarget;
import org.thingsboard.client.model.NotificationTemplate;
import org.thingsboard.client.model.NotificationTemplateConfig;
import org.thingsboard.client.model.NotificationType;
import org.thingsboard.client.model.PageDataNotification;
import org.thingsboard.client.model.PageDataNotificationRequestInfo;
import org.thingsboard.client.model.PageDataNotificationRuleInfo;
import org.thingsboard.client.model.PageDataNotificationTarget;
import org.thingsboard.client.model.PageDataNotificationTemplate;
import org.thingsboard.client.model.PlatformUsersNotificationTargetConfig;
import org.thingsboard.client.model.TenantAdministratorsFilter;
import org.thingsboard.client.model.WebDeliveryMethodNotificationTemplate;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class NotificationApiClientTest extends AbstractApiClientTest {

    @Test
    public void testNotificationLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();

        // === 1. Notification Target CRUD ===

        // Create target
        TenantAdministratorsFilter usersFilter = new TenantAdministratorsFilter();
        PlatformUsersNotificationTargetConfig targetConfig =
                new PlatformUsersNotificationTargetConfig().usersFilter(usersFilter);
        NotificationTarget target =
                new NotificationTarget()
                        .name("Test Target " + timestamp)
                        ._configuration(targetConfig);

        NotificationTarget savedTarget = client.saveNotificationTarget(SaveNotificationTargetArgs.builder()
                .notificationTarget(target)
                .build());
        assertNotNull(savedTarget);
        assertNotNull(savedTarget.getId());
        assertEquals("Test Target " + timestamp, savedTarget.getName());

        // Get target by ID
        NotificationTarget fetchedTarget =
                client.getNotificationTargetById(GetNotificationTargetByIdArgs.builder()
                        .id(savedTarget.getId().getId())
                        .build());
        assertEquals(savedTarget.getName(), fetchedTarget.getName());

        // List targets
        PageDataNotificationTarget targetsPage =
                client.getNotificationTargets(GetNotificationTargetsArgs.builder()
                        .pageSize(100)
                        .page(0)
                        .build());
        assertNotNull(targetsPage);
        assertNotNull(targetsPage.getData());
        assertTrue(
                targetsPage.getData().stream()
                        .anyMatch(t -> t.getName().equals(savedTarget.getName())));

        // Update target
        savedTarget.setName("Updated Target " + timestamp);
        NotificationTarget updatedTarget = client.saveNotificationTarget(SaveNotificationTargetArgs.builder()
                .notificationTarget(savedTarget)
                .build());
        assertEquals("Updated Target " + timestamp, updatedTarget.getName());

        // === 2. Notification Template CRUD ===

        // Create template
        WebDeliveryMethodNotificationTemplate webTemplate =
                new WebDeliveryMethodNotificationTemplate()
                        .subject("Test Subject")
                        .body("Test notification body")
                        .enabled(true);
        NotificationTemplateConfig templateConfig =
                new NotificationTemplateConfig()
                        .putDeliveryMethodsTemplatesItem("WEB", webTemplate);
        NotificationTemplate template =
                new NotificationTemplate()
                        .name("Test Template " + timestamp)
                        .notificationType(NotificationType.GENERAL)
                        ._configuration(templateConfig);

        NotificationTemplate savedTemplate = client.saveNotificationTemplate(SaveNotificationTemplateArgs.builder()
                .notificationTemplate(template)
                .build());
        assertNotNull(savedTemplate);
        assertNotNull(savedTemplate.getId());
        assertEquals("Test Template " + timestamp, savedTemplate.getName());

        // Get template by ID
        NotificationTemplate fetchedTemplate =
                client.getNotificationTemplateById(GetNotificationTemplateByIdArgs.builder()
                        .id(savedTemplate.getId().getId())
                        .build());
        assertEquals(savedTemplate.getName(), fetchedTemplate.getName());
        assertEquals(NotificationType.GENERAL, fetchedTemplate.getNotificationType());

        // List templates
        PageDataNotificationTemplate templatesPage =
                client.getNotificationTemplates(GetNotificationTemplatesArgs.builder()
                        .pageSize(100)
                        .page(0)
                        .build());
        assertNotNull(templatesPage);
        assertTrue(
                templatesPage.getData().stream()
                        .anyMatch(t -> t.getName().equals(savedTemplate.getName())));

        // Update template
        savedTemplate.setName("Updated Template " + timestamp);
        NotificationTemplate updatedTemplate = client.saveNotificationTemplate(SaveNotificationTemplateArgs.builder()
                .notificationTemplate(savedTemplate)
                .build());
        assertEquals("Updated Template " + timestamp, updatedTemplate.getName());

        // === 3. Send notification & read notifications ===

        // Send notification request
        NotificationRequest request =
                new NotificationRequest()
                        .targets(List.of(savedTarget.getId().getId()))
                        .templateId(savedTemplate.getId());
        NotificationRequest sentRequest = client.createNotificationRequest(CreateNotificationRequestArgs.builder()
                .notificationRequest(request)
                .build());
        assertNotNull(sentRequest);
        assertNotNull(sentRequest.getId());

        // Get request by ID
        NotificationRequestInfo fetchedRequest =
                client.getNotificationRequestById(GetNotificationRequestByIdArgs.builder()
                        .id(sentRequest.getId().getId())
                        .build());
        assertNotNull(fetchedRequest);

        // List requests
        PageDataNotificationRequestInfo requestsPage =
                client.getNotificationRequests(GetNotificationRequestsArgs.builder()
                        .pageSize(100)
                        .page(0)
                        .build());
        assertNotNull(requestsPage);
        assertFalse(requestsPage.getData().isEmpty());

        // Get notifications for current user
        PageDataNotification notificationsPage =
                client.getNotifications(GetNotificationsArgs.builder()
                        .pageSize(100)
                        .page(0)
                        .build());
        assertNotNull(notificationsPage);
        assertFalse(notificationsPage.getData().isEmpty());

        // Get unread count
        Integer unreadCount = client.getUnreadNotificationsCount(GetUnreadNotificationsCountArgs.builder()
                .deliveryMethod("WEB")
                .build());
        assertNotNull(unreadCount);
        assertTrue("Expected at least one unread notification", unreadCount > 0);

        // Mark single notification as read
        client.markNotificationAsRead(MarkNotificationAsReadArgs.builder()
                .id(notificationsPage.getData().get(0).getId().getId())
                .build());

        // Mark all as read
        client.markAllNotificationsAsRead(MarkAllNotificationsAsReadArgs.builder()
                .build());
        Integer unreadAfterMarkAll = client.getUnreadNotificationsCount(GetUnreadNotificationsCountArgs.builder()
                .build());
        assertEquals("Expected no unread notifications after marking all as read", 0, unreadAfterMarkAll.intValue());

        // === 4. Notification Settings ===

        NotificationSettings settings = client.getNotificationSettings();
        assertNotNull(settings);

        List<NotificationDeliveryMethod> deliveryMethods = client.getAvailableDeliveryMethods();
        assertNotNull(deliveryMethods);
        assertTrue(deliveryMethods.contains(NotificationDeliveryMethod.WEB));

        // === 5. Cleanup ===

        // Delete notification request
        client.deleteNotificationRequest(DeleteNotificationRequestArgs.builder()
                .id(sentRequest.getId().getId())
                .build());
        assertReturns404(() -> client.getNotificationRequestById(GetNotificationRequestByIdArgs.builder()
                .id(sentRequest.getId().getId())
                .build()));

        // Delete template
        client.deleteNotificationTemplateById(DeleteNotificationTemplateByIdArgs.builder()
                .id(savedTemplate.getId().getId())
                .build());
        assertReturns404(() -> client.getNotificationTemplateById(GetNotificationTemplateByIdArgs.builder()
                .id(savedTemplate.getId().getId())
                .build()));

        // Delete target
        client.deleteNotificationTargetById(DeleteNotificationTargetByIdArgs.builder()
                .id(savedTarget.getId().getId())
                .build());
        assertReturns404(() -> client.getNotificationTargetById(GetNotificationTargetByIdArgs.builder()
                .id(savedTarget.getId().getId())
                .build()));
    }

    @Test
    public void testNotificationRuleLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();

        // Create a target for the rule recipients
        TenantAdministratorsFilter usersFilter = new TenantAdministratorsFilter();
        PlatformUsersNotificationTargetConfig targetConfig =
                new PlatformUsersNotificationTargetConfig().usersFilter(usersFilter);
        NotificationTarget target =
                new NotificationTarget()
                        .name("Rule Test Target " + timestamp)
                        ._configuration(targetConfig);
        NotificationTarget savedTarget = client.saveNotificationTarget(SaveNotificationTargetArgs.builder()
                .notificationTarget(target)
                .build());

        // Create a template of type ENTITY_ACTION
        WebDeliveryMethodNotificationTemplate webTemplate =
                new WebDeliveryMethodNotificationTemplate()
                        .subject("Entity action: ${entityType}")
                        .body("Entity ${entityName} was ${actionType}")
                        .enabled(true);
        NotificationTemplateConfig templateConfig =
                new NotificationTemplateConfig()
                        .putDeliveryMethodsTemplatesItem("WEB", webTemplate);
        NotificationTemplate template =
                new NotificationTemplate()
                        .name("Rule Test Template " + timestamp)
                        .notificationType(NotificationType.ENTITY_ACTION)
                        ._configuration(templateConfig);
        NotificationTemplate savedTemplate = client.saveNotificationTemplate(SaveNotificationTemplateArgs.builder()
                .notificationTemplate(template)
                .build());

        // Build trigger config: fire on DEVICE create/update
        EntityActionNotificationRuleTriggerConfig triggerConfig =
                new EntityActionNotificationRuleTriggerConfig()
                        .addEntityTypesItem(EntityType.DEVICE)
                        .created(true)
                        .updated(true)
                        .deleted(false);

        // Build recipients config
        EntityActionRecipientsConfig recipientsConfig = new EntityActionRecipientsConfig()
                .addTargetsItem(savedTarget.getId().getId());

        // saveNotificationRule - create
        NotificationRule rule = new NotificationRule()
                .name("Test Rule " + timestamp)
                .enabled(true)
                .templateId(savedTemplate.getId())
                .triggerType(NotificationRuleTriggerType.ENTITY_ACTION)
                .triggerConfig(triggerConfig)
                .recipientsConfig(recipientsConfig);

        NotificationRule savedRule = client.saveNotificationRule(SaveNotificationRuleArgs.builder()
                .notificationRule(rule)
                .build());
        assertNotNull(savedRule);
        assertNotNull(savedRule.getId());
        assertEquals("Test Rule " + timestamp, savedRule.getName());
        assertEquals(NotificationRuleTriggerType.ENTITY_ACTION, savedRule.getTriggerType());
        assertEquals(Boolean.TRUE, savedRule.getEnabled());

        // getNotificationRuleById
        NotificationRuleInfo fetchedRule = client.getNotificationRuleById(GetNotificationRuleByIdArgs.builder()
                .id(savedRule.getId().getId())
                .build());
        assertNotNull(fetchedRule);
        assertEquals(savedRule.getName(), fetchedRule.getName());
        assertEquals(NotificationRuleTriggerType.ENTITY_ACTION, fetchedRule.getTriggerType());

        // getNotificationRules - verify it appears in the list
        PageDataNotificationRuleInfo rulesPage = client.getNotificationRules(GetNotificationRulesArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(rulesPage);
        assertTrue(rulesPage.getData().stream()
                .anyMatch(r -> r.getId().getId().equals(savedRule.getId().getId())));

        // deleteNotificationRule
        client.deleteNotificationRule(DeleteNotificationRuleArgs.builder()
                .id(savedRule.getId().getId())
                .build());
        assertReturns404(() -> client.getNotificationRuleById(GetNotificationRuleByIdArgs.builder()
                .id(savedRule.getId().getId())
                .build()));

        // Cleanup
        client.deleteNotificationTemplateById(DeleteNotificationTemplateByIdArgs.builder()
                .id(savedTemplate.getId().getId())
                .build());
        client.deleteNotificationTargetById(DeleteNotificationTargetByIdArgs.builder()
                .id(savedTarget.getId().getId())
                .build());
    }

}
