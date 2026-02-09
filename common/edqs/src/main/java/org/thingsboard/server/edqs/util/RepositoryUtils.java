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
package org.thingsboard.server.edqs.util;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edqs.DataPoint;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.query.EntityNameFilter;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.FilterPredicateType;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.DataKey;
import org.thingsboard.server.edqs.query.EdqsCountQuery;
import org.thingsboard.server.edqs.query.EdqsDataQuery;
import org.thingsboard.server.edqs.query.EdqsFilter;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.query.SortableEntityData;
import org.thingsboard.server.edqs.repo.KeyDictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.thingsboard.server.common.data.StringUtils.equalsAny;
import static org.thingsboard.server.common.data.StringUtils.splitByCommaWithoutQuotes;
import static org.thingsboard.server.common.data.query.ComplexFilterPredicate.ComplexOperation.AND;
import static org.thingsboard.server.common.data.query.ComplexFilterPredicate.ComplexOperation.OR;

@Slf4j
public class RepositoryUtils {

    public static final Comparator<SortableEntityData> SORT_ASC = Comparator.comparing(SortableEntityData::getSortValue, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(sp -> sp.getId().toString());

    public static final Comparator<SortableEntityData> SORT_DESC =  Comparator.comparing(SortableEntityData::getSortValue, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(sp -> sp.getId().toString()).reversed();

    public static EntityType resolveEntityType(EntityFilter entityFilter) {
        return switch (entityFilter.getType()) {
            case SINGLE_ENTITY -> ((SingleEntityFilter) entityFilter).getSingleEntity().getEntityType();
            case ENTITY_LIST -> ((EntityListFilter) entityFilter).getEntityType();
            case ENTITY_NAME -> ((EntityNameFilter) entityFilter).getEntityType();
            case ENTITY_TYPE -> ((EntityTypeFilter) entityFilter).getEntityType();
            case ASSET_TYPE, ASSET_SEARCH_QUERY -> EntityType.ASSET;
            case DEVICE_TYPE, DEVICE_SEARCH_QUERY -> EntityType.DEVICE;
            case ENTITY_VIEW_TYPE, ENTITY_VIEW_SEARCH_QUERY -> EntityType.ENTITY_VIEW;
            case EDGE_TYPE, EDGE_SEARCH_QUERY -> EntityType.EDGE;
            case RELATIONS_QUERY -> {
                RelationsQueryFilter rgf = (RelationsQueryFilter) entityFilter;
                yield rgf.isMultiRoot() ? rgf.getMultiRootEntitiesType() : rgf.getRootEntity().getEntityType();
            }
            case API_USAGE_STATE -> EntityType.API_USAGE_STATE;
        };
    }

    public static boolean customerUserIsTryingToAccessTenantEntity(QueryContext ctx, EntityFilter entityFilter) {
        if (ctx.isTenantUser()) {
            return false;
        } else {
            return switch (entityFilter.getType()) {
                case SINGLE_ENTITY -> {
                    SingleEntityFilter seFilter = (SingleEntityFilter) entityFilter;
                    yield isSystemOrTenantEntity(seFilter.getSingleEntity().getEntityType());
                }
                case ENTITY_LIST -> {
                    EntityListFilter elFilter = (EntityListFilter) entityFilter;
                    yield isSystemOrTenantEntity(elFilter.getEntityType());
                }
                case ENTITY_NAME -> {
                    EntityNameFilter enFilter = (EntityNameFilter) entityFilter;
                    yield isSystemOrTenantEntity(enFilter.getEntityType());
                }
                case ENTITY_TYPE -> {
                    EntityTypeFilter etFilter = (EntityTypeFilter) entityFilter;
                    yield isSystemOrTenantEntity(etFilter.getEntityType());
                }
                default -> false;
            };
        }
    }

    private static boolean isSystemOrTenantEntity(EntityType entityType) {
        return switch (entityType) {
            case DEVICE_PROFILE, ASSET_PROFILE, RULE_CHAIN, TENANT,
                    TENANT_PROFILE, WIDGET_TYPE, WIDGETS_BUNDLE -> true;
            default -> false;
        };
    }

    public static EdqsDataQuery toNewQuery(EntityDataQuery oldQuery) {
        var query = EdqsDataQuery.builder();
        query.page(oldQuery.getPageLink().getPage());
        query.pageSize(oldQuery.getPageLink().getPageSize());
        query.textSearch(oldQuery.getPageLink().getTextSearch());
        var sortOrder = oldQuery.getPageLink().getSortOrder();
        if (sortOrder != null && toNewKey(sortOrder.getKey()) != null) {
            query.sortKey(toNewKey(sortOrder.getKey()));
            query.sortDirection(sortOrder.getDirection());
        } else {
            query.sortKey(new DataKey(EntityKeyType.ENTITY_FIELD, "createdTime", null));
            query.sortDirection(EntityDataSortOrder.Direction.DESC);
        }
        query.entityFilter(oldQuery.getEntityFilter());
        query.keyFilters(toKeyFilters(oldQuery.getKeyFilters()));
        query.entityFields(toNewKeys(oldQuery.getEntityFields()));
        query.latestValues(toNewKeys(oldQuery.getLatestValues()));
        return query.build();
    }

    public static EdqsCountQuery toNewQuery(EntityCountQuery oldQuery) {
        return EdqsCountQuery.builder()
                .entityFilter(oldQuery.getEntityFilter())
                .hasKeyFilters(CollectionsUtil.isNotEmpty(oldQuery.getKeyFilters()))
                .keyFilters(toKeyFilters(oldQuery.getKeyFilters()))
                .build();
    }

    private static List<EdqsFilter> toKeyFilters(List<KeyFilter> keyFilters) {
        if (keyFilters == null || keyFilters.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<EdqsFilter> result = new ArrayList<>();
            for (KeyFilter entityFilter : keyFilters) {
                var newKey = toNewKey(entityFilter.getKey());
                if (newKey != null) {
                    result.add(new EdqsFilter(newKey, entityFilter.getValueType(), entityFilter.getPredicate()));
                }
            }
            return result;
        }
    }

    private static DataKey toNewKey(EntityKey entityKey) {
        if (EntityKeyType.ENTITY_FIELD.equals(entityKey.getType())) {
            return new DataKey(entityKey.getType(), entityKey.getKey(), null);
        }
        Integer keyId = KeyDictionary.get(entityKey.getKey());
        if (keyId != null) {
            return new DataKey(entityKey.getType(), entityKey.getKey(), keyId);
        } else {
            log.warn("Missing dictionary key for {}", entityKey.getKey());
            return null;
        }
    }

    private static List<DataKey> toNewKeys(List<EntityKey> entityKeys) {
        if (entityKeys == null || entityKeys.isEmpty()) {
            return Collections.emptyList();
        } else {
            var result = new ArrayList<DataKey>(entityKeys.size());
            for (EntityKey entityKey : entityKeys) {
                var newKey = toNewKey(entityKey);
                if (newKey != null) {
                    result.add(newKey);
                }
            }
            return result;
        }
    }

    public static boolean checkKeyFilters(EntityData entity, List<EdqsFilter> keyFilters) {
        for (EdqsFilter keyFilter : keyFilters) {
            EntityKeyValueType valueType = keyFilter.valueType();
            if (valueType == null) {
                valueType = switch (keyFilter.predicate().getType()) {
                    case STRING -> EntityKeyValueType.STRING;
                    case NUMERIC -> EntityKeyValueType.NUMERIC;
                    case BOOLEAN -> EntityKeyValueType.BOOLEAN;
                    default -> throw new IllegalStateException();
                };
            }
            DataKey dataKey = keyFilter.key();
            DataPoint dp = entity.getDataPoint(dataKey, null);
            boolean checkResult = switch (valueType) {
                case STRING -> {
                    String str = dp != null ? dp.valueToString() : null;
                    yield (dataKey.type() == EntityKeyType.ENTITY_FIELD) ? (str == null || checkKeyFilter(str, keyFilter.predicate())) :
                            (str != null && checkKeyFilter(str, keyFilter.predicate()));
                }
                case BOOLEAN -> {
                    Boolean booleanValue = dp != null ? dp.getBool() : null;
                    yield booleanValue != null && checkKeyFilter(booleanValue, keyFilter.predicate());
                }
                case DATE_TIME, NUMERIC -> {
                    Double doubleValue = dp != null ? dp.getDouble() : null;
                    yield doubleValue != null && checkKeyFilter(doubleValue, keyFilter.predicate());
                }
            };
            if (!checkResult) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkKeyFilter(String value, KeyFilterPredicate keyFilterPredicate) {
        if (keyFilterPredicate.getType() == FilterPredicateType.COMPLEX) {
            return checkComplexKeyFilter(value, (ComplexFilterPredicate) keyFilterPredicate, RepositoryUtils::checkKeyFilter);
        }
        if (keyFilterPredicate.getType() != FilterPredicateType.STRING) {
            throw new IllegalStateException("Not implemented");
        }
        StringFilterPredicate predicate = (StringFilterPredicate) keyFilterPredicate;
        String predicateValue = predicate.getValue().getValue();
        if (StringUtils.isEmpty(predicateValue)) {
            return true;
        }
        if (predicate.isIgnoreCase()) {
            predicateValue = predicateValue.toLowerCase();
            value = value.toLowerCase();
        }
        return switch (predicate.getOperation()) {
            case EQUAL -> value.equals(predicateValue);
            case STARTS_WITH -> toSqlLikePattern(predicateValue, "^", ".*").matcher(value).matches();
            case ENDS_WITH -> toSqlLikePattern(predicateValue, ".*", "$").matcher(value).matches();
            case NOT_EQUAL -> !value.equals(predicateValue);
            case CONTAINS -> toSqlLikePattern(predicateValue, ".*", ".*").matcher(value).matches();
            case NOT_CONTAINS -> !toSqlLikePattern(predicateValue, ".*", ".*").matcher(value).matches();
            case IN -> equalsAny(value, splitByCommaWithoutQuotes(predicateValue));
            case NOT_IN -> !equalsAny(value, splitByCommaWithoutQuotes(predicateValue));
        };
    }

    public static boolean checkKeyFilter(Double value, KeyFilterPredicate keyFilterPredicate) {
        if (keyFilterPredicate.getType() == FilterPredicateType.COMPLEX) {
            return checkComplexKeyFilter(value, (ComplexFilterPredicate) keyFilterPredicate, RepositoryUtils::checkKeyFilter);
        }
        if (keyFilterPredicate.getType() != FilterPredicateType.NUMERIC) {
            throw new IllegalStateException("Not implemented");
        }
        NumericFilterPredicate predicate = (NumericFilterPredicate) keyFilterPredicate;
        Double predicateValue = predicate.getValue().getValue();
        return switch (predicate.getOperation()) {
            case EQUAL -> value.equals(predicateValue);
            case NOT_EQUAL -> !value.equals(predicateValue);
            case GREATER -> value.compareTo(predicateValue) > 0;
            case LESS -> value.compareTo(predicateValue) < 0;
            case GREATER_OR_EQUAL -> value.compareTo(predicateValue) >= 0;
            case LESS_OR_EQUAL -> value.compareTo(predicateValue) <= 0;
        };
    }

    public static boolean checkKeyFilter(Boolean value, KeyFilterPredicate keyFilterPredicate) {
        if (keyFilterPredicate.getType() == FilterPredicateType.COMPLEX) {
            return checkComplexKeyFilter(value, (ComplexFilterPredicate) keyFilterPredicate, RepositoryUtils::checkKeyFilter);
        }
        if (keyFilterPredicate.getType() != FilterPredicateType.BOOLEAN) {
            throw new IllegalStateException("Not implemented");
        }
        BooleanFilterPredicate predicate = (BooleanFilterPredicate) keyFilterPredicate;
        Boolean predicateValue = predicate.getValue().getValue();
        return switch (predicate.getOperation()) {
            case EQUAL -> value.equals(predicateValue);
            case NOT_EQUAL -> !value.equals(predicateValue);
        };
    }

    public static <T> boolean checkComplexKeyFilter(T value, ComplexFilterPredicate filterPredicates,
                                                    SimpleKeyFilter<T> simpleKeyFilter) {
        if (filterPredicates.getOperation() == AND) {
            for (KeyFilterPredicate filterPredicate : filterPredicates.getPredicates()) {
                if (!simpleKeyFilter.check(value, filterPredicate)) {
                    return false;
                }
            }
            return true;
        } else if (filterPredicates.getOperation() == OR) {
            for (KeyFilterPredicate filterPredicate : filterPredicates.getPredicates()) {

                // Emulate the SQL-like behavior of ThingsBoard's Entity Data Query service:
                // for COMPLEX filters, return no results if filter value is empty
                if (filterPredicate instanceof StringFilterPredicate stringFilterPredicate) {
                    if (StringUtils.isEmpty(stringFilterPredicate.getValue().getValue())) {
                        continue;
                    }
                }

                if (simpleKeyFilter.check(value, filterPredicate)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    public static Pattern toEntityNameSqlLikePattern(String filter) {
        if (StringUtils.isNotBlank(filter)) {
            return toSqlLikePattern(filter, "", ".*", true);
        }
        return null;
    }

    private static Pattern toSqlLikePattern(String value, String prefix, String suffix) {
        return toSqlLikePattern(value, prefix, suffix, false);
    }

    private static Pattern toSqlLikePattern(String value, String prefix, String suffix, boolean ignoreCase) {
        String regexValue;
        if (value.contains("%") || value.contains("_")) {
            regexValue = value
                    .replace("_", ".")
                    .replace("%", ".*");
            if ("^".equals(prefix)) {
                regexValue = "^" + regexValue + (regexValue.endsWith(".*") ? "" : ".*");
            } else if ("$".equals(suffix)) {
                regexValue = (regexValue.startsWith(".*") ? "" : ".*") + regexValue + "$";
            }
        } else {
            regexValue = prefix + Pattern.quote(value) + suffix;
        }
        return ignoreCase ? Pattern.compile(regexValue, Pattern.CASE_INSENSITIVE) : Pattern.compile(regexValue);
    }

    @FunctionalInterface
    public interface SimpleKeyFilter<T> {

        boolean check(T value, KeyFilterPredicate predicate);

    }

    public static TsValue toTsValue(long ts, DataPoint dp) {
        if (dp != null) {
            return new TsValue(dp.getTs() > 0 ? dp.getTs() : ts, dp.valueToString());
        } else {
            return TsValue.EMPTY;
        }
    }

    public static DataPoint getSortValue(EntityData entity, DataKey sortKey, QueryContext queryContext) {
        if (sortKey == null) {
            return null;
        }
       return entity.getDataPoint(sortKey, queryContext);
    }

    public static boolean checkFilters(EdqsQuery query, EntityData entity) {
        if (entity == null || entity.getFields() == null) {
            return false; // Entity was already removed or not arrived yet;
        }
        if (query.isHasKeyFilters() && !checkKeyFilters(entity, query.getKeyFilters())) {
            return false;
        }
        if (query instanceof EdqsDataQuery dataQuery) {
            return !dataQuery.isHasTextSearch() || checkTextSearch(entity, dataQuery);
        }
        return true;
    }

    private static boolean checkTextSearch(EntityData entityData, EdqsDataQuery query) {
        return Stream.concat(query.getEntityFields().stream(), query.getLatestValues().stream())
                .anyMatch(key -> {
                    DataPoint value = entityData.getDataPoint(key, null);
                    return value != null && containsIgnoreCase(value.valueToString(), query.getTextSearch());
                });
    }

}
