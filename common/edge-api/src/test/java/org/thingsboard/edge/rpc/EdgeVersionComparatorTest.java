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
package org.thingsboard.edge.rpc;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;

import static org.assertj.core.api.Assertions.assertThat;

class EdgeVersionComparatorTest {

    @Test
    void compare_sameVersion_returnsZero() {
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_3_0, EdgeVersion.V_3_3_0)).isEqualTo(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_4_0_0, EdgeVersion.V_4_0_0)).isEqualTo(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_4_2_1_2, EdgeVersion.V_4_2_1_2)).isEqualTo(0);
    }

    @Test
    void compare_majorVersionDifference_returnsCorrectOrder() {
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_3_0, EdgeVersion.V_4_0_0)).isLessThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_4_0_0, EdgeVersion.V_3_3_0)).isGreaterThan(0);
    }

    @Test
    void compare_minorVersionDifference_returnsCorrectOrder() {
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_3_0, EdgeVersion.V_3_6_0)).isLessThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_6_0, EdgeVersion.V_3_3_0)).isGreaterThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_4_0_0, EdgeVersion.V_4_1_0)).isLessThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_4_1_0, EdgeVersion.V_4_0_0)).isGreaterThan(0);
    }

    @Test
    void compare_patchVersionDifference_returnsCorrectOrder() {
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_6_0, EdgeVersion.V_3_6_1)).isLessThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_6_1, EdgeVersion.V_3_6_0)).isGreaterThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_6_1, EdgeVersion.V_3_6_2)).isLessThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_6_2, EdgeVersion.V_3_6_4)).isLessThan(0);
    }

    @Test
    void compare_fourPartVersion_returnsCorrectOrder() {
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_4_2_0, EdgeVersion.V_4_2_1_2)).isLessThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_4_2_1_2, EdgeVersion.V_4_2_0)).isGreaterThan(0);
    }

    @Test
    void compare_threePartVsFourPart_treatsImplicitZero() {
        // V_4_2_0 should be less than V_4_2_1_2 (4.2.0.0 < 4.2.1.2)
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_4_2_0, EdgeVersion.V_4_2_1_2)).isLessThan(0);
    }

    @Test
    void getNewestEdgeVersion_excludesLatestAndUnrecognized() {
        EdgeVersion newest = EdgeVersionComparator.getNewestEdgeVersion();
        assertThat(newest).isNotNull();
        assertThat(newest).isNotEqualTo(EdgeVersion.V_LATEST);
        assertThat(newest).isNotEqualTo(EdgeVersion.UNRECOGNIZED);
    }
}
