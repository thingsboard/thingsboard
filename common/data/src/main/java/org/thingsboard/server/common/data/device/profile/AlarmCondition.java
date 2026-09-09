// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Schema(hidden = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Deprecated
public class AlarmCondition implements Serializable {

    @Valid
    @ArraySchema(schema = @Schema(ref = "#/components/schemas/AlarmConditionFilter"))
    private List<AlarmConditionFilter> condition;
    @Schema(description = "JSON object representing alarm condition type")
    private AlarmConditionSpec spec;

}
