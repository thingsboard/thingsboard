// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Schema
@Data
public abstract class BaseCalculatedFieldConfiguration implements ExpressionBasedCalculatedFieldConfiguration {

    protected Map<String, Argument> arguments;
    protected String expression;

    @NotNull
    protected Output output;

    @Override
    public void validate() {
        baseCalculatedFieldRestriction();
        if (arguments.values().stream().anyMatch(Argument::hasRelationQuerySource)) {
            throw new IllegalArgumentException("Calculated field with type: '" + getType() + "' doesn't support relation query configuration!");
        }
    }

    protected void baseCalculatedFieldRestriction() {
        if (arguments.containsKey("ctx")) {
            throw new IllegalArgumentException("Argument name 'ctx' is reserved and cannot be used.");
        }
    }

}
