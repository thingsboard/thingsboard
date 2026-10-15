// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util;

import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.EntitiesByGroupNameFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityGroupFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntitySearchQueryFilter;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.SchedulerEventFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.StateEntityFilter;
import org.thingsboard.server.common.data.query.StateEntityOwnerFilter;
import org.thingsboard.server.common.data.report.configuration.AlarmFilterConfig;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.DataSourceType;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.report.context.TbReportCtx;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.thingsboard.server.common.data.query.AliasEntityId.resolveAliasEntityId;
import static org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator.getTimeRange;
import static org.thingsboard.server.common.data.util.DataSourceUtils.setEntityKeyIfNotExists;

public class ReportQueryUtils {

    public static final EntityDataSortOrder DEFAULT_SORT_ORDER = new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "id"), EntityDataSortOrder.Direction.ASC);
    public static final EntityDataSortOrder DEFAULT_TS_CHART_SORT_ORDER = new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.DESC);
    public static final EntityDataSortOrder DEFAULT_ALARM_SORT_ORDER = new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime"), EntityDataSortOrder.Direction.DESC);

    public static EntityCountQuery toEntityCountQuery(DataSource dataSource, TbReportCtx ctx, EntityId stateEntityId) {
        EntityFilter entityFilter = buildEntityFilter(dataSource, ctx, stateEntityId);
        List<KeyFilter> keyFilters = findKeyFilters(dataSource, ctx.getConfiguration());

        return new EntityCountQuery(entityFilter, keyFilters);
    }

    public static AlarmCountQuery toAlarmCountQuery(DataSource dataSource, TbReportCtx ctx, EntityId stateEntityId) {
        EntityFilter entityFilter = buildEntityFilter(dataSource, ctx, stateEntityId);
        List<KeyFilter> keyFilters = findKeyFilters(dataSource, ctx.getConfiguration());
        AlarmCountQuery alarmCountQuery = new AlarmCountQuery(entityFilter, keyFilters);

        AlarmFilterConfig alarmFilterConfig = dataSource.getAlarmFilterConfig();
        if (alarmFilterConfig != null) {
            alarmCountQuery.setStatusList(alarmFilterConfig.getStatusList());
            alarmCountQuery.setSeverityList(alarmFilterConfig.getSeverityList());
            alarmCountQuery.setTypeList(alarmFilterConfig.getTypeList());
            alarmCountQuery.setAssigneeId(alarmFilterConfig.getAssigneeId());
        }
        return alarmCountQuery;
    }

    public static AlarmDataQuery toAlarmDataQuery(AlarmTableComponent component, TbReportCtx ctx, EntityId stateEntityId, PageLink pageLink) {
        DataSource alarmSource = component.getAlarmSource();
        EntityFilter entityFilter = buildEntityFilter(alarmSource, ctx, stateEntityId);
        List<KeyFilter> keyFilters = findKeyFilters(alarmSource, ctx.getConfiguration());

        List<EntityKey> alarmFields = alarmSource.getDataKeys().stream().filter(dataKey -> "alarm".equals(dataKey.getType())).map(dataKey ->
                new EntityKey(EntityKeyType.ALARM_FIELD, dataKey.getName())).toList();

        List<EntityKey> entityFields = alarmSource.getDataKeys().stream().filter(dataKey -> "entityField".equals(dataKey.getType())).map(dataKey ->
                new EntityKey(EntityKeyType.ENTITY_FIELD, dataKey.getName())).toList();

        entityFields = setEntityKeyIfNotExists(entityFields, EntityKeyType.ENTITY_FIELD, "name");
        entityFields = setEntityKeyIfNotExists(entityFields, EntityKeyType.ENTITY_FIELD, "label");

        List<EntityKey> attrFields = alarmSource.getDataKeys().stream().filter(dataKey -> "attribute".equals(dataKey.getType())).map(dataKey ->
                new EntityKey(EntityKeyType.ATTRIBUTE, dataKey.getName())).toList();

        List<EntityKey> tsFields = alarmSource.getDataKeys().stream().filter(dataKey -> "timeseries".equals(dataKey.getType())).map(dataKey ->
                new EntityKey(EntityKeyType.TIME_SERIES, dataKey.getName())).toList();

        List<EntityKey> latestValues = Stream.concat(attrFields.stream(), tsFields.stream()).toList();

        AlarmFilterConfig alarmFilterConfig = alarmSource.getAlarmFilterConfig();
        AlarmDataPageLink alarmDataPageLink = new AlarmDataPageLink();
        alarmDataPageLink.setPage(pageLink.getPage());
        alarmDataPageLink.setPageSize(pageLink.getPageSize());
        alarmDataPageLink.setSortOrder(DEFAULT_ALARM_SORT_ORDER);

        String targetTimezone = StringUtils.isNotBlank(component.getTimewindow().getTimezone()) ?
                component.getTimewindow().getTimezone() : ctx.getTimeZone();

        TimeIntervalCalculator.TimeRange timeRange = getTimeRange(component.getTimewindow(), targetTimezone);
        alarmDataPageLink.setStartTs(timeRange.startTs);
        alarmDataPageLink.setEndTs(timeRange.endTs);
        alarmDataPageLink.setSearchPropagatedAlarms(alarmFilterConfig.isSearchPropagatedAlarms());
        alarmDataPageLink.setSeverityList(alarmFilterConfig.getSeverityList());
        alarmDataPageLink.setStatusList(alarmFilterConfig.getStatusList());
        alarmDataPageLink.setTypeList(alarmFilterConfig.getTypeList());
        alarmDataPageLink.setAssigneeId(alarmFilterConfig.getAssigneeId());
        return new AlarmDataQuery(entityFilter, alarmDataPageLink, entityFields, latestValues, keyFilters, alarmFields);
    }

    public static EntityDataQuery toEntityDataQuery(DataSource dataSource, TbReportCtx ctx, EntityFilter filter, PageLink pageLink) {
        return toEntityDataQuery(dataSource, ctx, filter, pageLink, DEFAULT_SORT_ORDER);
    }

    public static EntityDataQuery toEntityDataQuery(DataSource dataSource, TbReportCtx ctx, EntityFilter filter, PageLink pageLink, EntityDataSortOrder sortOrder) {
        EntityDataPageLink entityDataPageLink = new EntityDataPageLink(pageLink.getPageSize(), pageLink.getPage(), pageLink.getTextSearch(), sortOrder);

        List<KeyFilter> keyFilters = findKeyFilters(dataSource, ctx.getConfiguration());

        List<EntityKey> entityFields = new ArrayList<>();
        List<EntityKey> latestValues = new ArrayList<>();
        if (dataSource.getDataKeys() != null) {
            for (DataKey dataKey : dataSource.getDataKeys()) {
                switch (dataKey.getType()) {
                    case "attribute" -> {
                        latestValues.add(new EntityKey(EntityKeyType.ATTRIBUTE, dataKey.getName()));
                    }
                    case "timeseries" -> {
                        if (dataKey.getAggregationType() == null || dataKey.getAggregationType() == Aggregation.NONE) {
                            latestValues.add(new EntityKey(EntityKeyType.TIME_SERIES, dataKey.getName()));
                        }
                    }
                    case "entityField" -> {
                        entityFields.add(new EntityKey(EntityKeyType.ENTITY_FIELD, dataKey.getName()));
                    }
                }
            }
        }
        entityFields = setEntityKeyIfNotExists(entityFields, EntityKeyType.ENTITY_FIELD, "name");
        entityFields = setEntityKeyIfNotExists(entityFields, EntityKeyType.ENTITY_FIELD, "label");
        return new EntityDataQuery(filter, entityDataPageLink, entityFields, latestValues, keyFilters);
    }

    public static EntityFilter buildEntityFilter(DataSource dataSource, TbReportCtx ctx, EntityId stateEntityId) {
        if (dataSource.getType() == DataSourceType.DEVICE) {
            return buildSingleEntityFilter(DeviceId.fromString(dataSource.getDeviceId()));
        }
        return buildAliasBasedFilter(dataSource, ctx, stateEntityId);
    }

    public static Optional<String> resolveAliasId(TbReportCtx ctx, String aliasName) {
        return ctx.getConfiguration().getEntityAliases().stream()
                .filter(alias -> alias.getAlias().equals(aliasName)).findFirst().map(EntityAlias::getId);
    }

    private static EntityFilter buildSingleEntityFilter(EntityId entityId) {
        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(AliasEntityId.fromEntityId(entityId));
        return filter;
    }

    private static EntityFilter buildAliasBasedFilter(DataSource dataSource, TbReportCtx ctx, EntityId stateEntityId) {
        EntityFilter filter = ctx.getConfiguration().getEntityAliases().stream()
                .filter(alias -> alias.getId().equals(dataSource.getEntityAliasId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Entity alias not found: " + dataSource.getEntityAliasId()))
                .getFilter();

        AliasEntityId resolvedEntity = resolveStateEntityId(stateEntityId, filter, ctx);

        if (filter instanceof StateEntityFilter) {
            return buildSingleEntityFilter(resolvedEntity);
        } else if (filter instanceof StateEntityOwnerFilter ownerFilter) {
            ownerFilter.setSingleEntity(resolvedEntity);
            return ownerFilter;
        } else if (filter instanceof RelationsQueryFilter queryFilter && queryFilter.isRootStateEntity()) {
            queryFilter.setRootEntity(resolvedEntity);
        } else if (filter instanceof EntitySearchQueryFilter queryFilter && queryFilter.isRootStateEntity()) {
            queryFilter.setRootEntity(resolvedEntity);
        } else if (filter instanceof SchedulerEventFilter queryFilter && queryFilter.isOriginatorStateEntity()) {
            queryFilter.setOriginator(resolvedEntity);
        } else if (filter instanceof EntityGroupFilter entityGroupFilter && entityGroupFilter.isGroupStateEntity()) {
            if (resolvedEntity != null) {
                entityGroupFilter.setGroupType(resolvedEntity.getEntityType());
                entityGroupFilter.setEntityGroup(resolvedEntity.getId().toString());
            }
        } else if (filter instanceof EntitiesByGroupNameFilter entitiesByGroupNameFilter && entitiesByGroupNameFilter.isGroupStateEntity()) {
             entitiesByGroupNameFilter.setOwnerId(resolvedEntity);
        }

        EntityFilter.resolveEntityFilter(filter, ctx.getTenantId(), ctx.getUserId(), ctx.getUserOwnerId());

        return filter;
    }

    private static AliasEntityId resolveStateEntityId(EntityId stateEntityId, EntityFilter filter, TbReportCtx ctx) {
        if (stateEntityId != null) {
            return AliasEntityId.fromEntityId(stateEntityId);
        }

        if (filter instanceof StateEntityFilter stateFilter) {
            return resolveAliasEntityId(stateFilter.getDefaultStateEntity(), ctx.getTenantId(), ctx.getUserId(), ctx.getUserOwnerId());
        } else if (filter instanceof StateEntityOwnerFilter ownerFilter) {
            return resolveAliasEntityId(ownerFilter.getDefaultStateEntity(), ctx.getTenantId(), ctx.getUserId(), ctx.getUserOwnerId());
        } else if (filter instanceof RelationsQueryFilter queryFilter) {
            return resolveAliasEntityId(queryFilter.getDefaultStateEntity(), ctx.getTenantId(), ctx.getUserId(), ctx.getUserOwnerId());
        } else if (filter instanceof EntitySearchQueryFilter queryFilter) {
            return resolveAliasEntityId(queryFilter.getDefaultStateEntity(), ctx.getTenantId(), ctx.getUserId(), ctx.getUserOwnerId());
        } else if (filter instanceof SchedulerEventFilter queryFilter) {
            return resolveAliasEntityId(queryFilter.getDefaultStateEntity(), ctx.getTenantId(), ctx.getUserId(), ctx.getUserOwnerId());
        } else if (filter instanceof EntityGroupFilter entityGroupFilter) {
            if (entityGroupFilter.getDefaultStateGroupType() != null && entityGroupFilter.getDefaultStateEntityGroup() != null) {
                return AliasEntityId.fromEntityId(EntityIdFactory.getByTypeAndId(entityGroupFilter.getDefaultStateGroupType(), entityGroupFilter.getDefaultStateEntityGroup()));
            }
        }
        return null;
    }


    private static List<KeyFilter> findKeyFilters(DataSource dataSource, ReportTemplateConfig reportTemplateConfig) {
        if (dataSource.getFilterId() != null) {
            return reportTemplateConfig.getFilters().stream()
                    .filter(filter -> filter.getId().equals(dataSource.getFilterId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Entity filter not found: " + dataSource.getFilterId()))
                    .getKeyFilters();
        } else {
            return null;
        }
    }


}
