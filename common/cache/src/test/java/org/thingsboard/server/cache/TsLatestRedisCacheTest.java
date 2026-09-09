// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.assertThat;

class VersionedRedisTbCacheTest {

    @Test
    void testUpsertTsLatestLUAScriptHash() {
        assertThat(getSHA1(VersionedRedisTbCache.SET_VERSIONED_VALUE_LUA_SCRIPT)).isEqualTo(new String(VersionedRedisTbCache.SET_VERSIONED_VALUE_SHA));
    }

    @SneakyThrows
    String getSHA1(byte[] script) {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(script);

        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

}