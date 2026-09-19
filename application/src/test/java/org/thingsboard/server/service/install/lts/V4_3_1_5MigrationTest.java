// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install.lts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.service.install.lts.V4_3_1_5Migration.SolutionTemplateMove;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V4_3_1_5MigrationTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private V4_3_1_5Migration migration;

    @Test
    void versionIs4315() {
        assertEquals("4.3.1.5", migration.getVersion());
    }

    @Test
    void issuesOneGuardedUpdatePerMove() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        migration.apply();

        verify(jdbcTemplate, times(V4_3_1_5Migration.SOLUTION_TEMPLATE_MOVES.size()))
                .update(eq(V4_3_1_5Migration.REMAP_SQL), any(Object[].class));
        for (SolutionTemplateMove move : V4_3_1_5Migration.SOLUTION_TEMPLATE_MOVES) {
            // SET item_id, item_name WHERE item_id = from AND NOT EXISTS (... item_id = to)
            verify(jdbcTemplate).update(eq(V4_3_1_5Migration.REMAP_SQL),
                    eq(move.toItemId()), eq(move.toItemName()), eq(move.fromItemId()), eq(move.toItemId()));
        }
    }

    @Test
    void movesFormOneHopWithNoDuplicateSources() {
        Set<UUID> sources = new HashSet<>();
        Set<UUID> targets = new HashSet<>();
        for (SolutionTemplateMove move : V4_3_1_5Migration.SOLUTION_TEMPLATE_MOVES) {
            assertNotEquals(move.fromItemId(), move.toItemId(), move.listingSlug());
            assertTrue(sources.add(move.fromItemId()), "duplicate source item: " + move.fromItemId());
            assertTrue(targets.add(move.toItemId()), "duplicate target item: " + move.toItemId());
        }
        // A target that is also a source would make the outcome depend on list order (A -> B, then B -> C).
        sources.retainAll(targets);
        assertTrue(sources.isEmpty(), "items used both as source and target: " + sources);
        assertFalse(V4_3_1_5Migration.SOLUTION_TEMPLATE_MOVES.isEmpty());
    }

}
