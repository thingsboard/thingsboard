// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.extension;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.exception.DataValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtensionSlugUtilsTest {

    @Test
    void slugifyHappyPath() {
        assertThat(ExtensionSlugUtils.slugify("Acme Billing")).isEqualTo("acme-billing");
        assertThat(ExtensionSlugUtils.slugify("Weather_Sync")).isEqualTo("weather-sync");
        assertThat(ExtensionSlugUtils.slugify("  Trim  Me  ")).isEqualTo("trim-me");
        assertThat(ExtensionSlugUtils.slugify("Multiple   spaces & symbols!!")).isEqualTo("multiple-spaces-symbols");
    }

    @Test
    void reservedSlugsAreFlagged() {
        // "router" -> tb-ext-router would collide with the operator-owned shared router objects
        assertThat(ExtensionSlugUtils.isReserved("router")).isTrue();
        assertThat(ExtensionSlugUtils.RESERVED_SLUGS).contains("router");
        assertThat(ExtensionSlugUtils.isReserved("billing")).isFalse();
        assertThat(ExtensionSlugUtils.isReserved(null)).isFalse();
    }

    @Test
    void slugifyEdgeCases() {
        assertThat(ExtensionSlugUtils.slugify(null)).isEqualTo("ext");
        assertThat(ExtensionSlugUtils.slugify("")).isEqualTo("ext");
        assertThat(ExtensionSlugUtils.slugify("***")).isEqualTo("ext");
        assertThat(ExtensionSlugUtils.slugify("---")).isEqualTo("ext");
    }

    @Test
    void slugifyTruncatesAndTrimsTrailingHyphen() {
        String longName = "a".repeat(45);
        String slug = ExtensionSlugUtils.slugify(longName);
        assertThat(slug).hasSize(ExtensionSlugUtils.MAX_BASE_LENGTH);
        assertThat(ExtensionSlugUtils.BASE_PATTERN.matcher(slug).matches()).isTrue();
    }

    @Test
    void normalizeBaseAcceptsValidBase() {
        assertThat(ExtensionSlugUtils.normalizeBase("Acme-Billing")).isEqualTo("acme-billing");
        assertThat(ExtensionSlugUtils.normalizeBase("svc1")).isEqualTo("svc1");
    }

    @Test
    void normalizeBaseRejectsInvalidBase() {
        assertThatThrownBy(() -> ExtensionSlugUtils.normalizeBase("-bad"))
                .isInstanceOf(DataValidationException.class);
        assertThatThrownBy(() -> ExtensionSlugUtils.normalizeBase("bad-"))
                .isInstanceOf(DataValidationException.class);
        assertThatThrownBy(() -> ExtensionSlugUtils.normalizeBase("has space"))
                .isInstanceOf(DataValidationException.class);
        assertThatThrownBy(() -> ExtensionSlugUtils.normalizeBase("a".repeat(41)))
                .isInstanceOf(DataValidationException.class);
        assertThatThrownBy(() -> ExtensionSlugUtils.normalizeBase(null))
                .isInstanceOf(DataValidationException.class);
    }

    @RepeatedTest(50)
    void random4HexIsFourLowercaseHex() {
        String suffix = ExtensionSlugUtils.random4Hex();
        assertThat(suffix).hasSize(4);
        assertThat(suffix).matches("[0-9a-f]{4}");
    }

}
