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