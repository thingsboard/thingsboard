// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link DaoUtil#constraintNameMatches}: the exact case-insensitive comparison
 * is tried first (so plain PostgreSQL behavior is unchanged), and only if it misses is a trailing
 * Citus shard-placement suffix ({@code _<digits>}) stripped and the comparison retried.
 */
class DaoUtilTest {

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            // exact match, case-insensitive
            "fk_asset_profile,           fk_asset_profile,          true",
            "FK_ASSET_PROFILE,           fk_asset_profile,          true",
            // Citus shard suffix stripped when the exact comparison misses
            "fk_asset_profile_102022,    fk_asset_profile,          true",
            "RULE_CHAIN_EXTERNAL_ID_UNQ_KEY_102011, rule_chain_external_id_unq_key, true",
            // exact match wins before any strip, even when the expected name itself ends in digits
            "fk_constraint_123,          fk_constraint_123,         true",
            // a legitimate digit tail must not make the name match a different expected constraint
            "fk_rule_node_123,           fk_rule_chain,             false",
            // only one trailing digit group is stripped
            "fk_asset_profile_102022_1,  fk_asset_profile,          false",
            // non-suffixed names pass through unchanged and mismatches stay mismatches
            "fk_device_profile,          fk_asset_profile,          false",
            // null-safe
            "NULL,                       fk_asset_profile,          false"
    })
    void constraintNameMatchesContract(String actual, String expected, boolean matches) {
        assertThat(DaoUtil.constraintNameMatches(actual, expected)).isEqualTo(matches);
    }

}
