// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.notification.info;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;

// The type id is the fully qualified class name (JsonTypeInfo.Id.CLASS). The subtypes and the mapping
// below only document it in the OpenAPI spec; they do not change how Jackson reads or writes the type.
@Schema(
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.AddonAccessErrorNotificationInfo", schema = AddonAccessErrorNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.AddonAccessRequestNotificationInfo", schema = AddonAccessRequestNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.AlarmAssignmentNotificationInfo", schema = AlarmAssignmentNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.AlarmCommentNotificationInfo", schema = AlarmCommentNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.AlarmNotificationInfo", schema = AlarmNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.ApiUsageLimitNotificationInfo", schema = ApiUsageLimitNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.DeviceActivityNotificationInfo", schema = DeviceActivityNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.EdgeCommunicationFailureNotificationInfo", schema = EdgeCommunicationFailureNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.EdgeConnectionNotificationInfo", schema = EdgeConnectionNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.EntitiesLimitIncreaseRequestNotificationInfo", schema = EntitiesLimitIncreaseRequestNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.EntitiesLimitNotificationInfo", schema = EntitiesLimitNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.EntityActionNotificationInfo", schema = EntityActionNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.GeneralNotificationInfo", schema = GeneralNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.IntegrationLifecycleEventNotificationInfo", schema = IntegrationLifecycleEventNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.NewPlatformVersionNotificationInfo", schema = NewPlatformVersionNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.PlanUpgradeRequestNotificationInfo", schema = PlanUpgradeRequestNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.RateLimitsNotificationInfo", schema = RateLimitsNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.ReportGeneratedNotificationInfo", schema = ReportGeneratedNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.ResourcesShortageNotificationInfo", schema = ResourcesShortageNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.RuleEngineComponentLifecycleEventNotificationInfo", schema = RuleEngineComponentLifecycleEventNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.RuleEngineOriginatedNotificationInfo", schema = RuleEngineOriginatedNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.TaskProcessingFailureNotificationInfo", schema = TaskProcessingFailureNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.UserActivatedNotificationInfo", schema = UserActivatedNotificationInfo.class),
                @DiscriminatorMapping(value = "org.thingsboard.server.common.data.notification.info.UserRegisteredNotificationInfo", schema = UserRegisteredNotificationInfo.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(AddonAccessErrorNotificationInfo.class),
        @JsonSubTypes.Type(AddonAccessRequestNotificationInfo.class),
        @JsonSubTypes.Type(AlarmAssignmentNotificationInfo.class),
        @JsonSubTypes.Type(AlarmCommentNotificationInfo.class),
        @JsonSubTypes.Type(AlarmNotificationInfo.class),
        @JsonSubTypes.Type(ApiUsageLimitNotificationInfo.class),
        @JsonSubTypes.Type(DeviceActivityNotificationInfo.class),
        @JsonSubTypes.Type(EdgeCommunicationFailureNotificationInfo.class),
        @JsonSubTypes.Type(EdgeConnectionNotificationInfo.class),
        @JsonSubTypes.Type(EntitiesLimitIncreaseRequestNotificationInfo.class),
        @JsonSubTypes.Type(EntitiesLimitNotificationInfo.class),
        @JsonSubTypes.Type(EntityActionNotificationInfo.class),
        @JsonSubTypes.Type(GeneralNotificationInfo.class),
        @JsonSubTypes.Type(IntegrationLifecycleEventNotificationInfo.class),
        @JsonSubTypes.Type(NewPlatformVersionNotificationInfo.class),
        @JsonSubTypes.Type(PlanUpgradeRequestNotificationInfo.class),
        @JsonSubTypes.Type(RateLimitsNotificationInfo.class),
        @JsonSubTypes.Type(ReportGeneratedNotificationInfo.class),
        @JsonSubTypes.Type(ResourcesShortageNotificationInfo.class),
        @JsonSubTypes.Type(RuleEngineComponentLifecycleEventNotificationInfo.class),
        @JsonSubTypes.Type(RuleEngineOriginatedNotificationInfo.class),
        @JsonSubTypes.Type(TaskProcessingFailureNotificationInfo.class),
        @JsonSubTypes.Type(UserActivatedNotificationInfo.class),
        @JsonSubTypes.Type(UserRegisteredNotificationInfo.class)
})
public interface NotificationInfo {

    @JsonIgnore
    Map<String, String> getTemplateData();

    default EntityId getStateEntityId() {
        return null;
    }

    default DashboardId getDashboardId() {
        return null;
    }

    static NotificationInfo userActivated(String userFullName, String userEmail) {
        return UserActivatedNotificationInfo.builder()
                .userFullName(userFullName)
                .userEmail(userEmail)
                .build();
    }

    static NotificationInfo userRegistered(String userFullName, String userEmail) {
        return UserRegisteredNotificationInfo.builder()
                .userFullName(userFullName)
                .userEmail(userEmail)
                .build();
    }

}
