// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Schema
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlarmCondition implements Serializable {

    @Valid
    @Schema(description = "JSON array of alarm condition filters")
    private List<AlarmConditionFilter> condition;
    @Schema(description = "JSON object representing alarm condition type")
    private AlarmConditionSpec spec;

}
