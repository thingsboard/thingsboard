// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TbVersionUtilsTest {

    @Test
    void compareEqualVersions() {
        assertThat(TbVersionUtils.compare("3.6.0", "3.6.0")).isZero();
    }

    @Test
    void compareTreatsMissingTrailingComponentsAsZero() {
        assertThat(TbVersionUtils.compare("3.6", "3.6.0")).isZero();
        assertThat(TbVersionUtils.compare("3.6.0", "3.6")).isZero();
    }

    @Test
    void compareOrdersByComponentNotLexically() {
        assertThat(TbVersionUtils.compare("3.6.1", "3.6.0")).isPositive();
        assertThat(TbVersionUtils.compare("3.6.0", "3.6.1")).isNegative();
        assertThat(TbVersionUtils.compare("3.10.0", "3.9.0")).isPositive();
    }

    @Test
    void compareTreatsNullAndEmptyAsLowest() {
        assertThat(TbVersionUtils.compare(null, "0.0.0")).isZero();
        assertThat(TbVersionUtils.compare("", "0")).isZero();
        assertThat(TbVersionUtils.compare(null, "1.0.0")).isNegative();
        assertThat(TbVersionUtils.compare("1.0.0", null)).isPositive();
    }

    @Test
    void compareToleratesNonNumericComponents() {
        assertThat(TbVersionUtils.compare("latest", "0")).isZero();
        assertThat(TbVersionUtils.compare("stable", "1.0.0")).isNegative();
        assertThat(TbVersionUtils.compare("3.8-stable", "3.7.0")).isPositive();
        assertThat(TbVersionUtils.compare("1.0.0PE", "1.0.0")).isZero();
        assertThat(TbVersionUtils.compare("latest-arm64", "latest")).isZero();
    }

    @Test
    void extractStartingDigitsStripsSuffix() {
        assertThat(TbVersionUtils.extractStartingDigits("4.3.1.1PE-SNAPSHOT")).isEqualTo("4.3.1.1");
        assertThat(TbVersionUtils.extractStartingDigits("4.3.1.2EDGEPE")).isEqualTo("4.3.1.2");
        assertThat(TbVersionUtils.extractStartingDigits("3.8-stable")).isEqualTo("3.8");
    }

    @Test
    void extractStartingDigitsHandlesNullAndPlainVersion() {
        assertThat(TbVersionUtils.extractStartingDigits(null)).isEmpty();
        assertThat(TbVersionUtils.extractStartingDigits("4.3.1")).isEqualTo("4.3.1");
    }
}
