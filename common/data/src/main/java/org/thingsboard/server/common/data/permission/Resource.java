// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.permission;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.security.Authority;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum Resource {

    ALL(),
    PROFILE(),
    ADMIN_SETTINGS(EntityType.ADMIN_SETTINGS),
    ALARM(EntityType.ALARM),
    DEVICE(EntityType.DEVICE),
    ASSET(EntityType.ASSET),
    CUSTOMER(EntityType.CUSTOMER),
    DASHBOARD(EntityType.DASHBOARD),
    ENTITY_VIEW(EntityType.ENTITY_VIEW),
    EDGE(EntityType.EDGE),
    TENANT(EntityType.TENANT),
    RULE_CHAIN(EntityType.RULE_CHAIN),
    USER(EntityType.USER),
    WIDGETS_BUNDLE(EntityType.WIDGETS_BUNDLE),
    WIDGET_TYPE(EntityType.WIDGET_TYPE),
    OAUTH2_CLIENT(EntityType.OAUTH2_CLIENT),
    DOMAIN(EntityType.DOMAIN),
    MOBILE_APP(EntityType.MOBILE_APP),
    MOBILE_APP_BUNDLE(EntityType.MOBILE_APP_BUNDLE),
    OAUTH2_CONFIGURATION_TEMPLATE(),
    TENANT_PROFILE(EntityType.TENANT_PROFILE),
    DEVICE_PROFILE(EntityType.DEVICE_PROFILE),
    ASSET_PROFILE(EntityType.ASSET_PROFILE),
    CONVERTER(EntityType.CONVERTER),
    INTEGRATION(EntityType.INTEGRATION),
    SCHEDULER_EVENT(EntityType.SCHEDULER_EVENT),
    BLOB_ENTITY(EntityType.BLOB_ENTITY),
    CUSTOMER_GROUP(EntityType.ENTITY_GROUP),
    DEVICE_GROUP(EntityType.ENTITY_GROUP),
    ASSET_GROUP(EntityType.ENTITY_GROUP),
    USER_GROUP(EntityType.ENTITY_GROUP),
    ENTITY_VIEW_GROUP(EntityType.ENTITY_GROUP),
    EDGE_GROUP(EntityType.ENTITY_GROUP),
    DASHBOARD_GROUP(EntityType.ENTITY_GROUP),
    AGENT_GROUP(EntityType.ENTITY_GROUP),
    ROLE(EntityType.ROLE),
    GROUP_PERMISSION(EntityType.GROUP_PERMISSION),
    WHITE_LABELING(),
    AUDIT_LOG(),
    API_USAGE_STATE(EntityType.API_USAGE_STATE),
    TB_RESOURCE(EntityType.TB_RESOURCE),
    OTA_PACKAGE(EntityType.OTA_PACKAGE),
    QUEUE(EntityType.QUEUE),
    QUEUE_STATS(EntityType.QUEUE_STATS),
    VERSION_CONTROL,
    NOTIFICATION(EntityType.NOTIFICATION_TARGET, EntityType.NOTIFICATION_TEMPLATE,
            EntityType.NOTIFICATION_REQUEST, EntityType.NOTIFICATION_RULE),
    MOBILE_APP_SETTINGS,
    CUSTOM_MENU,
    JOB(EntityType.JOB),
    SECRET(EntityType.SECRET),
    REPORT_TEMPLATE(EntityType.REPORT_TEMPLATE),
    REPORT(EntityType.REPORT),
    AI_MODEL(EntityType.AI_MODEL),
    AGENT(EntityType.AGENT),
    AGENT_APP_PROFILE(EntityType.AGENT_APP_PROFILE),
    AGENT_PROFILE(EntityType.AGENT_PROFILE),
    AI,
    API_KEY(EntityType.API_KEY);

    private static final Map<EntityType, Resource> groupResourceByGroupType = new HashMap<>();
    private static final Map<EntityType, Resource> resourceByEntityType = new HashMap<>();
    public static final Map<Resource, Set<Operation>> operationsByResource = new HashMap<>();
    public static final Map<Authority, Set<Resource>> resourcesByAuthority = new HashMap<>();

    static {
        groupResourceByGroupType.put(EntityType.CUSTOMER, CUSTOMER_GROUP);
        groupResourceByGroupType.put(EntityType.DEVICE, DEVICE_GROUP);
        groupResourceByGroupType.put(EntityType.ASSET, ASSET_GROUP);
        groupResourceByGroupType.put(EntityType.USER, USER_GROUP);
        groupResourceByGroupType.put(EntityType.ENTITY_VIEW, ENTITY_VIEW_GROUP);
        groupResourceByGroupType.put(EntityType.EDGE, EDGE_GROUP);
        groupResourceByGroupType.put(EntityType.DASHBOARD, DASHBOARD_GROUP);
        groupResourceByGroupType.put(EntityType.AGENT, AGENT_GROUP);

        for (EntityType entityType : EntityType.values()) {
            if (entityType.equals(EntityType.ENTITY_GROUP)) {
                continue;
            }
            for (Resource resource : Resource.values()) {
                if (resource.getEntityTypes().contains(entityType)) {
                    resourceByEntityType.put(entityType, resource);
                }
            }
        }

        operationsByResource.put(Resource.ALL, Set.of(Operation.values()));
        operationsByResource.put(Resource.PROFILE, Set.of(Operation.ALL, Operation.WRITE));
        operationsByResource.put(Resource.ADMIN_SETTINGS, Set.of(Operation.ALL, Operation.READ, Operation.WRITE));
        operationsByResource.put(Resource.OAUTH2_CLIENT, Operation.crudOperations);
        operationsByResource.put(Resource.DOMAIN, Operation.crudOperations);
        operationsByResource.put(Resource.MOBILE_APP, Operation.crudOperations);
        operationsByResource.put(Resource.MOBILE_APP_BUNDLE, Operation.crudOperations);
        operationsByResource.put(Resource.OAUTH2_CONFIGURATION_TEMPLATE, Operation.crudOperations);
        operationsByResource.put(Resource.ALARM, Set.of(Operation.ALL, Operation.READ, Operation.WRITE, Operation.CREATE));
        operationsByResource.put(Resource.DEVICE, Set.of(Operation.ALL, Operation.READ, Operation.WRITE,
                Operation.CREATE, Operation.DELETE, Operation.RPC_CALL, Operation.READ_CREDENTIALS, Operation.WRITE_CREDENTIALS,
                Operation.READ_ATTRIBUTES, Operation.WRITE_ATTRIBUTES, Operation.READ_TELEMETRY, Operation.WRITE_TELEMETRY,
                Operation.CLAIM_DEVICES, Operation.CHANGE_OWNER, Operation.ASSIGN_TO_TENANT, Operation.READ_CALCULATED_FIELD,
                Operation.WRITE_CALCULATED_FIELD));
        operationsByResource.put(Resource.DEVICE_PROFILE, Operation.defaultCFEntityOperations);
        operationsByResource.put(Resource.ASSET_PROFILE, Operation.defaultCFEntityOperations);
        operationsByResource.put(Resource.OTA_PACKAGE, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.ASSET, Operation.defaultCFEntityOperations);
        operationsByResource.put(Resource.CUSTOMER, Operation.defaultCFEntityOperations);
        operationsByResource.put(Resource.DASHBOARD, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.ENTITY_VIEW, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.EDGE, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.TENANT, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.TENANT_PROFILE, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.API_USAGE_STATE, Set.of(Operation.ALL, Operation.READ, Operation.READ_TELEMETRY));
        operationsByResource.put(Resource.RULE_CHAIN, Operation.defaultEntityOperations);
        Set<Operation> userOperations = new HashSet<>(Operation.defaultEntityOperations);
        userOperations.add(Operation.IMPERSONATE);
        operationsByResource.put(Resource.USER, userOperations);
        operationsByResource.put(Resource.WIDGETS_BUNDLE, Operation.crudOperations);
        operationsByResource.put(Resource.WIDGET_TYPE, Operation.crudOperations);
        operationsByResource.put(Resource.TB_RESOURCE, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.CONVERTER, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.INTEGRATION, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.SCHEDULER_EVENT, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.BLOB_ENTITY, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.CUSTOMER_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.DEVICE_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.ASSET_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.USER_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.ENTITY_VIEW_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.EDGE_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.DASHBOARD_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.AGENT_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.ROLE, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.GROUP_PERMISSION, Operation.crudOperations);
        operationsByResource.put(Resource.WHITE_LABELING, Set.of(Operation.ALL, Operation.READ, Operation.WRITE));
        operationsByResource.put(Resource.AUDIT_LOG, Set.of(Operation.ALL, Operation.READ));
        operationsByResource.put(Resource.QUEUE, Set.of(Operation.ALL, Operation.READ));
        operationsByResource.put(Resource.QUEUE_STATS, Set.of(Operation.ALL, Operation.READ));
        operationsByResource.put(Resource.VERSION_CONTROL, Set.of(Operation.ALL, Operation.READ, Operation.WRITE, Operation.DELETE));
        operationsByResource.put(Resource.NOTIFICATION, Operation.crudOperations);
        operationsByResource.put(Resource.MOBILE_APP_SETTINGS, Set.of(Operation.ALL, Operation.READ, Operation.WRITE));
        operationsByResource.put(Resource.JOB, Operation.crudOperations);
        operationsByResource.put(Resource.SECRET, Operation.crudOperations);
        operationsByResource.put(Resource.REPORT_TEMPLATE, Operation.crudOperations);
        operationsByResource.put(Resource.REPORT, Operation.crudOperations);
        operationsByResource.put(Resource.AI_MODEL, Operation.crudOperations);
        operationsByResource.put(Resource.AI, Set.of(Operation.ALL));
        operationsByResource.put(Resource.CUSTOM_MENU, Set.of(Operation.ALL, Operation.READ, Operation.WRITE, Operation.DELETE));
        operationsByResource.put(Resource.API_KEY, Operation.crudOperations);
        operationsByResource.put(Resource.AGENT, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.AGENT_APP_PROFILE, Operation.crudOperations);
        operationsByResource.put(Resource.AGENT_PROFILE, Operation.crudOperations);

        resourcesByAuthority.put(Authority.SYS_ADMIN, Set.of(
                Resource.ALL,
                Resource.PROFILE,
                Resource.ADMIN_SETTINGS,
                Resource.DASHBOARD,
                Resource.ALARM,
                Resource.TENANT,
                Resource.TENANT_PROFILE,
                Resource.USER,
                Resource.WIDGETS_BUNDLE,
                Resource.WIDGET_TYPE,
                Resource.ROLE,
                Resource.WHITE_LABELING,
                Resource.OAUTH2_CLIENT,
                Resource.DOMAIN,
                Resource.MOBILE_APP,
                Resource.MOBILE_APP_BUNDLE,
                Resource.OAUTH2_CONFIGURATION_TEMPLATE,
                Resource.TB_RESOURCE,
                Resource.QUEUE,
                Resource.QUEUE_STATS,
                Resource.NOTIFICATION,
                Resource.MOBILE_APP_SETTINGS,
                Resource.SECRET,
                Resource.API_KEY
        ));

        resourcesByAuthority.put(Authority.TENANT_ADMIN, Set.of(
                Resource.ALL,
                Resource.PROFILE,
                Resource.ALARM,
                Resource.DEVICE,
                Resource.DEVICE_PROFILE,
                Resource.ASSET_PROFILE,
                Resource.API_USAGE_STATE,
                Resource.ASSET,
                Resource.ENTITY_VIEW,
                Resource.EDGE,
                Resource.CUSTOMER,
                Resource.DASHBOARD,
                Resource.TENANT,
                Resource.USER,
                Resource.WIDGETS_BUNDLE,
                Resource.WIDGET_TYPE,
                Resource.RULE_CHAIN,
                Resource.ROLE,
                Resource.CONVERTER,
                Resource.INTEGRATION,
                Resource.SCHEDULER_EVENT,
                Resource.BLOB_ENTITY,
                Resource.CUSTOMER_GROUP,
                Resource.USER_GROUP,
                Resource.DEVICE_GROUP,
                Resource.ASSET_GROUP,
                Resource.DASHBOARD_GROUP,
                Resource.ENTITY_VIEW_GROUP,
                Resource.EDGE_GROUP,
                Resource.GROUP_PERMISSION,
                Resource.WHITE_LABELING,
                Resource.OAUTH2_CLIENT,
                Resource.DOMAIN,
                Resource.MOBILE_APP,
                Resource.MOBILE_APP_BUNDLE,
                Resource.AUDIT_LOG,
                Resource.TB_RESOURCE,
                Resource.OTA_PACKAGE,
                Resource.QUEUE,
                Resource.QUEUE_STATS,
                Resource.VERSION_CONTROL,
                Resource.NOTIFICATION,
                Resource.MOBILE_APP_SETTINGS,
                Resource.SECRET,
                Resource.JOB,
                Resource.REPORT_TEMPLATE,
                Resource.REPORT,
                Resource.AI_MODEL,
                Resource.AGENT,
                Resource.AGENT_APP_PROFILE,
                Resource.AGENT_PROFILE,
                Resource.AGENT_GROUP,
                Resource.AI,
                Resource.API_KEY
        ));

        resourcesByAuthority.put(Authority.CUSTOMER_USER, Set.of(
                Resource.ALL,
                Resource.PROFILE,
                Resource.ALARM,
                Resource.DEVICE,
                Resource.ASSET,
                Resource.ENTITY_VIEW,
                Resource.EDGE,
                Resource.CUSTOMER,
                Resource.DASHBOARD,
                Resource.USER,
                Resource.WIDGETS_BUNDLE,
                Resource.WIDGET_TYPE,
                Resource.ROLE,
                Resource.SCHEDULER_EVENT,
                Resource.BLOB_ENTITY,
                Resource.CUSTOMER_GROUP,
                Resource.USER_GROUP,
                Resource.DEVICE_GROUP,
                Resource.ASSET_GROUP,
                Resource.DASHBOARD_GROUP,
                Resource.ENTITY_VIEW_GROUP,
                Resource.EDGE_GROUP,
                Resource.GROUP_PERMISSION,
                Resource.WHITE_LABELING,
                Resource.DOMAIN,
                Resource.OAUTH2_CLIENT,
                Resource.AUDIT_LOG,
                Resource.DEVICE_PROFILE,
                Resource.ASSET_PROFILE,
                Resource.MOBILE_APP_SETTINGS,
                Resource.REPORT_TEMPLATE,
                Resource.REPORT,
                Resource.JOB,
                Resource.API_KEY
        ));

    }

    public static Resource groupResourceFromGroupType(EntityType groupType) {
        return groupResourceByGroupType.get(groupType);
    }

    public static Resource resourceFromEntityType(EntityType entityType) {
        return resourceByEntityType.get(entityType);
    }

    public static Set<Operation> operationsForResource(Resource resource) {
        return operationsByResource.get(resource);
    }

    private final Set<EntityType> entityTypes;

    Resource() {
        this.entityTypes = Collections.emptySet();
    }

    Resource(EntityType... entityTypes) {
        this.entityTypes = Set.of(entityTypes);
    }

    public Set<EntityType> getEntityTypes() {
        return entityTypes;
    }

}
