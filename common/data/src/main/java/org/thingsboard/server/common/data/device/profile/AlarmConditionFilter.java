// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;

@Schema
@Data
public class AlarmConditionFilter implements Serializable {

    @Valid
    @Schema(description = "JSON object for specifying alarm condition by specific key")
    private AlarmConditionFilterKey key;
    @Schema(description = "String representation of the type of the value", example = "NUMERIC")
    private EntityKeyValueType valueType;
    @NoXss
    @Schema(description = "Value used in Constant comparison. For other types, such as TIME_SERIES or ATTRIBUTE, the predicate condition is used")
    private Object value;
    @Valid
    @Schema(description = "JSON object representing filter condition")
    private KeyFilterPredicate predicate;

}
