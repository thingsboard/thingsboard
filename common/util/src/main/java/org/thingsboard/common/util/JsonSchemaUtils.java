// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaId;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.util.Set;

public final class JsonSchemaUtils {

    private JsonSchemaUtils() {}

    /**
     * Validates that the provided ObjectNode is a valid JSON Schema (Draft 2020-12).
     *
     * @param schemaNode the JSON Schema document as an ObjectNode
     * @return true if the schema is well-formed, false otherwise
     */
    public static boolean isValidJsonSchema(ObjectNode schemaNode) {
        Set<ValidationMessage> errors = JsonSchemaFactory
                .getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(SchemaLocation.of(SchemaId.V202012))
                .validate(schemaNode);
        return errors.isEmpty();
    }

}
