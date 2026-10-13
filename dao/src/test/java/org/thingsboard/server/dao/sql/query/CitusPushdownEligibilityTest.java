// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.query;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.query.AssetTypeFilter;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.query.EntityViewTypeFilter;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;

import static org.assertj.core.api.Assertions.assertThat;

class CitusPushdownEligibilityTest {

    @Test
    void singleEntityTableFiltersAreEligible() {
        assertThat(DefaultEntityQueryRepository.isSingleEntityTableFilter(new DeviceTypeFilter())).isTrue();
        assertThat(DefaultEntityQueryRepository.isSingleEntityTableFilter(new EntityListFilter())).isTrue();
        assertThat(DefaultEntityQueryRepository.isSingleEntityTableFilter(new AssetTypeFilter())).isTrue();
        assertThat(DefaultEntityQueryRepository.isSingleEntityTableFilter(new EntityViewTypeFilter())).isTrue();
    }

    @Test
    void relationsQueryIsNotEligible() {
        assertThat(DefaultEntityQueryRepository.isSingleEntityTableFilter(new RelationsQueryFilter())).isFalse();
    }
}
