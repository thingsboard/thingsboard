// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cf.configuration.OutputType;

@Data
public final class CalculatedFieldResult {

    private final OutputType type;
    private final AttributeScope scope;
    private final JsonNode result;

    public boolean isEmpty() {
        return result == null || result.isMissingNode() || result.isNull() ||
                (result.isObject() && result.isEmpty()) ||
                (result.isArray() && result.isEmpty()) ||
                (result.isTextual() && result.asText().isEmpty());
    }

}
