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
package org.thingsboard.server.common.data.edqs.fields;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;

import java.util.UUID;

public class FieldsUtil {

    public static EntityFields toFields(Object entity) {
        if (entity instanceof Customer customer) {
            return toFields(customer);
        } else if (entity instanceof Tenant tenant) {
            return toFields(tenant);
        } else if (entity instanceof TenantProfile tenantProfile) {
            return toFields(tenantProfile);
        } else if (entity instanceof Device device) {
            return toFields(device);
        } else if (entity instanceof Asset asset) {
            return toFields(asset);
        } else if (entity instanceof Edge edge) {
            return toFields(edge);
        } else if (entity instanceof EntityView entityView) {
            return toFields(entityView);
        } else if (entity instanceof User user) {
            return toFields(user);
        } else if (entity instanceof Dashboard dashboard) {
            return toFields(dashboard);
        } else if (entity instanceof RuleChain ruleChain) {
            return toFields(ruleChain);
        } else if (entity instanceof RuleNode ruleNode) {
            return toFields(ruleNode);
        } else if (entity instanceof WidgetType widgetType) {
            return toFields(widgetType);
        } else if (entity instanceof WidgetsBundle widgetsBundle) {
            return toFields(widgetsBundle);
        } else if (entity instanceof DeviceProfile deviceProfile) {
            return toFields(deviceProfile);
        } else if (entity instanceof AssetProfile assetProfile) {
            return toFields(assetProfile);
        } else if (entity instanceof QueueStats queueStats) {
            return toFields(queueStats);
        } else if (entity instanceof ApiUsageState apiUsageState) {
            return toFields(apiUsageState);
        } else {
            throw new IllegalArgumentException("Unsupported entity type: " + entity.getClass().getName());
        }
    }

    private static CustomerFields toFields(Customer entity) {
        return CustomerFields.builder()
                .id(entity.getUuidId())
                .createdTime(entity.getCreatedTime())
                .name(entity.getTitle())
                .additionalInfo(getText(entity.getAdditionalInfo()))
                .email(entity.getEmail())
                .country(entity.getCountry())
                .state(entity.getState())
                .city(entity.getCity())
                .address(entity.getAddress())
                .address2(entity.getAddress2())
                .zip(entity.getZip())
                .phone(entity.getPhone())
                .version(entity.getVersion())
                .build();
    }

    private static TenantFields toFields(Tenant entity) {
        return TenantFields.builder()
                .id(entity.getUuidId())
                .createdTime(entity.getCreatedTime())
                .name(entity.getTitle())
                .additionalInfo(getText(entity.getAdditionalInfo()))
                .email(entity.getEmail())
                .country(entity.getCountry())
                .state(entity.getState())
                .city(entity.getCity())
                .address(entity.getAddress())
                .address2(entity.getAddress2())
                .zip(entity.getZip())
                .phone(entity.getPhone())
                .region(entity.getRegion())
                .version(entity.getVersion())
                .build();
    }

    private static TenantProfileFields toFields(TenantProfile tenantProfile) {
        return TenantProfileFields.builder()
                .id(tenantProfile.getUuidId())
                .createdTime(tenantProfile.getCreatedTime())
                .name(tenantProfile.getName())
                .isDefault(tenantProfile.isDefault())
                .build();
    }

    private static DeviceFields toFields(Device entity) {
        return DeviceFields.builder()
                .id(entity.getUuidId())
                .createdTime(entity.getCreatedTime())
                .customerId(getCustomerId(entity.getCustomerId()))
                .name(entity.getName())
                .type(entity.getType())
                .deviceProfileId(entity.getDeviceProfileId().getId())
                .label(entity.getLabel())
                .additionalInfo(getText(entity.getAdditionalInfo()))
                .version(entity.getVersion())
                .build();
    }

    private static AssetFields toFields(Asset entity) {
        return AssetFields.builder()
                .id(entity.getUuidId())
                .createdTime(entity.getCreatedTime())
                .customerId(getCustomerId(entity.getCustomerId()))
                .name(entity.getName())
                .type(entity.getType())
                .assetProfileId(entity.getAssetProfileId().getId())
                .label(entity.getLabel())
                .additionalInfo(getText(entity.getAdditionalInfo()))
                .version(entity.getVersion())
                .build();
    }

    private static EdgeFields toFields(Edge entity) {
        return EdgeFields.builder()
                .id(entity.getUuidId())
                .createdTime(entity.getCreatedTime())
                .customerId(getCustomerId(entity.getCustomerId()))
                .name(entity.getName())
                .type(entity.getType())
                .label(entity.getLabel())
                .additionalInfo(getText(entity.getAdditionalInfo()))
                .version(entity.getVersion())
                .build();
    }

    private static EntityViewFields toFields(EntityView entity) {
        return EntityViewFields.builder()
                .id(entity.getUuidId())
                .createdTime(entity.getCreatedTime())
                .customerId(getCustomerId(entity.getCustomerId()))
                .name(entity.getName())
                .type(entity.getType())
                .additionalInfo(getText(entity.getAdditionalInfo()))
                .version(entity.getVersion())
                .build();
    }

    private static UserFields toFields(User entity) {
        return UserFields.builder()
                .id(entity.getUuidId())
                .createdTime(entity.getCreatedTime())
                .customerId(getCustomerId(entity.getCustomerId()))
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .additionalInfo(getText(entity.getAdditionalInfo()))
                .version(entity.getVersion())
                .build();
    }

    private static DashboardFields toFields(Dashboard entity) {
        return DashboardFields.builder()
                .id(entity.getUuidId())
                .createdTime(entity.getCreatedTime())
                .name(entity.getTitle())
                .version(entity.getVersion())
                .build();
    }

    private static RuleChainFields toFields(RuleChain entity) {
        return RuleChainFields.builder()
                .id(entity.getUuidId())
                .createdTime(entity.getCreatedTime())
                .name(entity.getName())
                .additionalInfo(getText(entity.getAdditionalInfo()))
                .version(entity.getVersion())
                .build();
    }

    private static RuleNodeFields toFields(RuleNode entity) {
        return RuleNodeFields.builder()
                .id(entity.getUuidId())
                .createdTime(entity.getCreatedTime())
                .name(entity.getName())
                .additionalInfo(getText(entity.getAdditionalInfo()))
                .build();
    }

    private static WidgetTypeFields toFields(WidgetType entity) {
        return WidgetTypeFields.builder()
                .id(entity.getUuidId())
                .createdTime(entity.getCreatedTime())
                .name(entity.getName())
                .version(entity.getVersion())
                .build();
    }

    private static WidgetsBundleFields toFields(WidgetsBundle entity) {
        return WidgetsBundleFields.builder()
                .id(entity.getUuidId())
                .createdTime(entity.getCreatedTime())
                .name(entity.getName())
                .version(entity.getVersion())
                .build();
    }

    private static AssetProfileFields toFields(AssetProfile entity) {
        return AssetProfileFields.builder()
                .id(entity.getUuidId())
                .createdTime(entity.getCreatedTime())
                .name(entity.getName())
                .isDefault(entity.isDefault())
                .version(entity.getVersion())
                .build();
    }

    private static DeviceProfileFields toFields(DeviceProfile entity) {
        return DeviceProfileFields.builder()
                .id(entity.getUuidId())
                .createdTime(entity.getCreatedTime())
                .name(entity.getName())
                .type(DeviceProfileType.DEFAULT.name())
                .isDefault(entity.isDefault())
                .version(entity.getVersion())
                .build();
    }

    private static QueueStatsFields toFields(QueueStats entity) {
        return QueueStatsFields.builder()
                .id(entity.getUuidId())
                .createdTime(entity.getCreatedTime())
                .queueName(entity.getQueueName())
                .serviceId(entity.getServiceId())
                .build();
    }

    private static ApiUsageStateFields toFields(ApiUsageState entity) {
        return ApiUsageStateFields.builder()
                .id(entity.getUuidId())
                .createdTime(entity.getCreatedTime())
                .customerId(entity.getEntityId().getEntityType() == EntityType.CUSTOMER ? entity.getEntityId().getId() : null)
                .entityId(entity.getEntityId())
                .transportState(entity.getTransportState())
                .dbStorageState(entity.getDbStorageState())
                .reExecState(entity.getReExecState())
                .jsExecState(entity.getJsExecState())
                .tbelExecState(entity.getTbelExecState())
                .emailExecState(entity.getEmailExecState())
                .smsExecState(entity.getSmsExecState())
                .alarmExecState(entity.getAlarmExecState())
                .version(entity.getVersion())
                .build();
    }

    public static String getText(JsonNode node) {
        return node != null && !node.isNull() ? node.toString() : "";
    }

    private static UUID getCustomerId(CustomerId customerId) {
        return (customerId != null && !customerId.getId().equals(CustomerId.NULL_UUID)) ? customerId.getId() : null;
    }

}
