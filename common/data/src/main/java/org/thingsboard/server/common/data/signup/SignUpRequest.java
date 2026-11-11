// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.signup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.selfregistration.SignUpFieldId;

import java.util.Map;

/**
 * Created by igor on 12/13/16.
 */
@Schema
@Data
@ToString
public class SignUpRequest {

    @Schema(description = "List of sign-up form fields")
    protected Map<SignUpFieldId, String> fields;
    @Schema(description = "Response from reCAPTCHA validation")
    private String recaptchaResponse;
    @Schema(description = "For mobile apps only. Mobile app package name")
    private String pkgName;
    @Schema(description = "For mobile apps only. Mobile app package platform")
    private PlatformType platform;
    @Schema(description = "For mobile apps only. Mobile app secret")
    private String appSecret;

    public SignUpRequest() {
        super();
    }

}
