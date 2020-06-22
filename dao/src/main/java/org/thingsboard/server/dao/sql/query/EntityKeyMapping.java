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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class EntityKeyMapping {

    public static final Map<String, String> entityFieldColumnMap = new HashMap<>();

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

    private int index;
    private String alias;
    private boolean isLatest;
    private boolean isSelection;
    private boolean isSortOrder;
    private boolean ignore = false;
    private List<KeyFilter> keyFilters;
    private EntityKey entityKey;

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

    public String toSelection() {
        if (entityKey.getType().equals(EntityKeyType.ENTITY_FIELD)) {
            String column = entityFieldColumnMap.get(entityKey.getKey());
            return String.format("e.%s as %s", column, getValueAlias());
        } else if (entityKey.getType().equals(EntityKeyType.TIME_SERIES)) {
            return buildTimeSeriesSelection();
        } else {
            return buildAttributeSelection();
        }
    }

    public Stream<String> toQueries() {
        if (hasFilter()) {
            String keyAlias = entityKey.getType().equals(EntityKeyType.ENTITY_FIELD) ? "e" : alias;
            return keyFilters.stream().map(keyFilter ->
                    this.buildKeyQuery(keyAlias, keyFilter));
        } else {
            return null;
        }
    }

    public String toLatestJoin(EntityFilter entityFilter, EntityType entityType) {
        String entityTypeStr;
        if (entityFilter.getType().equals(EntityFilterType.RELATIONS_QUERY)) {
            entityTypeStr = "entities.entity_type";
        } else {
            entityTypeStr = "'" + entityType.name() + "'";
        }
        String join = hasFilter() ? "left join" : "left outer join";
        if (entityKey.getType().equals(EntityKeyType.TIME_SERIES)) {
            return String.format("%s ts_kv_latest %s ON %s.entity_id=to_uuid(entities.id) AND %s.key = (select key_id from ts_kv_dictionary where key = '%s')",
                    join, alias, alias, alias, entityKey.getKey());
        } else {
            String query = String.format("%s attribute_kv %s ON %s.entity_id=entities.id AND %s.entity_type=%s AND %s.attribute_key='%s'",
                    join, alias, alias, alias, entityTypeStr, alias, entityKey.getKey());
            if (!entityKey.getType().equals(EntityKeyType.ATTRIBUTE)) {
                String scope;
                if (entityKey.getType().equals(EntityKeyType.CLIENT_ATTRIBUTE)) {
                    scope = DataConstants.CLIENT_SCOPE;
                } else if (entityKey.getType().equals(EntityKeyType.SHARED_ATTRIBUTE)) {
                    scope = DataConstants.SHARED_SCOPE;
                } else {
                    scope = DataConstants.SERVER_SCOPE;
                }
                query = String.format("%s AND %s.attribute_type=%s", query, alias, scope);
            }
            return query;
        }
    }

    public static String buildSelections(List<EntityKeyMapping> mappings) {
        return mappings.stream().map(EntityKeyMapping::toSelection).collect(
                Collectors.joining(", "));
    }

    public static String buildLatestJoins(EntityFilter entityFilter, EntityType entityType, List<EntityKeyMapping> latestMappings) {
        return latestMappings.stream().map(mapping -> mapping.toLatestJoin(entityFilter, entityType)).collect(
                Collectors.joining(" "));
    }

    public static String buildQuery(List<EntityKeyMapping> mappings) {
        return mappings.stream().flatMap(EntityKeyMapping::toQueries).collect(
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

    private String buildKeyQuery(String alias, KeyFilter keyFilter) {
        return this.buildPredicateQuery(alias, keyFilter.getKey(), keyFilter.getPredicate());
    }

    private String buildPredicateQuery(String alias, EntityKey key, KeyFilterPredicate predicate) {
        if (predicate.getType().equals(FilterPredicateType.COMPLEX)) {
            return this.buildComplexPredicateQuery(alias, key, (ComplexFilterPredicate) predicate);
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
                return this.buildNumericPredicateQuery(alias + "." + column, (NumericFilterPredicate) predicate);
            } else {
                String longQuery = this.buildNumericPredicateQuery(alias + ".long_v", (NumericFilterPredicate) predicate);
                String doubleQuery = this.buildNumericPredicateQuery(alias + ".dbl_v", (NumericFilterPredicate) predicate);
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
                return this.buildStringPredicateQuery(field, (StringFilterPredicate) predicate);
            } else {
                return this.buildBooleanPredicateQuery(field, (BooleanFilterPredicate) predicate);
            }
        }
    }

    private String buildStringPredicateQuery(String field, StringFilterPredicate stringFilterPredicate) {
        String operationField = field;
        String value = stringFilterPredicate.getValue();
        String stringOperationQuery = "";
        if (stringFilterPredicate.isIgnoreCase()) {
            value = value.toLowerCase();
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
                stringOperationQuery = String.format("%s like '%s%%'", operationField, value);
                break;
            case ENDS_WITH:
                stringOperationQuery = String.format("%s like '%%%s'", operationField, value);
                break;
            case CONTAINS:
                stringOperationQuery = String.format("%s like '%%%s%%'", operationField, value);
                break;
            case NOT_CONTAINS:
                stringOperationQuery = String.format("%s not like '%%%s%%'", operationField, value);
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
}
