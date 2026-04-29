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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.io.Serializable;
import java.util.UUID;

@JsonDeserialize(using = EntityIdDeserializer.class)
@JsonSerialize(using = EntityIdSerializer.class)
@Schema(
        discriminatorProperty = "entityType",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "ADMIN_SETTINGS", schema = AdminSettingsId.class),
                @DiscriminatorMapping(value = "AI_MODEL", schema = AiModelId.class),
                @DiscriminatorMapping(value = "ALARM", schema = AlarmId.class),
                @DiscriminatorMapping(value = "API_KEY", schema = ApiKeyId.class),
                @DiscriminatorMapping(value = "API_USAGE_STATE", schema = ApiUsageStateId.class),
                @DiscriminatorMapping(value = "ASSET", schema = AssetId.class),
                @DiscriminatorMapping(value = "ASSET_PROFILE", schema = AssetProfileId.class),
                @DiscriminatorMapping(value = "CALCULATED_FIELD", schema = CalculatedFieldId.class),
                @DiscriminatorMapping(value = "CUSTOMER", schema = CustomerId.class),
                @DiscriminatorMapping(value = "DASHBOARD", schema = DashboardId.class),
                @DiscriminatorMapping(value = "DEVICE", schema = DeviceId.class),
                @DiscriminatorMapping(value = "DEVICE_PROFILE", schema = DeviceProfileId.class),
                @DiscriminatorMapping(value = "DOMAIN", schema = DomainId.class),
                @DiscriminatorMapping(value = "EDGE", schema = EdgeId.class),
                @DiscriminatorMapping(value = "ENTITY_VIEW", schema = EntityViewId.class),
                @DiscriminatorMapping(value = "JOB", schema = JobId.class),
                @DiscriminatorMapping(value = "MOBILE_APP", schema = MobileAppId.class),
                @DiscriminatorMapping(value = "MOBILE_APP_BUNDLE", schema = MobileAppBundleId.class),
                @DiscriminatorMapping(value = "NOTIFICATION", schema = NotificationId.class),
                @DiscriminatorMapping(value = "NOTIFICATION_REQUEST", schema = NotificationRequestId.class),
                @DiscriminatorMapping(value = "NOTIFICATION_RULE", schema = NotificationRuleId.class),
                @DiscriminatorMapping(value = "NOTIFICATION_TARGET", schema = NotificationTargetId.class),
                @DiscriminatorMapping(value = "NOTIFICATION_TEMPLATE", schema = NotificationTemplateId.class),
                @DiscriminatorMapping(value = "OAUTH2_CLIENT", schema = OAuth2ClientId.class),
                @DiscriminatorMapping(value = "OTA_PACKAGE", schema = OtaPackageId.class),
                @DiscriminatorMapping(value = "QUEUE", schema = QueueId.class),
                @DiscriminatorMapping(value = "QUEUE_STATS", schema = QueueStatsId.class),
                @DiscriminatorMapping(value = "RPC", schema = RpcId.class),
                @DiscriminatorMapping(value = "RULE_CHAIN", schema = RuleChainId.class),
                @DiscriminatorMapping(value = "RULE_NODE", schema = RuleNodeId.class),
                @DiscriminatorMapping(value = "TB_RESOURCE", schema = TbResourceId.class),
                @DiscriminatorMapping(value = "TENANT", schema = TenantId.class),
                @DiscriminatorMapping(value = "TENANT_PROFILE", schema = TenantProfileId.class),
                @DiscriminatorMapping(value = "USER", schema = UserId.class),
                @DiscriminatorMapping(value = "WIDGETS_BUNDLE", schema = WidgetsBundleId.class),
                @DiscriminatorMapping(value = "WIDGET_TYPE", schema = WidgetTypeId.class)
        }
)
public interface EntityId extends HasUUID, Serializable { //NOSONAR, the constant is closely related to EntityId

    UUID NULL_UUID = UUID.fromString("13814000-1dd2-11b2-8080-808080808080");

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "ID of the entity, time-based UUID v1", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    UUID getId();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "DEVICE")
    EntityType getEntityType();

    @JsonIgnore
    default boolean isNullUid() {
        return NULL_UUID.equals(getId());
    }

}
