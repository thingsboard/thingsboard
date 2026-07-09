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
package org.thingsboard.server.service.iot_hub;

import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstallReportTest {

    private static String sha256(String s) throws Exception {
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(d.digest(s.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void buildInstallReport_hashesMatchSaltedFormula() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        InstallReport r = DefaultIotHubService.buildInstallReport(tenantId, userId, "4.2.0", "CE");

        assertEquals(sha256(tenantId.toString() + tenantId), r.tenantHash());
        assertEquals(sha256(tenantId.toString() + userId), r.userHash());
        assertEquals("4.2.0", r.tbVersion());
        assertEquals("CE", r.edition());
    }
}
