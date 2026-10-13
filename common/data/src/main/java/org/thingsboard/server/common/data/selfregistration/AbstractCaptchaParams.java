// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema
@Data
@NoArgsConstructor
public abstract class AbstractCaptchaParams implements CaptchaParams {

    @Schema(description = "Captcha site key for 'I'm not a robot' validation")
    protected String siteKey;
    @Schema(description = "Optional action name used for logging (for captcha version 'v3' and 'enterprise')")
    protected String logActionName;
    @Schema(description = "Secret key to validate the Captcha. Should match the Captcha Site Key.")
    private String secretKey;

}
