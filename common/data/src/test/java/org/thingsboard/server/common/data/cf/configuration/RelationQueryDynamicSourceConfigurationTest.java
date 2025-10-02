/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.cf.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RelationQueryDynamicSourceConfigurationTest {

    @Mock
    EntityId rootEntityId;

    @Mock
    EntityRelation rel1;
    @Mock
    EntityRelation rel2;

    @Test
    void typeShouldBeRelationQuery() {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        assertThat(cfg.getType()).isEqualTo(CFArgumentDynamicSourceType.RELATION_QUERY);
    }

    @Test
    void validateShouldThrowWhenMaxLevelLessThanOne() {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(0);
        cfg.setDirection(EntitySearchDirection.FROM);
        cfg.setRelationType(EntityRelation.CONTAINS_TYPE);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relation query dynamic source configuration max relation level can't be less than 1!");
    }

    @Test
    void validateShouldThrowWhenMaxLevelGreaterThanMaxAllowedLevelFromTenantProfile() {
        int maxAllowedRelationLevel = 2;
        int argumentMaxRelationLevel = 3;

        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(argumentMaxRelationLevel);
        cfg.setDirection(EntitySearchDirection.FROM);
        cfg.setRelationType(EntityRelation.CONTAINS_TYPE);

        String testRelationArgument = "testRelationArgument";
        assertThatThrownBy(() -> cfg.validateMaxRelationLevel(testRelationArgument, maxAllowedRelationLevel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Max relation level is greater than configured " +
                            "maximum allowed relation level in tenant profile: " + maxAllowedRelationLevel + " for argument: " + testRelationArgument);
    }

    @Test
    void validateShouldPassValidationWhenMaxLevelLessThanMaxAllowedLevelFromTenantProfile() {
        int maxAllowedRelationLevel = 5;
        int argumentMaxRelationLevel = 2;

        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(argumentMaxRelationLevel);
        cfg.setDirection(EntitySearchDirection.FROM);
        cfg.setRelationType(EntityRelation.CONTAINS_TYPE);

        String testRelationArgument = "testRelationArgument";
        assertThatCode(() -> cfg.validateMaxRelationLevel(testRelationArgument, maxAllowedRelationLevel)).doesNotThrowAnyException();
    }

    @Test
    void validateShouldThrowWhenDirectionIsNull() {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(1);
        cfg.setDirection(null);
        cfg.setRelationType(EntityRelation.CONTAINS_TYPE);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relation query dynamic source configuration direction must be specified!");
    }

    @ParameterizedTest
    @ValueSource(strings = {" "})
    @NullAndEmptySource
    void validateShouldThrowWhenRelationTypeIsNull(String relationType) {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(1);
        cfg.setDirection(EntitySearchDirection.TO);
        cfg.setRelationType(relationType);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relation query dynamic source configuration relation type must be specified!");
    }

    @Test
    void isSimpleRelationTrueWhenLevelIsOneAndEntityTypesEmptyOrNull() {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(1);
        assertThat(cfg.isSimpleRelation()).isTrue();
    }

    @Test
    void isSimpleRelationFalseWhenMaxLevelNotOne() {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(2);
        assertThat(cfg.isSimpleRelation()).isFalse();
    }

    @Test
    void toEntityRelationsQueryShouldThrowForSimpleRelation() {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(1);
        cfg.setFetchLastLevelOnly(false);
        cfg.setDirection(EntitySearchDirection.FROM);
        cfg.setRelationType(EntityRelation.CONTAINS_TYPE);

        assertThatThrownBy(() -> cfg.toEntityRelationsQuery(rootEntityId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Entity relations query can't be created for a simple relation!");
    }

    @Test
    void toEntityRelationsQueryShouldBuildQueryForNonSimpleRelation() {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(2);
        cfg.setFetchLastLevelOnly(true);
        cfg.setDirection(EntitySearchDirection.TO);
        cfg.setRelationType(EntityRelation.MANAGES_TYPE);

        var query = cfg.toEntityRelationsQuery(rootEntityId);

        assertThat(query).isNotNull();
        RelationsSearchParameters params = query.getParameters();
        assertThat(params).isNotNull();
        assertThat(params.getRootId()).isEqualTo(rootEntityId.getId());
        assertThat(params.getDirection()).isEqualTo(EntitySearchDirection.TO);
        assertThat(params.getMaxLevel()).isEqualTo(2);
        assertThat(params.isFetchLastLevelOnly()).isTrue();

        assertThat(query.getFilters()).hasSize(1);
        assertThat(query.getFilters().get(0)).isInstanceOf(RelationEntityTypeFilter.class);
        RelationEntityTypeFilter filter = query.getFilters().get(0);
        assertThat(filter.getRelationType()).isEqualTo(EntityRelation.MANAGES_TYPE);
    }

    @Test
    void resolveEntityIds_whenDirectionFROM_thenReturnsToIds() {
        when(rel1.getTo()).thenReturn(mock(EntityId.class));
        when(rel2.getTo()).thenReturn(mock(EntityId.class));

        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setDirection(EntitySearchDirection.FROM);

        var out = cfg.resolveEntityIds(List.of(rel1, rel2));

        assertThat(out).containsExactly(rel1.getTo(), rel2.getTo());
    }

    @Test
    void resolveEntityIds_whenDirectionTO_thenReturnsFromIds() {
        when(rel1.getFrom()).thenReturn(mock(EntityId.class));
        when(rel2.getFrom()).thenReturn(mock(EntityId.class));

        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setDirection(EntitySearchDirection.TO);

        var out = cfg.resolveEntityIds(List.of(rel1, rel2));

        assertThat(out).containsExactly(rel1.getFrom(), rel2.getFrom());
    }

    @Test
    void validateShouldPassForValidConfig() {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(2);
        cfg.setFetchLastLevelOnly(false);
        cfg.setDirection(EntitySearchDirection.FROM);
        cfg.setRelationType(EntityRelation.CONTAINS_TYPE);

        assertThatCode(cfg::validate).doesNotThrowAnyException();
    }

}
