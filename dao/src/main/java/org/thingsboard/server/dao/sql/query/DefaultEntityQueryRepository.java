// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.query;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.permission.MergedGroupTypePermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.ApiUsageStateFilter;
import org.thingsboard.server.common.data.query.AssetSearchQueryFilter;
import org.thingsboard.server.common.data.query.ComplexOperation;
import org.thingsboard.server.common.data.query.AssetTypeFilter;
import org.thingsboard.server.common.data.query.DeviceSearchQueryFilter;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EdgeSearchQueryFilter;
import org.thingsboard.server.common.data.query.EdgeTypeFilter;
import org.thingsboard.server.common.data.query.EntitiesByGroupNameFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityFilterType;
import org.thingsboard.server.common.data.query.EntityGroupFilter;
import org.thingsboard.server.common.data.query.EntityGroupListFilter;
import org.thingsboard.server.common.data.query.EntityGroupNameFilter;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.query.EntityNameFilter;
import org.thingsboard.server.common.data.query.EntitySearchQueryFilter;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.EntityViewSearchQueryFilter;
import org.thingsboard.server.common.data.query.EntityViewTypeFilter;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.SchedulerEventFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.StateEntityOwnerFilter;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.sql.AlarmEntity;
import org.thingsboard.server.dao.model.sql.AssetEntity;
import org.thingsboard.server.dao.model.sql.BlobEntityEntity;
import org.thingsboard.server.dao.model.sql.CustomerEntity;
import org.thingsboard.server.dao.model.sql.DashboardEntity;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.model.sql.EdgeEntity;
import org.thingsboard.server.dao.model.sql.EntityGroupEntity;
import org.thingsboard.server.dao.model.sql.EntityViewEntity;
import org.thingsboard.server.dao.model.sql.ReportEntity;
import org.thingsboard.server.dao.model.sql.ReportTemplateInfoEntity;
import org.thingsboard.server.dao.model.sql.RoleEntity;
import org.thingsboard.server.dao.model.sql.SchedulerEventEntity;
import org.thingsboard.server.dao.model.sql.UserEntity;
import org.thingsboard.server.dao.sql.alarm.AlarmRepository;
import org.thingsboard.server.dao.sql.citus.CitusTables;
import org.thingsboard.server.dao.sql.asset.AssetRepository;
import org.thingsboard.server.dao.sql.blob.BlobEntityRepository;
import org.thingsboard.server.dao.sql.customer.CustomerRepository;
import org.thingsboard.server.dao.sql.dashboard.DashboardRepository;
import org.thingsboard.server.dao.sql.device.DeviceRepository;
import org.thingsboard.server.dao.sql.edge.EdgeRepository;
import org.thingsboard.server.dao.sql.entityview.EntityViewRepository;
import org.thingsboard.server.dao.sql.group.EntityGroupRepository;
import org.thingsboard.server.dao.sql.report.ReportRepository;
import org.thingsboard.server.dao.sql.report.ReportTemplateInfoRepository;
import org.thingsboard.server.dao.sql.role.RoleRepository;
import org.thingsboard.server.dao.sql.scheduler.SchedulerEventRepository;
import org.thingsboard.server.dao.sql.user.UserRepository;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class DefaultEntityQueryRepository implements EntityQueryRepository {
    private static final Map<EntityType, String> entityTableMap = new HashMap<>();
    private static final Map<EntityType, String> entityNameColumns = new HashMap<>();
    private static final String SELECT_PHONE = " CASE WHEN entity.entity_type = 'TENANT' THEN (select phone from tenant where id = entity_id)" +
            " WHEN entity.entity_type = 'CUSTOMER' THEN (select phone from customer where id = entity_id)" +
            " WHEN entity.entity_type = 'USER' THEN (select phone from tb_user where id = entity_id) END as phone";
    private static final String SELECT_ZIP = " CASE WHEN entity.entity_type = 'TENANT' THEN (select zip from tenant where id = entity_id)" +
            " WHEN entity.entity_type = 'CUSTOMER' THEN (select zip from customer where id = entity_id) END as zip";
    private static final String SELECT_ADDRESS_2 = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select address2 from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select address2 from customer where id = entity_id) END as address2";
    private static final String SELECT_ADDRESS = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select address from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select address from customer where id = entity_id) END as address";
    private static final String SELECT_CITY = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select city from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select city from customer where id = entity_id) END as city";
    private static final String SELECT_STATE = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select state from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select state from customer where id = entity_id) END as state";
    private static final String SELECT_COUNTRY = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select country from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select country from customer where id = entity_id) END as country";
    private static final String SELECT_TITLE = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select title from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select title from customer where id = entity_id) END as title";
    private static final String SELECT_LAST_NAME = " CASE WHEN entity.entity_type = 'USER'" +
            " THEN (select last_name from tb_user where id = entity_id) END as last_name";
    private static final String SELECT_FIRST_NAME = " CASE WHEN entity.entity_type = 'USER'" +
            " THEN (select first_name from tb_user where id = entity_id) END as first_name";
    private static final String SELECT_REGION = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select region from tenant where id = entity_id) END as region";
    private static final String SELECT_EMAIL = " CASE" +
            " WHEN entity.entity_type = 'TENANT'" +
            " THEN (select email from tenant where id = entity_id)" +
            " WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select email from customer where id = entity_id)" +
            " WHEN entity.entity_type = 'USER'" +
            " THEN (select email from tb_user where id = entity_id)" +
            " END as email";
    /*
     * The seven selection arms below are the only ones that read from the DISTRIBUTED entity tables
     * (device / asset / entity_view). In plain PostgreSQL each such arm is a correlated subquery, e.g.
     * {@code (select name from device where id = entity_id)}. Under Citus that subquery is illegal from the
     * coordinator-local relation-traversal relation, so the arm must instead read the matching pre-fetched
     * column {@code entity.res_<col>} from the unnest(...) derived relation (see resolveRelationTraversal).
     *
     * These arms are therefore built per-instance (in the {@code initSelectArms()} {@link PostConstruct}, once
     * citusEnabled is injected) via the shared {@link #distArm} helper, which emits the correlated subquery in plain
     * mode and {@code entity.res_<col>} under Citus. Plain-mode output is byte-for-byte identical to the previous
     * static constants. The remaining (non-distributed) arms stay static finals further below.
     */
    private String selectCustomerId;
    private String selectTenantId;
    private String selectCreatedTime;
    private String selectName;
    private String selectType;
    private String selectLabel;
    private String selectAdditionalInfo;

    /**
     * Emits a selection arm that reads {@code col} from a DISTRIBUTED entity table. In plain mode it is the original
     * correlated subquery {@code (select col from table where id = idExpr)}; under Citus it is {@code entity.res_<col>},
     * read from the pre-fetched unnest(...) relation. device/asset arms key on {@code entity_id}; entity_view arms
     * key on {@code entity.entity_id}.
     */
    private static String distArm(boolean citus, String col, String table, String idExpr) {
        return citus ? "entity.res_" + col : "(select " + col + " from " + table + " where id = " + idExpr + ")";
    }

    private static String distDeviceArm(boolean citus, String col) {
        return distArm(citus, col, "device", "entity_id");
    }

    private static String distAssetArm(boolean citus, String col) {
        return distArm(citus, col, "asset", "entity_id");
    }

    private static String distEntityViewArm(boolean citus, String col) {
        return distArm(citus, col, "entity_view", "entity.entity_id");
    }

    /**
     * Builds the seven distributed-table selection arms once the injected {@code citusEnabled} flag is available. Kept
     * as its own {@link PostConstruct} (separate from {@link #validateNullsOrderStrategy()}) because it initializes an
     * unrelated concern; the two run independently and in no required order.
     */
    @PostConstruct
    void initSelectArms() {
        boolean citus = citusEnabled;
        selectCustomerId = "CASE" +
                " WHEN entity.entity_type = 'TENANT'" +
                " THEN UUID('" + TenantId.NULL_UUID + "')" +
                " WHEN entity.entity_type = 'CUSTOMER' THEN entity_id" +
                " WHEN entity.entity_type = 'ROLE'" +
                " THEN (select customer_id from role where id = entity_id)" +
                " WHEN entity.entity_type = 'SCHEDULER_EVENT'" +
                " THEN (select customer_id from scheduler_event where id = entity_id)" +
                " WHEN entity.entity_type = 'BLOB_ENTITY'" +
                " THEN (select customer_id from blob_entity where id = entity_id)" +
                " WHEN entity.entity_type = 'REPORT_TEMPLATE'" +
                " THEN (select customer_id from report_template where id = entity_id)" +
                " WHEN entity.entity_type = 'REPORT'" +
                " THEN (select customer_id from report where id = entity_id)" +
                " WHEN entity.entity_type = 'USER'" +
                " THEN (select customer_id from tb_user where id = entity_id)" +
                " WHEN entity.entity_type = 'DASHBOARD'" +
                //TODO: parse assigned customers or use contains?
                " THEN NULL" +
                " WHEN entity.entity_type = 'ASSET'" +
                " THEN " + distAssetArm(citus, "customer_id") +
                " WHEN entity.entity_type = 'DEVICE'" +
                " THEN " + distDeviceArm(citus, "customer_id") +
                " WHEN entity.entity_type = 'ENTITY_VIEW'" +
                " THEN " + distEntityViewArm(citus, "customer_id") +
                " WHEN entity.entity_type = 'EDGE'" +
                " THEN (select customer_id from edge where id = entity_id)" +
                " END as customer_id";
        selectTenantId = "SELECT CASE" +
                " WHEN entity.entity_type = 'TENANT' THEN entity_id" +
                " WHEN entity.entity_type = 'INTEGRATION'" +
                " THEN (select tenant_id from integration where id = entity_id)" +
                " WHEN entity.entity_type = 'CONVERTER'" +
                " THEN (select tenant_id from converter where id = entity_id)" +
                " WHEN entity.entity_type = 'ROLE'" +
                " THEN (select tenant_id from role where id = entity_id)" +
                " WHEN entity.entity_type = 'SCHEDULER_EVENT'" +
                " THEN (select tenant_id from scheduler_event where id = entity_id)" +
                " WHEN entity.entity_type = 'BLOB_ENTITY'" +
                " THEN (select tenant_id from blob_entity where id = entity_id)" +
                " WHEN entity.entity_type = 'REPORT_TEMPLATE'" +
                " THEN (select tenant_id from report_template where id = entity_id)" +
                " WHEN entity.entity_type = 'REPORT'" +
                " THEN (select tenant_id from report where id = entity_id)" +
                " WHEN entity.entity_type = 'CUSTOMER'" +
                " THEN (select tenant_id from customer where id = entity_id)" +
                " WHEN entity.entity_type = 'USER'" +
                " THEN (select tenant_id from tb_user where id = entity_id)" +
                " WHEN entity.entity_type = 'DASHBOARD'" +
                " THEN (select tenant_id from dashboard where id = entity_id)" +
                " WHEN entity.entity_type = 'ASSET'" +
                " THEN " + distAssetArm(citus, "tenant_id") +
                " WHEN entity.entity_type = 'DEVICE'" +
                " THEN " + distDeviceArm(citus, "tenant_id") +
                " WHEN entity.entity_type = 'ENTITY_VIEW'" +
                " THEN " + distEntityViewArm(citus, "tenant_id") +
                " WHEN entity.entity_type = 'EDGE'" +
                " THEN (select tenant_id from edge where id = entity_id)" +
                " END as tenant_id";
        selectCreatedTime = " CASE" +
                " WHEN entity.entity_type = 'TENANT'" +
                " THEN (select created_time from tenant where id = entity_id)" +
                " WHEN entity.entity_type = 'INTEGRATION'" +
                " THEN (select created_time from integration where id = entity_id)" +
                " WHEN entity.entity_type = 'CONVERTER'" +
                " THEN (select created_time from converter where id = entity_id)" +
                " WHEN entity.entity_type = 'ROLE'" +
                " THEN (select created_time from role where id = entity_id)" +
                " WHEN entity.entity_type = 'SCHEDULER_EVENT'" +
                " THEN (select created_time from scheduler_event where id = entity_id)" +
                " WHEN entity.entity_type = 'BLOB_ENTITY'" +
                " THEN (select created_time from blob_entity where id = entity_id)" +
                " WHEN entity.entity_type = 'REPORT_TEMPLATE'" +
                " THEN (select created_time from report_template where id = entity_id)" +
                " WHEN entity.entity_type = 'REPORT'" +
                " THEN (select created_time from report where id = entity_id)" +
                " WHEN entity.entity_type = 'CUSTOMER' " +
                " THEN (select created_time from customer where id = entity_id)" +
                " WHEN entity.entity_type = 'USER'" +
                " THEN (select created_time from tb_user where id = entity_id)" +
                " WHEN entity.entity_type = 'DASHBOARD'" +
                " THEN (select created_time from dashboard where id = entity_id)" +
                " WHEN entity.entity_type = 'ASSET'" +
                " THEN " + distAssetArm(citus, "created_time") +
                " WHEN entity.entity_type = 'DEVICE'" +
                " THEN " + distDeviceArm(citus, "created_time") +
                " WHEN entity.entity_type = 'ENTITY_VIEW'" +
                " THEN " + distEntityViewArm(citus, "created_time") +
                " WHEN entity.entity_type = 'EDGE'" +
                " THEN (select created_time from edge where id = entity_id)" +
                " END as created_time";
        selectName = " CASE" +
                " WHEN entity.entity_type = 'TENANT'" +
                " THEN (select title from tenant where id = entity_id)" +
                " WHEN entity.entity_type = 'INTEGRATION'" +
                " THEN (select name from integration where id = entity_id)" +
                " WHEN entity.entity_type = 'CONVERTER'" +
                " THEN (select name from converter where id = entity_id)" +
                " WHEN entity.entity_type = 'ROLE'" +
                " THEN (select name from role where id = entity_id)" +
                " WHEN entity.entity_type = 'SCHEDULER_EVENT'" +
                " THEN (select name from scheduler_event where id = entity_id)" +
                " WHEN entity.entity_type = 'BLOB_ENTITY'" +
                " THEN (select name from blob_entity where id = entity_id)" +
                " WHEN entity.entity_type = 'REPORT_TEMPLATE'" +
                " THEN (select name from report_template where id = entity_id)" +
                " WHEN entity.entity_type = 'REPORT'" +
                " THEN (select name from report where id = entity_id)" +
                " WHEN entity.entity_type = 'CUSTOMER' " +
                " THEN (select title from customer where id = entity_id)" +
                " WHEN entity.entity_type = 'USER'" +
                " THEN (select CONCAT (first_name, ' ', last_name) from tb_user where id = entity_id)" +
                " WHEN entity.entity_type = 'DASHBOARD'" +
                " THEN (select title from dashboard where id = entity_id)" +
                " WHEN entity.entity_type = 'ASSET'" +
                " THEN " + distAssetArm(citus, "name") +
                " WHEN entity.entity_type = 'DEVICE'" +
                " THEN " + distDeviceArm(citus, "name") +
                " WHEN entity.entity_type = 'ENTITY_VIEW'" +
                " THEN " + distEntityViewArm(citus, "name") +
                " WHEN entity.entity_type = 'EDGE'" +
                " THEN (select name from edge where id = entity_id)" +
                " END as name";
        selectType = " CASE" +
                " WHEN entity.entity_type = 'USER'" +
                " THEN (select authority from tb_user where id = entity_id)" +
                " WHEN entity.entity_type = 'ASSET'" +
                " THEN " + distAssetArm(citus, "type") +
                " WHEN entity.entity_type = 'DEVICE'" +
                " THEN " + distDeviceArm(citus, "type") +
                " WHEN entity.entity_type = 'ENTITY_VIEW'" +
                " THEN " + distEntityViewArm(citus, "type") +
                " WHEN entity.entity_type = 'EDGE'" +
                " THEN (select type from edge where id = entity_id)" +
                " WHEN entity.entity_type = 'SCHEDULER_EVENT'" +
                " THEN (select type from scheduler_event where id = entity_id)" +
                " WHEN entity.entity_type = 'BLOB_ENTITY'" +
                " THEN (select type from blob_entity where id = entity_id)" +
                " WHEN entity.entity_type = 'REPORT_TEMPLATE'" +
                " THEN (select type from report_template where id = entity_id)" +
                " ELSE entity.entity_type END as type";
        // NOTE asymmetry: the entity_view LABEL arm maps to res_name (entity_view has no label column), so it
        // emits distEntityViewArm(citus, "name") — i.e. (select name ...) in plain mode, entity.res_name under Citus.
        selectLabel = " CASE" +
                " WHEN entity.entity_type = 'TENANT'" +
                " THEN (select title from tenant where id = entity_id)" +
                " WHEN entity.entity_type = 'INTEGRATION'" +
                " THEN (select name from integration where id = entity_id)" +
                " WHEN entity.entity_type = 'CONVERTER'" +
                " THEN (select name from converter where id = entity_id)" +
                " WHEN entity.entity_type = 'ROLE'" +
                " THEN (select name from role where id = entity_id)" +
                " WHEN entity.entity_type = 'CUSTOMER' " +
                " THEN (select title from customer where id = entity_id)" +
                " WHEN entity.entity_type = 'USER'" +
                " THEN (select CONCAT (first_name, ' ', last_name) from tb_user where id = entity_id)" +
                " WHEN entity.entity_type = 'DASHBOARD'" +
                " THEN (select title from dashboard where id = entity_id)" +
                " WHEN entity.entity_type = 'ASSET'" +
                " THEN " + distAssetArm(citus, "label") +
                " WHEN entity.entity_type = 'DEVICE'" +
                " THEN " + distDeviceArm(citus, "label") +
                " WHEN entity.entity_type = 'ENTITY_VIEW'" +
                " THEN " + distEntityViewArm(citus, "name") +
                " WHEN entity.entity_type = 'EDGE'" +
                " THEN (select label from edge where id = entity_id)" +
                " END as label";
        selectAdditionalInfo = " CASE" +
                " WHEN entity.entity_type = 'TENANT'" +
                " THEN (select additional_info from tenant where id = entity_id)" +
                " WHEN entity.entity_type = 'CUSTOMER' " +
                " THEN (select additional_info from customer where id = entity_id)" +
                " WHEN entity.entity_type = 'USER'" +
                " THEN (select additional_info from tb_user where id = entity_id)" +
                " WHEN entity.entity_type = 'DASHBOARD'" +
                " THEN (select '' from dashboard where id = entity_id)" +
                " WHEN entity.entity_type = 'ASSET'" +
                " THEN " + distAssetArm(citus, "additional_info") +
                " WHEN entity.entity_type = 'DEVICE'" +
                " THEN " + distDeviceArm(citus, "additional_info") +
                " WHEN entity.entity_type = 'ENTITY_VIEW'" +
                " THEN " + distEntityViewArm(citus, "additional_info") +
                " WHEN entity.entity_type = 'EDGE'" +
                " THEN (select additional_info from edge where id = entity_id)" +
                " END as additional_info";
    }

    public static final String ATTR_READ_FLAG = "attr_read";
    public static final String TS_READ_FLAG = "ts_read";

    private static final String SELECT_RELATED_PARENT_ID = "entity.parent_id AS parent_id";

    private static final String SELECT_API_USAGE_STATE = "(select aus.id, aus.created_time, aus.tenant_id, aus.entity_id, " +
            "coalesce((select title from tenant where id = aus.entity_id), (select title from customer where id = aus.entity_id)) as name " +
            "from api_usage_state as aus)";
    public static final MergedUserPermissions SYS_ADMIN_PERMISSIONS = new MergedUserPermissions(Collections.singletonMap(Resource.ALL, Set.of(Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY)), Collections.emptyMap());

    static {
        entityTableMap.put(EntityType.ENTITY_GROUP, "entity_group");
        entityTableMap.put(EntityType.ASSET, "asset");
        entityTableMap.put(EntityType.DEVICE, "device");
        entityTableMap.put(EntityType.ENTITY_VIEW, "entity_view");
        entityTableMap.put(EntityType.DASHBOARD, "dashboard");
        entityTableMap.put(EntityType.CUSTOMER, "customer");
        entityTableMap.put(EntityType.USER, "tb_user");
        entityTableMap.put(EntityType.TENANT, "tenant");
        entityTableMap.put(EntityType.CONVERTER, "converter");
        entityTableMap.put(EntityType.INTEGRATION, "integration");
        entityTableMap.put(EntityType.SCHEDULER_EVENT, "scheduler_event");
        entityTableMap.put(EntityType.BLOB_ENTITY, "blob_entity");
        entityTableMap.put(EntityType.ROLE, "role");
        entityTableMap.put(EntityType.API_USAGE_STATE, SELECT_API_USAGE_STATE);
        entityTableMap.put(EntityType.EDGE, "edge");
        entityTableMap.put(EntityType.RULE_CHAIN, "rule_chain");
        entityTableMap.put(EntityType.DEVICE_PROFILE, "device_profile");
        entityTableMap.put(EntityType.ASSET_PROFILE, "asset_profile");
        entityTableMap.put(EntityType.TENANT_PROFILE, "tenant_profile");
        entityTableMap.put(EntityType.QUEUE_STATS, "queue_stats");
        entityTableMap.put(EntityType.REPORT_TEMPLATE, "report_template");
        entityTableMap.put(EntityType.REPORT, "report");
        entityTableMap.put(EntityType.AGENT, "agent");

        entityNameColumns.put(EntityType.DEVICE, "name");
        entityNameColumns.put(EntityType.CUSTOMER, "title");
        entityNameColumns.put(EntityType.DASHBOARD, "title");
        entityNameColumns.put(EntityType.RULE_CHAIN, "name");
        entityNameColumns.put(EntityType.RULE_NODE, "name");
        entityNameColumns.put(EntityType.OTA_PACKAGE, "title");
        entityNameColumns.put(EntityType.ASSET_PROFILE, "name");
        entityNameColumns.put(EntityType.ASSET, "name");
        entityNameColumns.put(EntityType.DEVICE_PROFILE, "name");
        entityNameColumns.put(EntityType.USER, "email");
        entityNameColumns.put(EntityType.TENANT_PROFILE, "name");
        entityNameColumns.put(EntityType.TENANT, "title");
        entityNameColumns.put(EntityType.WIDGETS_BUNDLE, "title");
        entityNameColumns.put(EntityType.WIDGET_TYPE, "name");
        entityNameColumns.put(EntityType.ENTITY_VIEW, "name");
        entityNameColumns.put(EntityType.TB_RESOURCE, "title");
        entityNameColumns.put(EntityType.EDGE, "name");
        entityNameColumns.put(EntityType.QUEUE, "name");
        entityNameColumns.put(EntityType.ENTITY_GROUP, "name");
        entityNameColumns.put(EntityType.CONVERTER, "name");
        entityNameColumns.put(EntityType.INTEGRATION, "name");
        entityNameColumns.put(EntityType.SCHEDULER_EVENT, "name");
        entityNameColumns.put(EntityType.BLOB_ENTITY, "name");
        entityNameColumns.put(EntityType.ROLE, "name");
        entityNameColumns.put(EntityType.QUEUE_STATS, "queue_name");
        entityNameColumns.put(EntityType.REPORT_TEMPLATE, "name");
        entityNameColumns.put(EntityType.REPORT, "name");
        entityNameColumns.put(EntityType.AGENT, "name");
    }

    public static EntityType[] RELATION_QUERY_ENTITY_TYPES = new EntityType[]{
            EntityType.TENANT, EntityType.CUSTOMER, EntityType.USER, EntityType.DASHBOARD, EntityType.ASSET, EntityType.DEVICE,
            EntityType.CONVERTER, EntityType.INTEGRATION, EntityType.ENTITY_VIEW, EntityType.EDGE, EntityType.ROLE, EntityType.SCHEDULER_EVENT, EntityType.BLOB_ENTITY,
            EntityType.REPORT_TEMPLATE, EntityType.REPORT};

    private static final String HIERARCHICAL_GROUPS_QUERY = "select id from entity_group where owner_id in (" +
            " (WITH RECURSIVE customers_ids(id) AS" +
            " (SELECT id id" +
            " FROM customer" +
            " WHERE tenant_id = :permissions_tenant_id" +
            " and id = :permissions_customer_id" +
            " UNION" +
            " SELECT c.id id" +
            " FROM customer c," +
            " customers_ids parent" +
            " WHERE c.tenant_id = :permissions_tenant_id" +
            " and c.parent_customer_id = parent.id)" +
            " SELECT id" +
            " FROM customers_ids))";

    private static final String HIERARCHICAL_GROUPS_ALL_QUERY = HIERARCHICAL_GROUPS_QUERY + " and name = 'All'";

    public static final String HIERARCHICAL_SUB_CUSTOMERS_QUERY = "(WITH RECURSIVE customers_ids(id) AS" +
            " (SELECT id id FROM customer WHERE tenant_id = :permissions_tenant_id and id = :permissions_customer_id" +
            " UNION SELECT c.id id FROM customer c, customers_ids parent WHERE c.tenant_id = :permissions_tenant_id" +
            " and c.parent_customer_id = parent.id) SELECT id FROM customers_ids)";

    private static final String HIERARCHICAL_QUERY_TEMPLATE = " FROM (WITH RECURSIVE related_entities(from_id, from_type, to_id, to_type, lvl, path) AS (" +
            " SELECT from_id, from_type, to_id, to_type," +
            "        1 as lvl," +
            "        ARRAY[$in_id] as path" + // initial path
            " FROM relation " +
            " WHERE $in_id $rootIdCondition and $in_type = :relation_root_type and relation_type_group = 'COMMON'" +
            " GROUP BY from_id, from_type, to_id, to_type, lvl, path" +
            " UNION ALL" +
            " SELECT r.from_id, r.from_type, r.to_id, r.to_type," +
            "        (re.lvl + 1) as lvl, " +
            "        (re.path || ARRAY[r.$in_id]) as path" +
            " FROM relation r" +
            " INNER JOIN related_entities re ON" +
            " r.$in_id = re.$out_id and r.$in_type = re.$out_type and" +
            " relation_type_group = 'COMMON' " +
            " AND r.$in_id NOT IN (SELECT * FROM unnest(re.path)) " +
            " %s" +
            " GROUP BY r.from_id, r.from_type, r.to_id, r.to_type, (re.lvl + 1), (re.path || ARRAY[r.$in_id])" +
            " )" +
            " SELECT re.$out_id entity_id, re.$out_type entity_type, $parenIdExp max(r_int.lvl) lvl" +
            " from related_entities r_int" +
            "  INNER JOIN relation re ON re.from_id = r_int.from_id AND re.from_type = r_int.from_type" +
            "                         AND re.to_id = r_int.to_id AND re.to_type = r_int.to_type" +
            "                         AND re.relation_type_group = 'COMMON'" +
            " %s GROUP BY entity_id, entity_type $parenIdSelection) entity";

    private static final String HIERARCHICAL_TO_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE
            .replace("$parenIdExp", "")
            .replace("$parenIdSelection", "")
            .replace("$in", "to").replace("$out", "from")
            .replace("$rootIdCondition", "= :relation_root_id");
    private static final String HIERARCHICAL_TO_MR_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE
            .replace("$parenIdExp", "re.$in_id parent_id, ")
            .replace("$parenIdSelection", ", parent_id")
            .replace("$in", "to").replace("$out", "from")
            .replace("$rootIdCondition", "in (:relation_root_ids)");

    private static final String HIERARCHICAL_FROM_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE
            .replace("$parenIdExp", "")
            .replace("$parenIdSelection", "")
            .replace("$in", "from").replace("$out", "to")
            .replace("$rootIdCondition", "= :relation_root_id");
    private static final String HIERARCHICAL_FROM_MR_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE
            .replace("$parenIdExp", "re.$in_id parent_id, ")
            .replace("$parenIdSelection", ", parent_id")
            .replace("$in", "from").replace("$out", "to")
            .replace("$rootIdCondition", "in (:relation_root_ids)");

    // Every HIERARCHICAL_*_QUERY_TEMPLATE wraps the recursive traversal as " FROM (<inner SQL>) entity". These
    // bookends let us derive the bare inner SQL (for running the traversal standalone under Citus) without
    // reverse-engineering it from an already-assembled FROM clause.
    private static final String RELATION_TRAVERSAL_FROM_PREFIX = " FROM (";
    private static final String RELATION_TRAVERSAL_FROM_SUFFIX = ") entity";

    private static final String NULLS_ORDER_DEFAULT = "default";
    private static final String NULLS_ORDER_FIRST = "nulls_first";
    private static final String NULLS_ORDER_LAST = "nulls_last";
    private static final Set<String> ACCEPTED_NULLS_ORDER_STRATEGIES = Set.of(NULLS_ORDER_DEFAULT, NULLS_ORDER_FIRST, NULLS_ORDER_LAST);

    @Getter
    @Value("${sql.relations.max_level:50}")
    int maxLevelAllowed; //This value has to be reasonable small to prevent infinite recursion as early as possible

    @Getter
    @Value("${sql.entity_data_query_nulls_order_strategy:default}")
    String nullsOrderStrategy;

    @Value("${database.citus.enabled:false}")
    boolean citusEnabled;

    // Citus-only defensive cap on the number of entities a single relation/reference recursion may materialize on the
    // coordinator heap before it is exploded into bound arrays (see resolveRelationTraversal / resolveReferenceRecursion).
    // Generous by default; a breach fails the query fast rather than letting a pathological subtree exhaust coordinator
    // memory. Names the property in the thrown message so operators can raise it or narrow the query.
    @Value("${database.citus.relation_query.max_resolved_entities:1000000}")
    int maxResolvedEntities;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final AssetRepository assetRepository;
    private final CustomerRepository customerRepository;
    private final DeviceRepository deviceRepository;
    private final EntityViewRepository entityViewRepository;
    private final EdgeRepository edgeRepository;
    private final UserRepository userRepository;
    private final DashboardRepository dashboardRepository;
    private final EntityGroupRepository entityGroupRepository;
    private final SchedulerEventRepository schedulerEventRepository;
    private final RoleRepository roleRepository;
    private final AlarmRepository alarmRepository;
    private final BlobEntityRepository blobEntityRepository;
    private final ReportTemplateInfoRepository reportTemplateInfoRepository;
    private final ReportRepository reportRepository;

    private final DefaultQueryLogComponent queryLog;

    public DefaultEntityQueryRepository(NamedParameterJdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate,
                                        AssetRepository assetRepository, CustomerRepository customerRepository,
                                        DeviceRepository deviceRepository, EntityViewRepository entityViewRepository,
                                        EdgeRepository edgeRepository,
                                        UserRepository userRepository, DashboardRepository dashboardRepository,
                                        EntityGroupRepository entityGroupRepository, SchedulerEventRepository schedulerEventRepository,
                                        RoleRepository roleRepository, AlarmRepository alarmRepository, BlobEntityRepository blobEntityRepository,
                                        ReportTemplateInfoRepository reportTemplateInfoRepository, ReportRepository reportRepository
            , DefaultQueryLogComponent queryLog) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.assetRepository = assetRepository;
        this.customerRepository = customerRepository;
        this.deviceRepository = deviceRepository;
        this.entityViewRepository = entityViewRepository;
        this.edgeRepository = edgeRepository;
        this.userRepository = userRepository;
        this.dashboardRepository = dashboardRepository;
        this.entityGroupRepository = entityGroupRepository;
        this.schedulerEventRepository = schedulerEventRepository;
        this.roleRepository = roleRepository;
        this.alarmRepository = alarmRepository;
        this.blobEntityRepository = blobEntityRepository;
        this.reportTemplateInfoRepository = reportTemplateInfoRepository;
        this.reportRepository = reportRepository;
        this.queryLog = queryLog;
    }

    @PostConstruct
    void validateNullsOrderStrategy() {
        if (!ACCEPTED_NULLS_ORDER_STRATEGIES.contains(nullsOrderStrategy)) {
            log.error("Invalid value '{}' for sql.entity_data_query_nulls_order_strategy. Accepted values are: {}. Falling back to '{}'.",
                    nullsOrderStrategy, ACCEPTED_NULLS_ORDER_STRATEGIES, NULLS_ORDER_DEFAULT);
            nullsOrderStrategy = NULLS_ORDER_DEFAULT;
        }
    }

    private String buildEntitiesFromClause(SqlQueryContext ctx, String selection, CharSequence entitiesQuery, String latestJoins, String aliasWhereQuery) {
        if (ctx.isPushdownEligible()) {
            // Co-located pushdown: inline the filtered anchor table as a derived "entities" relation (NOT a CTE,
            // which Citus would recursively plan/materialize on the coordinator and re-funnel). The latest-value
            // joins anchor directly on entities.id; because device/anchor is co-located with attribute_kv /
            // ts_kv_latest, the planner ships the join + sort + limit to the workers (distributed top-N).
            return String.format("from (select %s from (%s) entities %s) result %s",
                    selection, entitiesQuery, latestJoins, aliasWhereQuery);
        }
        if (ctx.isCitusEnabled()) {
            // On Citus, expose the filtered entity set as a CTE so the latest-value joins can reference it via
            // "entity_id in (select id from entities)" (see EntityKeyMapping#toLatestJoin). Citus materializes and
            // broadcasts the CTE once, letting each shard probe only the requested entities instead of a correlated
            // LATERAL join (which Citus rejects against a distributed table).
            return String.format("from (with entities as (%s) select %s from entities %s) result %s",
                    entitiesQuery, selection, latestJoins, aliasWhereQuery);
        }
        return String.format("from (select %s from (%s) entities %s) result %s",
                selection, entitiesQuery, latestJoins, aliasWhereQuery);
    }

    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityCountQuery query) {
        SqlQueryContext ctx = buildQueryContext(tenantId, customerId, userPermissions, query.getEntityFilter(), TenantId.SYS_TENANT_ID.equals(tenantId));
        MergedGroupTypePermissionInfo readPermissions = ctx.getSecurityCtx().getMergedReadPermissionsByEntityType();
        if (readPermissions == null) {
            return 0L;
        }
        if (query.getEntityFilter().getType().equals(EntityFilterType.STATE_ENTITY_OWNER)) {
            if (ctx.getEntityType() == EntityType.TENANT && !ctx.isTenantUser()) {
                return 0L;
            }
        } else if (query.getEntityFilter().getType().equals(EntityFilterType.RELATIONS_QUERY)) {
            if (hasNoPermissionsForAllRelationQueryResources(ctx.getSecurityCtx().getMergedReadEntityPermissionsMap())) {
                return 0L;
            }
        } else if (!readPermissions.isHasGenericRead() && readPermissions.getEntityGroupIds().isEmpty()) {
            return 0L;
        } else if (customerUserIsTryingToAccessTenantEntity(ctx, query.getEntityFilter())) {
            return 0L;
        }

        ComplexOperation operation = query.getKeyFiltersOperationOrDefault();
        boolean isOr = operation == ComplexOperation.OR;

        List<EntityKeyMapping> mappings = EntityKeyMapping.prepareEntityCountKeyMapping(query);

        List<EntityKeyMapping> selectionMapping = new ArrayList<>(mappings.stream().filter(EntityKeyMapping::isSelection)
                .collect(Collectors.toList()));

        List<EntityKeyMapping> filterMapping = mappings.stream().filter(EntityKeyMapping::hasFilter)
                .collect(Collectors.toList());
        List<EntityKeyMapping> entityFieldsFiltersMapping = filterMapping.stream().filter(mapping -> !mapping.isLatest() && mapping.getEntityKeyColumn() != null)
                .collect(Collectors.toList());

        // Under OR: entity field filter columns must be in inner SELECT for outer WHERE reference.
        // Mirror the ignore=true fix from findEntityDataByQuery so the inner subquery still emits the
        // extra column but downstream response shape (benign for count) stays symmetric with the data path.
        if (isOr) {
            for (EntityKeyMapping m : entityFieldsFiltersMapping) {
                if (!selectionMapping.contains(m)) {
                    m.setIgnore(true);
                    selectionMapping.add(m);
                }
            }
        }

        List<EntityKeyMapping> entityFieldsSelectionMapping = selectionMapping.stream().filter(mapping -> !mapping.isLatest())
                .collect(Collectors.toList());

        List<EntityKeyMapping> allLatestMappings = mappings.stream().filter(EntityKeyMapping::isLatest)
                .collect(Collectors.toList());

        // Under OR: entity field filters move to outer WHERE (not inner WHERE)
        List<EntityKeyMapping> innerEntityFieldsFilters = isOr ? Collections.emptyList() : entityFieldsFiltersMapping;

        String entityWhereClause = DefaultEntityQueryRepository.this.buildEntityWhere(ctx, query.getEntityFilter(), innerEntityFieldsFilters);

        // Under OR: combine ALL filter mappings (entity fields + aliases) with OR joiner into a middle-layer WHERE
        // (after the JOINs, where alias table references are visible), appended to latestJoinsCnt once the latest-value
        // joins are built below. Under AND: the filter stays at the outer result layer (existing behavior).
        String aliasWhereQuery;
        String orMiddleWhere = "";
        if (isOr) {
            String combinedFilterQuery = EntityKeyMapping.buildQuery(ctx, filterMapping, query.getEntityFilter().getType(), ComplexOperation.OR);
            if (!combinedFilterQuery.isEmpty()) {
                orMiddleWhere = " where (" + combinedFilterQuery + ")";
            }
            aliasWhereQuery = "";
        } else {
            aliasWhereQuery = DefaultEntityQueryRepository.this.buildAliasWhereQuery(ctx, query.getEntityFilter(), selectionMapping, "");
        }

        String entityFieldsSelection = EntityKeyMapping.buildSelections(entityFieldsSelectionMapping, query.getEntityFilter().getType(), ctx.getEntityType());
        String entityTypeStr;
        if (query.getEntityFilter().getType().equals(EntityFilterType.RELATIONS_QUERY)) {
            entityTypeStr = "e.entity_type";
        } else if (query.getEntityFilter().getType().equals(EntityFilterType.ENTITY_GROUP_NAME)) {
            entityTypeStr = "'ENTITY_GROUP'";
        } else {
            entityTypeStr = "'" + ctx.getEntityType().name() + "'";
        }
        if (!StringUtils.isEmpty(entityFieldsSelection)) {
            entityFieldsSelection = String.format("e.id id, %s entity_type, %s", entityTypeStr, entityFieldsSelection);
        } else {
            entityFieldsSelection = String.format("e.id id, %s entity_type", entityTypeStr);
        }

        StringBuilder entitiesQuery;
        switch (query.getEntityFilter().getType()) {
            case RELATIONS_QUERY:
                entitiesQuery = buildRelationsEntitiesQuery(query, ctx, entityWhereClause, entityFieldsSelection);
                break;
            case ENTITY_GROUP_NAME:
            case ENTITY_GROUP_LIST:
                entitiesQuery = buildGroupEntitiesQuery(query, ctx, readPermissions, entityWhereClause, entityFieldsSelection);
                break;
            case SINGLE_ENTITY:
                if (ctx.getSecurityCtx().isEntityGroup()) {
                    entitiesQuery = buildGroupEntitiesQuery(query, ctx, readPermissions, entityWhereClause, entityFieldsSelection);
                } else {
                    entitiesQuery = buildCommonEntitiesQuery(query, ctx, readPermissions, entityWhereClause, entityFieldsSelection);
                }
                break;
            default:
                entitiesQuery = buildCommonEntitiesQuery(query, ctx, readPermissions, entityWhereClause, entityFieldsSelection);
                break;
        }

        // Built after entitiesQuery so the pushdown latest-value joins can re-inline it (see EntityKeyMapping#toLatestJoin).
        // Under OR (isOr) the latest-value joins are forced LEFT and the combined filter is re-emitted at the middle
        // layer (orMiddleWhere), so it must be appended after the joins are assembled.
        String latestJoinsCnt = EntityKeyMapping.buildLatestJoins(ctx, query.getEntityFilter(), entitiesQuery, allLatestMappings, true, isOr) + orMiddleWhere;

        String fromClauseCount = DefaultEntityQueryRepository.this.buildEntitiesFromClause(ctx, "entities.*", entitiesQuery, latestJoinsCnt, aliasWhereQuery);

        String countQuery = String.format("select count(*) %s", fromClauseCount);

        return transactionTemplate.execute(status -> {
            long startTs = System.currentTimeMillis();
            try {
                return jdbcTemplate.queryForObject(countQuery, ctx, Long.class);
            } finally {
                queryLog.logQuery(ctx, countQuery, System.currentTimeMillis() - startTs);
            }
        });
    }

    @Override
    public PageData<EntityData> findEntityDataByQueryInternal(EntityDataQuery query) {
        return findEntityDataByQuery(null, null, null, query, true);
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityDataQuery query) {
        return findEntityDataByQuery(tenantId, customerId, userPermissions, query, false);
    }

    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityDataQuery query, boolean ignorePermissionCheck) {
        // Pushdown eligibility is set inside buildQueryContext so this path and countEntitiesByQuery stay in lockstep.
        SqlQueryContext ctx = buildQueryContext(tenantId, customerId, userPermissions, query.getEntityFilter(), ignorePermissionCheck);
        MergedGroupTypePermissionInfo readPermissions = ctx.getSecurityCtx().getMergedReadPermissionsByEntityType();
        if (readPermissions == null) {
            return new PageData<>();
        }
        if (query.getEntityFilter().getType().equals(EntityFilterType.STATE_ENTITY_OWNER)) {
            if (ctx.getEntityType() == EntityType.TENANT && !ctx.isTenantUser()) {
                return new PageData<>();
            }
        } else if (query.getEntityFilter().getType().equals(EntityFilterType.RELATIONS_QUERY)) {
            if (hasNoPermissionsForAllRelationQueryResources(ctx.getSecurityCtx().getMergedReadEntityPermissionsMap())) {
                return new PageData<>();
            }
        } else if (!readPermissions.isHasGenericRead() && readPermissions.getEntityGroupIds().isEmpty()) {
            return new PageData<>();
        } else if (customerUserIsTryingToAccessTenantEntity(ctx, query.getEntityFilter())) {
            return new PageData<>();
        }
        return transactionTemplate.execute(status -> {
            EntityDataPageLink pageLink = query.getPageLink();
            // Assemble the count + data SQL via the shared builder so the SQL executed here is the SAME SQL a test
            // can obtain without executing it (see buildEntityDataSql). This keeps the generator the single source of
            // truth for the emitted query shapes (plain / Citus-CTE / Citus-pushdown) instead of having them
            // hand-transcribed in a test.
            EntityDataSql sql = buildEntityDataSql(ctx, query, readPermissions);

            long startTs = System.currentTimeMillis();
            int totalElements;
            try {
                totalElements = jdbcTemplate.queryForObject(sql.countQuery(), ctx, Integer.class);
            } finally {
                queryLog.logQuery(ctx, sql.countQuery(), System.currentTimeMillis() - startTs);
            }

            if (totalElements == 0) {
                return new PageData<>();
            }
            String dataQuery = sql.dataQuery();
            startTs = System.currentTimeMillis();
            List<Map<String, Object>> rows;
            try {
                rows = jdbcTemplate.queryForList(dataQuery, ctx);
            } finally {
                queryLog.logQuery(ctx, dataQuery, System.currentTimeMillis() - startTs);
            }
            return EntityDataAdapter.createEntityData(pageLink, sql.selectionMapping(), rows, totalElements);
        });
    }

    /**
     * Holds the count + data SQL and the selection mapping produced for an {@link EntityDataQuery}. The SQL strings
     * are exactly what {@link #findEntityDataByQuery} executes; the mapping is needed both to assemble the data SQL
     * ordering and to adapt the result rows.
     */
    public record EntityDataSql(String countQuery, String dataQuery, List<EntityKeyMapping> selectionMapping) {
    }

    /**
     * Builds the count and data SQL for an entity-data query WITHOUT executing it. This is the single place where the
     * emitted query shape is assembled, so the SQL strings returned here are byte-for-byte what
     * {@link #findEntityDataByQuery} runs (it was extracted verbatim from that method, which now calls this builder).
     * <p>
     * Public (the parity test lives in the application module) so the Citus pushdown-parity integration test can drive the REAL generator (capturing the
     * pushdown vs. non-pushdown forms by toggling {@code ctx.pushdownEligible}) and run/compare the produced SQL on a
     * live cluster, instead of transcribing the SQL templates by hand (which can silently drift from this generator).
     */
    public EntityDataSql buildEntityDataSql(SqlQueryContext ctx, EntityDataQuery query, MergedGroupTypePermissionInfo readPermissions) {
        EntityDataPageLink pageLink = query.getPageLink();

        List<EntityKeyMapping> mappings = EntityKeyMapping.prepareKeyMapping(ctx.getEntityType(), query);

        boolean isOr = query.getKeyFiltersOperationOrDefault() == ComplexOperation.OR;

        List<EntityKeyMapping> selectionMapping = new ArrayList<>(mappings.stream().filter(EntityKeyMapping::isSelection)
                .collect(Collectors.toList()));
        List<EntityKeyMapping> latestSelectionMapping = selectionMapping.stream().filter(EntityKeyMapping::isLatest)
                .collect(Collectors.toList());

        List<EntityKeyMapping> filterMapping = mappings.stream().filter(EntityKeyMapping::hasFilter)
                .collect(Collectors.toList());
        List<EntityKeyMapping> entityFieldsFiltersMapping = filterMapping.stream().filter(mapping -> !mapping.isLatest() && mapping.getEntityKeyColumn() != null)
                .collect(Collectors.toList());

        // Under OR: entity field filter columns must be in inner SELECT for outer WHERE reference.
        // Mark force-added filter-only mappings as ignored so EntityDataAdapter does not expose
        // them in EntityData.latest — keeps the response shape identical to AND.
        if (isOr) {
            for (EntityKeyMapping m : entityFieldsFiltersMapping) {
                if (!selectionMapping.contains(m)) {
                    m.setIgnore(true);
                    selectionMapping.add(m);
                }
            }
        }

        List<EntityKeyMapping> entityFieldsSelectionMapping = selectionMapping.stream().filter(mapping -> !mapping.isLatest())
                .collect(Collectors.toList());

        List<EntityKeyMapping> allLatestMappings = mappings.stream().filter(EntityKeyMapping::isLatest)
                .collect(Collectors.toList());

        // Under OR: entity field filters move to outer WHERE (not inner WHERE)
        List<EntityKeyMapping> innerEntityFieldsFilters = isOr ? Collections.emptyList() : entityFieldsFiltersMapping;

        String entityWhereClause = DefaultEntityQueryRepository.this.buildEntityWhere(ctx, query.getEntityFilter(), innerEntityFieldsFilters);

        // Under OR: combine ALL filter mappings (entity fields + aliases) plus the text search with the OR joiner into a
        // middle-layer WHERE (after the JOINs, where alias table references are visible), appended to the latest-value
        // joins once they are built below. Under AND: the filter and text search stay at the outer result layer.
        String aliasWhereQuery;
        String orMiddleWhere = "";
        if (isOr) {
            String combinedFilterQuery = EntityKeyMapping.buildQuery(ctx, filterMapping, query.getEntityFilter().getType(), ComplexOperation.OR);
            if (!combinedFilterQuery.isEmpty()) {
                orMiddleWhere = " where (" + combinedFilterQuery + ")";
            }
            String searchTextQuery = buildTextSearchQuery(ctx, selectionMapping, pageLink.getTextSearch());
            if (!searchTextQuery.isEmpty()) {
                orMiddleWhere += (orMiddleWhere.isEmpty() ? " where " : " and ") + "(" + searchTextQuery + ") ";
            }
            aliasWhereQuery = "";
        } else {
            aliasWhereQuery = DefaultEntityQueryRepository.this.buildAliasWhereQuery(ctx, query.getEntityFilter(), selectionMapping, pageLink.getTextSearch());
        }

        String entityFieldsSelection = EntityKeyMapping.buildSelections(entityFieldsSelectionMapping, query.getEntityFilter().getType(), ctx.getEntityType());
        String entityTypeStr;
        if (query.getEntityFilter().getType().equals(EntityFilterType.RELATIONS_QUERY)) {
            entityTypeStr = "e.entity_type";
        } else if (query.getEntityFilter().getType().equals(EntityFilterType.ENTITY_GROUP_NAME)) {
            entityTypeStr = "'ENTITY_GROUP'";
        } else {
            entityTypeStr = "'" + ctx.getEntityType().name() + "'";
        }

        if (!StringUtils.isEmpty(entityFieldsSelection)) {
            entityFieldsSelection = String.format("e.id id, %s entity_type, %s", entityTypeStr, entityFieldsSelection);
        } else {
            entityFieldsSelection = String.format("e.id id, %s entity_type", entityTypeStr);
        }
        String latestSelection = EntityKeyMapping.buildSelections(latestSelectionMapping, query.getEntityFilter().getType(), ctx.getEntityType());
        String topSelection = "entities.*";
        if (!StringUtils.isEmpty(latestSelection)) {
            topSelection = topSelection + ", " + latestSelection;
        }

        StringBuilder entitiesQuery;
        switch (query.getEntityFilter().getType()) {
            case RELATIONS_QUERY:
                entitiesQuery = buildRelationsEntitiesQuery(query, ctx, entityWhereClause, entityFieldsSelection);
                break;
            case ENTITY_GROUP_NAME:
            case ENTITY_GROUP_LIST:
                entitiesQuery = buildGroupEntitiesQuery(query, ctx, readPermissions, entityWhereClause, entityFieldsSelection);
                break;
            case SINGLE_ENTITY:
                if (ctx.getSecurityCtx().isEntityGroup()) {
                    entitiesQuery = buildGroupEntitiesQuery(query, ctx, readPermissions, entityWhereClause, entityFieldsSelection);
                } else {
                    entitiesQuery = buildCommonEntitiesQuery(query, ctx, readPermissions, entityWhereClause, entityFieldsSelection);
                }
                break;
            default:
                entitiesQuery = buildCommonEntitiesQuery(query, ctx, readPermissions, entityWhereClause, entityFieldsSelection);
                break;
        }

        // Built after entitiesQuery so the pushdown latest-value joins can re-inline it (see EntityKeyMapping#toLatestJoin).
        // Under OR (isOr) the latest-value joins are forced LEFT and the combined filter + text search are re-emitted at
        // the middle layer (orMiddleWhere), so it is appended after the joins are assembled.
        String latestJoinsCnt = EntityKeyMapping.buildLatestJoins(ctx, query.getEntityFilter(), entitiesQuery, allLatestMappings, true, isOr) + orMiddleWhere;
        String latestJoinsData = EntityKeyMapping.buildLatestJoins(ctx, query.getEntityFilter(), entitiesQuery, allLatestMappings, false, isOr) + orMiddleWhere;

        String fromClauseCount = DefaultEntityQueryRepository.this.buildEntitiesFromClause(ctx, "entities.*", entitiesQuery, latestJoinsCnt, aliasWhereQuery);

        String fromClauseData = DefaultEntityQueryRepository.this.buildEntitiesFromClause(ctx, topSelection, entitiesQuery, latestJoinsData, aliasWhereQuery);

        if (!StringUtils.isEmpty(pageLink.getTextSearch())) {
            //Unfortunately, we need to sacrifice performance in case of full text search, because it is applied to all joined records.
            fromClauseCount = fromClauseData;
        }

        String countQuery = String.format("select count(*) %s", fromClauseCount);

        String dataQuery = String.format("select * %s", fromClauseData);

        EntityDataSortOrder sortOrder = pageLink.getSortOrder();
        if (sortOrder != null) {
            Optional<EntityKeyMapping> sortOrderMappingOpt = mappings.stream().filter(EntityKeyMapping::isSortOrder).findFirst();
            if (sortOrderMappingOpt.isPresent()) {
                EntityKeyMapping sortOrderMapping = sortOrderMappingOpt.get();
                String direction = sortOrder.getDirection() == EntityDataSortOrder.Direction.ASC ? "asc" : "desc";
                String nullsOrder = resolveNullsOrder();
                if (sortOrderMapping.getEntityKey().getType() == EntityKeyType.ENTITY_FIELD) {
                    dataQuery = String.format("%s order by %s %s%s, result.id %s", dataQuery, sortOrderMapping.getValueAlias(), direction, nullsOrder, direction);
                } else {
                    dataQuery = String.format("%s order by %s %s%s, %s %s, result.id %s", dataQuery,
                            sortOrderMapping.getSortOrderNumAlias(), direction, nullsOrder, sortOrderMapping.getSortOrderStrAlias(), direction, direction);
                }
            }
        }
        int startIndex = pageLink.getPageSize() * pageLink.getPage();
        if (pageLink.getPageSize() > 0) {
            dataQuery = String.format("%s limit %s offset %s", dataQuery, pageLink.getPageSize(), startIndex);
        }
        return new EntityDataSql(countQuery, dataQuery, selectionMapping);
    }

    private boolean customerUserIsTryingToAccessTenantEntity(SqlQueryContext ctx, EntityFilter entityFilter) {
        if (ctx.isTenantUser()) {
            return false;
        } else {
            switch (entityFilter.getType()) {
                case SINGLE_ENTITY:
                    SingleEntityFilter seFilter = (SingleEntityFilter) entityFilter;
                    return isSystemOrTenantEntity(seFilter.getSingleEntity().getEntityType());
                case ENTITY_LIST:
                    EntityListFilter elFilter = (EntityListFilter) entityFilter;
                    return isSystemOrTenantEntity(elFilter.getEntityType());
                case ENTITY_NAME:
                    EntityNameFilter enFilter = (EntityNameFilter) entityFilter;
                    return isSystemOrTenantEntity(enFilter.getEntityType());
                case ENTITY_TYPE:
                    EntityTypeFilter etFilter = (EntityTypeFilter) entityFilter;
                    return isSystemOrTenantEntity(etFilter.getEntityType());
                default:
                    return false;
            }
        }
    }

    private boolean isSystemOrTenantEntity(EntityType entityType) {
        switch (entityType) {
            case INTEGRATION:
            case CONVERTER:
            case DEVICE_PROFILE:
            case ASSET_PROFILE:
            case RULE_CHAIN:
            case TENANT:
            case TENANT_PROFILE:
            case WIDGET_TYPE:
            case WIDGETS_BUNDLE:
                return true;
            default:
                return false;
        }
    }

    private SqlQueryContext buildQueryContext(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityFilter filter, boolean ignorePermissionCheck) {
        QueryContext securityContext;
        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            securityContext = new QueryContext(tenantId, customerId, resolveEntityType(filter), SYS_ADMIN_PERMISSIONS, filter, ignorePermissionCheck);
        } else {
            switch (filter.getType()) {
                case STATE_ENTITY_OWNER:
                    EntityId ownerId = getOwnerId(tenantId, filter);
                    securityContext = new QueryContext(tenantId, customerId, ownerId.getEntityType(), userPermissions, filter, ownerId, ignorePermissionCheck);
                    break;
                case SINGLE_ENTITY:
                    SingleEntityFilter seFilter = (SingleEntityFilter) filter;
                    EntityId entityId = seFilter.getSingleEntity();
                    if (entityId != null && entityId.getEntityType().equals(EntityType.ENTITY_GROUP)) {
                        EntityGroupEntity entityGroupEntity = getEntityGroup(tenantId, entityId);
                        if (entityGroupEntity != null) {
                            securityContext = new QueryContext(tenantId, customerId, EntityType.ENTITY_GROUP, userPermissions, filter, entityGroupEntity.getType(), ignorePermissionCheck);
                        } else {
                            securityContext = new QueryContext(tenantId, customerId, resolveEntityType(filter), userPermissions, filter, ignorePermissionCheck);
                        }
                    } else {
                        securityContext = new QueryContext(tenantId, customerId, resolveEntityType(filter), userPermissions, filter, ignorePermissionCheck);
                    }
                    break;
                default:
                    securityContext = new QueryContext(tenantId, customerId, resolveEntityType(filter), userPermissions, filter, ignorePermissionCheck);
            }
        }
        SqlQueryContext ctx = new SqlQueryContext(securityContext, citusEnabled);
        // Compute pushdown eligibility here so BOTH entry points (findEntityDataByQuery and countEntitiesByQuery)
        // share the exact same eligibility — they build their ctx via this single helper. Previously only the data
        // path set it, so the count path silently used the CTE FROM-clause shape while the data path used the
        // co-located pushdown shape for the same filter. Pushdown only changes the FROM-clause plan, not the row
        // set, so the returned count is unaffected; keeping them in lockstep avoids plan divergence between the two.
        boolean pushdown = ctx.isCitusEnabled()
                && isSingleEntityTableFilter(filter)
                && !(filter.getType() == EntityFilterType.SINGLE_ENTITY && ctx.getSecurityCtx().isEntityGroup())
                && isPushdownAnchor(resolveEntityType(filter));
        ctx.setPushdownEligible(pushdown);
        return ctx;
    }

    /**
     * True when the resolved anchor entity type maps to a table that exists on every worker (distributed or reference),
     * so the co-located latest-value pushdown join is legal. Coordinator-local anchors — the partitioned
     * {@code blob_entity}/{@code report}/{@code alarm_comment} tables (and any non-Citus-managed table) — return false:
     * for these a SINGLE_ENTITY/ENTITY_LIST/ENTITY_NAME/ENTITY_TYPE query would otherwise attempt a
     * coordinator-local ⋈ distributed (attribute_kv / ts_kv_latest) pushdown join that Citus rejects/misplans, so they
     * must keep the coordinator-side CTE join form. Only invoked once {@link #isSingleEntityTableFilter} has passed, so
     * {@link #resolveEntityType} is always defined for the filter here.
     */
    private static boolean isPushdownAnchor(EntityType entityType) {
        String table = entityTableMap.get(entityType);
        return table != null && CitusTables.isDistributedOrReference(table);
    }

    private EntityGroupEntity getEntityGroup(TenantId tenantId, EntityId entityGroupId) {
        return entityGroupRepository.findById(entityGroupId.getId()).orElse(null);
    }

    private EntityId getOwnerId(TenantId tenantId, EntityFilter queryFilter) {
        StateEntityOwnerFilter filter = (StateEntityOwnerFilter) queryFilter;
        EntityId stateEntityId = filter.getSingleEntity();
        switch (stateEntityId.getEntityType()) {
            case TENANT:
            case RULE_CHAIN:
            case RULE_NODE:
            case INTEGRATION:
            case CONVERTER:
            case WIDGETS_BUNDLE:
            case WIDGET_TYPE:
            case GROUP_PERMISSION:
                return tenantId;
            case ASSET:
                AssetEntity assetEntity = assetRepository.findById(stateEntityId.getId()).orElse(null);
                if (assetEntity != null) {
                    return getOwnerId(assetEntity.getTenantId(), assetEntity.getCustomerId());
                }
                break;
            case CUSTOMER:
                CustomerEntity customerEntity = customerRepository.findById(stateEntityId.getId()).orElse(null);
                if (customerEntity != null) {
                    return getOwnerId(customerEntity.getTenantId(), customerEntity.getParentCustomerId());
                }
                break;
            case DEVICE:
                DeviceEntity deviceEntity = deviceRepository.findById(stateEntityId.getId()).orElse(null);
                if (deviceEntity != null) {
                    return getOwnerId(deviceEntity.getTenantId(), deviceEntity.getCustomerId());
                }
                break;
            case ENTITY_VIEW:
                EntityViewEntity entityView = entityViewRepository.findById(stateEntityId.getId()).orElse(null);
                if (entityView != null) {
                    return getOwnerId(entityView.getTenantId(), entityView.getCustomerId());
                }
                break;
            case EDGE:
                EdgeEntity edgeEntity = edgeRepository.findById(stateEntityId.getId()).orElse(null);
                if (edgeEntity != null) {
                    return getOwnerId(edgeEntity.getTenantId(), edgeEntity.getCustomerId());
                }
                break;
            case USER:
                UserEntity userEntity = userRepository.findById(stateEntityId.getId()).orElse(null);
                if (userEntity != null) {
                    return getOwnerId(userEntity.getTenantId(), userEntity.getCustomerId());
                }
                break;
            case DASHBOARD:
                DashboardEntity dashboardEntity = dashboardRepository.findById(stateEntityId.getId()).orElse(null);
                if (dashboardEntity != null) {
                    return getOwnerId(dashboardEntity.getTenantId(), dashboardEntity.getCustomerId());
                }
                break;
            case ENTITY_GROUP:
                EntityGroupEntity egEntity = entityGroupRepository.findById(stateEntityId.getId()).orElse(null);
                return EntityIdFactory.getByTypeAndUuid(egEntity.getOwnerType(), egEntity.getOwnerId());
            case SCHEDULER_EVENT:
                SchedulerEventEntity seEntity = schedulerEventRepository.findById(stateEntityId.getId()).orElse(null);
                if (seEntity != null) {
                    return getOwnerId(seEntity.getTenantId(), seEntity.getCustomerId());
                }
                break;
            case ROLE:
                RoleEntity rEntity = roleRepository.findById(stateEntityId.getId()).orElse(null);
                if (rEntity != null) {
                    return getOwnerId(rEntity.getTenantId(), rEntity.getCustomerId());
                }
                break;
            case ALARM:
                AlarmEntity aEntity = alarmRepository.findById(stateEntityId.getId()).orElse(null);
                if (aEntity != null) {
                    StateEntityOwnerFilter newFilter = new StateEntityOwnerFilter();
                    newFilter.setSingleEntity(AliasEntityId.fromEntityId(EntityIdFactory.getByTypeAndUuid(aEntity.getOriginatorType(), aEntity.getOriginatorId())));
                    return getOwnerId(tenantId, newFilter);
                }
                break;
            case BLOB_ENTITY:
                BlobEntityEntity bEntity = blobEntityRepository.findById(stateEntityId.getId()).orElse(null);
                if (bEntity != null) {
                    return getOwnerId(bEntity.getTenantId(), bEntity.getCustomerId());
                }
                break;
            case REPORT_TEMPLATE:
                ReportTemplateInfoEntity rtiEntity = reportTemplateInfoRepository.findById(stateEntityId.getId()).orElse(null);
                if (rtiEntity != null) {
                    return getOwnerId(rtiEntity.getTenantId(), rtiEntity.getCustomerId());
                }
                break;
            case REPORT:
                ReportEntity riEntity = reportRepository.findById(stateEntityId.getId()).orElse(null);
                if (riEntity != null) {
                    return getOwnerId(riEntity.getTenantId(), riEntity.getCustomerId());
                }
                break;
        }
        return tenantId;
    }

    private EntityId getOwnerId(UUID tenantId, UUID customerId) {
        if (customerId == null || customerId.equals(CustomerId.NULL_UUID)) {
            return new TenantId(tenantId);
        } else {
            return new CustomerId(customerId);
        }
    }

    @Override
    public <T> PageData<T> findInCustomerHierarchyByRootCustomerIdOrOtherGroupIdsAndType(
            TenantId tenantId, CustomerId customerId, EntityType entityType, String type,
            List<EntityGroupId> groupIds, PageLink pageLink, EntityMapping<?, T> mapping, boolean mobile) {
        return transactionTemplate.execute(status -> {
            SqlQueryContext ctx = new SqlQueryContext(new QueryContext(tenantId, customerId, entityType, null, null), citusEnabled);
            StringBuilder fromClause = new StringBuilder();

            fromClause.append("FROM ");
            fromClause.append(entityTableMap.get(ctx.getEntityType()));
            fromClause.append(" as e");
            fromClause.append(" WHERE ");

            boolean customerIdSet = customerId != null && !customerId.isNullUid();
            boolean typeSet = type != null && !type.trim().isEmpty();
            boolean groupIdsSet = groupIds != null && !groupIds.isEmpty();
            ctx.addUuidParameter("permissions_tenant_id", ctx.getTenantId().getId());
            fromClause.append(" e.tenant_id = :permissions_tenant_id ");

            if (customerIdSet) {
                ctx.addUuidParameter("permissions_customer_id", ctx.getCustomerId().getId());
                fromClause.append(" AND ");
                if (groupIdsSet) {
                    fromClause.append("(");
                }
                if (entityType.equals(EntityType.CUSTOMER)) {
                    fromClause.append(" e.parent_customer_id in ").append(subCustomersInClause(ctx));
                } else {
                    fromClause.append(" e.customer_id in ").append(subCustomersInClause(ctx));
                }
                if (groupIdsSet) {
                    ctx.addUuidListParameter("group_ids", groupIds.stream().map(EntityGroupId::getId).collect(Collectors.toList()));
                    fromClause.append(" OR e.id in ");
                    fromClause.append(" ( SELECT rattr.to_id FROM relation rattr WHERE rattr.to_id = e.id AND rattr.to_type = '");
                    fromClause.append(entityType.name());
                    fromClause.append("' and rattr.from_id in (:").append("group_ids").append(") AND rattr.from_type = 'ENTITY_GROUP' " +
                            "AND rattr.relation_type_group = 'FROM_ENTITY_GROUP' AND rattr.relation_type = 'Contains')");
                    fromClause.append(")");
                }
            } else if (groupIdsSet) {
                fromClause.append(" AND ");
                ctx.addUuidListParameter("group_ids", groupIds.stream().map(EntityGroupId::getId).collect(Collectors.toList()));
                fromClause.append("e.id in ");
                fromClause.append("( SELECT rattr.to_id FROM relation rattr WHERE rattr.to_id = e.id AND rattr.to_type = '");
                fromClause.append(entityType.name());
                fromClause.append("' and rattr.from_id in (:").append("group_ids").append(") AND rattr.from_type = 'ENTITY_GROUP' " +
                        "AND rattr.relation_type_group = 'FROM_ENTITY_GROUP' AND rattr.relation_type = 'Contains')");
                fromClause.append(")");
            }
            if (typeSet) {
                fromClause.append(" AND ");
                ctx.addStringParameter("type", type);
                fromClause.append(" e.type = :type ");
            }

            if (mobile) {
                if (EntityType.DASHBOARD.equals(entityType)) {
                    fromClause.append(" AND ");
                    ctx.addBooleanParameter("mobileHide", false);
                    fromClause.append(" e.mobile_hide = :mobileHide ");
                }
            }

            if (!StringUtils.isEmpty(pageLink.getTextSearch())) {
                ctx.addStringParameter("textSearch", "%" + pageLink.getTextSearch() + "%");
                fromClause.append(" AND e.").append(getNameColumn(entityType)).append(" ILIKE :textSearch");
            }

            int totalElements = jdbcTemplate.queryForObject(String.format("select count(*) %s", fromClause), ctx, Integer.class);

            String dataQuery = "SELECT " + mapping.getMappings().keySet().stream()
                    .map(field -> "e." + field)
                    .collect(Collectors.joining(", ")) + " " + fromClause;

            SortOrder sortOrder = pageLink.getSortOrder();
            if (mobile) {
                if (EntityType.DASHBOARD.equals(entityType)) {
                    dataQuery = String.format("%s order by %s asc NULLS LAST", dataQuery, "mobile_order");
                    if (sortOrder != null) {
                        String directionStr;
                        if (sortOrder.getDirection() == SortOrder.Direction.ASC) {
                            directionStr = "asc";
                        } else {
                            directionStr = "desc";
                        }
                        dataQuery = String.format("%s, %s %s", dataQuery, EntityKeyMapping.getEntityFieldColumnName(sortOrder.getProperty()), directionStr);
                    }
                }
            } else if (sortOrder != null) {
                dataQuery = String.format("%s order by %s", dataQuery, EntityKeyMapping.getEntityFieldColumnName(sortOrder.getProperty()));
                if (sortOrder.getDirection() == SortOrder.Direction.ASC) {
                    dataQuery += " asc";
                } else {
                    dataQuery += " desc";
                }
            }
            int startIndex = pageLink.getPageSize() * pageLink.getPage();
            if (pageLink.getPageSize() > 0) {
                dataQuery = String.format("%s limit %s offset %s", dataQuery, pageLink.getPageSize(), startIndex);
            }
            //TODO 3.1: remove this before release
            if (log.isTraceEnabled()) {
                log.trace("QUERY: {}", dataQuery);
            }
            if (log.isTraceEnabled()) {
                Arrays.asList(ctx.getParameterNames()).forEach(param -> log.trace("QUERY PARAM: {}->{}", param, ctx.getValue(param)));
            }
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(dataQuery, ctx);

            int totalPages = pageLink.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageLink.getPageSize()) : 1;
            boolean hasNext = pageLink.getPageSize() > 0 && totalElements > startIndex + rows.size();
            List<T> entitiesData = rows.stream().map(mapping::map).collect(Collectors.toList());
            return new PageData<>(entitiesData, totalPages, totalElements, hasNext);
        });
    }

    /**
     * Returns the SQL fragment used after {@code <column> in } to scope to the sub-customers subtree.
     * <p>
     * In plain Postgres mode this is the embedded {@link #HIERARCHICAL_SUB_CUSTOMERS_QUERY} recursive subquery,
     * byte-for-byte unchanged. Under Citus the enclosing entity-data query is distributed, and Citus rejects a
     * {@code WITH RECURSIVE} subquery embedded in a distributed query. Since the recursion is entirely over the
     * {@code customer} reference table, we pre-resolve it standalone on the coordinator (the params it references —
     * {@code permissions_tenant_id} / {@code permissions_customer_id} — are already bound on {@code ctx} at every
     * call site) and splice a {@code (select unnest(:resolvedIds))} subquery in its place. An empty result renders as
     * {@code (null)}, matching the "no rows" semantics of the recursive query returning nothing.
     */
    private String subCustomersInClause(SqlQueryContext ctx) {
        if (!ctx.isCitusEnabled()) {
            return HIERARCHICAL_SUB_CUSTOMERS_QUERY;
        }
        return resolveReferenceRecursion(ctx, HIERARCHICAL_SUB_CUSTOMERS_QUERY, "resolved_sub_customers");
    }

    /**
     * Returns the {@code entity_group}-ids subquery scoped to the sub-customers subtree.
     * <p>
     * Plain mode returns {@link #HIERARCHICAL_GROUPS_QUERY} (which embeds the customer recursion) unchanged. Under
     * Citus the inner customer recursion is pre-resolved (see {@link #subCustomersInClause}); the resulting plain
     * {@code select id from entity_group where owner_id in (select unnest(:resolvedIds))} over the
     * {@code entity_group} reference table is acceptable inside the distributed query.
     */
    private String groupsByOwnerQuery(SqlQueryContext ctx) {
        if (!ctx.isCitusEnabled()) {
            return HIERARCHICAL_GROUPS_QUERY;
        }
        return "select id from entity_group where owner_id in " + subCustomersInClause(ctx);
    }

    /**
     * Executes a recursive (or otherwise reference-table-only) id-producing query standalone on the coordinator
     * using the parameters already bound on {@code ctx}, binds the resulting ids as a single typed {@code uuid[]}
     * array parameter, and returns a {@code (select unnest(:param))} fragment that can be spliced in place of the
     * original subquery (every call site splices it after {@code in }). The query must select exactly one uuid column.
     * <p>
     * A single array bind (rather than a {@code (:param)} in-list) is required because pgjdbc expands an in-list to one
     * placeholder per element and is hard-capped at 32767 total binds — a large resolved set would overflow it — while
     * an array is a single placeholder regardless of size. {@code unnest} of a bound array is the same construct
     * {@link #resolveRelationTraversal} relies on and stays legal inside the distributed (Citus) query.
     * <p>
     * When the recursion resolves to no ids we return the literal {@code (null)} rather than binding a zero-length
     * array: {@code in (null)} is valid and matches no rows — the same "no rows" semantics the original subquery would
     * have.
     * <p>
     * The resolution is memoized on {@code ctx} keyed by the recursive SQL text: one query build can request the
     * same recursion many times (the sub-customers subtree alone is spliced into permission checks, group-type
     * arms, etc.), and the result depends only on ctx params that are fixed for the whole build (rebinding a param
     * to a different value throws), so repeated calls reuse the fragment resolved by the first one.
     */
    private String resolveReferenceRecursion(SqlQueryContext ctx, String recursiveSql, String paramPrefix) {
        String cached = ctx.getResolvedRecursions().get(recursiveSql);
        if (cached != null) {
            return cached;
        }
        // INVARIANT (Citus bind-before-build): unlike plain mode, which defers execution until the whole statement
        // runs, this fragment EXECUTES queries at SQL-construction time. Every ctx param that recursiveSql reads
        // (permissions_tenant_id, permissions_customer_id, and any relation-root params for the recursion) MUST
        // already be bound on ctx before this method is invoked, otherwise jdbcTemplate.queryForList throws here at
        // build time (missing named parameter) rather than at query execution. Callers must bind those params first.
        List<UUID> ids = jdbcTemplate.queryForList(recursiveSql, ctx, UUID.class);
        checkResolvedEntityCap(ids.size());
        String fragment;
        if (ids.isEmpty()) {
            // Keep the "(null)" fragment for the empty case: it is drop-in valid after "in " (every call site appends
            // this fragment there), "in (null)" matches no rows, and it avoids binding a zero-length array. This is
            // byte-for-byte the same empty-case fragment as before.
            fragment = "(null)";
        } else {
            // Bind the pre-resolved ids as a SINGLE typed uuid[] parameter and splice a "(select unnest(:param))"
            // subquery rather than an "(:param)" in-list. pgjdbc expands an in-list bind to ONE placeholder per element
            // and is hard-capped at 32767 total bind parameters, so a large resolved set (e.g. a deep
            // DEVICE_SEARCH_QUERY subtree) would overflow that limit and fail outright; a single array bind is one
            // placeholder regardless of element count. The "(select unnest(:uuid[]))" form is drop-in valid after
            // "in " at every call site and is the same reference-table-safe construct resolveRelationTraversal binds —
            // Citus accepts unnest of a bound array where these fragments land, so distributed-query pushdown legality
            // is preserved.
            DataSource dataSource = jdbcTemplate.getJdbcTemplate().getDataSource();
            Connection connection = DataSourceUtils.getConnection(dataSource);
            String param;
            try {
                param = bindArray(ctx, connection, paramPrefix, "uuid", ids.toArray(new UUID[0]));
            } finally {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
            fragment = "(select unnest(" + param + "))";
        }
        ctx.getResolvedRecursions().put(recursiveSql, fragment);
        return fragment;
    }

    /**
     * Citus-only guard against a single relation/reference recursion materializing an unbounded set on the coordinator
     * heap (see {@link #resolveRelationTraversal} / {@link #resolveReferenceRecursion}). On breach the query fails fast,
     * naming the {@code database.citus.relation_query.max_resolved_entities} property and the offending count so an
     * operator can raise the cap or narrow the query. Typed as {@link IncorrectParameterException} so the REST layer
     * maps the breach to a client error with the message intact instead of an unclassified 500.
     */
    private void checkResolvedEntityCap(int resolvedCount) {
        if (resolvedCount > maxResolvedEntities) {
            throw new IncorrectParameterException("Citus relation query resolved " + resolvedCount + " entities on the coordinator, "
                    + "exceeding the configured limit of " + maxResolvedEntities
                    + " (database.citus.relation_query.max_resolved_entities). Narrow the query or raise the limit.");
        }
    }

    private StringBuilder buildRelationsEntitiesQuery(EntityCountQuery query, SqlQueryContext ctx, String entityWhereClause, String entityFieldsSelection) {
        StringBuilder entitiesQuery = new StringBuilder();

        Map<Resource, MergedGroupTypePermissionInfo> readPermMap = ctx.getSecurityCtx().getMergedReadEntityPermissionsMap();
        Map<Resource, MergedGroupTypePermissionInfo> readAttrPermMap = ctx.getSecurityCtx().getMergedReadAttrPermissionsMap();
        Map<Resource, MergedGroupTypePermissionInfo> readTsPermMap = ctx.getSecurityCtx().getMergedReadTsPermissionsMap();
        entitiesQuery.append("select ").append(entityFieldsSelection);

        buildPermissionsSelect(ctx, entitiesQuery, readAttrPermMap, "permissions_read_attr_group_ids", ATTR_READ_FLAG);
        buildPermissionsSelect(ctx, entitiesQuery, readTsPermMap, "permissions_read_ts_group_ids", TS_READ_FLAG);

        entitiesQuery.append(" FROM ")
                .append(addEntityTableQuery(ctx, query.getEntityFilter()))
                .append(" e WHERE ");

        ctx.addUuidParameter("permissions_tenant_id", ctx.getTenantId().getId());
        ctx.addUuidParameter("permissions_customer_id", ctx.getCustomerId().getId());

        if (hasGenericForAllRelationQueryResources(readPermMap) && noGroupPermissionsForAllRelationQueryResources(readPermMap)) {
            entitiesQuery.append(" e.tenant_id =:permissions_tenant_id ");
            if (!ctx.isTenantUser()) {
                entitiesQuery.append(" AND (e.customer_id =:permissions_customer_id OR e.customer_id IN " + subCustomersInClause(ctx) + ") ");
            }
            if (!entityWhereClause.isEmpty()) {
                entitiesQuery.append(" AND ").append(entityWhereClause);
            }
        } else {
            entitiesQuery.append(" e.tenant_id =:permissions_tenant_id AND ");
            entitiesQuery.append("(");
            // Entity is Tenant;
            addTenantEntityCheck(ctx, entitiesQuery, readPermMap, EntityType.TENANT);
            entitiesQuery.append(" OR ");
            addTenantEntityCheck(ctx, entitiesQuery, readPermMap, EntityType.INTEGRATION);
            entitiesQuery.append(" OR ");
            addTenantEntityCheck(ctx, entitiesQuery, readPermMap, EntityType.CONVERTER);
            entitiesQuery.append(" OR ");
            addTenantEntityCheck(ctx, entitiesQuery, readPermMap, EntityType.AI_MODEL);
            entitiesQuery.append(" OR ");
            addCustomerEntityCheck(ctx, entitiesQuery, readPermMap, EntityType.ROLE);
            entitiesQuery.append(" OR ");
            addCustomerEntityCheck(ctx, entitiesQuery, readPermMap, EntityType.BLOB_ENTITY);
            entitiesQuery.append(" OR ");
            addCustomerEntityCheck(ctx, entitiesQuery, readPermMap, EntityType.SCHEDULER_EVENT);
            entitiesQuery.append(" OR ");
            addCustomerEntityCheck(ctx, entitiesQuery, readPermMap, EntityType.REPORT_TEMPLATE);
            entitiesQuery.append(" OR ");
            addCustomerEntityCheck(ctx, entitiesQuery, readPermMap, EntityType.REPORT);
            // Entity is one of group entities;
            for (EntityType entityType : EntityGroup.groupTypes) {
                entitiesQuery.append(" OR (e.entity_type = '").append(entityType.name()).append("'");
                MergedGroupTypePermissionInfo permissions = readPermMap.get(Resource.resourceFromEntityType(entityType));
                if (ctx.isTenantUser()) {
                    if (permissions.isHasGenericRead()) {
                        //Do nothing here. Query is ok.
                    } else if (permissions.getEntityGroupIds().isEmpty()) {
                        entitiesQuery.append(" AND FALSE");
                    } else {
                        entitiesQuery.append(" AND EXISTS ( SELECT rattr.to_id FROM relation rattr WHERE rattr.to_id = e.id AND rattr.to_type = '");
                        entitiesQuery.append(entityType.name());
                        String param = "permissions_read_group_ids_" + entityType.name().toLowerCase();
                        ctx.addUuidListParameter(param,
                                permissions.getEntityGroupIds().stream().map(EntityGroupId::getId).collect(Collectors.toList()));
                        entitiesQuery.append("' and rattr.from_id in (:").append(param).append(") AND rattr.from_type = 'ENTITY_GROUP' " +
                                "AND rattr.relation_type_group = 'FROM_ENTITY_GROUP' AND rattr.relation_type = 'Contains')");
                    }
                } else {
                    String customerIdField = entityType == EntityType.CUSTOMER ? "id" : "customer_id";
                    if (permissions.isHasGenericRead()) {
                        if (permissions.getEntityGroupIds().isEmpty()) {
                            entitiesQuery.append(" AND e.").append(customerIdField).append(" in ").append(subCustomersInClause(ctx));
                        } else {
                            entitiesQuery.append(" AND (e.").append(customerIdField).append(" in ").append(subCustomersInClause(ctx));
                            entitiesQuery.append(" OR EXISTS ( SELECT rattr.to_id FROM relation rattr WHERE rattr.to_id = e.id AND rattr.to_type = '");
                            entitiesQuery.append(entityType.name());
                            String param = "permissions_read_group_ids_" + entityType.name().toLowerCase();
                            ctx.addUuidListParameter(param,
                                    permissions.getEntityGroupIds().stream().map(EntityGroupId::getId).collect(Collectors.toList()));
                            entitiesQuery.append("' and rattr.from_id in (:").append(param).append(") AND rattr.from_type = 'ENTITY_GROUP' " +
                                    "AND rattr.relation_type_group = 'FROM_ENTITY_GROUP' AND rattr.relation_type = 'Contains'))");
                        }
                    } else {
                        if (permissions.getEntityGroupIds().isEmpty()) {
                            entitiesQuery.append(" AND FALSE");
                        } else {
                            entitiesQuery.append(" AND EXISTS ( SELECT rattr.to_id FROM relation rattr WHERE rattr.to_id = e.id AND rattr.to_type = '");
                            entitiesQuery.append(entityType.name());
                            String param = "permissions_read_group_ids_" + entityType.name().toLowerCase();
                            ctx.addUuidListParameter(param,
                                    permissions.getEntityGroupIds().stream().map(EntityGroupId::getId).collect(Collectors.toList()));
                            entitiesQuery.append("' and rattr.from_id in (:").append(param).append(") AND rattr.from_type = 'ENTITY_GROUP' " +
                                    "AND rattr.relation_type_group = 'FROM_ENTITY_GROUP' AND rattr.relation_type = 'Contains')");
                        }
                    }
                }
                entitiesQuery.append(")");
            }
            entitiesQuery.append(")");
            if (!entityWhereClause.isEmpty()) {
                entitiesQuery.append(" AND ").append(entityWhereClause);
            }
        }

        return entitiesQuery;
    }

    private void addTenantEntityCheck(SqlQueryContext ctx, StringBuilder entitiesQuery, Map<Resource, MergedGroupTypePermissionInfo> readPermMap, EntityType entityType) {
        entitiesQuery.append("(e.entity_type = '").append(entityType.name()).append("' AND ");
        entitiesQuery.append(ctx.isTenantUser() && readPermMap.get(Resource.resourceFromEntityType(entityType)).isHasGenericRead() ? "true" : "false");
        entitiesQuery.append(")");
    }

    private void addCustomerEntityCheck(SqlQueryContext ctx, StringBuilder entitiesQuery, Map<Resource, MergedGroupTypePermissionInfo> readPermMap, EntityType entityType) {
        if (ctx.isTenantUser()) {
            entitiesQuery.append("(e.entity_type = '").append(entityType.name()).append("' AND ");
            entitiesQuery.append(readPermMap.get(Resource.resourceFromEntityType(entityType)).isHasGenericRead() ? "true" : "false");
            entitiesQuery.append(")");
        } else {
            entitiesQuery.append("(e.entity_type = '").append(entityType.name()).append("' AND ");
            if (readPermMap.get(Resource.resourceFromEntityType(entityType)).isHasGenericRead()) {
                entitiesQuery.append("e.customer_id in ").append(subCustomersInClause(ctx));
            } else {
                entitiesQuery.append("FALSE");
            }
            entitiesQuery.append(")");
        }
    }

    private void buildPermissionsSelect(SqlQueryContext ctx, StringBuilder entitiesQuery,
                                        Map<Resource, MergedGroupTypePermissionInfo> permissionsMap,
                                        String groupIdParamNamePrefix, String selectionName) {
        if (hasGenericForAllRelationQueryResources(permissionsMap)) {
            entitiesQuery.append(", 1 as ").append(selectionName);
        } else if (hasNoPermissionsForAllRelationQueryResources(permissionsMap)) {
            entitiesQuery.append(", 0 as ").append(selectionName);
        } else {
            entitiesQuery.append(", CASE");
            entitiesQuery.append(" WHEN e.entity_type = 'TENANT' THEN ").append(permissionsMap.get(Resource.resourceFromEntityType(EntityType.TENANT)).isHasGenericRead() ? "1" : "0");
            for (EntityType entityType : EntityGroup.groupTypes) {
                entitiesQuery.append(" WHEN e.entity_type = '").append(entityType.name()).append("' THEN ");
                MergedGroupTypePermissionInfo permissions = permissionsMap.get(Resource.resourceFromEntityType(entityType));
                if (permissions.isHasGenericRead()) {
                    entitiesQuery.append("1");
                } else if (permissions.getEntityGroupIds().isEmpty()) {
                    entitiesQuery.append("0");
                } else {
                    entitiesQuery.append("(CASE WHEN EXISTS ( SELECT rattr.to_id FROM relation rattr WHERE rattr.to_id = e.id AND rattr.to_type = '");
                    entitiesQuery.append(entityType.name());
                    String param = groupIdParamNamePrefix + "_" + entityType.name().toLowerCase();
                    ctx.addUuidListParameter(param,
                            permissions.getEntityGroupIds().stream().map(EntityGroupId::getId).collect(Collectors.toList()));
                    entitiesQuery.append("' and rattr.from_id in (:").append(param).append(") AND rattr.from_type = 'ENTITY_GROUP' " +
                            "AND rattr.relation_type_group = 'FROM_ENTITY_GROUP' AND rattr.relation_type = 'Contains') THEN 1 ELSE 0 END)");
                }
            }
            entitiesQuery.append(" END as ").append(selectionName);
        }
    }

    private StringBuilder buildCommonEntitiesQuery(EntityCountQuery query, SqlQueryContext ctx,
                                                   MergedGroupTypePermissionInfo readPermissions,
                                                   String entityWhereClause, String entityFieldsSelection) {
        StringBuilder entitiesQuery = new StringBuilder();

        MergedGroupTypePermissionInfo readAttrPermissions = ctx.getSecurityCtx().getMergedReadAttrPermissionsByEntityType();
        MergedGroupTypePermissionInfo readTsPermissions = ctx.getSecurityCtx().getMergedReadTsPermissionsByEntityType();

        entitiesQuery.append("select ").append(entityFieldsSelection);

        boolean allReadPermissions = readPermissions.isHasGenericRead() && readAttrPermissions.isHasGenericRead() && readTsPermissions.isHasGenericRead();
        boolean noGroupPermissions = emptyGroups(readPermissions) && emptyGroups(readAttrPermissions) && emptyGroups(readTsPermissions);
        boolean tenantUserWithAllReadPermissions = ctx.isTenantUser() && allReadPermissions;
        boolean customerUserWithAllReadPermissionsAndNoGroupPermissions = !ctx.isTenantUser() && allReadPermissions && noGroupPermissions;
        if (tenantUserWithAllReadPermissions || customerUserWithAllReadPermissionsAndNoGroupPermissions) {
            entitiesQuery.append(", 1 as ").append(ATTR_READ_FLAG);
            entitiesQuery.append(", 1 as ").append(TS_READ_FLAG);
            entitiesQuery.append(" from ")
                    .append(addEntityTableQuery(ctx, query.getEntityFilter()))
                    .append(" e where ")
                    .append(entityWhereClause);
        } else {
            GroupIdsWrapper groupIds = new GroupIdsWrapper(readPermissions, readAttrPermissions, readTsPermissions);
            boolean innerJoin = true;
            boolean hasFilters;
            StringBuilder entityFlagsQuery = new StringBuilder();
            int paramIdx = 0;
            if (ctx.isTenantUser()) {
                if (readPermissions.isHasGenericRead()) {
                    innerJoin = false;
                    hasFilters = addGroupEntitiesByGroupIdsFilters(ctx, groupIds, entityFlagsQuery, paramIdx, permKey -> !permKey.isAttr() && !permKey.isTs());
                } else {
                    hasFilters = addGroupEntitiesByGroupIdsFilters(ctx, groupIds, entityFlagsQuery, paramIdx, null);
                }
                if (readAttrPermissions.isHasGenericRead() || readTsPermissions.isHasGenericRead()) {
                    if (hasFilters) {
                        entityFlagsQuery.append(" UNION ALL ");
                    } else {
                        hasFilters = true;
                    }
                    entityFlagsQuery.append(" select e.id to_id, ")
                            .append(boolToIntStr(readPermissions.isHasGenericRead())).append(" as readFlag").append(",")
                            .append(boolToIntStr(readAttrPermissions.isHasGenericRead())).append(" as readAttrFlag").append(",")
                            .append(boolToIntStr(readTsPermissions.isHasGenericRead())).append(" as readTsFlag")
                            .append(" from ").append(addEntityTableQuery(ctx, query.getEntityFilter())).append(" e ");
                }
            } else {
                ctx.addUuidParameter("permissions_customer_id", ctx.getCustomerId().getId());
                hasFilters = addGroupEntitiesByGroupIdsFilters(ctx, groupIds, entityFlagsQuery, paramIdx, null);
                if (hasFilters) {
                    entityFlagsQuery.append(" UNION ALL ");
                } else {
                    hasFilters = true;
                }
                if (!ctx.getEntityType().equals(EntityType.CUSTOMER)) {
                    entityFlagsQuery.append("select e.id to_id, ")
                            .append(boolToIntStr(readPermissions.isHasGenericRead())).append(" as readFlag").append(",")
                            .append(boolToIntStr(readAttrPermissions.isHasGenericRead())).append(" as readAttrFlag").append(",")
                            .append(boolToIntStr(readTsPermissions.isHasGenericRead())).append(" as readTsFlag");
                    entityFlagsQuery.append(" from ").append(addEntityTableQuery(ctx, query.getEntityFilter())).append(" e ");
                    entityFlagsQuery.append(" where ").append(entityWhereClause);
                    if (ctx.isCitusEnabled()) {
                        // subCustomersInClause already yields a parenthesized fragment — a (select unnest(:resolvedIds))
                        // subquery, or (null) when empty — so it is spliced directly after "in " with no extra parens.
                        // Plain mode keeps the original ((WITH RECURSIVE ...)) double-paren subquery form byte-for-byte.
                        entityFlagsQuery.append(" AND e.customer_id in ").append(subCustomersInClause(ctx));
                    } else {
                        entityFlagsQuery.append(" AND e.customer_id in (").append(subCustomersInClause(ctx)).append(")");
                    }
                } else {
                    entityFlagsQuery.append("select c.id to_id, ")
                            .append(boolToIntStr(readPermissions.isHasGenericRead())).append(" as readFlag").append(",")
                            .append(boolToIntStr(readAttrPermissions.isHasGenericRead())).append(" as readAttrFlag").append(",")
                            .append(boolToIntStr(readTsPermissions.isHasGenericRead())).append(" as readTsFlag");
                    entityFlagsQuery.append(" from customer c WHERE ");
                    entityFlagsQuery.append(" c.id in ").append(subCustomersInClause(ctx));
                }
            }
            if (innerJoin || hasFilters) {
                entitiesQuery.append(", COALESCE(readAttrFlag, 0) as ").append(ATTR_READ_FLAG);
                entitiesQuery.append(", COALESCE(readTsFlag, 0) as ").append(TS_READ_FLAG);
                entitiesQuery.append(" from ").append(addEntityTableQuery(ctx, query.getEntityFilter())).append(" e ");
                entitiesQuery.append(innerJoin ? "inner" : "left").append(" join ");
                if (hasFilters) {
                    entitiesQuery.append(" (select to_id as id, max(readFlag) as readFlag, max(readAttrFlag) as readAttrFlag, max(readTsFlag) as readTsFlag ");
                    entitiesQuery.append(" from (");
                    entitiesQuery.append(entityFlagsQuery);
                    entitiesQuery.append(" ) ids group by to_id) entity_flags on e.id = entity_flags.id ");
                } else {
                    entitiesQuery.append(" (select NULL::uuid as id, 0 as readFlag, 0 as readAttrFlag, 0 as readTsFlag) entity_flags on e.id = entity_flags.id ");
                }
                if (innerJoin) {
                    entitiesQuery.append(" and entity_flags.readFlag = 1");
                }
            } else {
                entitiesQuery.append(", 0 as ").append(ATTR_READ_FLAG);
                entitiesQuery.append(", 0 as ").append(TS_READ_FLAG);
                entitiesQuery.append(" from ").append(addEntityTableQuery(ctx, query.getEntityFilter())).append(" e ");
            }
            entitiesQuery.append(" where ").append(entityWhereClause);
        }
        return entitiesQuery;
    }

    //Created as a workaround of HSQLDB bug with selecting multiple constants with different names.
    private static String boolToIntStr(boolean bool) {
        return bool ? "1" : "0";
    }

    private boolean addGroupIdsFilters(SqlQueryContext ctx, GroupIdsWrapper groupIds, StringBuilder entityFlagsQuery, int paramIdx, Predicate<GroupIdPermKey> keyFilter) {
        boolean first = true;
        for (Map.Entry<GroupIdPermKey, Set<EntityGroupId>> e : groupIds.getGroupIdsMap().entrySet()) {
            GroupIdPermKey permKey = e.getKey();
            if (keyFilter != null && keyFilter.test(permKey)) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                entityFlagsQuery.append(" UNION ALL ");
            }
            ctx.addUuidListParameter("permissions_read_group_ids_" + paramIdx,
                    e.getValue().stream().map(EntityGroupId::getId).collect(Collectors.toList()));
            entityFlagsQuery.append("select ge.id, ")
                    .append(boolToIntStr(permKey.isRead())).append(" as readFlag").append(",")
                    .append(boolToIntStr(permKey.isAttr())).append(" as readAttrFlag").append(",")
                    .append(boolToIntStr(permKey.isTs())).append(" as readTsFlag");
            entityFlagsQuery.append(" from entity_group ge WHERE ");
            entityFlagsQuery.append(" ge.id in (:permissions_read_group_ids_").append(paramIdx++).append(")");
        }
        return !first;
    }


    private boolean addGroupEntitiesByGroupIdsFilters(SqlQueryContext ctx, GroupIdsWrapper groupIds, StringBuilder entityFlagsQuery, int paramIdx, Predicate<GroupIdPermKey> keyFilter) {
        boolean first = true;
        for (Map.Entry<GroupIdPermKey, Set<EntityGroupId>> e : groupIds.getGroupIdsMap().entrySet()) {
            GroupIdPermKey permKey = e.getKey();
            if (keyFilter != null && keyFilter.test(permKey)) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                entityFlagsQuery.append(" UNION ALL ");
            }
            ctx.addUuidListParameter("permissions_read_group_ids_" + paramIdx,
                    e.getValue().stream().map(EntityGroupId::getId).collect(Collectors.toList()));
            entityFlagsQuery.append("select re.to_id, ")
                    .append(boolToIntStr(permKey.isRead())).append(" as readFlag").append(",")
                    .append(boolToIntStr(permKey.isAttr())).append(" as readAttrFlag").append(",")
                    .append(boolToIntStr(permKey.isTs())).append(" as readTsFlag");
            entityFlagsQuery.append(" from relation re WHERE re.relation_type_group = 'FROM_ENTITY_GROUP' AND re.relation_type = 'Contains'");
            entityFlagsQuery.append(" AND re.from_id in (:permissions_read_group_ids_").append(paramIdx++).append(")");
            entityFlagsQuery.append(" AND re.from_type = 'ENTITY_GROUP'");
        }
        return !first;
    }

    private boolean emptyGroups(MergedGroupTypePermissionInfo permissions) {
        return permissions.getEntityGroupIds() == null || permissions.getEntityGroupIds().isEmpty();
    }

    private StringBuilder buildGroupEntitiesQuery(EntityCountQuery query, SqlQueryContext ctx,
                                                  MergedGroupTypePermissionInfo readPermissions, String entityWhereClause, String entityFieldsSelection) {
        StringBuilder entitiesQuery = new StringBuilder();

        MergedGroupTypePermissionInfo readAttrPermissions = ctx.getSecurityCtx().getMergedReadAttrPermissionsByEntityType();
        MergedGroupTypePermissionInfo readTsPermissions = ctx.getSecurityCtx().getMergedReadTsPermissionsByEntityType();

        entitiesQuery.append("select ").append(entityFieldsSelection);

        boolean allReadPermissions = readPermissions.isHasGenericRead() && readAttrPermissions.isHasGenericRead() && readTsPermissions.isHasGenericRead();
        boolean noGroupPermissions = emptyGroups(readPermissions) && emptyGroups(readAttrPermissions) && emptyGroups(readTsPermissions);
        boolean tenantUserWithAllReadPermissions = ctx.isTenantUser() && allReadPermissions;
        boolean customerUserWithAllReadPermissionsAndNoGroupPermissions = !ctx.isTenantUser() && allReadPermissions && noGroupPermissions;
        if (tenantUserWithAllReadPermissions || customerUserWithAllReadPermissionsAndNoGroupPermissions) {
            entitiesQuery.append(", 1 as ").append(ATTR_READ_FLAG);
            entitiesQuery.append(", 1 as ").append(TS_READ_FLAG);
            entitiesQuery.append(" from ")
                    .append(addEntityTableQuery(ctx, query.getEntityFilter()))
                    .append(" e where ")
                    .append(entityWhereClause);
        } else {
            GroupIdsWrapper groupIds = new GroupIdsWrapper(readPermissions, readAttrPermissions, readTsPermissions);
            boolean innerJoin = true;
            boolean hasFilters;
            StringBuilder entityFlagsQuery = new StringBuilder();
            int paramIdx = 0;
            if (ctx.isTenantUser()) {
                if (readPermissions.isHasGenericRead()) {
                    innerJoin = false;
                    hasFilters = addGroupIdsFilters(ctx, groupIds, entityFlagsQuery, paramIdx, permKey -> !permKey.isAttr() && !permKey.isTs());
                } else {
                    hasFilters = addGroupIdsFilters(ctx, groupIds, entityFlagsQuery, paramIdx, null);
                }
                if (readAttrPermissions.isHasGenericRead() || readTsPermissions.isHasGenericRead()) {
                    if (hasFilters) {
                        entityFlagsQuery.append(" UNION ALL ");
                    } else {
                        hasFilters = true;
                    }
                    entityFlagsQuery.append(" select e.id id, ")
                            .append(boolToIntStr(readPermissions.isHasGenericRead())).append(" as readFlag").append(",")
                            .append(boolToIntStr(readAttrPermissions.isHasGenericRead())).append(" as readAttrFlag").append(",")
                            .append(boolToIntStr(readTsPermissions.isHasGenericRead())).append(" as readTsFlag")
                            .append(" from ").append(addEntityTableQuery(ctx, query.getEntityFilter())).append(" e ");
                }
            } else {
                ctx.addUuidParameter("permissions_customer_id", ctx.getCustomerId().getId());
                hasFilters = addGroupIdsFilters(ctx, groupIds, entityFlagsQuery, paramIdx, null);
                if (hasFilters) {
                    entityFlagsQuery.append(" UNION ALL ");
                } else {
                    hasFilters = true;
                }
                entityFlagsQuery.append("select ge.id, ")
                        .append(boolToIntStr(readPermissions.isHasGenericRead())).append(" as readFlag").append(",")
                        .append(boolToIntStr(readAttrPermissions.isHasGenericRead())).append(" as readAttrFlag").append(",")
                        .append(boolToIntStr(readTsPermissions.isHasGenericRead())).append(" as readTsFlag");
                entityFlagsQuery.append(" from entity_group ge WHERE");
                entityFlagsQuery.append(" ge.id in (").append(groupsByOwnerQuery(ctx));
                Optional.ofNullable(ctx.getSecurityCtx().getEntityGroupType())
                        .ifPresentOrElse(
                                groupType -> entityFlagsQuery.append(" and type = '").append(groupType).append("')"),
                                () -> entityFlagsQuery.append(")"));
            }
            if (innerJoin || hasFilters) {
                entitiesQuery.append(", COALESCE(readAttrFlag, 0) as ").append(ATTR_READ_FLAG);
                entitiesQuery.append(", COALESCE(readTsFlag, 0) as ").append(TS_READ_FLAG);
                entitiesQuery.append(" from ").append(addEntityTableQuery(ctx, query.getEntityFilter())).append(" e ");
                entitiesQuery.append(innerJoin ? "inner" : "left").append(" join ");
                if (hasFilters) {
                    entitiesQuery.append(" (select id as id, max(readFlag) as readFlag, max(readAttrFlag) as readAttrFlag, max(readTsFlag) as readTsFlag ");
                    entitiesQuery.append(" from (");
                    entitiesQuery.append(entityFlagsQuery);
                    entitiesQuery.append(" ) ids group by id) entity_flags on e.id = entity_flags.id ");
                } else {
                    entitiesQuery.append(" (select NULL::uuid as id, 0 as readFlag, 0 as readAttrFlag, 0 as readTsFlag) entity_flags on e.id = entity_flags.id ");
                }
                if (innerJoin) {
                    entitiesQuery.append(" and entity_flags.readFlag = 1");
                }
            } else {
                entitiesQuery.append(", 0 as ").append(ATTR_READ_FLAG);
                entitiesQuery.append(", 0 as ").append(TS_READ_FLAG);
                entitiesQuery.append(" from ").append(addEntityTableQuery(ctx, query.getEntityFilter())).append(" e ");
            }
            entitiesQuery.append(" where ").append(entityWhereClause);
        }
        return entitiesQuery;
    }

    private boolean hasGenericForAllRelationQueryResources(Map<Resource, MergedGroupTypePermissionInfo> permissionsMap) {
        if (!permissionsMap.get(Resource.resourceFromEntityType(EntityType.TENANT)).isHasGenericRead()) {
            return false;
        }
        for (EntityType entityType : EntityGroup.groupTypes) {
            if (!permissionsMap.get(Resource.resourceFromEntityType(entityType)).isHasGenericRead()) {
                return false;
            }
            if (!permissionsMap.get(Resource.groupResourceFromGroupType(entityType)).isHasGenericRead()) {
                return false;
            }
        }
        return true;
    }

    private boolean noGroupPermissionsForAllRelationQueryResources(Map<Resource, MergedGroupTypePermissionInfo> permissionsMap) {
        for (EntityType entityType : EntityGroup.groupTypes) {
            if (!permissionsMap.get(Resource.resourceFromEntityType(entityType)).getEntityGroupIds().isEmpty()) {
                return false;
            }
            if (!permissionsMap.get(Resource.groupResourceFromGroupType(entityType)).getEntityGroupIds().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasNoPermissionsForAllRelationQueryResources(Map<Resource, MergedGroupTypePermissionInfo> permissionsMap) {
        if (permissionsMap.get(Resource.resourceFromEntityType(EntityType.TENANT)).isHasGenericRead()) {
            return false;
        }
        for (EntityType entityType : EntityGroup.groupTypes) {
            if (permissionsMap.get(Resource.resourceFromEntityType(entityType)).isHasGenericRead() ||
                    !permissionsMap.get(Resource.resourceFromEntityType(entityType)).getEntityGroupIds().isEmpty()) {
                return false;
            }
            if (permissionsMap.get(Resource.groupResourceFromGroupType(entityType)).isHasGenericRead() ||
                    !permissionsMap.get(Resource.groupResourceFromGroupType(entityType)).getEntityGroupIds().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String resolveNullsOrder() {
        return switch (nullsOrderStrategy) {
            case NULLS_ORDER_FIRST -> " NULLS FIRST";
            case NULLS_ORDER_LAST -> " NULLS LAST";
            default -> "";
        };
    }

    private String buildEntityWhere(SqlQueryContext ctx, EntityFilter entityFilter, List<EntityKeyMapping> entityFieldsFilters) {
        String permissionQuery = this.buildPermissionQuery(ctx, entityFilter);
        String entityFilterQuery = this.buildEntityFilterQuery(ctx, entityFilter);
        String entityFieldsQuery = EntityKeyMapping.buildQuery(ctx, entityFieldsFilters, entityFilter.getType());
        String result = permissionQuery;
        if (!entityFilterQuery.isEmpty()) {
            if (!result.isEmpty()) {
                result += " and (" + entityFilterQuery + ")";
            } else {
                result = "(" + entityFilterQuery + ")";
            }
        }
        if (!entityFieldsQuery.isEmpty()) {
            if (!result.isEmpty()) {
                result += " and (" + entityFieldsQuery + ")";
            } else {
                result = "(" + entityFieldsQuery + ")";
            }
        }
        return result;
    }

    private String buildPermissionQuery(SqlQueryContext ctx, EntityFilter entityFilter) {
        if (ctx.isIgnorePermissionCheck() || (ctx.getTenantId().isSysTenantId() &&
                (ctx.getEntityType() == EntityType.TENANT || ctx.getEntityType() == EntityType.TENANT_PROFILE))) {
            return "1=1";
        }
        switch (entityFilter.getType()) {
            case RELATIONS_QUERY:
                return "";
            case DEVICE_SEARCH_QUERY:
            case ASSET_SEARCH_QUERY:
            case ENTITY_VIEW_SEARCH_QUERY:
            case EDGE_SEARCH_QUERY:
            case ENTITY_GROUP:
            case ENTITY_GROUP_NAME:
            case SCHEDULER_EVENT:
                return this.defaultPermissionQuery(ctx, entityFilter);
            case API_USAGE_STATE:
                CustomerId filterCustomerId = ((ApiUsageStateFilter) entityFilter).getCustomerId();
                if (ctx.getCustomerId() != null && !ctx.getCustomerId().isNullUid()) {
                    if (filterCustomerId != null && !filterCustomerId.equals(ctx.getCustomerId())) {
                        throw new SecurityException("Customer is not allowed to query other customer's data");
                    }
                    filterCustomerId = ctx.getCustomerId();
                }

                ctx.addUuidParameter("permissions_tenant_id", ctx.getTenantId().getId());
                if (filterCustomerId != null) {
                    ctx.addUuidParameter("permissions_customer_id", filterCustomerId.getId());
                    return "e.tenant_id=:permissions_tenant_id and e.entity_id=:permissions_customer_id";
                } else {
                    return "e.tenant_id=:permissions_tenant_id and e.entity_id=:permissions_tenant_id";
                }
            default:
                if (ctx.getEntityType() == EntityType.TENANT) {
                    ctx.addUuidParameter("permissions_tenant_id", ctx.getTenantId().getId());
                    return "e.id=:permissions_tenant_id";
                } else {
                    return this.defaultPermissionQuery(ctx, entityFilter);
                }
        }
    }

    private String defaultPermissionQuery(SqlQueryContext ctx, EntityFilter entityFilter) {
        ctx.addUuidParameter("permissions_tenant_id", ctx.getTenantId().getId());
        QueryContext securityCtx = ctx.getSecurityCtx();
        if (!securityCtx.isTenantUser() && securityCtx.hasGeneric(Operation.READ) && securityCtx.getMergedReadPermissionsByEntityType().getEntityGroupIds().isEmpty()) {
            ctx.addUuidParameter("permissions_customer_id", ctx.getCustomerId().getId());
            if (ctx.getEntityType() == EntityType.CUSTOMER && entityFilter.getType() != EntityFilterType.ENTITY_GROUP_LIST
                    && entityFilter.getType() != EntityFilterType.ENTITY_GROUP_NAME) {
                return "e.tenant_id=:permissions_tenant_id and e.id in " + subCustomersInClause(ctx);
            } else if (ctx.getEntityType() == EntityType.API_USAGE_STATE) {
                return "e.tenant_id=:permissions_tenant_id and e.entity_id in " + subCustomersInClause(ctx);
            } else {
                return "e.tenant_id=:permissions_tenant_id and e.customer_id in " + subCustomersInClause(ctx);
            }
        }
        return "e.tenant_id=:permissions_tenant_id";
    }

    private String buildEntityFilterQuery(SqlQueryContext ctx, EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case SINGLE_ENTITY:
                return this.singleEntityQuery(ctx, (SingleEntityFilter) entityFilter);
            case ENTITY_LIST:
                return this.entityListQuery(ctx, (EntityListFilter) entityFilter);
            case ENTITY_NAME:
                return this.entityNameQuery(ctx, (EntityNameFilter) entityFilter);
            case ENTITY_GROUP_NAME:
                return this.entityGroupNameQuery(ctx, (EntityGroupNameFilter) entityFilter);
            case SCHEDULER_EVENT:
                return this.schedulerEventQuery(ctx, (SchedulerEventFilter) entityFilter);
            case ASSET_TYPE:
            case DEVICE_TYPE:
            case ENTITY_VIEW_TYPE:
            case EDGE_TYPE:
                return this.typeQuery(ctx, entityFilter);
            case STATE_ENTITY_OWNER:
                return this.singleEntityByStateOwner(ctx);
            case ENTITY_GROUP:
            case ENTITY_GROUP_LIST:
            case ENTITIES_BY_GROUP_NAME:
            case RELATIONS_QUERY:
            case DEVICE_SEARCH_QUERY:
            case ASSET_SEARCH_QUERY:
            case ENTITY_VIEW_SEARCH_QUERY:
            case EDGE_SEARCH_QUERY:
            case API_USAGE_STATE:
            case ENTITY_TYPE:
                return "";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    private String schedulerEventQuery(SqlQueryContext ctx, SchedulerEventFilter entityFilter) {
        String query = "";
        if (StringUtils.isNotBlank(entityFilter.getEventType())) {
            ctx.addStringParameter("entity_filter_scheduler_event_type", entityFilter.getEventType());
            query = "e.type=:entity_filter_scheduler_event_type";
        }
        if (entityFilter.getOriginator() != null) {
            ctx.addUuidParameter("entity_filter_scheduler_originator", entityFilter.getOriginator().getId());
            query = (StringUtils.isEmpty(query) ? "" : query + " AND ") + "e.originator_id=:entity_filter_scheduler_originator";
        }

        return query;
    }

    private String addEntityTableQuery(SqlQueryContext ctx, EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case ENTITY_GROUP:
                return entityGroupQuery(ctx, (EntityGroupFilter) entityFilter);
            case ENTITIES_BY_GROUP_NAME:
                return entityByGroupNameQuery(ctx, (EntitiesByGroupNameFilter) entityFilter);
            case ENTITY_GROUP_LIST:
                return entityGroupQueryByGroupList(ctx, (EntityGroupListFilter) entityFilter);
            case ENTITY_GROUP_NAME:
                return entityGroupQueryByGroupName(ctx, (EntityGroupNameFilter) entityFilter);
            case SINGLE_ENTITY:
                if (ctx.getSecurityCtx().isEntityGroup()) {
                    return entityGroupQueryById(ctx, (SingleEntityFilter) entityFilter);
                } else {
                    return entityTableMap.get(ctx.getEntityType());
                }
            case RELATIONS_QUERY:
                return relationQuery(ctx, (RelationsQueryFilter) entityFilter);
            case DEVICE_SEARCH_QUERY:
                DeviceSearchQueryFilter deviceQuery = (DeviceSearchQueryFilter) entityFilter;
                return entitySearchQuery(ctx, deviceQuery, EntityType.DEVICE, deviceQuery.getDeviceTypes());
            case ASSET_SEARCH_QUERY:
                AssetSearchQueryFilter assetQuery = (AssetSearchQueryFilter) entityFilter;
                return entitySearchQuery(ctx, assetQuery, EntityType.ASSET, assetQuery.getAssetTypes());
            case ENTITY_VIEW_SEARCH_QUERY:
                EntityViewSearchQueryFilter entityViewQuery = (EntityViewSearchQueryFilter) entityFilter;
                return entitySearchQuery(ctx, entityViewQuery, EntityType.ENTITY_VIEW, entityViewQuery.getEntityViewTypes());
            case EDGE_SEARCH_QUERY:
                EdgeSearchQueryFilter edgeQuery = (EdgeSearchQueryFilter) entityFilter;
                return entitySearchQuery(ctx, edgeQuery, EntityType.EDGE, edgeQuery.getEdgeTypes());
            default:
                return entityTableMap.get(ctx.getEntityType());
        }
    }

    private String entityGroupQueryByGroupList(SqlQueryContext ctx, EntityGroupListFilter entityFilter) {
        EntityType entityType = entityFilter.getGroupType();
        String select = "SELECT * ," +
                " CASE WHEN owner_type = 'CUSTOMER' THEN (select tenant_id from customer where id = owner_id) ELSE owner_id END as tenant_id," +
                " CASE WHEN owner_type = 'CUSTOMER' THEN owner_id END as customer_id" +
                " FROM entity_group WHERE type = :entity_group_type and id in (:entity_group_list_ids)";
        ctx.addStringParameter("entity_group_type", entityType.name());
        ctx.addUuidListParameter("entity_group_list_ids", entityFilter.getEntityGroupList().stream().map(UUID::fromString).collect(Collectors.toList()));
        return "(" + select + ")";
    }

    private String entityGroupQuery(SqlQueryContext ctx, EntityGroupFilter entityFilter) {
        EntityType entityType = entityFilter.getGroupType();
        EntityGroupId entityGroupId = new EntityGroupId(UUID.fromString(entityFilter.getEntityGroup()));
        String selectFields = "SELECT * FROM " + entityTableMap.get(entityType);
        String from = " WHERE id in (SELECT to_id from relation where from_id = :where_group_id and from_type = '" + EntityType.ENTITY_GROUP.name() + "'" +
                "and relation_type_group='" + RelationTypeGroup.FROM_ENTITY_GROUP + "' and relation_type='" + EntityRelation.CONTAINS_TYPE + "')";
        ctx.addUuidParameter("where_group_id", entityGroupId.getId());
        return "( " + selectFields + from + ")";
    }

    private String entityGroupQueryByGroupName(SqlQueryContext ctx, EntityGroupNameFilter entityFilter) {
        EntityType entityType = entityFilter.getGroupType();
        String select = "SELECT * ," +
                " CASE WHEN owner_type = 'CUSTOMER' THEN (select tenant_id from customer where id = owner_id) ELSE owner_id END as tenant_id," +
                " CASE WHEN owner_type = 'CUSTOMER' THEN owner_id END as customer_id" +
                " FROM entity_group WHERE type = :entity_group_type";
        ctx.addStringParameter("entity_group_type", entityType.name());
        if (StringUtils.isNotEmpty(entityFilter.getEntityGroupNameFilter())) {
            select = select + " and name ILIKE concat(:entity_group_name_prefix, '%%')";
            ctx.addStringParameter("entity_group_name_prefix", entityFilter.getEntityGroupNameFilter());
        }
        return "(" + select + ")";
    }

    private String entityGroupQueryById(SqlQueryContext ctx, SingleEntityFilter entityFilter) {
        EntityId entityId = entityFilter.getSingleEntity();
        String select = "SELECT * ," +
                " CASE WHEN owner_type = 'CUSTOMER' THEN (select tenant_id from customer where id = owner_id) ELSE owner_id END as tenant_id," +
                " CASE WHEN owner_type = 'CUSTOMER' THEN owner_id END as customer_id" +
                " FROM entity_group WHERE id = :entity_group_id";
        ctx.addUuidParameter("entity_group_id", entityId.getId());
        return "(" + select + ")";
    }

    private String entityByGroupNameQuery(SqlQueryContext ctx, EntitiesByGroupNameFilter entityFilter) {
        EntityType entityType = entityFilter.getGroupType();
        String selectFields = "SELECT * FROM " + entityTableMap.get(entityType);
        MergedGroupTypePermissionInfo groupTypePermissionInfo = ctx.getSecurityCtx().getMergedReadGroupPermissionsByEntityType();
        String where;
        if (groupTypePermissionInfo.isHasGenericRead() || !groupTypePermissionInfo.getEntityGroupIds().isEmpty()) {
            EntityId customOwnerId = entityFilter.getOwnerId();
            if (customOwnerId != null && !customOwnerId.getEntityType().equals(EntityType.TENANT) && !customOwnerId.getEntityType().equals(EntityType.CUSTOMER)) {
                customOwnerId = null;
            }
            String allowedGroupIdsSelect = "(";
            boolean genericPartAdded = false;
            if (groupTypePermissionInfo.isHasGenericRead()) {
                if (customOwnerId == null || ctx.getOwnerId().equals(customOwnerId.getId())) {
                    // No custom owner - just select a list of groups that belong to current owner
                    allowedGroupIdsSelect += "owner_id = :where_group_owner_id";
                    ctx.addUuidParameter("where_group_owner_id", ctx.getOwnerId());
                    genericPartAdded = true;
                } else if (ctx.isTenantUser()) {
                    // Tenant user with different custom owner id. We need to check that this is our customer.
                    allowedGroupIdsSelect += "owner_id in (select id from customer where id = :where_group_owner_id and tenant_id = :where_real_tenant_id)";
                    ctx.addUuidParameter("where_group_owner_id", customOwnerId.getId());
                    ctx.addUuidParameter("where_real_tenant_id", ctx.getOwnerId());
                    genericPartAdded = true;
                } else if (customOwnerId.getEntityType().equals(EntityType.CUSTOMER)) {
                    // Customer user with different custom owner id. We need to check the hierarchy now
                    ctx.addUuidParameter("where_group_owner_id", customOwnerId.getId());
                    ctx.addUuidParameter("where_real_tenant_id", ctx.getTenantId().getId());
                    ctx.addUuidParameter("where_real_owner_id", ctx.getOwnerId());
                    // Recursion is over the customer reference table. Under Citus the enclosing entity query is
                    // distributed and rejects an embedded WITH RECURSIVE, so pre-resolve the subtree standalone.
                    String customersSubtree = "(WITH RECURSIVE customers_ids(id) AS" +
                            " (SELECT id id FROM customer WHERE tenant_id = :where_real_tenant_id and id = :where_real_owner_id" +
                            " UNION SELECT c.id id FROM customer c, customers_ids parent WHERE c.tenant_id = :where_real_tenant_id" +
                            " and c.parent_customer_id = parent.id) SELECT id FROM customers_ids)";
                    if (ctx.isCitusEnabled()) {
                        customersSubtree = resolveReferenceRecursion(ctx, customersSubtree, "resolved_group_name_customers");
                    }
                    allowedGroupIdsSelect += "owner_id in (select id from customer where id = :where_group_owner_id and tenant_id = :where_real_tenant_id and id in ";
                    allowedGroupIdsSelect += customersSubtree;
                    allowedGroupIdsSelect += ")";
                    genericPartAdded = true;
                }
            }
            if (!groupTypePermissionInfo.getEntityGroupIds().isEmpty()) {
                if (genericPartAdded) {
                    allowedGroupIdsSelect += " or ";
                }
                if (customOwnerId == null) {
                    allowedGroupIdsSelect += "id in (:where_group_ids)";
                } else {
                    allowedGroupIdsSelect += "(id in (:where_group_ids) and owner_id = :where_group_owner_id) ";
                    ctx.addUuidParameter("where_group_owner_id", customOwnerId.getId());
                }
                ctx.addUuidListParameter("where_group_ids",
                        groupTypePermissionInfo.getEntityGroupIds().stream()
                                .map(EntityGroupId::getId).collect(Collectors.toList()));
            }
            allowedGroupIdsSelect += ")";

            where = " WHERE id in (SELECT to_id from relation where from_id in " +
                    "(select id from entity_group where name=:where_group_name and type='" + entityType.name() + "' and " + allowedGroupIdsSelect + " limit 1)" +
                    " and from_type = '" + EntityType.ENTITY_GROUP.name() + "'" +
                    " and relation_type_group='" + RelationTypeGroup.FROM_ENTITY_GROUP + "' and relation_type='" + EntityRelation.CONTAINS_TYPE + "')";
            ctx.addStringParameter("where_group_name", entityFilter.getEntityGroupNameFilter());
        } else {
            where = " WHERE false";
        }

        return "( " + selectFields + where + ")";
    }

    private String entitySearchQuery(SqlQueryContext ctx, EntitySearchQueryFilter entityFilter, EntityType entityType, List<String> types) {
        EntityId rootId = entityFilter.getRootEntity();
        String lvlFilter = getLvlFilter(entityFilter.getMaxLevel());
        String selectFields = "SELECT tenant_id, customer_id, id, created_time, type, name, additional_info "
                + (entityType.equals(EntityType.ENTITY_VIEW) ? "" : ", label ")
                + "FROM " + entityType.name() + " WHERE id in ";
        String from = getQueryTemplate(entityFilter.getDirection(), false);
        String whereFilter = " WHERE";
        if (!StringUtils.isEmpty(entityFilter.getRelationType())) {
            ctx.addStringParameter("where_relation_type", entityFilter.getRelationType());
            whereFilter += " re.relation_type = :where_relation_type AND";
        }
        String toOrFrom = (entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "to" : "from");
        whereFilter += " re." + (entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "to" : "from") + "_type = :where_entity_type";
        if (entityFilter.isFetchLastLevelOnly()) {
            String fromOrTo = (entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "from" : "to");
            StringBuilder notExistsPart = new StringBuilder();
            notExistsPart.append(" NOT EXISTS (SELECT 1 from relation nr ")
                    .append(whereFilter.replaceAll("re\\.", "nr\\."))
                    .append(" and ")
                    .append("nr.").append(fromOrTo).append("_id").append(" = re.").append(toOrFrom).append("_id")
                    .append(" and ")
                    .append("nr.").append(fromOrTo).append("_type").append(" = re.").append(toOrFrom).append("_type");
            notExistsPart.append(" and nr.relation_type_group = 'COMMON'"); // hit the index, the same condition are on the recursive query
            notExistsPart.append(")");
            whereFilter += " and ( r_int.lvl = " + entityFilter.getMaxLevel() + " OR " + notExistsPart.toString() + ")";
        }
        from = String.format(from, lvlFilter, whereFilter);
        // The relation traversal recursion is fully over the reference "relation" table and is self-contained
        // (it references only relation_root_id/relation_root_type, bound below). Wrap it as the id-membership
        // subquery scoping the outer entity table.
        ctx.addUuidParameter("relation_root_id", rootId.getId());
        ctx.addStringParameter("relation_root_type", rootId.getEntityType().name());
        ctx.addStringParameter("where_entity_type", entityType.name());
        String idInClause;
        if (ctx.isCitusEnabled()) {
            // The outer "FROM <entityType>" is a distributed table; Citus rejects a WITH RECURSIVE embedded in a
            // distributed query. Pre-resolve the reference-only recursion standalone on the coordinator and bind the
            // ids as a single typed uuid[] parameter, splicing a "(select unnest(:resolved_search_ids))" subquery
            // (the same trick IN_CUSTOMER_IDS uses to dodge the pgjdbc 32767-bind cap) that routes fine on the
            // distributed table.
            idInClause = resolveReferenceRecursion(ctx, "SELECT entity_id" + from, "resolved_search_ids");
        } else {
            idInClause = "( SELECT entity_id" + from + ")";
        }
        String query = "( " + selectFields + idInClause;
        if (types != null && !types.isEmpty()) {
            query += " and type in (:relation_sub_types)";
            ctx.addStringListParameter("relation_sub_types", types);
        }
        query += " )";
        return query;
    }

    private String relationQuery(SqlQueryContext ctx, RelationsQueryFilter entityFilter) {
        EntityId rootId = entityFilter.getRootEntity();
        String lvlFilter = getLvlFilter(entityFilter.getMaxLevel());
        String selectFields = selectTenantId + ", " + selectCustomerId
                + ", " + selectCreatedTime + ", " +
                " entity.entity_id as id,"
                + selectType + ", " + selectName + ", " + selectLabel + ", " +
                SELECT_FIRST_NAME + ", " + SELECT_LAST_NAME + ", " + SELECT_EMAIL + ", " + SELECT_REGION + ", " +
                SELECT_TITLE + ", " + SELECT_COUNTRY + ", " + SELECT_STATE + ", " + SELECT_CITY + ", " +
                SELECT_ADDRESS + ", " + SELECT_ADDRESS_2 + ", " + SELECT_ZIP + ", " + SELECT_PHONE + ", " +
                selectAdditionalInfo + (entityFilter.isMultiRoot() ? (", " + SELECT_RELATED_PARENT_ID) : "") +
                ", entity.entity_type as entity_type";
        /*
        * FIXME:
        *  target entities are duplicated in result list, if search direction is TO and multiple relations are references to target entity
        * */
        if (entityFilter.isMultiRoot()) {
            ctx.addUuidListParameter("relation_root_ids", entityFilter.getMultiRootEntityIds().stream().map(UUID::fromString).collect(Collectors.toList()));
            ctx.addStringParameter("relation_root_type", entityFilter.getMultiRootEntitiesType().name());
        } else {
            ctx.addUuidParameter("relation_root_id", rootId.getId());
            ctx.addStringParameter("relation_root_type", rootId.getEntityType().name());
        }

        StringBuilder whereFilter = new StringBuilder();
        boolean noConditions = true;
        boolean single = entityFilter.getFilters() != null && entityFilter.getFilters().size() == 1;
        if (entityFilter.getFilters() != null && !entityFilter.getFilters().isEmpty()) {
            int entityTypeFilterIdx = 0;
            for (RelationEntityTypeFilter etf : entityFilter.getFilters()) {
                String etfCondition = buildEtfCondition(ctx, etf, entityFilter.getDirection(), entityTypeFilterIdx++);
                if (!etfCondition.isEmpty()) {
                    if (noConditions) {
                        noConditions = false;
                    } else {
                        whereFilter.append(" OR ");
                    }
                    if (!single) {
                        whereFilter.append(" (");
                    }
                    whereFilter.append(etfCondition);
                    if (!single) {
                        whereFilter.append(" )");
                    }
                }
            }
        }
        if (noConditions) {
            whereFilter.append(" re.")
                    .append(entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "to" : "from")
                    .append("_type in (:where_entity_types").append(")");
            ctx.addStringListParameter("where_entity_types", Arrays.stream(RELATION_QUERY_ENTITY_TYPES).map(EntityType::name).collect(Collectors.toList()));
        } else {
            if (!single) {
                whereFilter = new StringBuilder()
                        .append(entityFilter.isNegate() ? " NOT (" : "(")
                        .append(whereFilter).append(")");
            } else if (entityFilter.isNegate()) {
                whereFilter = new StringBuilder()
                        .append(" NOT (")
                        .append(whereFilter).append(")");
            }
        }

        if (entityFilter.isFetchLastLevelOnly()) {
            String toOrFrom = (entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "to" : "from");
            String fromOrTo = (entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "from" : "to");

            StringBuilder notExistsPart = new StringBuilder();
            notExistsPart.append(" NOT EXISTS (SELECT 1 from relation nr WHERE ");
            notExistsPart
                    .append("nr.").append(fromOrTo).append("_id").append(" = re.").append(toOrFrom).append("_id")
                    .append(" and ")
                    .append("nr.").append(fromOrTo).append("_type").append(" = re.").append(toOrFrom).append("_type")
                    .append(" and ")
                    .append(whereFilter.toString().replaceAll("re\\.", "nr\\."));
            notExistsPart.append(" and nr.relation_type_group = 'COMMON'"); // hit the index, the same condition are on the recursive query
            notExistsPart.append(")");
            whereFilter.append(" and ( r_int.lvl = ").append(entityFilter.getMaxLevel()).append(" OR ").append(notExistsPart.toString()).append(")");
        }
        String from;
        if (ctx.isCitusEnabled()) {
            // The relation traversal is a WITH RECURSIVE over the reference "relation" table, but it is embedded in
            // the distributed entity-data query (the latest-value joins touch distributed tables), which Citus
            // rejects. Pre-resolve the traversal standalone on the coordinator and inject the resulting
            // (entity_id, entity_type, lvl[, parent_id]) tuples as an unnest(...) derived relation. The device/asset/
            // entity_view selection arms are correlated subqueries into those DISTRIBUTED tables, which Citus also
            // rejects from the coordinator-local relation, so the selection arms already emit entity.res_<col>
            // directly (see initSelectArms / distArm) and resolveRelationTraversal pre-fetches those columns into
            // matching res_<col> arrays of the unnest relation.
            from = resolveRelationTraversal(ctx, entityFilter.getDirection(), entityFilter.isMultiRoot(),
                    lvlFilter, whereFilter.toString());
        } else {
            from = buildRelationTraversalFrom(entityFilter.getDirection(), entityFilter.isMultiRoot(), lvlFilter, whereFilter.toString());
        }
        return "( " + selectFields + from + ")";
    }

    /**
     * Builds the relation-traversal FROM clause (the {@code FROM (WITH RECURSIVE ... ) entity} fragment) for plain
     * PostgreSQL: the per-direction template with the level filter and WHERE filter spliced in. Output is identical
     * to the previous {@code String.format(getQueryTemplate(...), lvlFilter, " WHERE " + whereFilter)}.
     */
    private String buildRelationTraversalFrom(EntitySearchDirection direction, boolean multiRoot, String lvlFilter, String whereFilter) {
        return String.format(getQueryTemplate(direction, multiRoot), lvlFilter, " WHERE " + whereFilter);
    }

    /**
     * Builds the bare relation-traversal SQL (the inner {@code WITH RECURSIVE ...} SELECT) without the
     * {@code FROM (...) entity} wrapper, so it can be executed standalone. Constructed directly from the per-direction
     * template by stripping the fixed wrapper bookends — not reverse-engineered from an assembled FROM clause.
     */
    private String buildStandaloneRelationTraversalSql(EntitySearchDirection direction, boolean multiRoot, String lvlFilter, String whereFilter) {
        String wrapped = buildRelationTraversalFrom(direction, multiRoot, lvlFilter, whereFilter);
        return wrapped.substring(RELATION_TRAVERSAL_FROM_PREFIX.length(),
                wrapped.length() - RELATION_TRAVERSAL_FROM_SUFFIX.length());
    }

    /**
     * Citus-only. Runs the recursive relation traversal standalone on the coordinator (it touches only the reference
     * {@code relation} table) using the params already bound on {@code ctx}, then returns a replacement FROM clause
     * exposing the resolved {@code (entity_id, entity_type, lvl[, parent_id])} tuples — plus pre-fetched selection
     * columns for any DISTRIBUTED-table rows (device / asset / entity_view) — as an {@code unnest(...) AS entity(...)}
     * derived relation.
     * <p>
     * Embedding the recursion or a correlated subquery into a distributed table inside the distributed entity-data
     * query crashes the Citus worker connection, so both the traversal and the distributed selection columns are
     * resolved in Java here. The resolved columns are bound as typed SQL array parameters (NOT inlined as literals),
     * so the emitted SQL stays small and needs no value escaping. The selection arms read {@code entity.res_<col>}
     * (see initSelectArms / distArm). An empty result yields an empty unnest (zero rows), matching the recursive
     * query returning nothing.
     */
    private String resolveRelationTraversal(SqlQueryContext ctx, EntitySearchDirection direction, boolean multiRoot,
                                            String lvlFilter, String whereFilter) {
        // NON-EDQS FALLBACK. Relation-hierarchy entity-data queries are EDQS-valid (BaseEntityService.validForEdqs
        // excludes only StateEntityOwnerFilter over ALARM), so for non-sys tenants with EDQS enabled they are served
        // from the EDQS in-memory store and never reach this SQL path. This method is the fallback when EDQS is off.
        //
        // It performs a coordinator-heap materialization before any paging: the full recursive traversal result (every
        // reachable entity, not just the requested page) is pulled into the `rows` list, plus three prefetch sets
        // (device / asset / entity_view selection columns) are loaded into `resolved`, then the whole thing is exploded
        // into per-column arrays. For a pathological subtree (deep/wide hierarchy) this is proportional to the entire
        // reachable set, paid in full on the coordinator regardless of page size. There is NO paging redesign here. The
        // defensive maxResolvedEntities cap enforced below runs AFTER queryForList has already materialized the full
        // traversal, so it does NOT protect the resolution query's own heap use; it fails loud (naming the property)
        // instead of silently feeding an oversized resolved-entity set into the downstream array binds / pushdown,
        // bounding only what proceeds past resolution.
        String standaloneSql = buildStandaloneRelationTraversalSql(direction, multiRoot, lvlFilter, whereFilter);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(standaloneSql, ctx);
        checkResolvedEntityCap(rows.size());

        Map<UUID, Map<String, Object>> resolved = new HashMap<>();
        prefetchDistributedRows(ctx, rows, "DEVICE", "device", true, resolved);
        prefetchDistributedRows(ctx, rows, "ASSET", "asset", true, resolved);
        prefetchDistributedRows(ctx, rows, "ENTITY_VIEW", "entity_view", false, resolved);

        // Single source of truth for the pre-fetched distributed-table selection columns: each descriptor pairs the
        // emitted res_<col> column name + its pg array type + the extractor reading the value out of the resolved row
        // Map. This list drives BOTH the array-building loop and the unnest/alias emission below, so the columns stay
        // in lockstep without hand-synchronizing parallel array declarations against the unnest() args against the
        // AS entity(...) alias list. The order here defines the emitted column order (must stay stable).
        List<ResolvedColumn> resCols = RESOLVED_COLUMNS;

        int n = rows.size();
        UUID[] ids = new UUID[n];
        String[] types = new String[n];
        Integer[] lvls = new Integer[n];
        UUID[] parents = new UUID[n];
        Object[][] resValues = new Object[resCols.size()][n];
        for (int i = 0; i < n; i++) {
            Map<String, Object> row = rows.get(i);
            ids[i] = (UUID) row.get("entity_id");
            types[i] = (String) row.get("entity_type");
            lvls[i] = ((Number) row.get("lvl")).intValue();
            if (multiRoot) {
                parents[i] = (UUID) row.get("parent_id");
            }
            Map<String, Object> e = resolved.get(row.get("entity_id"));
            if (e != null) {
                for (int c = 0; c < resCols.size(); c++) {
                    resValues[c][i] = resCols.get(c).extractor().apply(e);
                }
            }
        }

        // Bind every column as a typed SQL array parameter and unnest them in lockstep into the entity relation.
        // All arrays are created on a single short-lived connection (the PG driver's array objects remain usable
        // after the connection is released).
        DataSource dataSource = jdbcTemplate.getJdbcTemplate().getDataSource();
        Connection connection = DataSourceUtils.getConnection(dataSource);
        StringBuilder unnest = new StringBuilder(" FROM unnest(");
        try {
            unnest.append(bindArray(ctx, connection, "uuid", ids)).append(", ")
                    .append(bindArray(ctx, connection, "varchar", types)).append(", ")
                    .append(bindArray(ctx, connection, "int4", lvls));
            if (multiRoot) {
                unnest.append(", ").append(bindArray(ctx, connection, "uuid", parents));
            }
            for (int c = 0; c < resCols.size(); c++) {
                unnest.append(", ").append(bindArray(ctx, connection, resCols.get(c).pgType(), resValues[c]));
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
        unnest.append(") AS entity(entity_id, entity_type, lvl");
        if (multiRoot) {
            unnest.append(", parent_id");
        }
        for (ResolvedColumn col : resCols) {
            unnest.append(", ").append(col.name());
        }
        unnest.append(")");
        return unnest.toString();
    }

    /**
     * Descriptor for one pre-fetched distributed-table selection column injected into the Citus relation-traversal
     * {@code unnest(...) AS entity(...)} relation: the emitted {@code res_<col>} column {@code name}, its PostgreSQL
     * array element type {@code pgType}, and the {@code extractor} that pulls the value from a resolved row Map (keyed
     * by the raw entity-table column). Drives the array-building loop, the array binding, and the alias list together.
     */
    private record ResolvedColumn(String name, String pgType, Function<Map<String, Object>, Object> extractor) {
    }

    /**
     * The pre-fetched distributed-table selection columns, in emitted order. The {@code res_<col>} names must match the
     * {@link #distArm} output (e.g. {@code entity.res_tenant_id}); changing this list changes the generated SQL.
     */
    private static final List<ResolvedColumn> RESOLVED_COLUMNS = List.of(
            new ResolvedColumn("res_tenant_id", "uuid", e -> e.get("tenant_id")),
            new ResolvedColumn("res_customer_id", "uuid", e -> e.get("customer_id")),
            new ResolvedColumn("res_created_time", "int8", e -> {
                Object createdTime = e.get("created_time");
                return createdTime == null ? null : ((Number) createdTime).longValue();
            }),
            new ResolvedColumn("res_name", "varchar", e -> e.get("name")),
            new ResolvedColumn("res_type", "varchar", e -> e.get("type")),
            new ResolvedColumn("res_label", "varchar", e -> e.get("label")),
            new ResolvedColumn("res_additional_info", "varchar", e -> e.get("additional_info")));

    /**
     * Creates a typed SQL array from {@code elements} on {@code connection}, binds it on {@code ctx} under a
     * freshly-minted parameter name, and returns the {@code :param} placeholder to splice into the {@code unnest(...)}
     * call.
     */
    private String bindArray(SqlQueryContext ctx, Connection connection, String pgType, Object[] elements) {
        return bindArray(ctx, connection, "relation_res_arr", pgType, elements);
    }

    private String bindArray(SqlQueryContext ctx, Connection connection, String paramPrefix, String pgType, Object[] elements) {
        String paramName = ctx.nextResolvedParamName(paramPrefix);
        try {
            ctx.addArrayParameter(paramName, connection.createArrayOf(pgType, elements));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to bind " + pgType + "[] array parameter", e);
        }
        return ":" + paramName;
    }

    /**
     * Pre-fetches the selection columns for all {@code entityType} rows in {@code rows} from the distributed
     * {@code table} (a plain standalone SELECT the coordinator runs without touching workers from a local context),
     * keyed by id into {@code out}. {@code hasLabel} is false for {@code entity_view} (no {@code label} column) — its
     * label is selected as NULL and the entity_view label arm instead maps to {@code res_name}.
     */
    private void prefetchDistributedRows(SqlQueryContext ctx, List<Map<String, Object>> rows, String entityType,
                                         String table, boolean hasLabel, Map<UUID, Map<String, Object>> out) {
        List<UUID> entityIds = rows.stream()
                .filter(row -> entityType.equals(row.get("entity_type")))
                .map(row -> (UUID) row.get("entity_id"))
                .collect(Collectors.toList());
        if (entityIds.isEmpty()) {
            return;
        }
        SqlQueryContext prefetchCtx = new SqlQueryContext(ctx.getSecurityCtx(), citusEnabled);
        // Bind the ids as a SINGLE typed uuid[] parameter and match via "id in (select unnest(:param))" rather than an
        // "in (:list)" bind: pgjdbc expands an in-list to one placeholder per element (hard-capped at 32767 total
        // binds), so a large traversal result would overflow it — an array is one placeholder regardless of size.
        DataSource dataSource = jdbcTemplate.getJdbcTemplate().getDataSource();
        Connection connection = DataSourceUtils.getConnection(dataSource);
        String idsParam;
        try {
            idsParam = bindArray(prefetchCtx, connection, "relation_prefetch_ids", "uuid", entityIds.toArray(new UUID[0]));
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
        String labelColumn = hasLabel ? "label" : "null as label";
        String sql = "select id, tenant_id, customer_id, created_time, name, type, " + labelColumn +
                ", additional_info from " + table + " where id in (select unnest(" + idsParam + "))";
        for (Map<String, Object> row : jdbcTemplate.queryForList(sql, prefetchCtx)) {
            out.put((UUID) row.get("id"), row);
        }
    }

    private String buildEtfCondition(SqlQueryContext ctx, RelationEntityTypeFilter etf, EntitySearchDirection direction, int entityTypeFilterIdx) {
        StringBuilder whereFilter = new StringBuilder();
        String relationType = etf.getRelationType();
        List<EntityType> entityTypes = etf.getEntityTypes();
        List<String> whereEntityTypes;
        if (entityTypes == null || entityTypes.isEmpty()) {
            whereEntityTypes = Collections.emptyList();
        } else {
            whereEntityTypes = etf.getEntityTypes().stream().map(EntityType::name).collect(Collectors.toList());
        }
        boolean hasRelationType = !StringUtils.isEmpty(relationType);
        if (hasRelationType) {
            ctx.addStringParameter("where_relation_type" + entityTypeFilterIdx, relationType);
            if (etf.isNegate()) {
                whereFilter.append("re.relation_type != :where_relation_type");
            } else {
                whereFilter.append("re.relation_type = :where_relation_type");
            }
            whereFilter.append(entityTypeFilterIdx).append(" and ");
        }

        whereFilter.append("re.")
                .append(direction.equals(EntitySearchDirection.FROM) ? "to" : "from")
                .append("_type in (:where_entity_types").append(entityTypeFilterIdx).append(")");
        if (!whereEntityTypes.isEmpty()) {
            ctx.addStringListParameter("where_entity_types" + entityTypeFilterIdx, whereEntityTypes);
        } else {
            ctx.addStringListParameter("where_entity_types" + entityTypeFilterIdx,
                    Arrays.stream(RELATION_QUERY_ENTITY_TYPES).map(EntityType::name).collect(Collectors.toList()));
        }
        return whereFilter.toString();
    }

    String getLvlFilter(int maxLevel) {
        return "and re.lvl <= " + (getMaxLevel(maxLevel) - 1);
    }

    int getMaxLevel(int maxLevel) {
        return (maxLevel <= 0 || maxLevel > this.maxLevelAllowed) ? this.maxLevelAllowed : maxLevel;
    }

    private String getQueryTemplate(EntitySearchDirection direction, boolean isMultiRoot) {
        String from;
        if (direction.equals(EntitySearchDirection.FROM)) {
            from = isMultiRoot ? HIERARCHICAL_FROM_MR_QUERY_TEMPLATE : HIERARCHICAL_FROM_QUERY_TEMPLATE;
        } else {
            from = isMultiRoot ? HIERARCHICAL_TO_MR_QUERY_TEMPLATE : HIERARCHICAL_TO_QUERY_TEMPLATE;
        }
        return from;
    }

    private String buildAliasWhereQuery(SqlQueryContext ctx, EntityFilter entityFilter, List<EntityKeyMapping> selectionMapping, String searchText) {
        List<EntityKeyMapping> aliasFiltersMapping = selectionMapping.stream().filter(mapping -> !mapping.isLatest() && mapping.getEntityKeyColumn() == null)
                .collect(Collectors.toList());
        String entityFieldsQuery = EntityKeyMapping.buildQuery(ctx, aliasFiltersMapping, entityFilter.getType());
        String searchTextQuery = buildTextSearchQuery(ctx, selectionMapping, searchText);
        String result = "";
        if (!entityFieldsQuery.isEmpty()) {
            result += " where (" + entityFieldsQuery + ")";
        }
        if (!searchTextQuery.isEmpty()) {
            result += (result.isEmpty() ? " where ": " and ") + "(" + searchTextQuery + ") ";
        }
        return result;
    }

    private String buildTextSearchQuery(SqlQueryContext ctx, List<EntityKeyMapping> selectionMapping, String searchText) {
        if (!StringUtils.isEmpty(searchText) && !selectionMapping.isEmpty()) {
            String sqlSearchText = "%" + searchText + "%";
            ctx.addStringParameter("lowerSearchTextParam", sqlSearchText);
            List<String> searchAliases = selectionMapping.stream().filter(EntityKeyMapping::isSearchable).map(EntityKeyMapping::getValueAlias).collect(Collectors.toList());
            String searchAliasesExpression;
            if (searchAliases.size() > 1) {
                searchAliasesExpression = "CONCAT(" + String.join(" , ", searchAliases) + ")";
            } else {
                searchAliasesExpression = searchAliases.get(0);
            }
            return String.format(" %s ILIKE :%s", searchAliasesExpression, "lowerSearchTextParam");
        } else {
            return "";
        }
    }

    private String singleEntityByStateOwner(SqlQueryContext ctx) {
        ctx.addUuidParameter("entity_filter_single_owner_id", ctx.getStateEntityOwnerId().getId());
        return "e.id=:entity_filter_single_owner_id";
    }

    private String singleEntityQuery(SqlQueryContext ctx, SingleEntityFilter filter) {
        ctx.addUuidParameter("entity_filter_single_entity_id", filter.getSingleEntity().getId());
        return "e.id=:entity_filter_single_entity_id";
    }

    private String entityListQuery(SqlQueryContext ctx, EntityListFilter filter) {
        ctx.addUuidListParameter("entity_filter_entity_ids", filter.getEntityList().stream().map(UUID::fromString).collect(Collectors.toList()));
        return "e.id in (:entity_filter_entity_ids)";
    }

    private String entityNameQuery(SqlQueryContext ctx, EntityNameFilter filter) {
        ctx.addStringParameter("entity_filter_name_filter", filter.getEntityNameFilter());
        String nameColumn = getNameColumn(filter.getEntityType());
        if (filter.getEntityNameFilter().startsWith("%") || filter.getEntityNameFilter().endsWith("%")) {
            return String.format("e.%s ILIKE :entity_filter_name_filter", nameColumn);
        }

        return String.format("e.%s ILIKE concat(:entity_filter_name_filter, '%%')", nameColumn);
    }

    private String entityGroupNameQuery(SqlQueryContext ctx, EntityGroupNameFilter filter) {
        ctx.addStringParameter("entity_filter_group_name_filter", filter.getEntityGroupNameFilter());
        if (filter.getEntityGroupNameFilter().startsWith("%") || filter.getEntityGroupNameFilter().endsWith("%")) {
            return "e.name ILIKE :entity_filter_group_name_filter";
        }
        return "e.name ILIKE concat(:entity_filter_group_name_filter, '%%')";
    }

    private String typeQuery(SqlQueryContext ctx, EntityFilter filter) {
        List<String> types;
        String name;
        String nameColumn;
        switch (filter.getType()) {
            case ASSET_TYPE:
                types = ((AssetTypeFilter) filter).getAssetTypes();
                name = ((AssetTypeFilter) filter).getAssetNameFilter();
                nameColumn = getNameColumn(EntityType.ASSET);
                break;
            case DEVICE_TYPE:
                types = ((DeviceTypeFilter) filter).getDeviceTypes();
                name = ((DeviceTypeFilter) filter).getDeviceNameFilter();
                nameColumn = getNameColumn(EntityType.DEVICE);
                break;
            case ENTITY_VIEW_TYPE:
                types = ((EntityViewTypeFilter) filter).getEntityViewTypes();
                name = ((EntityViewTypeFilter) filter).getEntityViewNameFilter();
                nameColumn = getNameColumn(EntityType.ENTITY_VIEW);
                break;
            case EDGE_TYPE:
                types = ((EdgeTypeFilter) filter).getEdgeTypes();
                name = ((EdgeTypeFilter) filter).getEdgeNameFilter();
                nameColumn = getNameColumn(EntityType.EDGE);
                break;
            default:
                throw new RuntimeException("Not supported!");
        }
        String typesFilter = "e.type in (:entity_filter_type_query_types)";
        ctx.addStringListParameter("entity_filter_type_query_types", types);
        if (!StringUtils.isEmpty(name)) {
            ctx.addStringParameter("entity_filter_type_query_name", name);
            if (name.startsWith("%") || name.endsWith("%")) {
                return typesFilter + " and e." + nameColumn + " ILIKE :entity_filter_type_query_name";
            }
            return typesFilter + " and e." + nameColumn + " ILIKE concat(:entity_filter_type_query_name, '%%')";
        } else {
            return typesFilter;
        }
    }

    public static String getNameColumn(EntityType entityType) {
        String nameColumn = entityNameColumns.get(entityType);
        if (nameColumn == null) {
            log.error("Name column is not defined in the entityNameColumns map for entity type {}.", entityType);
            throw new RuntimeException("Name column is not defined for entity type: " + entityType);
        }
        return nameColumn;
    }

    /**
     * True when the entity-data query builds its FROM from a single entity table (so its anchor table exists on
     * every worker — distributed device or a reference dimension table — and the latest-value join can push down
     * to the workers). False for RELATIONS_QUERY (polymorphic (to_id, to_type) output) and the entity-group paths
     * (group-membership join), which keep the coordinator-join form.
     */
    public static boolean isSingleEntityTableFilter(EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case SINGLE_ENTITY:
            case ENTITY_LIST:
            case ENTITY_NAME:
            case ENTITY_TYPE:
            case DEVICE_TYPE:
            case ASSET_TYPE:
            case ENTITY_VIEW_TYPE:
            case EDGE_TYPE:
            case DEVICE_SEARCH_QUERY:
            case ASSET_SEARCH_QUERY:
            case ENTITY_VIEW_SEARCH_QUERY:
            case EDGE_SEARCH_QUERY:
                return true;
            default:
                // RELATIONS_QUERY, ENTITY_GROUP*, ENTITIES_BY_GROUP_NAME, STATE_ENTITY*, API_USAGE_STATE, SCHEDULER_EVENT, ...
                return false;
        }
    }

    public static EntityType resolveEntityType(EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case SINGLE_ENTITY:
                return ((SingleEntityFilter) entityFilter).getSingleEntity().getEntityType();
            case ENTITY_GROUP:
                return ((EntityGroupFilter) entityFilter).getGroupType();
            case ENTITY_LIST:
                return ((EntityListFilter) entityFilter).getEntityType();
            case ENTITY_NAME:
                return ((EntityNameFilter) entityFilter).getEntityType();
            case ENTITY_TYPE:
                return ((EntityTypeFilter) entityFilter).getEntityType();
            case ENTITY_GROUP_LIST:
            case ENTITY_GROUP_NAME:
                return EntityType.ENTITY_GROUP;
            case ENTITIES_BY_GROUP_NAME:
                return ((EntitiesByGroupNameFilter) entityFilter).getGroupType();
            case STATE_ENTITY_OWNER:
                throw new RuntimeException("TODO: Not implemented!");
            case ASSET_TYPE:
            case ASSET_SEARCH_QUERY:
                return EntityType.ASSET;
            case DEVICE_TYPE:
            case DEVICE_SEARCH_QUERY:
                return EntityType.DEVICE;
            case ENTITY_VIEW_TYPE:
            case ENTITY_VIEW_SEARCH_QUERY:
                return EntityType.ENTITY_VIEW;
            case EDGE_TYPE:
            case EDGE_SEARCH_QUERY:
                return EntityType.EDGE;
            case RELATIONS_QUERY:
                RelationsQueryFilter rgf = (RelationsQueryFilter) entityFilter;
                return rgf.isMultiRoot() ? rgf.getMultiRootEntitiesType() : rgf.getRootEntity().getEntityType();
            case API_USAGE_STATE:
                return EntityType.API_USAGE_STATE;
            case SCHEDULER_EVENT:
                return EntityType.SCHEDULER_EVENT;
            default:
                throw new RuntimeException("Not implemented!");
        }
    }
}
