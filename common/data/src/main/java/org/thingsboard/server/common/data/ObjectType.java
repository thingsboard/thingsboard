// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public enum ObjectType {
    TENANT,
    TENANT_PROFILE,
    CUSTOMER,
    QUEUE,
    RPC,
    RULE_CHAIN,
    OTA_PACKAGE,
    RESOURCE,
    ROLE,
    ENTITY_GROUP,
    DEVICE_GROUP_OTA_PACKAGE,
    GROUP_PERMISSION,
    BLOB_ENTITY,
    SCHEDULER_EVENT,
    EVENT,
    RULE_NODE,
    CONVERTER,
    INTEGRATION,
    USER,
    EDGE,
    WIDGETS_BUNDLE,
    WIDGET_TYPE,
    DASHBOARD,
    REPORT_TEMPLATE,
    REPORT,
    DEVICE_PROFILE,
    DEVICE,
    DEVICE_CREDENTIALS,
    ASSET_PROFILE,
    ASSET,
    ENTITY_VIEW,
    ALARM,
    ENTITY_ALARM,
    OAUTH2_CLIENT,
    OAUTH2_DOMAIN,
    OAUTH2_MOBILE,
    USER_SETTINGS,
    NOTIFICATION_TARGET,
    NOTIFICATION_TEMPLATE,
    NOTIFICATION_RULE,
    WHITE_LABELING,
    CUSTOM_TRANSLATION,
    ALARM_COMMENT,
    API_USAGE_STATE,
    QUEUE_STATS,

    AUDIT_LOG,
    RELATION,
    ATTRIBUTE_KV,
    LATEST_TS_KV;

    public static final Set<ObjectType> edqsTenantTypes = EnumSet.of(
            TENANT, CUSTOMER, DEVICE_PROFILE, DEVICE, ASSET_PROFILE, ASSET, EDGE, ENTITY_VIEW, USER, DASHBOARD,
            RULE_CHAIN, WIDGET_TYPE, WIDGETS_BUNDLE, CONVERTER, INTEGRATION, SCHEDULER_EVENT, ROLE,
            BLOB_ENTITY, API_USAGE_STATE, QUEUE_STATS, REPORT_TEMPLATE, REPORT
    );
    public static final Set<ObjectType> edqsTypes = EnumSet.copyOf(edqsTenantTypes);
    public static final Set<ObjectType> edqsSystemTypes = EnumSet.of(TENANT, USER, DASHBOARD,
            API_USAGE_STATE, ATTRIBUTE_KV, LATEST_TS_KV);
    public static final Set<ObjectType> unversionedTypes = EnumSet.of(
            QUEUE_STATS, // created once, never updated
            BLOB_ENTITY, // created once, never updated
            REPORT // created once, never updated
    );

    static {
        edqsTypes.addAll(List.of(ENTITY_GROUP, RELATION, ATTRIBUTE_KV, LATEST_TS_KV));
    }

    public EntityType toEntityType() {
        return EntityType.valueOf(name());
    }

    public static ObjectType fromEntityType(EntityType entityType) {
        try {
            return ObjectType.valueOf(entityType.name());
        } catch (Exception e) {
            return null;
        }
    }

}
