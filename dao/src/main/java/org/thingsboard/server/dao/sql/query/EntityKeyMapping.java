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

import lombok.Data;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class EntityKeyMapping {

    private static final Map<EntityType, Set<String>> allowedEntityFieldMap = new HashMap<>();
    private static final Map<String, String> entityFieldColumnMap = new HashMap<>();
    private static final Map<EntityType, Map<String, String>> aliases = new HashMap<>();

    private static final String CREATED_TIME = "createdTime";
    private static final String ENTITY_TYPE = "entityType";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String LABEL = "label";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String EMAIL = "email";
    private static final String TITLE = "title";
    private static final String REGION = "region";
    private static final String COUNTRY = "country";
    private static final String STATE = "state";
    private static final String CITY = "city";
    private static final String ADDRESS = "address";
    private static final String ADDRESS_2 = "address2";
    private static final String ZIP = "zip";
    private static final String PHONE = "phone";

    public static final List<String> commonEntityFields = Arrays.asList(CREATED_TIME, ENTITY_TYPE, NAME);
    public static final List<String> dashboardEntityFields = Arrays.asList(CREATED_TIME, ENTITY_TYPE, TITLE);
    public static final List<String> labeledEntityFields = Arrays.asList(CREATED_TIME, ENTITY_TYPE, NAME, TYPE, LABEL);
    public static final List<String> contactBasedEntityFields = Arrays.asList(CREATED_TIME, ENTITY_TYPE, EMAIL, TITLE, COUNTRY, STATE, CITY, ADDRESS, ADDRESS_2, ZIP, PHONE);

    public static final Set<String> commonEntityFieldsSet = new HashSet<>(commonEntityFields);
    public static final Set<String> relationQueryEntityFieldsSet = new HashSet<>(Arrays.asList(CREATED_TIME, ENTITY_TYPE, NAME, TYPE, LABEL, FIRST_NAME, LAST_NAME, EMAIL, REGION, TITLE, COUNTRY, STATE, CITY, ADDRESS, ADDRESS_2, ZIP, PHONE));

    static {
        allowedEntityFieldMap.put(EntityType.DEVICE, new HashSet<>(labeledEntityFields));
        allowedEntityFieldMap.put(EntityType.ASSET, new HashSet<>(labeledEntityFields));
        allowedEntityFieldMap.put(EntityType.ENTITY_VIEW, new HashSet<>(labeledEntityFields));

        allowedEntityFieldMap.put(EntityType.TENANT, new HashSet<>(contactBasedEntityFields));
        allowedEntityFieldMap.get(EntityType.TENANT).add(REGION);
        allowedEntityFieldMap.put(EntityType.CUSTOMER, new HashSet<>(contactBasedEntityFields));

        allowedEntityFieldMap.put(EntityType.USER, new HashSet<>(Arrays.asList(CREATED_TIME, FIRST_NAME, LAST_NAME, EMAIL)));

        allowedEntityFieldMap.put(EntityType.DASHBOARD, new HashSet<>(dashboardEntityFields));
        allowedEntityFieldMap.put(EntityType.RULE_CHAIN, new HashSet<>(commonEntityFields));
        allowedEntityFieldMap.put(EntityType.RULE_NODE, new HashSet<>(commonEntityFields));
        allowedEntityFieldMap.put(EntityType.WIDGET_TYPE, new HashSet<>(commonEntityFields));
        allowedEntityFieldMap.put(EntityType.WIDGETS_BUNDLE, new HashSet<>(commonEntityFields));

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

        Map<String, String> contactBasedAliases = new HashMap<>();
        contactBasedAliases.put(NAME, TITLE);
        contactBasedAliases.put(LABEL, TITLE);
        aliases.put(EntityType.TENANT, contactBasedAliases);
        aliases.put(EntityType.CUSTOMER, contactBasedAliases);
        aliases.put(EntityType.DASHBOARD, contactBasedAliases);
        Map<String, String> commonEntityAliases = new HashMap<>();
        commonEntityAliases.put(TITLE, NAME);
        aliases.put(EntityType.DEVICE, commonEntityAliases);
        aliases.put(EntityType.ASSET, commonEntityAliases);
        aliases.put(EntityType.ENTITY_VIEW, commonEntityAliases);
        aliases.put(EntityType.WIDGETS_BUNDLE, commonEntityAliases);

        Map<String, String> userEntityAliases = new HashMap<>();
        userEntityAliases.put(TITLE, EMAIL);
        userEntityAliases.put(LABEL, EMAIL);
        userEntityAliases.put(NAME, EMAIL);
        aliases.put(EntityType.USER, userEntityAliases);
    }

    private int index;
    private String alias;
    private boolean isLatest;
    private boolean isSelection;
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
                Set<String> existingEntityFields = getExistingEntityFields(filterType, entityType);
                String alias = getEntityFieldAlias(filterType, entityType);
                if (existingEntityFields.contains(alias)) {
                    String column = entityFieldColumnMap.get(alias);
                    return String.format("e.%s as %s", column, getValueAlias());
                } else {
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

    public Stream<String> toQueries(QueryContext ctx, EntityFilterType filterType) {
        if (hasFilter()) {
            String keyAlias = entityKey.getType().equals(EntityKeyType.ENTITY_FIELD) ? "e" : alias;
            return keyFilters.stream().map(keyFilter ->
                    this.buildKeyQuery(ctx, keyAlias, keyFilter, filterType));
        } else {
            return null;
        }
    }

    public String toLatestJoin(QueryContext ctx, EntityFilter entityFilter, EntityType entityType) {
        String entityTypeStr;
        if (entityFilter.getType().equals(EntityFilterType.RELATIONS_QUERY)) {
            entityTypeStr = "entities.entity_type";
        } else {
            entityTypeStr = "'" + entityType.name() + "'";
        }
        ctx.addStringParameter(alias + "_key_id", entityKey.getKey());
        if (entityKey.getType().equals(EntityKeyType.TIME_SERIES)) {
            String join = hasFilter() ? "left join" : "left outer join";
            return String.format("%s ts_kv_latest %s ON %s.entity_id=entities.id AND %s.key = (select key_id from ts_kv_dictionary where key = :%s_key_id)",
                    join, alias, alias, alias, alias);
        } else {
            String query;
            if (!entityKey.getType().equals(EntityKeyType.ATTRIBUTE)) {
                String join = hasFilter() ? "left join" : "left outer join";
                query = String.format("%s attribute_kv %s ON %s.entity_id=entities.id AND %s.entity_type=%s AND %s.attribute_key=:%s_key_id",
                        join, alias, alias, alias, entityTypeStr, alias, alias);
                String scope;
                if (entityKey.getType().equals(EntityKeyType.CLIENT_ATTRIBUTE)) {
                    scope = DataConstants.CLIENT_SCOPE;
                } else if (entityKey.getType().equals(EntityKeyType.SHARED_ATTRIBUTE)) {
                    scope = DataConstants.SHARED_SCOPE;
                } else {
                    scope = DataConstants.SERVER_SCOPE;
                }
                query = String.format("%s AND %s.attribute_type='%s'", query, alias, scope);
            } else {
                String join = hasFilter() ? "join LATERAL" : "left join LATERAL";
                query = String.format("%s (select * from attribute_kv %s WHERE %s.entity_id=entities.id AND %s.entity_type=%s AND %s.attribute_key=:%s_key_id " +
                                "ORDER BY %s.last_update_ts DESC limit 1) as %s ON true",
                        join, alias, alias, alias, entityTypeStr, alias, alias, alias, alias);
            }
            return query;
        }
    }

    public static String buildSelections(List<EntityKeyMapping> mappings, EntityFilterType filterType, EntityType entityType) {
        return mappings.stream().map(mapping -> mapping.toSelection(filterType, entityType)).collect(
                Collectors.joining(", "));
    }

    public static String buildLatestJoins(QueryContext ctx, EntityFilter entityFilter, EntityType entityType, List<EntityKeyMapping> latestMappings) {
        return latestMappings.stream().map(mapping -> mapping.toLatestJoin(ctx, entityFilter, entityType)).collect(
                Collectors.joining(" "));
    }

    public static String buildQuery(QueryContext ctx, List<EntityKeyMapping> mappings, EntityFilterType filterType) {
        return mappings.stream().flatMap(mapping -> mapping.toQueries(ctx, filterType)).filter(Objects::nonNull).collect(
                Collectors.joining(" AND "));
    }

    public static List<EntityKeyMapping> prepareKeyMapping(EntityDataQuery query) {
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
                    mapping.setEntityKey(key);
                    return mapping;
                }
        ).collect(Collectors.toList());
        List<EntityKeyMapping> latestMappings = latestValues.stream().map(
                key -> {
                    EntityKeyMapping mapping = new EntityKeyMapping();
                    mapping.setLatest(true);
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
                mapping.setSelection(false);
                mapping.setEntityKey(filterField);
                mappings.add(mapping);
                index += 1;
            }
        }

        return mappings;
    }

    private String buildAttributeSelection() {
        String attrValAlias = getValueAlias();
        String attrTsAlias = getTsAlias();
        String attrValSelection =
                String.format("(coalesce(cast(%s.bool_v as varchar), '') || " +
                        "coalesce(%s.str_v, '') || " +
                        "coalesce(cast(%s.long_v as varchar), '') || " +
                        "coalesce(cast(%s.dbl_v as varchar), '') || " +
                        "coalesce(cast(%s.json_v as varchar), '')) as %s", alias, alias, alias, alias, alias, attrValAlias);
        String attrTsSelection = String.format("%s.last_update_ts as %s", alias, attrTsAlias);
        return String.join(", ", attrValSelection, attrTsSelection);
    }

    private String buildTimeSeriesSelection() {
        String attrValAlias = getValueAlias();
        String attrTsAlias = getTsAlias();
        String attrValSelection =
                String.format("(coalesce(cast(%s.bool_v as varchar), '') || " +
                        "coalesce(%s.str_v, '') || " +
                        "coalesce(cast(%s.long_v as varchar), '') || " +
                        "coalesce(cast(%s.dbl_v as varchar), '') || " +
                        "coalesce(cast(%s.json_v as varchar), '')) as %s", alias, alias, alias, alias, alias, attrValAlias);
        String attrTsSelection = String.format("%s.ts as %s", alias, attrTsAlias);
        return String.join(", ", attrValSelection, attrTsSelection);
    }

    private String buildKeyQuery(QueryContext ctx, String alias, KeyFilter keyFilter,
                                 EntityFilterType filterType) {
        return this.buildPredicateQuery(ctx, alias, keyFilter.getKey(), keyFilter.getPredicate(), filterType);
    }

    private String buildPredicateQuery(QueryContext ctx, String alias, EntityKey key,
                                       KeyFilterPredicate predicate, EntityFilterType filterType) {
        if (predicate.getType().equals(FilterPredicateType.COMPLEX)) {
            return this.buildComplexPredicateQuery(ctx, alias, key, (ComplexFilterPredicate) predicate, filterType);
        } else {
            return this.buildSimplePredicateQuery(ctx, alias, key, predicate, filterType);
        }
    }

    private String buildComplexPredicateQuery(QueryContext ctx, String alias, EntityKey key,
                                              ComplexFilterPredicate predicate, EntityFilterType filterType) {
        return predicate.getPredicates().stream()
                .map(keyFilterPredicate -> this.buildPredicateQuery(ctx, alias, key, keyFilterPredicate, filterType))
                .filter(Objects::nonNull).collect(Collectors.joining(
                        " " + predicate.getOperation().name() + " "
                ));
    }

    private String buildSimplePredicateQuery(QueryContext ctx, String alias, EntityKey key,
                                             KeyFilterPredicate predicate, EntityFilterType filterType) {
        if (key.getType().equals(EntityKeyType.ENTITY_FIELD)) {
            Set<String> existingEntityFields = getExistingEntityFields(filterType, ctx.getEntityType());
            String entityFieldAlias = getEntityFieldAlias(filterType, ctx.getEntityType());
            String column = null;
            if (existingEntityFields.contains(entityFieldAlias)) {
                column = entityFieldColumnMap.get(entityFieldAlias);
            }
            if (column != null) {
                String field = alias + "." + column;
                if (predicate.getType().equals(FilterPredicateType.NUMERIC)) {
                    return this.buildNumericPredicateQuery(ctx, field, (NumericFilterPredicate) predicate);
                } else if (predicate.getType().equals(FilterPredicateType.STRING)) {
                    return this.buildStringPredicateQuery(ctx, field, (StringFilterPredicate) predicate);
                } else {
                    return this.buildBooleanPredicateQuery(ctx, field, (BooleanFilterPredicate) predicate);
                }
            } else {
                return null;
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

    private String buildStringPredicateQuery(QueryContext ctx, String field, StringFilterPredicate stringFilterPredicate) {
        String operationField = field;
        String paramName = getNextParameterName(field);
        String value = stringFilterPredicate.getValue().getValue();
        String stringOperationQuery = "";
        if (stringFilterPredicate.isIgnoreCase()) {
            value = value.toLowerCase();
            operationField = String.format("lower(%s)", operationField);
        }
        switch (stringFilterPredicate.getOperation()) {
            case EQUAL:
                stringOperationQuery = String.format("%s = :%s) or (%s is null and :%s = '')", operationField, paramName, operationField, paramName);
                break;
            case NOT_EQUAL:
                stringOperationQuery = String.format("%s != :%s) or (%s is null and :%s != '')", operationField, paramName, operationField, paramName);
                break;
            case STARTS_WITH:
                value += "%";
                stringOperationQuery = String.format("%s like :%s) or (%s is null and :%s = '%%')", operationField, paramName, operationField, paramName);
                break;
            case ENDS_WITH:
                value = "%" + value;
                stringOperationQuery = String.format("%s like :%s) or (%s is null and :%s = '%%')", operationField, paramName, operationField, paramName);
                break;
            case CONTAINS:
                if (value.length()>1) {
                    value = "%" + value + "%";
                }
                stringOperationQuery = String.format("%s like :%s) or (%s is null and :%s = '')", operationField, paramName, operationField, paramName);
                break;
            case NOT_CONTAINS:
                if (value.length()>1) {
                    value = "%" + value + "%";
                }
                stringOperationQuery = String.format("%s not like :%s) or (%s is null and :%s != '')", operationField, paramName, operationField, paramName);
                break;
        }
        ctx.addStringParameter(paramName, value);
        return String.format("((%s is not null and %s)", field, stringOperationQuery);
    }

    private String buildNumericPredicateQuery(QueryContext ctx, String field, NumericFilterPredicate numericFilterPredicate) {
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

    private String buildBooleanPredicateQuery(QueryContext ctx, String field,
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
