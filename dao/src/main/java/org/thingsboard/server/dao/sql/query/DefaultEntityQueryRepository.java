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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AssetTypeFilter;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.query.EntityNameFilter;
import org.thingsboard.server.common.data.query.EntityViewTypeFilter;
import org.thingsboard.server.common.data.query.FilterPredicateType;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@SqlDao
@Repository
@Slf4j
public class DefaultEntityQueryRepository implements EntityQueryRepository {

    private static final Map<String, String> entityFieldColumnMap = new HashMap<>();
    static {
        entityFieldColumnMap.put("createdTime", "id");
        entityFieldColumnMap.put("name", "name");
        entityFieldColumnMap.put("type", "type");
        entityFieldColumnMap.put("label", "label");
        entityFieldColumnMap.put("firstName", "first_name");
        entityFieldColumnMap.put("lastName", "last_name");
        entityFieldColumnMap.put("email", "email");
        entityFieldColumnMap.put("title", "title");
        entityFieldColumnMap.put("country", "country");
        entityFieldColumnMap.put("state", "state");
        entityFieldColumnMap.put("city", "city");
        entityFieldColumnMap.put("address", "address");
        entityFieldColumnMap.put("address2", "address2");
        entityFieldColumnMap.put("zip", "zip");
        entityFieldColumnMap.put("phone", "phone");
    }

    private static final Map<EntityType, String> entityTableMap = new HashMap<>();
    static {
        entityTableMap.put(EntityType.ASSET, "asset");
        entityTableMap.put(EntityType.DEVICE, "device");
        entityTableMap.put(EntityType.ENTITY_VIEW, "entity_view");
        entityTableMap.put(EntityType.DASHBOARD, "dashboard");
        entityTableMap.put(EntityType.CUSTOMER, "customer");
        entityTableMap.put(EntityType.USER, "tb_user");
        entityTableMap.put(EntityType.TENANT, "tenant");
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, EntityCountQuery query) {
        EntityType entityType = resolveEntityType(query.getEntityFilter());
        String countQuery = String.format("select count(e.id) from %s e where %s",
                entityTableMap.get(entityType), this.buildEntityWhere(tenantId, customerId, query.getEntityFilter(),
                        Collections.emptyList(), entityType));
        return ((BigInteger)entityManager.createNativeQuery(countQuery)
                .getSingleResult()).longValue();
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query) {
        EntityType entityType = resolveEntityType(query.getEntityFilter());

        List<EntityKeyMapping> mappings = prepareKeyMapping(query);

        List<EntityKeyMapping> selectionMapping = mappings.stream().filter(mapping -> mapping.isSelection())
                .collect(Collectors.toList());
        List<EntityKeyMapping> entityFieldsSelectionMapping = selectionMapping.stream().filter(mapping -> !mapping.isLatest())
                .collect(Collectors.toList());
        List<EntityKeyMapping> latestSelectionMapping = selectionMapping.stream().filter(mapping -> mapping.isLatest())
                .collect(Collectors.toList());

        List<EntityKeyMapping> filterMapping = mappings.stream().filter(mapping -> mapping.hasFilter())
                .collect(Collectors.toList());
        List<EntityKeyMapping> entityFieldsFiltersMapping = filterMapping.stream().filter(mapping -> !mapping.isLatest())
                .collect(Collectors.toList());
        List<EntityKeyMapping> latestFiltersMapping = filterMapping.stream().filter(mapping -> mapping.isLatest())
                .collect(Collectors.toList());

        List<EntityKeyMapping> allLatestMappings = mappings.stream().filter(mapping -> mapping.isLatest())
                .collect(Collectors.toList());


        String entityWhereClause = this.buildEntityWhere(tenantId, customerId, query.getEntityFilter(), entityFieldsFiltersMapping, entityType);
        String latestJoins = this.buildLatestJoins(entityType, allLatestMappings);
        String latestFilters = this.buildLatestQuery(latestFiltersMapping);

        String countQuery = String.format("select count(*) from (select e.id from %s e where %s) entities %s",
                entityTableMap.get(entityType), entityWhereClause, latestJoins);
        if (!StringUtils.isEmpty(latestFilters)) {
            countQuery = String.format("%s where %s", countQuery, latestFilters);
        }
        int totalElements = ((BigInteger)entityManager.createNativeQuery(countQuery)
                .getSingleResult()).intValue();

        String entityFieldsSelection = this.buildEntityFieldsSelection(entityFieldsSelectionMapping);
        if (!StringUtils.isEmpty(entityFieldsSelection)) {
            entityFieldsSelection = String.format("e.id, %s", entityFieldsSelection);
        } else {
            entityFieldsSelection = "e.id";
        }
        String latestSelection = this.buildLatestSelections(latestSelectionMapping);
        String topSelection = "entities.*";
        if (!StringUtils.isEmpty(latestSelection)) {
            topSelection = topSelection + ", " + latestSelection;
        }

        String dataQuery = String.format("select %s from (select %s from %s e where %s) entities %s", topSelection,
                entityFieldsSelection,
                entityTableMap.get(entityType),
                entityWhereClause,
                latestJoins);

        if (!StringUtils.isEmpty(latestFilters)) {
            dataQuery = String.format("%s where %s", dataQuery, latestFilters);
        }

        EntityDataPageLink pageLink = query.getPageLink();

        // TODO: order by

        int startIndex = pageLink.getPageSize() * pageLink.getPage();
        if (pageLink.getPageSize() > 0) {
            dataQuery = String.format("%s limit %s offset %s", dataQuery, pageLink.getPageSize(), startIndex);
        }
        List result = entityManager.createNativeQuery(dataQuery).getResultList();
        int totalPages = pageLink.getPageSize() > 0 ? (int)Math.ceil((float)totalElements / pageLink.getPageSize()) : 1;
        boolean hasNext = pageLink.getPageSize() > 0 && totalElements > startIndex + result.size();
        List<EntityData> entitiesData = convertListToEntityData(result, entityType, selectionMapping);
        return new PageData<>(entitiesData, totalPages, totalElements, hasNext);
    }

    private List<EntityData> convertListToEntityData(List<Object> result, EntityType entityType, List<EntityKeyMapping> selectionMapping) {
        return result.stream().map(obj -> this.toEntityData(obj, entityType, selectionMapping)).collect(Collectors.toList());
    }

    private EntityData toEntityData(Object obj, EntityType entityType, List<EntityKeyMapping> selectionMapping) {
        String id = obj instanceof String ? (String)obj : (String)((Object[]) obj)[0];
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, UUIDConverter.fromString(id));
        Map<EntityKeyType, Map<String, TsValue>> latest = new HashMap<>();
        Map<String, TsValue[]> timeseries = new HashMap<>();
        EntityData entityData = new EntityData(entityId, latest, timeseries);
        for (EntityKeyMapping mapping: selectionMapping) {
            Object value = ((Object[]) obj)[mapping.getIndex()];
            // TODO:
        }
        return entityData;
    }

    private List<EntityKeyMapping> prepareKeyMapping(EntityDataQuery query) {
        List<EntityKey> entityFields = query.getEntityFields() != null ? query.getEntityFields() : Collections.emptyList();
        List<EntityKey> latestValues = query.getLatestValues() != null ? query.getLatestValues() : Collections.emptyList();
        Map<EntityKey, List<KeyFilter>> filters =
                query.getKeyFilters() != null ?
                        query.getKeyFilters().stream().collect(Collectors.groupingBy(KeyFilter::getKey)) : Collections.emptyMap();
        int index = 1;
        List<EntityKeyMapping> mappings = new ArrayList<>();
        for (EntityKey entityField : entityFields) {
            EntityKeyMapping mapping = new EntityKeyMapping();
            mapping.setIndex(index);
            mapping.setAlias(String.format("alias%s", index));
            mapping.setKeyFilters(filters.remove(entityField));
            mapping.setLatest(false);
            mapping.setSelection(true);
            mapping.setEntityKey(entityField);
            mappings.add(mapping);
            index++;
        }
        for (EntityKey latestField : latestValues) {
            EntityKeyMapping mapping = new EntityKeyMapping();
            mapping.setIndex(index);
            mapping.setAlias(String.format("alias%s", index));
            mapping.setKeyFilters(filters.remove(latestField));
            mapping.setLatest(true);
            mapping.setSelection(true);
            mapping.setEntityKey(latestField);
            mappings.add(mapping);
            index +=2;
        }
        if (!filters.isEmpty()) {
            for (EntityKey filterField : filters.keySet()) {
                EntityKeyMapping mapping = new EntityKeyMapping();
                mapping.setIndex(index);
                mapping.setAlias(String.format("alias%s", index));
                mapping.setKeyFilters(filters.get(filterField));
                mapping.setLatest(!filterField.getType().equals(EntityKeyType.ENTITY_FIELD));
                mapping.setSelection(false);
                mapping.setEntityKey(filterField);
                mappings.add(mapping);
                index +=1;
            }
        }
        return mappings;
    }

    private String buildEntityFieldsSelection(List<EntityKeyMapping> entityFieldsSelectionMapping) {
        return entityFieldsSelectionMapping.stream().map(mapping -> {
           String column = entityFieldColumnMap.get(mapping.getEntityKey().getKey());
           return String.format("e.%s as %s", column, mapping.getAlias());
        }).collect(
                Collectors.joining(", "));
    }

    private String buildLatestSelections(List<EntityKeyMapping> latestSelectionMapping) {
        return latestSelectionMapping.stream().map(mapping -> this.buildLatestSelection(mapping))
                .collect(
                        Collectors.joining(", "));
    }

    private String buildLatestSelection(EntityKeyMapping mapping) {
        if (mapping.getEntityKey().getType().equals(EntityKeyType.TIME_SERIES)) {
            return buildTimeseriesSelection(mapping);
        } else {
            return buildAttributeSelection(mapping);
        }
    }

    private String buildAttributeSelection(EntityKeyMapping mapping) {
        String alias = mapping.getAlias();
        String attrValAlias = alias + "_value";
        String attrTsAlias = alias + "_ts";
        String attrTsSelection = String.format("%s.last_update_ts as %s", alias, attrTsAlias);
        String attrValSelection =
                String.format("coalesce(cast(%s.bool_v as varchar), '') || " +
                      "coalesce(%s.str_v, '') || " +
                      "coalesce(cast(%s.long_v as varchar), '') || " +
                      "coalesce(cast(%s.dbl_v as varchar), '') || " +
                      "coalesce(cast(%s.json_v as varchar), '')) as %s", alias, alias, alias, alias, alias, attrValAlias);
        return String.join(", ", attrTsSelection, attrValSelection);
    }

    private String buildTimeseriesSelection(EntityKeyMapping mapping) {
        // TODO:
        String alias = mapping.getAlias();
        String attrValAlias = alias + "_value";
        String attrTsAlias = alias + "_ts";
        return String.format("(select 1) as %s, (select '') as %s", attrTsAlias, attrValAlias);
    }

    private String buildLatestJoins(EntityType entityType, List<EntityKeyMapping> latestMappings) {
        return latestMappings.stream().map(mapping -> this.buildLatestJoin(entityType, mapping)).collect(
                Collectors.joining(" "));
    }

    private String buildLatestJoin(EntityType entityType, EntityKeyMapping mapping) {
        String join = mapping.hasFilter() ? "left join" : "left outer join";
        if (mapping.getEntityKey().getType().equals(EntityKeyType.TIME_SERIES)) {
            // TODO:
            throw new RuntimeException("Not implemented!");
        } else {
            String alias = mapping.getAlias();
            String query = String.format("%s attribute_kv %s ON %s.entity_id=entities.id AND %s.entity_type='%s' AND %s.attribute_key='%s'",
                    join, alias, alias, alias, entityType.name(), alias, mapping.getEntityKey().getKey());
            if (!mapping.getEntityKey().getType().equals(EntityKeyType.ATTRIBUTE)) {
                String scope;
                if (mapping.getEntityKey().getType().equals(EntityKeyType.CLIENT_ATTRIBUTE)) {
                    scope = DataConstants.CLIENT_SCOPE;
                } else if (mapping.getEntityKey().getType().equals(EntityKeyType.SHARED_ATTRIBUTE)) {
                    scope = DataConstants.SHARED_SCOPE;
                } else {
                    scope = DataConstants.SERVER_SCOPE;
                }
                query = String.format("%s AND %s.attribute_type=%s", query, alias, scope);
            }
            return query;
        }
    }

    private String buildEntityWhere(TenantId tenantId,
                              CustomerId customerId,
                              EntityFilter entityFilter,
                              List<EntityKeyMapping> entityFieldsFilters,
                              EntityType entityType) {
        String permissionQuery = this.buildPermissionQuery(tenantId, customerId, entityType);
        String entityFilterQuery = this.buildEntityFilterQuery(entityFilter);
        if (!entityFieldsFilters.isEmpty()) {
            String entityFieldsQuery = this.buildEntityFieldsQuery(entityFieldsFilters);
            return String.join(" and ", permissionQuery, entityFilterQuery, entityFieldsQuery);
        } else {
            return String.join(" and ", permissionQuery, entityFilterQuery);
        }
    }

    private String buildPermissionQuery(TenantId tenantId, CustomerId customerId, EntityType entityType) {
        String permissionQuery = String.format("e.tenant_id=%s", UUIDConverter.fromTimeUUID(tenantId.getId()));
        if (entityType != EntityType.TENANT && entityType != EntityType.CUSTOMER) {
            permissionQuery = String.format("%s and e.customerId=%s", permissionQuery, UUIDConverter.fromTimeUUID(customerId.getId()));
        }
        return permissionQuery;
    }

    private String buildEntityFilterQuery(EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case SINGLE_ENTITY:
                return this.singleEntityQuery((SingleEntityFilter) entityFilter);
            case ENTITY_LIST:
                return this.entityListQuery((EntityListFilter) entityFilter);
            case ENTITY_NAME:
                return this.entityNameQuery((EntityNameFilter) entityFilter);
            case ASSET_TYPE:
            case DEVICE_TYPE:
            case ENTITY_VIEW_TYPE:
                return this.typeQuery(entityFilter);
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    private String buildLatestQuery(List<EntityKeyMapping> latestFilters) {
        List<String> latestQueries = new ArrayList<>();
        for (EntityKeyMapping mapping: latestFilters) {
            latestQueries.addAll(mapping.getKeyFilters().stream().map(keyFilter ->
                    this.buildKeyQuery(mapping.getAlias(), keyFilter))
                    .collect(Collectors.toList()));
        }
        return latestQueries.stream().collect(Collectors.joining(" AND "));
    }

    private String buildEntityFieldsQuery(List<EntityKeyMapping> entityFieldsFilters) {
        return entityFieldsFilters.stream().flatMap(mapping -> mapping.getKeyFilters().stream())
                .map(keyFilter -> this.buildKeyQuery("e", keyFilter)).collect(
                Collectors.joining(" AND ")
        );
    }

    private String buildKeyQuery(String alias, KeyFilter keyFilter) {
        return this.buildPredicateQuery(alias, keyFilter.getKey(), keyFilter.getPredicate());
    }

    private String buildPredicateQuery(String alias, EntityKey key, KeyFilterPredicate predicate) {
        if (predicate.getType().equals(FilterPredicateType.COMPLEX)) {
            return this.buildComplexPredicateQuery(alias, key, (ComplexFilterPredicate)predicate);
        } else {
            return this.buildSimplePredicateQuery(alias, key, predicate);
        }
    }

    private String buildComplexPredicateQuery(String alias, EntityKey key, ComplexFilterPredicate predicate) {
        return predicate.getPredicates().stream()
                .map(keyFilterPredicate -> this.buildPredicateQuery(alias, key, keyFilterPredicate)).collect(Collectors.joining(
                        " " + predicate.getOperation().name() + " "
                ));
    }

    private String buildSimplePredicateQuery(String alias, EntityKey key, KeyFilterPredicate predicate) {
        if (predicate.getType().equals(FilterPredicateType.NUMERIC)) {
            if (key.getType().equals(EntityKeyType.ENTITY_FIELD)) {
                String column = entityFieldColumnMap.get(key.getKey());
                return this.buildNumericPredicateQuery(alias + "." + column, (NumericFilterPredicate)predicate);
            } else {
                String longQuery = this.buildNumericPredicateQuery(alias + ".long_v", (NumericFilterPredicate)predicate);
                String doubleQuery = this.buildNumericPredicateQuery(alias + ".dbl_v", (NumericFilterPredicate)predicate);
                return String.format("(%s or %s)", longQuery, doubleQuery);
            }
        } else {
            String column;
            if (key.getType().equals(EntityKeyType.ENTITY_FIELD)) {
                column = entityFieldColumnMap.get(key.getKey());
            } else {
                column = predicate.getType().equals(FilterPredicateType.STRING) ? "str_v" : "bool_v";
            }
            String field = alias + "." + column;
            if (predicate.getType().equals(FilterPredicateType.STRING)) {
                return this.buildStringPredicateQuery(field, (StringFilterPredicate)predicate);
            } else {
                return this.buildBooleanPredicateQuery(field, (BooleanFilterPredicate)predicate);
            }
        }
    }

    private String buildStringPredicateQuery(String field, StringFilterPredicate stringFilterPredicate) {
        String operationField = field;
        String value = stringFilterPredicate.getValue();
        String stringOperationQuery = "";
        if (stringFilterPredicate.isIgnoreCase()) {
            value.toLowerCase();
            operationField = String.format("lower(%s)", operationField);
        }
        switch (stringFilterPredicate.getOperation()) {
            case EQUAL:
                stringOperationQuery = String.format("%s = '%s'", operationField, value);
                break;
            case NOT_EQUAL:
                stringOperationQuery = String.format("%s != '%s'", operationField, value);
                break;
            case STARTS_WITH:
                stringOperationQuery = String.format("%s like '%s%'", operationField, value);
                break;
            case ENDS_WITH:
                stringOperationQuery = String.format("%s like '%%s'", operationField, value);
                break;
            case CONTAINS:
                stringOperationQuery = String.format("%s like '%%s%'", operationField, value);
                break;
            case NOT_CONTAINS:
                stringOperationQuery = String.format("%s not like '%%s%'", operationField, value);
                break;
        }
        return String.format("(%s is not null and %s)", field, stringOperationQuery);
    }

    private String buildNumericPredicateQuery(String field, NumericFilterPredicate numericFilterPredicate) {
        double value = numericFilterPredicate.getValue();
        String numericOperationQuery = "";
        switch (numericFilterPredicate.getOperation()) {
            case EQUAL:
                numericOperationQuery = String.format("%s = %s", field, value);
                break;
            case NOT_EQUAL:
                numericOperationQuery = String.format("%s != '%s'", field, value);
                break;
            case GREATER:
                numericOperationQuery = String.format("%s > %s", field, value);
                break;
            case GREATER_OR_EQUAL:
                numericOperationQuery = String.format("%s >= %s", field, value);
                break;
            case LESS:
                numericOperationQuery = String.format("%s < %s", field, value);
                break;
            case LESS_OR_EQUAL:
                numericOperationQuery = String.format("%s <= %s", field, value);
                break;
        }
        return String.format("(%s is not null and %s)", field, numericOperationQuery);
    }

    private String buildBooleanPredicateQuery(String field,
                                              BooleanFilterPredicate booleanFilterPredicate) {
        boolean value = booleanFilterPredicate.isValue();
        String booleanOperationQuery = "";
        switch (booleanFilterPredicate.getOperation()) {
            case EQUAL:
                booleanOperationQuery = String.format("%s = %s", field, value);
                break;
            case NOT_EQUAL:
                booleanOperationQuery = String.format("%s != %s", field, value);
                break;
        }
        return String.format("(%s is not null and %s)", field, booleanOperationQuery);
    }

    private String singleEntityQuery(SingleEntityFilter filter) {
        return String.format("e.id=%s", UUIDConverter.fromTimeUUID(filter.getSingleEntity().getId()));
    }

    private String entityListQuery(EntityListFilter filter) {
        return String.format("e.id in (%s)",
                filter.getEntityList().stream().map(UUID::fromString).map(UUIDConverter::fromTimeUUID).collect(Collectors.joining(",")));
    }

    private String entityNameQuery(EntityNameFilter filter) {
        return String.format("lower(e.searchText) like lower(concat(%s, '%'))", filter.getEntityNameFilter());
    }

    private String typeQuery(EntityFilter filter) {
        String type;
        String name;
        switch (filter.getType()) {
            case ASSET_TYPE:
                type = ((AssetTypeFilter)filter).getAssetType();
                name = ((AssetTypeFilter)filter).getAssetNameFilter();
                break;
            case DEVICE_TYPE:
                type = ((DeviceTypeFilter)filter).getDeviceType();
                name = ((DeviceTypeFilter)filter).getDeviceNameFilter();
                break;
            case ENTITY_VIEW_TYPE:
                type = ((EntityViewTypeFilter)filter).getEntityViewType();
                name = ((EntityViewTypeFilter)filter).getEntityViewNameFilter();
                break;
            default:
                throw new RuntimeException("Not supported!");
        }
        return String.format("e.type = %s and lower(e.searchText) like lower(concat(%s, '%'))", type, name);
    }

    private EntityType resolveEntityType(EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case SINGLE_ENTITY:
                return ((SingleEntityFilter)entityFilter).getSingleEntity().getEntityType();
            case ENTITY_LIST:
                return ((EntityListFilter)entityFilter).getEntityType();
            case ENTITY_NAME:
                return ((EntityNameFilter)entityFilter).getEntityType();
            case ASSET_TYPE:
            case ASSET_SEARCH_QUERY:
                return EntityType.ASSET;
            case DEVICE_TYPE:
            case DEVICE_SEARCH_QUERY:
                return EntityType.DEVICE;
            case ENTITY_VIEW_TYPE:
            case ENTITY_VIEW_SEARCH_QUERY:
                return EntityType.ENTITY_VIEW;
           default:
               throw new RuntimeException("Not implemented!");
        }
    }
}
