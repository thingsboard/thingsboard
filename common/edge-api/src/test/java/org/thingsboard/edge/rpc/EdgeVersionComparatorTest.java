// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

    @Test
    void compare_vLatest_treatedAsNewestVersion() {
        EdgeVersion newest = EdgeVersionComparator.getNewestEdgeVersion();
        // V_LATEST equals the newest version
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_LATEST, newest)).isEqualTo(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(newest, EdgeVersion.V_LATEST)).isEqualTo(0);
        // V_LATEST is greater than older versions
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_LATEST, EdgeVersion.V_3_3_0)).isGreaterThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_3_0, EdgeVersion.V_LATEST)).isLessThan(0);
    }

    @Test
    void compare_vLatest_withItself_returnsZero() {
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_LATEST, EdgeVersion.V_LATEST)).isEqualTo(0);
    }

    @Test
    void compare_unrecognized_isLessThanAnyVersion() {
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.UNRECOGNIZED, EdgeVersion.V_3_3_0)).isLessThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.UNRECOGNIZED, EdgeVersion.V_LATEST)).isLessThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_3_0, EdgeVersion.UNRECOGNIZED)).isGreaterThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_LATEST, EdgeVersion.UNRECOGNIZED)).isGreaterThan(0);
    }

    @Test
    void compare_unrecognized_withItself_returnsZero() {
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.UNRECOGNIZED, EdgeVersion.UNRECOGNIZED)).isEqualTo(0);
    }
}
