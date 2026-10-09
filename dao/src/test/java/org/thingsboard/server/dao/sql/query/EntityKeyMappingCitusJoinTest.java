// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.query;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityFilterType;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the latest-value join SQL emitted by {@link EntityKeyMapping#toLatestJoin} and
 * {@link EntityKeyMapping#buildLatestJoins}. The Citus branches are the most intricate SQL in the Citus PR
 * (CTE entity-id pushdown for time-series / scoped attributes, plus the DISTINCT-ON rewrite of the any-scope
 * LATERAL), and nothing else exercises the code that emits them — so these tests pin the generated shapes
 * (entity_id pushdown, read-flag predicate placement, DISTINCT-ON ordering) for both Citus and plain modes
 * to catch a regression (transposed alias, dropped predicate, wrong ordering) without a database.
 */
class EntityKeyMappingCitusJoinTest {

    // Stand-in for the filtered entities subquery threaded through toLatestJoin; only the pushdown any-scope
    // ATTRIBUTE arm re-inlines it (see EntityKeyMapping#toLatestJoin). Kept distinctive so assertions can match it.
    private static final String ENTITIES_QUERY = "select id from device_entities_src";

    private final EntityFilter filter = mockFilter();

    private static EntityFilter mockFilter() {
        EntityFilter f = mock(EntityFilter.class);
        when(f.getType()).thenReturn(EntityFilterType.SINGLE_ENTITY);
        return f;
    }

    private SqlQueryContext ctx(boolean citusEnabled) {
        QueryContext securityCtx = mock(QueryContext.class);
        when(securityCtx.getEntityType()).thenReturn(EntityType.DEVICE);
        return new SqlQueryContext(securityCtx, citusEnabled);
    }

    private SqlQueryContext pushdownCtx() {
        SqlQueryContext ctx = ctx(true);
        ctx.setPushdownEligible(true);
        return ctx;
    }

    private static EntityKeyMapping latestMapping(EntityKeyType type, String alias) {
        EntityKeyMapping mapping = new EntityKeyMapping();
        mapping.setLatest(true);
        mapping.setSelection(true);
        mapping.setSearchable(true);
        mapping.setEntityKey(new EntityKey(type, "temperature"));
        mapping.setAlias(alias);
        return mapping;
    }

    private static EntityKeyMapping latestMappingWithNumericFilter(EntityKeyType type, String alias) {
        EntityKeyMapping mapping = latestMapping(type, alias);
        KeyFilter keyFilter = new KeyFilter();
        keyFilter.setKey(mapping.getEntityKey());
        keyFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        predicate.setValue(new FilterPredicateValue<>(42.0));
        keyFilter.setPredicate(predicate);
        mapping.setKeyFilters(List.of(keyFilter));
        return mapping;
    }

    @Test
    void timeSeriesCitusJoinPushesEntityIdSetDown() {
        String join = latestMapping(EntityKeyType.TIME_SERIES, "ts")
                .toLatestJoin(ctx(true), filter, ENTITIES_QUERY);

        assertThat(join)
                .contains("select * from ts_kv_latest ts")
                .contains("ts.entity_id in (select id from entities)")
                .contains("ON ts.entity_id=entities.id AND entities." + DefaultEntityQueryRepository.TS_READ_FLAG + " = 1")
                .doesNotContain("LATERAL");
    }

    @Test
    void timeSeriesPushdownJoinUsesDirectColocatedJoin() {
        String join = latestMapping(EntityKeyType.TIME_SERIES, "ts")
                .toLatestJoin(pushdownCtx(), filter, ENTITIES_QUERY);

        assertThat(join)
                .contains("ts_kv_latest ts ON ts.entity_id = entities.id AND entities." + DefaultEntityQueryRepository.TS_READ_FLAG + " = 1")
                .contains("AND ts.key = (select key_id from key_dictionary where key = :")
                .doesNotContain("in (select id from entities)")
                .doesNotContain("select * from ts_kv_latest");
    }

    @Test
    void timeSeriesPlainJoinUsesDirectJoinWithReadFlagPrefix() {
        String join = latestMapping(EntityKeyType.TIME_SERIES, "ts")
                .toLatestJoin(ctx(false), filter, ENTITIES_QUERY);

        assertThat(join)
                .contains("ts_kv_latest ts ON")
                .contains("entities." + DefaultEntityQueryRepository.TS_READ_FLAG + " = 1 AND")
                .doesNotContain("entity_id in (select id from entities)");
    }

    @Test
    void anyScopeAttributeCitusJoinUsesDistinctOnRewrite() {
        String join = latestMapping(EntityKeyType.ATTRIBUTE, "a")
                .toLatestJoin(ctx(true), filter, ENTITIES_QUERY);

        assertThat(join)
                .contains("select distinct on (a.entity_id) * from attribute_kv a")
                .contains("a.entity_id in (select id from entities)")
                .contains("ORDER BY a.entity_id, a.last_update_ts DESC")
                .contains("ON a.entity_id=entities.id AND entities." + DefaultEntityQueryRepository.ATTR_READ_FLAG + " = 1");
    }

    @Test
    void anyScopeAttributePushdownJoinBoundsDistinctOnByReInlinedEntities() {
        String join = latestMapping(EntityKeyType.ATTRIBUTE, "a")
                .toLatestJoin(pushdownCtx(), filter, ENTITIES_QUERY);

        assertThat(join)
                .contains("select distinct on (a.entity_id) * from attribute_kv a")
                // The pushdown arm RE-INLINES the entities subquery (the sibling "entities" derived table is out of
                // scope for this nested subquery), bounding the per-shard DISTINCT ON to the page's entity set.
                .contains("and a.entity_id in (select id from (" + ENTITIES_QUERY + ") entities)")
                .contains("order by a.entity_id, a.last_update_ts DESC")
                .contains("ON a.entity_id = entities.id AND entities." + DefaultEntityQueryRepository.ATTR_READ_FLAG + " = 1")
                // ...not the bare CTE-arm reference to the sibling "entities" relation, which is invisible here.
                .doesNotContain("in (select id from entities)");
    }

    /**
     * A value key filter on an any-scope attribute must be applied INSIDE the DISTINCT ON subquery WHERE
     * (filter-then-pick-latest, matching the citus CTE and plain LATERAL arms), never in the outer join ON
     * clause -- there it would run AFTER the DISTINCT ON already collapsed to the newest row per entity, dropping
     * entities whose newest-scope row fails the filter while an older-scope row passes.
     */
    @Test
    void anyScopeAttributePushdownAppliesValueFilterInsideSubqueryWhere() {
        String join = latestMappingWithNumericFilter(EntityKeyType.ATTRIBUTE, "a")
                .toLatestJoin(pushdownCtx(), filter, ENTITIES_QUERY);

        int onClauseStart = join.indexOf(") as a ON ");
        assertThat(onClauseStart).as("derived table followed by the join ON clause").isPositive();
        String subquery = join.substring(0, onClauseStart);
        String onClause = join.substring(onClauseStart);

        assertThat(subquery)
                .contains("a.long_v > :")
                .contains("a.dbl_v > :")
                .contains("order by a.entity_id, a.last_update_ts DESC");
        assertThat(onClause)
                .doesNotContain("long_v")
                .doesNotContain("dbl_v");
    }

    @Test
    void anyScopeAttributePlainJoinUsesLateral() {
        String join = latestMapping(EntityKeyType.ATTRIBUTE, "a")
                .toLatestJoin(ctx(false), filter, ENTITIES_QUERY);

        assertThat(join)
                .contains("LATERAL")
                .contains("ORDER BY a.last_update_ts DESC limit 1")
                .doesNotContain("distinct on");
    }

    @Test
    void scopedAttributeCitusJoinPushesEntityIdSetDownWithScope() {
        String join = latestMapping(EntityKeyType.SERVER_ATTRIBUTE, "a")
                .toLatestJoin(ctx(true), filter, ENTITIES_QUERY);

        assertThat(join)
                .contains("select * from attribute_kv a")
                .contains("a.attribute_type=" + AttributeScope.SERVER_SCOPE.getId())
                .contains("a.entity_id in (select id from entities)")
                .contains("ON a.entity_id=entities.id AND entities." + DefaultEntityQueryRepository.ATTR_READ_FLAG + " = 1");
    }

    @Test
    void scopedAttributePushdownPlacesScopeAndValueFilterInJoinOn() {
        String join = latestMappingWithNumericFilter(EntityKeyType.SERVER_ATTRIBUTE, "a")
                .toLatestJoin(pushdownCtx(), filter, ENTITIES_QUERY);

        assertThat(join)
                .contains("attribute_kv a ON a.entity_id = entities.id AND entities." + DefaultEntityQueryRepository.ATTR_READ_FLAG + " = 1")
                .contains("a.attribute_type=" + AttributeScope.SERVER_SCOPE.getId())
                .contains("a.long_v > :")
                .doesNotContain("in (select id from entities)")
                .doesNotContain("select * from attribute_kv");
    }

    @Test
    void scopedAttributePlainJoinKeepsReadFilterPrefixAndScope() {
        String join = latestMapping(EntityKeyType.CLIENT_ATTRIBUTE, "a")
                .toLatestJoin(ctx(false), filter, ENTITIES_QUERY);

        assertThat(join)
                .contains("attribute_kv a ON entities." + DefaultEntityQueryRepository.ATTR_READ_FLAG + " = 1 AND")
                .contains("a.attribute_type=" + AttributeScope.CLIENT_SCOPE.getId())
                .doesNotContain("entity_id in (select id from entities)");
    }

    @Test
    void buildLatestJoinsReadsCitusFlagFromContext() {
        String citusJoins = EntityKeyMapping.buildLatestJoins(
                ctx(true), filter, ENTITIES_QUERY, List.of(latestMapping(EntityKeyType.TIME_SERIES, "ts")), false);
        assertThat(citusJoins).contains("ts.entity_id in (select id from entities)");

        String plainJoins = EntityKeyMapping.buildLatestJoins(
                ctx(false), filter, ENTITIES_QUERY, List.of(latestMapping(EntityKeyType.TIME_SERIES, "ts")), false);
        assertThat(plainJoins).doesNotContain("entity_id in (select id from entities)");
    }

}
