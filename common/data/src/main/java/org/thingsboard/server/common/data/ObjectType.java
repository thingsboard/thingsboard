// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    EVENT,
    RULE_NODE,
    USER,
    EDGE,
    WIDGETS_BUNDLE,
    WIDGET_TYPE,
    DASHBOARD,
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
    ALARM_COMMENT,
    API_USAGE_STATE,
    QUEUE_STATS,

    AUDIT_LOG,
    RELATION,
    ATTRIBUTE_KV,
    LATEST_TS_KV;

    public static final Set<ObjectType> edqsTenantTypes = EnumSet.of(
            TENANT, CUSTOMER, DEVICE_PROFILE, DEVICE, ASSET_PROFILE, ASSET, EDGE, ENTITY_VIEW, USER, DASHBOARD,
            RULE_CHAIN, WIDGET_TYPE, WIDGETS_BUNDLE, API_USAGE_STATE, QUEUE_STATS
    );
    public static final Set<ObjectType> edqsTypes =  EnumSet.copyOf(edqsTenantTypes);
    public static final Set<ObjectType> edqsSystemTypes = EnumSet.of(TENANT, USER, DASHBOARD,
            API_USAGE_STATE, ATTRIBUTE_KV, LATEST_TS_KV);
    public static final Set<ObjectType> unversionedTypes = EnumSet.of(
            QUEUE_STATS // created once, never updated
    );

    static {
        edqsTypes.addAll(List.of(RELATION, ATTRIBUTE_KV, LATEST_TS_KV));
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
