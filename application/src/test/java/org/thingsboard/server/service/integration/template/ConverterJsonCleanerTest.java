// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConverterJsonCleanerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConverterJsonCleaner cleaner = new ConverterJsonCleaner();

    @Test
    void clean_strips_id_tenantId_createdTime_version_externalId_resets_debugMode()
            throws Exception {
        ObjectNode src = (ObjectNode) mapper.readTree("""
            {
              "id": { "entityType": "CONVERTER", "id": "cuuid" },
              "tenantId": { "entityType": "TENANT", "id": "tuid" },
              "createdTime": 12345,
              "version": 3,
              "externalId": { "entityType": "CONVERTER", "id": "ext" },
              "name": "Acme Uplink",
              "type": "UPLINK",
              "debugMode": false,
              "configuration": { "scriptLang": "TBEL", "tbelDecoder": "..." }
            }
            """);

        JsonNode cleaned = cleaner.clean(src);

        assertThat(cleaned.has("id")).isFalse();
        assertThat(cleaned.has("tenantId")).isFalse();
        assertThat(cleaned.has("createdTime")).isFalse();
        assertThat(cleaned.has("version")).isFalse();
        assertThat(cleaned.has("externalId")).isFalse();
        assertThat(cleaned.get("name").asText()).isEqualTo("Acme Uplink");
        assertThat(cleaned.get("type").asText()).isEqualTo("UPLINK");
        assertThat(cleaned.get("debugMode").asBoolean()).isTrue();         // reset
        assertThat(cleaned.get("configuration").get("scriptLang").asText()).isEqualTo("TBEL");
    }
}
