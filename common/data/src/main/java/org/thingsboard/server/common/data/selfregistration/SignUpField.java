// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Schema
@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class SignUpField {

    @Schema(description = "Signup field id")
    @NotNull
    private SignUpFieldId id;
    @Schema(description = "Signup field label")
    @NotNull
    private String label;
    @Schema(description = "Indicates if field is required")
    private boolean required;

}
