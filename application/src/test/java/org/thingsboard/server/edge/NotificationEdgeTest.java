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
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.rule.EscalatedNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.TenantAdministratorsFilter;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.EmailDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.HasSubject;
import org.thingsboard.server.common.data.notification.template.MobileAppDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.notification.template.SmsDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.WebDeliveryMethodNotificationTemplate;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.NotificationRuleUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTargetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTemplateUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class NotificationEdgeTest extends AbstractEdgeTest {

    @Test
    public void testNotificationTemplate() throws Exception {
        // create notification template
        edgeImitator.expectMessageAmount(1);
        NotificationDeliveryMethod[] deliveryMethods = new NotificationDeliveryMethod[]{
                NotificationDeliveryMethod.WEB
        };
        NotificationTemplate template = createNotificationTemplate(NotificationType.GENERAL, deliveryMethods);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof NotificationTemplateUpdateMsg);
        NotificationTemplateUpdateMsg notificationTemplateUpdateMsg = (NotificationTemplateUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, notificationTemplateUpdateMsg.getMsgType());
        NotificationTemplate notificationTemplate = JacksonUtil.fromString(notificationTemplateUpdateMsg.getEntity(), NotificationTemplate.class, true);
        Assert.assertNotNull(notificationTemplate);
        Assert.assertEquals(template.getId(), notificationTemplate.getId());
        Assert.assertEquals(template.getName(), notificationTemplate.getName());
        Assert.assertEquals(template.getNotificationType(), notificationTemplate.getNotificationType());

        // update notification template
        edgeImitator.expectMessageAmount(1);
        template.setName(StringUtils.randomAlphanumeric(15));
        saveNotificationTemplate(template);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof NotificationTemplateUpdateMsg);
        notificationTemplateUpdateMsg = (NotificationTemplateUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, notificationTemplateUpdateMsg.getMsgType());
        notificationTemplate = JacksonUtil.fromString(notificationTemplateUpdateMsg.getEntity(), NotificationTemplate.class, true);
        Assert.assertNotNull(notificationTemplate);
        Assert.assertEquals(template.getId(), notificationTemplate.getId());
        Assert.assertEquals(template.getName(), notificationTemplate.getName());
        Assert.assertEquals(template.getNotificationType(), notificationTemplate.getNotificationType());

        // delete notification template
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/notification/template/" + notificationTemplate.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof NotificationTemplateUpdateMsg);
        notificationTemplateUpdateMsg = (NotificationTemplateUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, notificationTemplateUpdateMsg.getMsgType());
        Assert.assertEquals(notificationTemplate.getUuidId().getMostSignificantBits(), notificationTemplateUpdateMsg.getIdMSB());
        Assert.assertEquals(notificationTemplate.getUuidId().getLeastSignificantBits(), notificationTemplateUpdateMsg.getIdLSB());
    }

    @Test
    public void testNotificationTarget() throws Exception {
        // create notification target
        edgeImitator.expectMessageAmount(1);
        NotificationTarget target = createNotificationTarget();
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof NotificationTargetUpdateMsg);
        NotificationTargetUpdateMsg notificationTargetUpdateMsg = (NotificationTargetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, notificationTargetUpdateMsg.getMsgType());
        NotificationTarget notificationTarget = JacksonUtil.fromString(notificationTargetUpdateMsg.getEntity(), NotificationTarget.class, true);
        Assert.assertNotNull(notificationTarget);
        Assert.assertEquals(target, notificationTarget);

        // update notification target
        edgeImitator.expectMessageAmount(1);
        target.setName(StringUtils.randomAlphanumeric(15));
        target = saveNotificationTarget(target);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof NotificationTargetUpdateMsg);
        notificationTargetUpdateMsg = (NotificationTargetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, notificationTargetUpdateMsg.getMsgType());
        notificationTarget = JacksonUtil.fromString(notificationTargetUpdateMsg.getEntity(), NotificationTarget.class, true);
        Assert.assertNotNull(notificationTarget);
        Assert.assertEquals(target, notificationTarget);

        // delete notification target
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/notification/target/" + notificationTarget.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof NotificationTargetUpdateMsg);
        notificationTargetUpdateMsg = (NotificationTargetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, notificationTargetUpdateMsg.getMsgType());
        Assert.assertEquals(notificationTarget.getUuidId().getMostSignificantBits(), notificationTargetUpdateMsg.getIdMSB());
        Assert.assertEquals(notificationTarget.getUuidId().getLeastSignificantBits(), notificationTargetUpdateMsg.getIdLSB());
    }

    @Test
    public void testNotificationRule() throws Exception {
        // create notification template for notification rule
        edgeImitator.expectMessageAmount(1);
        NotificationDeliveryMethod[] deliveryMethods = new NotificationDeliveryMethod[]{
                NotificationDeliveryMethod.WEB, NotificationDeliveryMethod.EMAIL
        };
        NotificationTemplate template = createNotificationTemplate(NotificationType.EDGE_CONNECTION, deliveryMethods);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof NotificationTemplateUpdateMsg);
        NotificationTemplateUpdateMsg notificationTemplateUpdateMsg = (NotificationTemplateUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, notificationTemplateUpdateMsg.getMsgType());
        NotificationTemplate notificationTemplate = JacksonUtil.fromString(notificationTemplateUpdateMsg.getEntity(), NotificationTemplate.class, true);
        Assert.assertNotNull(notificationTemplate);
        Assert.assertEquals(template.getId(), notificationTemplate.getId());
        Assert.assertEquals(template.getName(), notificationTemplate.getName());
        Assert.assertEquals(template.getNotificationType(), notificationTemplate.getNotificationType());

        // create notification rule
        edgeImitator.expectMessageAmount(1);
        NotificationRule rule = createNotificationRule(template);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof NotificationRuleUpdateMsg);
        NotificationRuleUpdateMsg notificationRuleUpdateMsg = (NotificationRuleUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, notificationRuleUpdateMsg.getMsgType());
        NotificationRule notificationRule = JacksonUtil.fromString(notificationRuleUpdateMsg.getEntity(), NotificationRule.class, true);
        Assert.assertNotNull(notificationRule);
        Assert.assertEquals(rule, notificationRule);

        // update notification rule
        edgeImitator.expectMessageAmount(1);
        rule.setEnabled(false);
        rule = saveNotificationRule(rule);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof NotificationRuleUpdateMsg);
        notificationRuleUpdateMsg = (NotificationRuleUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, notificationRuleUpdateMsg.getMsgType());
        notificationRule = JacksonUtil.fromString(notificationRuleUpdateMsg.getEntity(), NotificationRule.class, true);
        Assert.assertNotNull(notificationRule);
        Assert.assertEquals(rule, notificationRule);

        // delete notification rule
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/notification/rule/" + notificationRule.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof NotificationRuleUpdateMsg);
        notificationRuleUpdateMsg = (NotificationRuleUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, notificationRuleUpdateMsg.getMsgType());
        Assert.assertEquals(notificationRule.getUuidId().getMostSignificantBits(), notificationRuleUpdateMsg.getIdMSB());
        Assert.assertEquals(notificationRule.getUuidId().getLeastSignificantBits(), notificationRuleUpdateMsg.getIdLSB());

        // delete notification template
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/notification/template/" + notificationTemplate.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof NotificationTemplateUpdateMsg);
        notificationTemplateUpdateMsg = (NotificationTemplateUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, notificationTemplateUpdateMsg.getMsgType());
        Assert.assertEquals(notificationTemplate.getUuidId().getMostSignificantBits(), notificationTemplateUpdateMsg.getIdMSB());
        Assert.assertEquals(notificationTemplate.getUuidId().getLeastSignificantBits(), notificationTemplateUpdateMsg.getIdLSB());
    }

    private NotificationTemplate createNotificationTemplate(NotificationType notificationType, NotificationDeliveryMethod... deliveryMethods) {
        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setTenantId(tenantId);
        notificationTemplate.setName(StringUtils.randomAlphanumeric(15));
        notificationTemplate.setNotificationType(notificationType);
        NotificationTemplateConfig config = new NotificationTemplateConfig();
        config.setDeliveryMethodsTemplates(new HashMap<>());
        for (NotificationDeliveryMethod deliveryMethod : deliveryMethods) {
            DeliveryMethodNotificationTemplate deliveryMethodNotificationTemplate;
            switch (deliveryMethod) {
                case WEB -> deliveryMethodNotificationTemplate = new WebDeliveryMethodNotificationTemplate();
                case EMAIL -> deliveryMethodNotificationTemplate = new EmailDeliveryMethodNotificationTemplate();
                case SMS -> deliveryMethodNotificationTemplate = new SmsDeliveryMethodNotificationTemplate();
                case MOBILE_APP -> deliveryMethodNotificationTemplate = new MobileAppDeliveryMethodNotificationTemplate();
                default -> throw new IllegalArgumentException("Unsupported delivery method " + deliveryMethod);
            }
            deliveryMethodNotificationTemplate.setEnabled(true);
            deliveryMethodNotificationTemplate.setBody("Test text");
            if (deliveryMethodNotificationTemplate instanceof HasSubject) {
                ((HasSubject) deliveryMethodNotificationTemplate).setSubject("Test subject");
            }
            config.getDeliveryMethodsTemplates().put(deliveryMethod, deliveryMethodNotificationTemplate);
        }
        notificationTemplate.setConfiguration(config);
        return saveNotificationTemplate(notificationTemplate);
    }

    private NotificationTarget createNotificationTarget() {
        NotificationTarget notificationTarget = new NotificationTarget();
        notificationTarget.setTenantId(tenantId);
        notificationTarget.setName("Test target");

        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        TenantAdministratorsFilter tenantAdministratorsFilter = new TenantAdministratorsFilter();
        tenantAdministratorsFilter.setTenantsIds(Set.of());
        tenantAdministratorsFilter.setTenantProfilesIds(Set.of());
        targetConfig.setUsersFilter(tenantAdministratorsFilter);
        notificationTarget.setConfiguration(targetConfig);
        return saveNotificationTarget(notificationTarget);
    }

    private NotificationRule createNotificationRule(NotificationTemplate notificationTemplate) {
        NotificationRule notificationRule = new NotificationRule();
        notificationRule.setName("Web notification on any alarm");
        notificationRule.setEnabled(true);
        notificationRule.setTemplateId(notificationTemplate.getId());
        notificationRule.setTriggerType(NotificationRuleTriggerType.ALARM);

        AlarmNotificationRuleTriggerConfig triggerConfig = new AlarmNotificationRuleTriggerConfig();
        triggerConfig.setAlarmTypes(null);
        triggerConfig.setAlarmSeverities(null);
        triggerConfig.setNotifyOn(Set.of(AlarmNotificationRuleTriggerConfig.AlarmAction.CREATED, AlarmNotificationRuleTriggerConfig.AlarmAction.SEVERITY_CHANGED, AlarmNotificationRuleTriggerConfig.AlarmAction.ACKNOWLEDGED, AlarmNotificationRuleTriggerConfig.AlarmAction.CLEARED));
        notificationRule.setTriggerConfig(triggerConfig);

        EscalatedNotificationRuleRecipientsConfig recipientsConfig = new EscalatedNotificationRuleRecipientsConfig();
        recipientsConfig.setTriggerType(NotificationRuleTriggerType.ALARM);
        Map<Integer, List<UUID>> escalationTable = new HashMap<>();
        escalationTable.put(Integer.valueOf("1"), new ArrayList<>());
        recipientsConfig.setEscalationTable(escalationTable);
        notificationRule.setRecipientsConfig(recipientsConfig);
        return saveNotificationRule(notificationRule);
    }

    private NotificationRule saveNotificationRule(NotificationRule notificationRule) {
        return doPost("/api/notification/rule", notificationRule, NotificationRule.class);
    }

}
