// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.notification.rule.DefaultNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.EscalatedNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.DeviceActivityNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EdgeCommunicationFailureNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EdgeConnectionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
public class NotificationRuleExportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> extends BaseEntityExportService<NotificationRuleId, NotificationRule, EntityExportData<NotificationRule>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, NotificationRule notificationRule, EntityExportData<NotificationRule> exportData) {
        notificationRule.setTemplateId(getExternalIdOrElseInternal(ctx, notificationRule.getTemplateId()));

        NotificationRuleTriggerConfig ruleTriggerConfig = notificationRule.getTriggerConfig();
        switch (ruleTriggerConfig.getTriggerType()) {
            case DEVICE_ACTIVITY: {
                DeviceActivityNotificationRuleTriggerConfig triggerConfig = (DeviceActivityNotificationRuleTriggerConfig) ruleTriggerConfig;
                Set<UUID> devices = triggerConfig.getDevices();
                if (devices != null) {
                    triggerConfig.setDevices(toExternalIds(devices, DeviceId::new, ctx).collect(Collectors.toSet()));
                }

                Set<UUID> deviceProfiles = triggerConfig.getDeviceProfiles();
                if (deviceProfiles != null) {
                    triggerConfig.setDeviceProfiles(toExternalIds(deviceProfiles, DeviceProfileId::new, ctx).collect(Collectors.toSet()));
                }
                break;
            }
            case RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT: {
                RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig triggerConfig = (RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig) ruleTriggerConfig;
                Set<UUID> ruleChains = triggerConfig.getRuleChains();
                if (ruleChains != null) {
                    triggerConfig.setRuleChains(toExternalIds(ruleChains, RuleChainId::new, ctx).collect(Collectors.toSet()));
                }
                break;
            }
            case EDGE_CONNECTION: {
                EdgeConnectionNotificationRuleTriggerConfig triggerConfig = (EdgeConnectionNotificationRuleTriggerConfig) ruleTriggerConfig;
                triggerConfig.setEdges(null);
                break;
            }
            case EDGE_COMMUNICATION_FAILURE: {
                EdgeCommunicationFailureNotificationRuleTriggerConfig triggerConfig = (EdgeCommunicationFailureNotificationRuleTriggerConfig) ruleTriggerConfig;
                triggerConfig.setEdges(null);
                break;
            }
        }

        NotificationRuleRecipientsConfig ruleRecipientsConfig = notificationRule.getRecipientsConfig();
        switch (ruleTriggerConfig.getTriggerType()) {
            case ALARM: {
                EscalatedNotificationRuleRecipientsConfig recipientsConfig = (EscalatedNotificationRuleRecipientsConfig) ruleRecipientsConfig;
                Map<Integer, List<UUID>> escalationTable = new LinkedHashMap<>(recipientsConfig.getEscalationTable());
                escalationTable.replaceAll((delay, targets) -> {
                    return toExternalIds(targets, NotificationTargetId::new, ctx).collect(Collectors.toList());
                });
                recipientsConfig.setEscalationTable(escalationTable);
                break;
            }
            default: {
                DefaultNotificationRuleRecipientsConfig recipientsConfig = (DefaultNotificationRuleRecipientsConfig) ruleRecipientsConfig;
                List<UUID> targets = recipientsConfig.getTargets();
                targets = toExternalIds(targets, NotificationTargetId::new, ctx).collect(Collectors.toList());
                recipientsConfig.setTargets(targets);
                break;
            }
        }
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.NOTIFICATION_RULE);
    }

}
