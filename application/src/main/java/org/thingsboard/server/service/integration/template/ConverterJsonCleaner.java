// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Stateless cleaner for a Converter entity JSON tree, applied before annotation-driven
 * tokenization at IoT Hub package export time.
 */
@Component
public class ConverterJsonCleaner {

    private static final Set<String> FIELDS_TO_REMOVE = Set.of(
            "id", "tenantId", "createdTime", "version", "externalId"
    );

    /** Returns a fresh JsonNode with the cleanup rules applied; does not mutate input. */
    public JsonNode clean(JsonNode source) {
        ObjectNode out = source.deepCopy();
        FIELDS_TO_REMOVE.forEach(out::remove);
        out.put("debugMode", true);
        return out;
    }
}
