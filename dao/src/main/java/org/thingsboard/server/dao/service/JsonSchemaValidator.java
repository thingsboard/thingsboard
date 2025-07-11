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
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.thingsboard.common.util.JsonSchemaUtils;
import org.thingsboard.server.common.data.validation.ValidJsonSchema;

public final class JsonSchemaValidator implements ConstraintValidator<ValidJsonSchema, ObjectNode> {

    @Override
    public boolean isValid(ObjectNode schema, ConstraintValidatorContext context) {
        return schema == null || JsonSchemaUtils.isValidJsonSchema(schema);
    }

}
