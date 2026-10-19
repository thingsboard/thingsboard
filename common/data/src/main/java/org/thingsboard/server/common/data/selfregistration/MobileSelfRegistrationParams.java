// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
public class MobileSelfRegistrationParams extends AbstractSelfRegistrationParams {

    @Schema(description = "Mobile redirect params.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private MobileRedirectParams redirect;

    @Override
    public SelfRegistrationType getType() {
        return SelfRegistrationType.MOBILE;
    }
}
