// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RelationPathQueryDynamicSourceConfigurationTest {

    @Test
    void typeShouldBeRelationQuery() {
        var cfg = new RelationPathQueryDynamicSourceConfiguration();
        assertThat(cfg.getType()).isEqualTo(CFArgumentDynamicSourceType.RELATION_PATH_QUERY);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void validateShouldThrowWhenLevelsIsNull(List<RelationPathLevel> levels) {
        var cfg = new RelationPathQueryDynamicSourceConfiguration();
        cfg.setLevels(levels);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one relation level must be specified!");
    }

    @Test
    void validateShouldCallValidateForPathLevels() {
        List<RelationPathLevel> levels = new ArrayList<>();

        RelationPathLevel lvl1 = mock(RelationPathLevel.class);
        RelationPathLevel lvl2 = mock(RelationPathLevel.class);
        levels.add(lvl1);
        levels.add(lvl2);

        var cfg = new RelationPathQueryDynamicSourceConfiguration();
        cfg.setLevels(levels);

        assertThatCode(cfg::validate).doesNotThrowAnyException();

        verify(lvl1).validate();
        verify(lvl2).validate();
    }

    @Test
    void resolveEntityIds_whenDirectionFROM_thenReturnsToIds() {
        List<RelationPathLevel> levels = new ArrayList<>();

        RelationPathLevel lvl1 = mock(RelationPathLevel.class);
        RelationPathLevel lvl2 = mock(RelationPathLevel.class);
        levels.add(lvl1);
        levels.add(lvl2);

        when(lvl2.direction()).thenReturn(EntitySearchDirection.FROM);

        EntityRelation rel1 = mock(EntityRelation.class);
        EntityRelation rel2 = mock(EntityRelation.class);

        when(rel1.getTo()).thenReturn(mock(EntityId.class));
        when(rel2.getTo()).thenReturn(mock(EntityId.class));

        var cfg = new RelationPathQueryDynamicSourceConfiguration();
        cfg.setLevels(levels);

        var out = cfg.resolveEntityIds(List.of(rel1, rel2));

        assertThat(out).containsExactly(rel1.getTo(), rel2.getTo());
    }

    @Test
    void resolveEntityIds_whenDirectionTO_thenReturnsFromIds() {
        List<RelationPathLevel> levels = new ArrayList<>();

        RelationPathLevel lvl1 = mock(RelationPathLevel.class);
        RelationPathLevel lvl2 = mock(RelationPathLevel.class);
        levels.add(lvl1);
        levels.add(lvl2);

        when(lvl2.direction()).thenReturn(EntitySearchDirection.TO);

        EntityRelation rel1 = mock(EntityRelation.class);
        EntityRelation rel2 = mock(EntityRelation.class);

        when(rel1.getFrom()).thenReturn(mock(EntityId.class));
        when(rel2.getFrom()).thenReturn(mock(EntityId.class));

        var cfg = new RelationPathQueryDynamicSourceConfiguration();
        cfg.setLevels(levels);

        var out = cfg.resolveEntityIds(List.of(rel1, rel2));

        assertThat(out).containsExactly(rel1.getFrom(), rel2.getFrom());
    }

}
