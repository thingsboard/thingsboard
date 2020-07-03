/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.query;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SqlDao
@Repository
@Slf4j
public class DefaultAlarmQueryRepository implements AlarmQueryRepository {

    private static final Map<String, String> alarmFieldColumnMap = new HashMap<>();

    static {
        alarmFieldColumnMap.put("createdTime", ModelConstants.CREATED_TIME_PROPERTY);
        alarmFieldColumnMap.put("ackTs", ModelConstants.ALARM_ACK_TS_PROPERTY);
        alarmFieldColumnMap.put("clearTs", ModelConstants.ALARM_CLEAR_TS_PROPERTY);
        alarmFieldColumnMap.put("details", ModelConstants.ADDITIONAL_INFO_PROPERTY);
        alarmFieldColumnMap.put("endTs", ModelConstants.ALARM_END_TS_PROPERTY);
        alarmFieldColumnMap.put("startTs", ModelConstants.ALARM_START_TS_PROPERTY);
        alarmFieldColumnMap.put("status", ModelConstants.ALARM_STATUS_PROPERTY);
        alarmFieldColumnMap.put("type", ModelConstants.ALARM_TYPE_PROPERTY);
        alarmFieldColumnMap.put("severity", ModelConstants.ALARM_SEVERITY_PROPERTY);
        alarmFieldColumnMap.put("originator_id", ModelConstants.ALARM_ORIGINATOR_ID_PROPERTY);
        alarmFieldColumnMap.put("originator_type", ModelConstants.ALARM_ORIGINATOR_TYPE_PROPERTY);
    }

    public static final String SELECT_ORIGINATOR_NAME = " CASE" +
            " WHEN a.originator_type = "+ EntityType.TENANT.ordinal() +
            " THEN (select title from tenant where id = a.originator_id)" +
            " WHEN a.originator_type = "+ EntityType.CUSTOMER.ordinal() +
            " THEN (select title from customer where id = a.originator_id)" +
            " WHEN a.originator_type = " + EntityType.USER.ordinal() +
            " THEN (select CONCAT (first_name, ' ', last_name) from tb_user where id = a.originator_id)" +
            " WHEN a.originator_type = " + EntityType.DASHBOARD.ordinal() +
            " THEN (select title from dashboard where id = a.originator_id)" +
            " WHEN a.originator_type = " + EntityType.ASSET.ordinal() +
            " THEN (select name from asset where id = a.originator_id)" +
            " WHEN a.originator_type = " + EntityType.DEVICE.ordinal() +
            " THEN (select name from device where id = a.originator_id)" +
            " WHEN a.originator_type = " + EntityType.ENTITY_VIEW.ordinal() +
            " THEN (select name from entity_view where id = a.originator_id)" +
            " END as originator_name";

    public static final String FIELDS_SELECTION = "select a.id as id," +
            " a.created_time as created_time," +
            " a.ack_ts as ack_ts," +
            " a.clear_ts as clear_ts," +
            " a.additional_info as additional_info," +
            " a.end_ts as end_ts," +
            " a.originator_id as originator_id," +
            " a.originator_type as originator_type," +
            " a.propagate as propagate," +
            " a.severity as severity," +
            " a.start_ts as start_ts," +
            " a.status as status, " +
            " a.tenant_id as tenant_id, " +
            " a.propagate_relation_types as propagate_relation_types, " +
            " a.type as type," + SELECT_ORIGINATOR_NAME + ", ";

    public static final String JOIN_RELATIONS = "left join relation r on r.relation_type_group = 'ALARM' and r.relation_type = 'ALARM_ANY' and a.id = r.to_id";

    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;


    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, CustomerId customerId,
                                                               AlarmDataPageLink pageLink, Collection<EntityId> orderedEntityIds) {
        QueryContext ctx = new QueryContext();

        StringBuilder selectPart = new StringBuilder(FIELDS_SELECTION);
        StringBuilder fromPart = new StringBuilder(" from alarm a ");
        StringBuilder wherePart = new StringBuilder(" where ");
        StringBuilder sortPart = new StringBuilder(" order by ");
        boolean addAnd = false;
        if (pageLink.isSearchPropagatedAlarms()) {
            selectPart.append(" r.from_id as entity_id ");
            fromPart.append(JOIN_RELATIONS);
            wherePart.append(buildPermissionsQuery(tenantId, customerId, ctx));
            addAnd = true;
        } else {
            selectPart.append(" a.originator_id as entity_id ");
            //No need to check permissions if we select by originator.
        }
        EntityDataSortOrder sortOrder = pageLink.getSortOrder();
        if (sortOrder != null && sortOrder.getKey().getType().equals(EntityKeyType.ALARM_FIELD)) {
            String sortOrderKey = sortOrder.getKey().getKey();
            sortPart.append("a.").append(alarmFieldColumnMap.getOrDefault(sortOrderKey, sortOrderKey))
                    .append(" ").append(sortOrder.getDirection().name());
            ctx.addUuidListParameter("entity_ids", orderedEntityIds.stream().map(EntityId::getId).collect(Collectors.toList()));
            if (pageLink.isSearchPropagatedAlarms()) {
                fromPart.append(" and r.from_id in (:entity_ids)");
            } else {
                addAndIfNeeded(wherePart, addAnd);
                addAnd = true;
                wherePart.append(" a.originator_id in (:entity_ids)");
            }
        } else {
            fromPart.append(" left join (select * from (VALUES");
            int entityIdIdx = 0;
            int lastEntityIdIdx = orderedEntityIds.size() - 1;
            for (EntityId entityId : orderedEntityIds) {
                fromPart.append("(uuid('").append(entityId.getId().toString()).append("'), ").append(entityIdIdx).append(")");
                if (entityIdIdx != lastEntityIdIdx) {
                    fromPart.append(",");
                } else {
                    fromPart.append(")");
                }
                entityIdIdx++;
            }
            fromPart.append(" as e(id, priority)) e ");
            if (pageLink.isSearchPropagatedAlarms()) {
                fromPart.append("on r.from_id = e.id");
            } else {
                fromPart.append("on a.originator_id = e.id");
            }
            sortPart.append("e.priority");
        }

        if (pageLink.getStartTs() > 0) {
            addAndIfNeeded(wherePart, addAnd);
            addAnd = true;
            ctx.addLongParameter("startTime", pageLink.getStartTs());
            wherePart.append("a.created_time >= :startTime");
        }

        if (pageLink.getEndTs() > 0) {
            addAndIfNeeded(wherePart, addAnd);
            addAnd = true;
            ctx.addLongParameter("endTime", pageLink.getEndTs());
            wherePart.append("a.created_time <= :endTime");
        }

        if (pageLink.getTypeList() != null && !pageLink.getTypeList().isEmpty()) {
            addAndIfNeeded(wherePart, addAnd);
            addAnd = true;
            ctx.addStringListParameter("alarmTypes", pageLink.getTypeList());
            wherePart.append("a.type in (:alarmTypes)");
        }

        if (pageLink.getSeverityList() != null && !pageLink.getSeverityList().isEmpty()) {
            addAndIfNeeded(wherePart, addAnd);
            addAnd = true;
            ctx.addStringListParameter("alarmSeverities", pageLink.getSeverityList().stream().map(AlarmSeverity::name).collect(Collectors.toList()));
            wherePart.append("a.severity in (:alarmSeverities)");
        }

        if (pageLink.getStatusList() != null && !pageLink.getStatusList().isEmpty()) {
            Set<AlarmStatus> statusSet = toStatusSet(pageLink.getStatusList());
            if (!statusSet.isEmpty()) {
                addAndIfNeeded(wherePart, addAnd);
                addAnd = true;
                ctx.addStringListParameter("alarmStatuses", statusSet.stream().map(AlarmStatus::name).collect(Collectors.toList()));
                wherePart.append(" a.status in (:alarmStatuses)");
            }
        }

        String countQuery = fromPart.toString() + wherePart.toString();
        int totalElements = jdbcTemplate.queryForObject(String.format("select count(*) %s", countQuery), ctx, Integer.class);

        String dataQuery = selectPart.toString() + countQuery + sortPart;

        int startIndex = pageLink.getPageSize() * pageLink.getPage();
        if (pageLink.getPageSize() > 0) {
            dataQuery = String.format("%s limit %s offset %s", dataQuery, pageLink.getPageSize(), startIndex);
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(dataQuery, ctx);
        return AlarmDataAdapter.createAlarmData(pageLink, rows, totalElements);
    }

    private String buildPermissionsQuery(TenantId tenantId, CustomerId customerId, QueryContext ctx) {
        StringBuilder permissionsQuery = new StringBuilder();
        ctx.addUuidParameter("permissions_tenant_id", tenantId.getId());
        permissionsQuery.append(" a.tenant_id = :permissions_tenant_id ");
        if (customerId != null && !customerId.isNullUid()) {
            ctx.addUuidParameter("permissions_customer_id", customerId.getId());
            ctx.addUuidParameter("permissions_device_customer_id", customerId.getId());
            ctx.addUuidParameter("permissions_asset_customer_id", customerId.getId());
            ctx.addUuidParameter("permissions_user_customer_id", customerId.getId());
            ctx.addUuidParameter("permissions_entity_view_customer_id", customerId.getId());
            permissionsQuery.append(" and (");
            permissionsQuery.append("(a.originator_type = '").append(EntityType.DEVICE.ordinal()).append("' and exists (select 1 from device cd where cd.id = a.originator_id and cd.customer_id = :permissions_device_customer_id))");
            permissionsQuery.append(" or ");
            permissionsQuery.append("(a.originator_type = '").append(EntityType.ASSET.ordinal()).append("' and exists (select 1 from asset ca where ca.id = a.originator_id and ca.customer_id = :permissions_device_customer_id))");
            permissionsQuery.append(" or ");
            permissionsQuery.append("(a.originator_type = '").append(EntityType.CUSTOMER.ordinal()).append("' and exists (select 1 from customer cc where cc.id = a.originator_id and cc.id = :permissions_customer_id))");
            permissionsQuery.append(" or ");
            permissionsQuery.append("(a.originator_type = '").append(EntityType.USER.ordinal()).append("' and exists (select 1 from tb_user cu where cu.id = a.originator_id and cu.customer_id = :permissions_user_customer_id))");
            permissionsQuery.append(" or ");
            permissionsQuery.append("(a.originator_type = '").append(EntityType.ENTITY_VIEW.ordinal()).append("' and exists (select 1 from entity_view cv where cv.id = a.originator_id and cv.customer_id = :permissions_entity_view_customer_id))");
            permissionsQuery.append(")");
        }
        return permissionsQuery.toString();
    }

    private Set<AlarmStatus> toStatusSet(List<AlarmSearchStatus> statusList) {
        Set<AlarmStatus> result = new HashSet<>();
        for (AlarmSearchStatus searchStatus : statusList) {
            switch (searchStatus) {
                case ACK:
                    result.add(AlarmStatus.ACTIVE_ACK);
                    result.add(AlarmStatus.CLEARED_ACK);
                    break;
                case UNACK:
                    result.add(AlarmStatus.ACTIVE_UNACK);
                    result.add(AlarmStatus.CLEARED_UNACK);
                    break;
                case CLEARED:
                    result.add(AlarmStatus.CLEARED_ACK);
                    result.add(AlarmStatus.CLEARED_UNACK);
                    break;
                case ACTIVE:
                    result.add(AlarmStatus.ACTIVE_ACK);
                    result.add(AlarmStatus.ACTIVE_UNACK);
                    break;
                default:
                    break;
            }
            if (searchStatus == AlarmSearchStatus.ANY || result.size() == AlarmStatus.values().length) {
                result.clear();
                return result;
            }
        }
        return result;
    }

    private void addAndIfNeeded(StringBuilder wherePart, boolean addAnd) {
        if (addAnd) {
            wherePart.append(" and ");
        }
    }
}
