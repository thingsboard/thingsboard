// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EntityTypeTest {

    private static final List<EntityType> groupEntityTypes = List.of(
            EntityType.DEVICE,
            EntityType.ASSET,
            EntityType.USER,
            EntityType.CUSTOMER,
            EntityType.ENTITY_VIEW,
            EntityType.DASHBOARD,
            EntityType.EDGE,
            EntityType.AGENT
    );


    // backward-compatibility test
    @Test
    void getNormalNameTest() {
        assertThat(EntityType.ENTITY_VIEW.getNormalName()).isEqualTo("Entity View");
        assertThat(EntityType.ENTITY_GROUP.getNormalName()).isEqualTo("Entity Group");
    }

    @Test
    void getGroupEntityTypesTest() {
        assertThat(EntityType.GROUP_ENTITY_TYPES).hasSameSizeAs(groupEntityTypes);
        assertThat(EntityType.GROUP_ENTITY_TYPES).containsExactlyInAnyOrderElementsOf(groupEntityTypes);
    }

}
