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
package org.thingsboard.server.dao.sql.query;

import lombok.Data;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityFilterType;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.FilterPredicateType;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.thingsboard.server.common.data.StringUtils.splitByCommaWithoutQuotes;
import static org.thingsboard.server.common.data.id.EntityId.NULL_UUID;
import static org.thingsboard.server.dao.sql.query.DefaultEntityQueryRepository.resolveEntityType;

@Data
public class EntityKeyMapping {

    private static final Map<EntityType, Set<String>> allowedEntityFieldMap = new HashMap<>();
    private static final Map<String, String> entityFieldColumnMap = new HashMap<>();
    private static final Map<EntityType, Map<String, String>> aliases = new HashMap<>();
    private static final Map<EntityType, Map<String, String>> propertiesFunctions = new EnumMap<>(EntityType.class);

    public static final String CREATED_TIME = "createdTime";
    public static final String ENTITY_TYPE = "entityType";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String LABEL = "label";
    public static final String DISPLAY_NAME = "displayName";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String EMAIL = "email";
    public static final String TITLE = "title";
    public static final String REGION = "region";
    public static final String COUNTRY = "country";
    public static final String STATE = "state";
    public static final String CITY = "city";
    public static final String ADDRESS = "address";
    public static final String ADDRESS_2 = "address2";
    public static final String ZIP = "zip";
    public static final String PHONE = "phone";
    public static final String ADDITIONAL_INFO = "additionalInfo";
    public static final String RELATED_PARENT_ID = "parentId";
    public static final String QUEUE_NAME = "queueName";
    public static final String SERVICE_ID = "serviceId";
    public static final String OWNER_NAME = "ownerName";
    public static final String OWNER_TYPE = "ownerType";
    public static final String LABELED_ENTITY_DISPLAY_NAME_SELECT_QUERY = "COALESCE(NULLIF(TRIM(e." + LABEL + "), ''), e." + NAME + ")";
    public static final String USER_DISPLAY_NAME_SELECT_QUERY  = "COALESCE(NULLIF(TRIM(CONCAT_WS(' ', e.first_name, e.last_name)), ''), e.email)";
    public static final String OWNER_NAME_SELECT_QUERY = "case when e.customer_id = '" + NULL_UUID + "' " +
            "then (select title from tenant where id = e.tenant_id) " +
            "else (select title from customer where id = e.customer_id) end";
    public static final String OWNER_TYPE_SELECT_QUERY = "case when e.customer_id = '" + NULL_UUID + "' " +
            "then 'TENANT' " +
            "else 'CUSTOMER' end";
    public static final String QUEUE_STATS_NAME_QUERY = "concat(e.queue_name, '_', e.service_id)";
    public static final Map<String, String> ownerPropertiesFunctions = Map.of(
            OWNER_NAME, OWNER_NAME_SELECT_QUERY,
            OWNER_TYPE, OWNER_TYPE_SELECT_QUERY
    );
    public static final Map<String, String> labeledPropertiesFunctions = Map.of(
            OWNER_NAME, OWNER_NAME_SELECT_QUERY,
            OWNER_TYPE, OWNER_TYPE_SELECT_QUERY,
            DISPLAY_NAME, LABELED_ENTITY_DISPLAY_NAME_SELECT_QUERY
    );
    public static final Map<String, String> userPropertiesFunctions = Map.of(
            OWNER_NAME, OWNER_NAME_SELECT_QUERY,
            OWNER_TYPE, OWNER_TYPE_SELECT_QUERY,
            DISPLAY_NAME, USER_DISPLAY_NAME_SELECT_QUERY
    );
    public static final Map<String, String> queueStatsPropertiesFunctions = Map.of(NAME, QUEUE_STATS_NAME_QUERY);

    public static final List<String> typedEntityFields = Arrays.asList(CREATED_TIME, ENTITY_TYPE, NAME, TYPE, ADDITIONAL_INFO);
    public static final List<String> widgetEntityFields = Arrays.asList(CREATED_TIME, ENTITY_TYPE, NAME);
    public static final List<String> commonEntityFields = Arrays.asList(CREATED_TIME, ENTITY_TYPE, NAME, ADDITIONAL_INFO);
    public static final List<String> dashboardEntityFields = Arrays.asList(CREATED_TIME, ENTITY_TYPE, TITLE);
    public static final List<String> labeledEntityFields = Arrays.asList(CREATED_TIME, ENTITY_TYPE, NAME, TYPE, LABEL, ADDITIONAL_INFO);
    public static final List<String> contactBasedEntityFields = Arrays.asList(CREATED_TIME, ENTITY_TYPE, EMAIL, TITLE, COUNTRY, STATE, CITY, ADDRESS, ADDRESS_2, ZIP, PHONE, ADDITIONAL_INFO);

    public static final Set<String> apiUsageStateEntityFields = new HashSet<>(Arrays.asList(CREATED_TIME, ENTITY_TYPE, NAME));
    public static final Set<String> commonEntityFieldsSet = new HashSet<>(commonEntityFields);
    public static final Set<String> relationQueryEntityFieldsSet = new HashSet<>(Arrays.asList(CREATED_TIME, ENTITY_TYPE, NAME, TYPE, LABEL, FIRST_NAME, LAST_NAME, EMAIL, REGION, TITLE, COUNTRY, STATE, CITY, ADDRESS, ADDRESS_2, ZIP, PHONE, ADDITIONAL_INFO, RELATED_PARENT_ID));

    static {
        allowedEntityFieldMap.put(EntityType.DEVICE, new HashSet<>(labeledEntityFields));
        allowedEntityFieldMap.put(EntityType.ASSET, new HashSet<>(labeledEntityFields));
        allowedEntityFieldMap.put(EntityType.ENTITY_VIEW, new HashSet<>(typedEntityFields));

        allowedEntityFieldMap.put(EntityType.TENANT, new HashSet<>(contactBasedEntityFields));
        allowedEntityFieldMap.get(EntityType.TENANT).add(REGION);
        allowedEntityFieldMap.put(EntityType.CUSTOMER, new HashSet<>(contactBasedEntityFields));

        allowedEntityFieldMap.put(EntityType.USER, new HashSet<>(Arrays.asList(CREATED_TIME, FIRST_NAME, LAST_NAME, EMAIL, PHONE, ADDITIONAL_INFO)));

        allowedEntityFieldMap.put(EntityType.DASHBOARD, new HashSet<>(dashboardEntityFields));
        allowedEntityFieldMap.put(EntityType.RULE_CHAIN, new HashSet<>(commonEntityFields));
        allowedEntityFieldMap.put(EntityType.RULE_NODE, new HashSet<>(commonEntityFields));
        allowedEntityFieldMap.put(EntityType.WIDGET_TYPE, new HashSet<>(widgetEntityFields));
        allowedEntityFieldMap.put(EntityType.WIDGETS_BUNDLE, new HashSet<>(widgetEntityFields));
        allowedEntityFieldMap.put(EntityType.API_USAGE_STATE, apiUsageStateEntityFields);
        allowedEntityFieldMap.put(EntityType.DEVICE_PROFILE, Set.of(CREATED_TIME, NAME, TYPE));
        allowedEntityFieldMap.put(EntityType.ASSET_PROFILE, Set.of(CREATED_TIME, NAME));
        allowedEntityFieldMap.put(EntityType.QUEUE_STATS, new HashSet<>(Arrays.asList(CREATED_TIME, QUEUE_NAME, SERVICE_ID)));

        entityFieldColumnMap.put(CREATED_TIME, ModelConstants.CREATED_TIME_PROPERTY);
        entityFieldColumnMap.put(ENTITY_TYPE, ModelConstants.ENTITY_TYPE_PROPERTY);
        entityFieldColumnMap.put(REGION, ModelConstants.TENANT_REGION_PROPERTY);
        entityFieldColumnMap.put(NAME, "name");
        entityFieldColumnMap.put(TYPE, "type");
        entityFieldColumnMap.put(LABEL, "label");
        entityFieldColumnMap.put(FIRST_NAME, ModelConstants.USER_FIRST_NAME_PROPERTY);
        entityFieldColumnMap.put(LAST_NAME, ModelConstants.USER_LAST_NAME_PROPERTY);
        entityFieldColumnMap.put(EMAIL, ModelConstants.EMAIL_PROPERTY);
        entityFieldColumnMap.put(TITLE, ModelConstants.TITLE_PROPERTY);
        entityFieldColumnMap.put(COUNTRY, ModelConstants.COUNTRY_PROPERTY);
        entityFieldColumnMap.put(STATE, ModelConstants.STATE_PROPERTY);
        entityFieldColumnMap.put(CITY, ModelConstants.CITY_PROPERTY);
        entityFieldColumnMap.put(ADDRESS, ModelConstants.ADDRESS_PROPERTY);
        entityFieldColumnMap.put(ADDRESS_2, ModelConstants.ADDRESS2_PROPERTY);
        entityFieldColumnMap.put(ZIP, ModelConstants.ZIP_PROPERTY);
        entityFieldColumnMap.put(PHONE, ModelConstants.PHONE_PROPERTY);
        entityFieldColumnMap.put(ADDITIONAL_INFO, ModelConstants.ADDITIONAL_INFO_PROPERTY);
        entityFieldColumnMap.put(RELATED_PARENT_ID, "parent_id");
        entityFieldColumnMap.put(QUEUE_NAME, ModelConstants.QUEUE_STATS_QUEUE_NAME_PROPERTY);
        entityFieldColumnMap.put(SERVICE_ID, ModelConstants.QUEUE_STATS_SERVICE_ID_PROPERTY);

        Map<String, String> contactBasedAliases = new HashMap<>();
        contactBasedAliases.put(NAME, TITLE);
        contactBasedAliases.put(LABEL, TITLE);
        contactBasedAliases.put(DISPLAY_NAME, TITLE);
        aliases.put(EntityType.TENANT, contactBasedAliases);
        aliases.put(EntityType.CUSTOMER, contactBasedAliases);
        aliases.put(EntityType.DASHBOARD, contactBasedAliases);
        Map<String, String> deviceAndAssetAliases = new HashMap<>();
        deviceAndAssetAliases.put(TITLE, NAME);
        aliases.put(EntityType.DEVICE, deviceAndAssetAliases);
        aliases.put(EntityType.ASSET, deviceAndAssetAliases);
        Map<String, String> commonEntityAliases = new HashMap<>();
        commonEntityAliases.put(TITLE, NAME);
        commonEntityAliases.put(DISPLAY_NAME, NAME);
        aliases.put(EntityType.ENTITY_VIEW, commonEntityAliases);
        aliases.put(EntityType.WIDGETS_BUNDLE, commonEntityAliases);

        propertiesFunctions.put(EntityType.DEVICE, labeledPropertiesFunctions);
        propertiesFunctions.put(EntityType.ASSET, labeledPropertiesFunctions);
        propertiesFunctions.put(EntityType.ENTITY_VIEW, ownerPropertiesFunctions);
        propertiesFunctions.put(EntityType.USER, userPropertiesFunctions);
        propertiesFunctions.put(EntityType.DASHBOARD, ownerPropertiesFunctions);
        propertiesFunctions.put(EntityType.QUEUE_STATS, queueStatsPropertiesFunctions);

        Map<String, String> userEntityAliases = new HashMap<>();
        userEntityAliases.put(TITLE, EMAIL);
        userEntityAliases.put(LABEL, EMAIL);
        userEntityAliases.put(NAME, EMAIL);
        aliases.put(EntityType.USER, userEntityAliases);
    }

    private int index;
    private String alias;
    private boolean isLatest;
    private String entityKeyColumn;
    private boolean isSelection;
    private boolean isSearchable;
    private boolean isSortOrder;
    private boolean ignore = false;
    private List<KeyFilter> keyFilters;
    private EntityKey entityKey;
    private int paramIdx = 0;

    public boolean hasFilter() {
        return keyFilters != null && !keyFilters.isEmpty();
    }

    public String getValueAlias() {
        if (entityKey.getType().equals(EntityKeyType.ENTITY_FIELD)) {
            return alias;
        } else {
            return alias + "_value";
        }
    }

    public String getTsAlias() {
        return alias + "_ts";
    }

    public String toSelection(EntityFilterType filterType, EntityType entityType) {
        if (entityKey.getType().equals(EntityKeyType.ENTITY_FIELD)) {
            if (entityKey.getKey().equals("entityType") && !filterType.equals(EntityFilterType.RELATIONS_QUERY)) {
                return String.format("'%s' as %s", entityType.name(), getValueAlias());
            } else {
                if (getEntityKeyColumn() != null) {
                    return String.format("cast(e.%s as varchar) as %s", getEntityKeyColumn(), getValueAlias());
                } else {
                    Map<String, String> entityPropertiesFunctions = propertiesFunctions.get(entityType);
                    String entityFieldAlias = getEntityFieldAlias(filterType, entityType);
                    if (entityPropertiesFunctions != null && entityPropertiesFunctions.containsKey(entityFieldAlias)) {
                        return String.format("%s as %s", entityPropertiesFunctions.get(entityFieldAlias), getValueAlias());
                    }
                    return String.format("'' as %s", getValueAlias());
                }
            }
        } else if (entityKey.getType().equals(EntityKeyType.TIME_SERIES)) {
            return buildTimeSeriesSelection();
        } else {
            return buildAttributeSelection();
        }
    }

    private String getEntityFieldAlias(EntityFilterType filterType, EntityType entityType) {
        String alias;
        if (filterType.equals(EntityFilterType.RELATIONS_QUERY)) {
            alias = entityKey.getKey();
        } else {
            alias = getAliasByEntityKeyAndType(entityKey.getKey(), entityType);
        }
        return alias;
    }

    private Set<String> getExistingEntityFields(EntityFilterType filterType, EntityType entityType) {
        Set<String> existingEntityFields;
        if (filterType.equals(EntityFilterType.RELATIONS_QUERY)) {
            existingEntityFields = relationQueryEntityFieldsSet;
        } else {
            existingEntityFields = allowedEntityFieldMap.get(entityType);
            if (existingEntityFields == null) {
                existingEntityFields = commonEntityFieldsSet;
            }
        }
        return existingEntityFields;
    }

    private String getAliasByEntityKeyAndType(String key, EntityType entityType) {
        String alias;
        Map<String, String> entityAliases = aliases.get(entityType);
        if (entityAliases != null) {
            alias = entityAliases.get(key);
        } else {
            alias = null;
        }
        if (alias == null) {
            alias = key;
        }
        return alias;
    }

    public Stream<String> toQueries(SqlQueryContext ctx, EntityFilterType filterType) {
        if (hasFilter()) {
            String keyAlias = (entityKey.getType().equals(EntityKeyType.ENTITY_FIELD) && getEntityKeyColumn() != null) ? "e" : alias;
            return keyFilters.stream().map(keyFilter ->
                    this.buildKeyQuery(ctx, keyAlias, keyFilter, filterType));
        } else {
            return Stream.empty();
        }
    }

    public String toLatestJoin(SqlQueryContext ctx, EntityFilter entityFilter, EntityType entityType) {
        String entityTypeStr;
        if (entityFilter.getType().equals(EntityFilterType.RELATIONS_QUERY)) {
            entityTypeStr = "entities.entity_type";
        } else {
            entityTypeStr = "'" + entityType.name() + "'";
        }
        ctx.addStringParameter(getKeyId(), entityKey.getKey());
        String filterQuery = toQueries(ctx, entityFilter.getType())
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining(" and "));
        if (StringUtils.isNotEmpty(filterQuery)) {
            filterQuery = " AND (" + filterQuery + ")";
        }
        if (entityKey.getType().equals(EntityKeyType.TIME_SERIES)) {
            String join = (hasFilter() && hasFilterValues(ctx)) ? "inner join" : "left join";
            return String.format("%s ts_kv_latest %s ON %s.entity_id=entities.id AND %s.key = (select key_id from key_dictionary where key = :%s_key_id) %s",
                    join, alias, alias, alias, alias, filterQuery);
        } else {
            String query;
            if (!entityKey.getType().equals(EntityKeyType.ATTRIBUTE)) {
                String join = (hasFilter() && hasFilterValues(ctx)) ? "inner join" : "left join";
                query = String.format("%s attribute_kv %s ON %s.entity_id=entities.id AND %s.attribute_key=(select key_id from key_dictionary where key = :%s_key_id) ",
                        join, alias, alias, alias, alias);
                int scope;
                if (entityKey.getType().equals(EntityKeyType.CLIENT_ATTRIBUTE)) {
                    scope = AttributeScope.CLIENT_SCOPE.getId();
                } else if (entityKey.getType().equals(EntityKeyType.SHARED_ATTRIBUTE)) {
                    scope = AttributeScope.SHARED_SCOPE.getId(); ;
                } else {
                    scope = AttributeScope.SERVER_SCOPE.getId(); ;
                }
                query = String.format("%s AND %s.attribute_type=%s %s", query, alias, scope, filterQuery);
            } else {
                String join = (hasFilter() && hasFilterValues(ctx)) ? "join LATERAL" : "left join LATERAL";
                query = String.format("%s (select * from attribute_kv %s WHERE %s.entity_id=entities.id  AND %s.attribute_key=(select key_id from key_dictionary where key = :%s_key_id) %s " +
                                "ORDER BY %s.last_update_ts DESC limit 1) as %s ON true",
                        join, alias, alias, alias, alias, filterQuery, alias, alias);
            }
            return query;
        }
    }

    private boolean hasFilterValues(SqlQueryContext ctx) {
        return Arrays.stream(ctx.getParameterNames()).anyMatch(parameterName -> {
            return !parameterName.equals(getKeyId()) && parameterName.startsWith(alias);
        });
    }

    private String getKeyId() {
        return alias + "_key_id";
    }

    public static String buildSelections(List<EntityKeyMapping> mappings, EntityFilterType filterType, EntityType entityType) {
        return mappings.stream().map(mapping -> mapping.toSelection(filterType, entityType)).collect(
                Collectors.joining(", "));
    }

    public static String buildLatestJoins(SqlQueryContext ctx, EntityFilter entityFilter, EntityType entityType, List<EntityKeyMapping> latestMappings, boolean countQuery) {
        return latestMappings.stream()
                .filter(mapping -> !countQuery || mapping.hasFilter())
                .map(mapping -> mapping.toLatestJoin(ctx, entityFilter, entityType))
                .collect(Collectors.joining(" "));
    }

    public static String buildQuery(SqlQueryContext ctx, List<EntityKeyMapping> mappings, EntityFilterType filterType) {
        return mappings.stream()
                .flatMap(mapping -> mapping.toQueries(ctx, filterType))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining(" AND "));
    }

    public static List<EntityKeyMapping> prepareKeyMapping(EntityType entityType, EntityDataQuery query) {
        EntityFilterType entityFilterType = query.getEntityFilter().getType();

        List<EntityKey> entityFields = query.getEntityFields() != null ? query.getEntityFields() : Collections.emptyList();
        List<EntityKey> latestValues = query.getLatestValues() != null ? query.getLatestValues() : Collections.emptyList();
        Map<EntityKey, List<KeyFilter>> filters =
                query.getKeyFilters() != null ?
                        query.getKeyFilters().stream().collect(Collectors.groupingBy(KeyFilter::getKey)) : Collections.emptyMap();
        EntityDataSortOrder sortOrder = query.getPageLink().getSortOrder();
        EntityKey sortOrderKey = sortOrder != null ? sortOrder.getKey() : null;
        int index = 2;
        List<EntityKeyMapping> entityFieldsMappings = entityFields.stream().map(
                key -> {
                    EntityKeyMapping mapping = new EntityKeyMapping();
                    mapping.setLatest(false);
                    mapping.setSelection(true);
                    mapping.setSearchable(!key.getKey().equals(ADDITIONAL_INFO));
                    mapping.setEntityKey(key);
                    mapping.setEntityKeyColumn(entityType, entityFilterType);
                    return mapping;
                }
        ).collect(Collectors.toList());
        List<EntityKeyMapping> latestMappings = latestValues.stream().map(
                key -> {
                    EntityKeyMapping mapping = new EntityKeyMapping();
                    mapping.setLatest(true);
                    mapping.setSearchable(true);
                    mapping.setSelection(true);
                    mapping.setEntityKey(key);
                    return mapping;
                }
        ).collect(Collectors.toList());
        if (sortOrderKey != null) {
            Optional<EntityKeyMapping> existing;
            if (sortOrderKey.getType().equals(EntityKeyType.ENTITY_FIELD)) {
                existing =
                        entityFieldsMappings.stream().filter(mapping -> mapping.entityKey.equals(sortOrderKey)).findFirst();
            } else {
                existing =
                        latestMappings.stream().filter(mapping -> mapping.entityKey.equals(sortOrderKey)).findFirst();
            }
            if (existing.isPresent()) {
                existing.get().setSortOrder(true);
            } else {
                EntityKeyMapping sortOrderMapping = new EntityKeyMapping();
                sortOrderMapping.setLatest(!sortOrderKey.getType().equals(EntityKeyType.ENTITY_FIELD));
                sortOrderMapping.setSelection(true);
                sortOrderMapping.setEntityKey(sortOrderKey);
                sortOrderMapping.setSortOrder(true);
                sortOrderMapping.setIgnore(true);
                sortOrderMapping.setEntityKeyColumn(entityType, entityFilterType);
                if (sortOrderKey.getType().equals(EntityKeyType.ENTITY_FIELD)) {
                    entityFieldsMappings.add(sortOrderMapping);
                } else {
                    latestMappings.add(sortOrderMapping);
                }
            }
        }
        List<EntityKeyMapping> mappings = new ArrayList<>();
        mappings.addAll(entityFieldsMappings);
        mappings.addAll(latestMappings);
        for (EntityKeyMapping mapping : mappings) {
            mapping.setIndex(index);
            mapping.setAlias(String.format("alias%s", index));
            mapping.setKeyFilters(filters.remove(mapping.entityKey));
            if (mapping.getEntityKey().getType().equals(EntityKeyType.ENTITY_FIELD)) {
                index++;
            } else {
                index += 2;
            }
        }
        if (!filters.isEmpty()) {
            for (EntityKey filterField : filters.keySet()) {
                EntityKeyMapping mapping = new EntityKeyMapping();
                mapping.setIndex(index);
                mapping.setAlias(String.format("alias%s", index));
                mapping.setKeyFilters(filters.get(filterField));
                mapping.setLatest(!filterField.getType().equals(EntityKeyType.ENTITY_FIELD));
                mapping.setEntityKey(filterField);
                mapping.setEntityKeyColumn(entityType, entityFilterType);
                mapping.setSelection(mapping.getEntityKeyColumn() == null);
                mappings.add(mapping);
                index += 1;
            }
        }

        return mappings;
    }

    private void setEntityKeyColumn(EntityType entityType, EntityFilterType entityFilterType) {
        Set<String> existingEntityFields = getExistingEntityFields(entityFilterType, entityType);
        String entityFieldAlias = getEntityFieldAlias(entityFilterType, entityType);
        if (existingEntityFields.contains(entityFieldAlias)) {
            entityKeyColumn = entityFieldColumnMap.get(entityFieldAlias);
        }
    }

    public static List<EntityKeyMapping> prepareEntityCountKeyMapping(EntityCountQuery query) {
        EntityType entityType = resolveEntityType(query.getEntityFilter());
        EntityFilterType entityFilterType = query.getEntityFilter().getType();

        Map<EntityKey, List<KeyFilter>> filters =
                query.getKeyFilters() != null ?
                        query.getKeyFilters().stream().collect(Collectors.groupingBy(KeyFilter::getKey)) : Collections.emptyMap();
        int index = 2;
        List<EntityKeyMapping> mappings = new ArrayList<>();
        if (!filters.isEmpty()) {
            for (EntityKey filterField : filters.keySet()) {
                EntityKeyMapping mapping = new EntityKeyMapping();
                mapping.setIndex(index);
                mapping.setAlias(String.format("alias%s", index));
                mapping.setKeyFilters(filters.get(filterField));
                mapping.setLatest(!filterField.getType().equals(EntityKeyType.ENTITY_FIELD));
                mapping.setEntityKey(filterField);
                mapping.setEntityKeyColumn(entityType, entityFilterType);
                mapping.setSelection(mapping.getEntityKeyColumn() == null);
                mappings.add(mapping);
                index += 1;
            }
        }

        return mappings;
    }


    private String buildAttributeSelection() {
        return buildTimeSeriesOrAttrSelection(true);
    }

    private String buildTimeSeriesSelection() {
        return buildTimeSeriesOrAttrSelection(false);
    }

    private String buildTimeSeriesOrAttrSelection(boolean attr) {
        String attrValAlias = getValueAlias();
        String attrTsAlias = getTsAlias();
        String attrValSelection =
                String.format("(coalesce(cast(%s.bool_v as varchar), '') || " +
                        "coalesce(%s.str_v, '') || " +
                        "coalesce(cast(%s.long_v as varchar), '') || " +
                        "coalesce(cast(%s.dbl_v as varchar), '') || " +
                        "coalesce(cast(%s.json_v as varchar), '')) as %s", alias, alias, alias, alias, alias, attrValAlias);
        String attrTsSelection = String.format("%s.%s as %s", alias, attr ? "last_update_ts" : "ts", attrTsAlias);
        if (this.isSortOrder) {
            String attrNumAlias = getSortOrderNumAlias();
            String attrVarcharAlias = getSortOrderStrAlias();
            String attrSortOrderSelection =
                    String.format("coalesce(%s.dbl_v, cast(%s.long_v as double precision), (case when %s.bool_v then 1 else 0 end)) %s," +
                            "coalesce(%s.str_v, cast(%s.json_v as varchar), '') %s", alias, alias, alias, attrNumAlias, alias, alias, attrVarcharAlias);
            return String.join(", ", attrValSelection, attrTsSelection, attrSortOrderSelection);
        } else {
            return String.join(", ", attrValSelection, attrTsSelection);
        }
    }

    public String getSortOrderStrAlias() {
        return getValueAlias() + "_so_varchar";
    }

    public String getSortOrderNumAlias() {
        return getValueAlias() + "_so_num";
    }

    private String buildKeyQuery(SqlQueryContext ctx, String alias, KeyFilter keyFilter,
                                 EntityFilterType filterType) {
        return this.buildPredicateQuery(ctx, alias, keyFilter.getKey(), keyFilter.getPredicate(), filterType);
    }

    private String buildPredicateQuery(SqlQueryContext ctx, String alias, EntityKey key,
                                       KeyFilterPredicate predicate, EntityFilterType filterType) {
        if (predicate.getType().equals(FilterPredicateType.COMPLEX)) {
            return this.buildComplexPredicateQuery(ctx, alias, key, (ComplexFilterPredicate) predicate, filterType);
        } else {
            return this.buildSimplePredicateQuery(ctx, alias, key, predicate, filterType);
        }
    }

    private String buildComplexPredicateQuery(SqlQueryContext ctx, String alias, EntityKey key,
                                              ComplexFilterPredicate predicate, EntityFilterType filterType) {
        String result = predicate.getPredicates().stream()
                .map(keyFilterPredicate -> this.buildPredicateQuery(ctx, alias, key, keyFilterPredicate, filterType))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining(" " + predicate.getOperation().name() + " "));
        if (!result.trim().isEmpty()) {
            result = "( " + result + " )";
        }
        return result;
    }

    private String buildSimplePredicateQuery(SqlQueryContext ctx, String alias, EntityKey key,
                                             KeyFilterPredicate predicate, EntityFilterType filterType) {
        if (key.getType().equals(EntityKeyType.ENTITY_FIELD)) {
            String field = (getEntityKeyColumn() != null) ? alias + "." + getEntityKeyColumn() : alias;
            if (predicate.getType().equals(FilterPredicateType.NUMERIC)) {
                return this.buildNumericPredicateQuery(ctx, field, (NumericFilterPredicate) predicate);
            } else if (predicate.getType().equals(FilterPredicateType.STRING)) {
                if (key.getKey().equals("entityType") && !filterType.equals(EntityFilterType.RELATIONS_QUERY)) {
                    field = ctx.getEntityType().toString();
                    return this.buildStringPredicateQuery(ctx, field, (StringFilterPredicate) predicate)
                            .replace("lower(" + field, "lower('" + field + "'")
                            .replace(field + " ", "'" + field + "' ");
                } else {
                    return this.buildStringPredicateQuery(ctx, field, (StringFilterPredicate) predicate);
                }
            } else {
                return this.buildBooleanPredicateQuery(ctx, field, (BooleanFilterPredicate) predicate);
            }
        } else {
            if (predicate.getType().equals(FilterPredicateType.NUMERIC)) {
                String longQuery = this.buildNumericPredicateQuery(ctx, alias + ".long_v", (NumericFilterPredicate) predicate);
                String doubleQuery = this.buildNumericPredicateQuery(ctx, alias + ".dbl_v", (NumericFilterPredicate) predicate);
                return String.format("(%s or %s)", longQuery, doubleQuery);
            } else {
                String column = predicate.getType().equals(FilterPredicateType.STRING) ? "str_v" : "bool_v";
                String field = alias + "." + column;
                if (predicate.getType().equals(FilterPredicateType.STRING)) {
                    return this.buildStringPredicateQuery(ctx, field, (StringFilterPredicate) predicate);
                } else {
                    return this.buildBooleanPredicateQuery(ctx, field, (BooleanFilterPredicate) predicate);
                }
            }
        }
    }

    private String buildStringPredicateQuery(SqlQueryContext ctx, String field, StringFilterPredicate stringFilterPredicate) {
        String operationField = field;
        String paramName = getNextParameterName(field);
        String value = stringFilterPredicate.getValue().getValue();
        if (value.isEmpty()) {
            return "";
        }
        String stringOperationQuery = "";
        if (stringFilterPredicate.isIgnoreCase()) {
            value = value.toLowerCase();
            operationField = String.format("lower(%s)", operationField);
        }
        switch (stringFilterPredicate.getOperation()) {
            case EQUAL:
                stringOperationQuery = String.format("%s = :%s)", operationField, paramName);
                break;
            case NOT_EQUAL:
                stringOperationQuery = String.format("%s != :%s or %s is null)", operationField, paramName, operationField);
                break;
            case STARTS_WITH:
                value += "%";
                stringOperationQuery = String.format("%s like :%s)", operationField, paramName);
                break;
            case ENDS_WITH:
                value = "%" + value;
                stringOperationQuery = String.format("%s like :%s)", operationField, paramName);
                break;
            case CONTAINS:
                value = "%" + value + "%";
                stringOperationQuery = String.format("%s like :%s)", operationField, paramName);
                break;
            case NOT_CONTAINS:
                value = "%" + value + "%";
                stringOperationQuery = String.format("%s not like :%s or %s is null)", operationField, paramName, operationField);
                break;
            case IN:
                stringOperationQuery = String.format("%s in (:%s))", operationField, paramName);
                break;
            case NOT_IN:
                stringOperationQuery = String.format("%s not in (:%s))", operationField, paramName);
                break;
        }
        switch (stringFilterPredicate.getOperation()) {
            case IN:
            case NOT_IN:
                ctx.addStringListParameter(paramName, splitByCommaWithoutQuotes(value));
                break;
            default:
                ctx.addStringParameter(paramName, value);
        }
        return String.format("((%s is not null and %s)", field, stringOperationQuery);
    }

    private String buildNumericPredicateQuery(SqlQueryContext ctx, String field, NumericFilterPredicate numericFilterPredicate) {
        String paramName = getNextParameterName(field);
        ctx.addDoubleParameter(paramName, numericFilterPredicate.getValue().getValue());
        String numericOperationQuery = "";
        switch (numericFilterPredicate.getOperation()) {
            case EQUAL:
                numericOperationQuery = String.format("%s = :%s", field, paramName);
                break;
            case NOT_EQUAL:
                numericOperationQuery = String.format("%s != :%s", field, paramName);
                break;
            case GREATER:
                numericOperationQuery = String.format("%s > :%s", field, paramName);
                break;
            case GREATER_OR_EQUAL:
                numericOperationQuery = String.format("%s >= :%s", field, paramName);
                break;
            case LESS:
                numericOperationQuery = String.format("%s < :%s", field, paramName);
                break;
            case LESS_OR_EQUAL:
                numericOperationQuery = String.format("%s <= :%s", field, paramName);
                break;
        }
        return String.format("(%s is not null and %s)", field, numericOperationQuery);
    }

    private String buildBooleanPredicateQuery(SqlQueryContext ctx, String field,
                                              BooleanFilterPredicate booleanFilterPredicate) {
        String paramName = getNextParameterName(field);
        ctx.addBooleanParameter(paramName, booleanFilterPredicate.getValue().getValue());
        String booleanOperationQuery = "";
        switch (booleanFilterPredicate.getOperation()) {
            case EQUAL:
                booleanOperationQuery = String.format("%s = :%s", field, paramName);
                break;
            case NOT_EQUAL:
                booleanOperationQuery = String.format("%s != :%s", field, paramName);
                break;
        }
        return String.format("(%s is not null and %s)", field, booleanOperationQuery);
    }

    private String getNextParameterName(String field) {
        paramIdx++;
        return field.replace(".", "_") + "_" + paramIdx;
    }
}
