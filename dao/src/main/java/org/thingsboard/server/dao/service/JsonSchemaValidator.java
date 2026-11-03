// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
