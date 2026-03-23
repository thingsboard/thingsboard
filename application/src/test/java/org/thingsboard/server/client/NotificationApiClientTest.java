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
package org.thingsboard.server.client;

import org.junit.Test;
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

        NotificationTarget savedTarget = client.saveNotificationTarget(target);
        assertNotNull(savedTarget);
        assertNotNull(savedTarget.getId());
        assertEquals("Test Target " + timestamp, savedTarget.getName());

        // Get target by ID
        NotificationTarget fetchedTarget =
                client.getNotificationTargetById(savedTarget.getId().getId());
        assertEquals(savedTarget.getName(), fetchedTarget.getName());

        // List targets
        PageDataNotificationTarget targetsPage =
                client.getNotificationTargets(100, 0, null, null, null);
        assertNotNull(targetsPage);
        assertNotNull(targetsPage.getData());
        assertTrue(
                targetsPage.getData().stream()
                        .anyMatch(t -> t.getName().equals(savedTarget.getName())));

        // Update target
        savedTarget.setName("Updated Target " + timestamp);
        NotificationTarget updatedTarget = client.saveNotificationTarget(savedTarget);
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

        NotificationTemplate savedTemplate = client.saveNotificationTemplate(template);
        assertNotNull(savedTemplate);
        assertNotNull(savedTemplate.getId());
        assertEquals("Test Template " + timestamp, savedTemplate.getName());

        // Get template by ID
        NotificationTemplate fetchedTemplate =
                client.getNotificationTemplateById(savedTemplate.getId().getId());
        assertEquals(savedTemplate.getName(), fetchedTemplate.getName());
        assertEquals(NotificationType.GENERAL, fetchedTemplate.getNotificationType());

        // List templates
        PageDataNotificationTemplate templatesPage =
                client.getNotificationTemplates(100, 0, null, null, null, null);
        assertNotNull(templatesPage);
        assertTrue(
                templatesPage.getData().stream()
                        .anyMatch(t -> t.getName().equals(savedTemplate.getName())));

        // Update template
        savedTemplate.setName("Updated Template " + timestamp);
        NotificationTemplate updatedTemplate = client.saveNotificationTemplate(savedTemplate);
        assertEquals("Updated Template " + timestamp, updatedTemplate.getName());

        // === 3. Send notification & read notifications ===

        // Send notification request
        NotificationRequest request =
                new NotificationRequest()
                        .targets(List.of(savedTarget.getId().getId()))
                        .templateId(savedTemplate.getId());
        NotificationRequest sentRequest = client.createNotificationRequest(request);
        assertNotNull(sentRequest);
        assertNotNull(sentRequest.getId());

        // Get request by ID
        NotificationRequestInfo fetchedRequest =
                client.getNotificationRequestById(sentRequest.getId().getId());
        assertNotNull(fetchedRequest);

        // List requests
        PageDataNotificationRequestInfo requestsPage =
                client.getNotificationRequests(100, 0, null, null, null);
        assertNotNull(requestsPage);
        assertFalse(requestsPage.getData().isEmpty());

        // Get notifications for current user
        PageDataNotification notificationsPage =
                client.getNotifications(100, 0, null, null, null, null, null);
        assertNotNull(notificationsPage);
        assertFalse(notificationsPage.getData().isEmpty());

        // Get unread count
        Integer unreadCount = client.getUnreadNotificationsCount("WEB");
        assertNotNull(unreadCount);
        assertTrue("Expected at least one unread notification", unreadCount > 0);

        // Mark single notification as read
        client.markNotificationAsRead(
                notificationsPage.getData().get(0).getId().getId());

        // Mark all as read
        client.markAllNotificationsAsRead(null);
        Integer unreadAfterMarkAll = client.getUnreadNotificationsCount(null);
        assertEquals("Expected no unread notifications after marking all as read", 0, unreadAfterMarkAll.intValue());

        // === 4. Notification Settings ===

        NotificationSettings settings = client.getNotificationSettings();
        assertNotNull(settings);

        List<NotificationDeliveryMethod> deliveryMethods = client.getAvailableDeliveryMethods();
        assertNotNull(deliveryMethods);
        assertTrue(deliveryMethods.contains(NotificationDeliveryMethod.WEB));

        // === 5. Cleanup ===

        // Delete notification request
        client.deleteNotificationRequest(sentRequest.getId().getId());
        assertReturns404(() -> client.getNotificationRequestById(sentRequest.getId().getId()));

        // Delete template
        client.deleteNotificationTemplateById(savedTemplate.getId().getId());
        assertReturns404(() -> client.getNotificationTemplateById(savedTemplate.getId().getId()));

        // Delete target
        client.deleteNotificationTargetById(savedTarget.getId().getId());
        assertReturns404(() -> client.getNotificationTargetById(savedTarget.getId().getId()));
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
        NotificationTarget savedTarget = client.saveNotificationTarget(target);

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
        NotificationTemplate savedTemplate = client.saveNotificationTemplate(template);

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

        NotificationRule savedRule = client.saveNotificationRule(rule);
        assertNotNull(savedRule);
        assertNotNull(savedRule.getId());
        assertEquals("Test Rule " + timestamp, savedRule.getName());
        assertEquals(NotificationRuleTriggerType.ENTITY_ACTION, savedRule.getTriggerType());
        assertEquals(Boolean.TRUE, savedRule.getEnabled());

        // getNotificationRuleById
        NotificationRuleInfo fetchedRule = client.getNotificationRuleById(savedRule.getId().getId());
        assertNotNull(fetchedRule);
        assertEquals(savedRule.getName(), fetchedRule.getName());
        assertEquals(NotificationRuleTriggerType.ENTITY_ACTION, fetchedRule.getTriggerType());

        // getNotificationRules - verify it appears in the list
        PageDataNotificationRuleInfo rulesPage = client.getNotificationRules(100, 0, null, null, null);
        assertNotNull(rulesPage);
        assertTrue(rulesPage.getData().stream()
                .anyMatch(r -> r.getId().getId().equals(savedRule.getId().getId())));

        // deleteNotificationRule
        client.deleteNotificationRule(savedRule.getId().getId());
        assertReturns404(() -> client.getNotificationRuleById(savedRule.getId().getId()));

        // Cleanup
        client.deleteNotificationTemplateById(savedTemplate.getId().getId());
        client.deleteNotificationTargetById(savedTarget.getId().getId());
    }

}
