/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaId;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.util.Set;

public final class JsonSchemaUtils {

    private JsonSchemaUtils() {
        throw new AssertionError("Can't instantiate utility class");
    }

    /**
     * Validates that the provided JsonNode is a valid JSON Schema (Draft 2020-12).
     *
     * @param schemaNode the JSON Schema document as a JsonNode
     * @return true if the schema is well-formed, false otherwise
     */
    public static boolean isValidJsonSchema(JsonNode schemaNode) {
        Set<ValidationMessage> errors = JsonSchemaFactory
                .getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(SchemaLocation.of(SchemaId.V202012))
                .validate(schemaNode);
        return errors.isEmpty();
    }

}
