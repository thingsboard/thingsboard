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
package org.thingsboard.server.common.data.id;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEventType;

import java.util.UUID;

public class EntityIdFactory {

    public static EntityId getByTypeAndUuid(int type, String uuid) {
        return getByTypeAndUuid(EntityType.values()[type], UUID.fromString(uuid));
    }

    public static EntityId getByTypeAndUuid(int type, UUID uuid) {
        return getByTypeAndUuid(EntityType.values()[type], uuid);
    }

    public static EntityId getByTypeAndUuid(String type, String uuid) {
        return getByTypeAndUuid(EntityType.valueOf(type), UUID.fromString(uuid));
    }

    public static EntityId getByTypeAndId(String type, String uuid) {
        return getByTypeAndUuid(EntityType.valueOf(type), UUID.fromString(uuid));
    }

    public static EntityId getByTypeAndUuid(String type, UUID uuid) {
        return getByTypeAndUuid(EntityType.valueOf(type), uuid);
    }

    public static EntityId getByTypeAndUuid(EntityType type, String uuid) {
        return getByTypeAndUuid(type, UUID.fromString(uuid));
    }

    public static EntityId getByTypeAndUuid(EntityType type, UUID uuid) {
        return switch (type) {
            case TENANT -> TenantId.fromUUID(uuid);
            case CUSTOMER -> new CustomerId(uuid);
            case USER -> new UserId(uuid);
            case DASHBOARD -> new DashboardId(uuid);
            case DEVICE -> new DeviceId(uuid);
            case ASSET -> new AssetId(uuid);
            case ALARM -> new AlarmId(uuid);
            case RULE_CHAIN -> new RuleChainId(uuid);
            case RULE_NODE -> new RuleNodeId(uuid);
            case ENTITY_VIEW -> new EntityViewId(uuid);
            case WIDGETS_BUNDLE -> new WidgetsBundleId(uuid);
            case WIDGET_TYPE -> new WidgetTypeId(uuid);
            case DEVICE_PROFILE -> new DeviceProfileId(uuid);
            case ASSET_PROFILE -> new AssetProfileId(uuid);
            case TENANT_PROFILE -> new TenantProfileId(uuid);
            case API_USAGE_STATE -> new ApiUsageStateId(uuid);
            case TB_RESOURCE -> new TbResourceId(uuid);
            case OTA_PACKAGE -> new OtaPackageId(uuid);
            case EDGE -> new EdgeId(uuid);
            case RPC -> new RpcId(uuid);
            case QUEUE -> new QueueId(uuid);
            case NOTIFICATION_TARGET -> new NotificationTargetId(uuid);
            case NOTIFICATION_REQUEST -> new NotificationRequestId(uuid);
            case NOTIFICATION_RULE -> new NotificationRuleId(uuid);
            case NOTIFICATION_TEMPLATE -> new NotificationTemplateId(uuid);
            case NOTIFICATION -> new NotificationId(uuid);
            case QUEUE_STATS -> new QueueStatsId(uuid);
            case OAUTH2_CLIENT -> new OAuth2ClientId(uuid);
            case MOBILE_APP -> new MobileAppId(uuid);
            case DOMAIN -> new DomainId(uuid);
            case MOBILE_APP_BUNDLE -> new MobileAppBundleId(uuid);
            case CALCULATED_FIELD -> new CalculatedFieldId(uuid);
            case JOB -> new JobId(uuid);
            case ADMIN_SETTINGS -> new AdminSettingsId(uuid);
            case AI_MODEL -> new AiModelId(uuid);
            case API_KEY -> new ApiKeyId(uuid);
        };
    }

    public static EntityId getByEdgeEventTypeAndUuid(EdgeEventType edgeEventType, UUID uuid) {
        return switch (edgeEventType) {
            case TENANT -> TenantId.fromUUID(uuid);
            case CUSTOMER -> new CustomerId(uuid);
            case USER -> new UserId(uuid);
            case DASHBOARD -> new DashboardId(uuid);
            case DEVICE -> new DeviceId(uuid);
            case ASSET -> new AssetId(uuid);
            case ALARM -> new AlarmId(uuid);
            case RULE_CHAIN -> new RuleChainId(uuid);
            case ENTITY_VIEW -> new EntityViewId(uuid);
            case WIDGETS_BUNDLE -> new WidgetsBundleId(uuid);
            case WIDGET_TYPE -> new WidgetTypeId(uuid);
            case DEVICE_PROFILE -> new DeviceProfileId(uuid);
            case ASSET_PROFILE -> new AssetProfileId(uuid);
            case TENANT_PROFILE -> new TenantProfileId(uuid);
            case OTA_PACKAGE -> new OtaPackageId(uuid);
            case EDGE -> EdgeId.fromUUID(uuid);
            case QUEUE -> new QueueId(uuid);
            case TB_RESOURCE -> new TbResourceId(uuid);
            case NOTIFICATION_RULE -> new NotificationRuleId(uuid);
            case NOTIFICATION_TARGET -> new NotificationTargetId(uuid);
            case NOTIFICATION_TEMPLATE -> new NotificationTemplateId(uuid);
            case OAUTH2_CLIENT -> new OAuth2ClientId(uuid);
            case DOMAIN -> new DomainId(uuid);
            case CALCULATED_FIELD -> new CalculatedFieldId(uuid);
            case AI_MODEL -> new AiModelId(uuid);
            default -> throw new IllegalArgumentException("EdgeEventType " + edgeEventType + " is not supported!");
        };
    }

}
