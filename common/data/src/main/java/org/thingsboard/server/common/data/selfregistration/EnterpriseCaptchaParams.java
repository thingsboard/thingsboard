// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.oauth2.PlatformType;

import static org.thingsboard.server.common.data.selfregistration.CaptchaVersion.ENTERPRISE;

@Schema
@Data
@NoArgsConstructor
public class EnterpriseCaptchaParams implements CaptchaParams {

    @Schema(description = "Your Google Cloud project ID")
    protected String projectId;

    @Schema(description = "Service account credentials")
    private String serviceAccountCredentials;
    @Schema(description = "Service account credentials file name")
    private String serviceAccountCredentialsFileName;
    @Schema(description = "The reCAPTCHA key associated with android app.")
    protected String androidKey;
    @Schema(description = "The reCAPTCHA key associated with iOS app.")
    protected String iosKey;
    @Schema(description = "Optional action name used for logging")
    protected String logActionName;


    public EnterpriseCaptchaParams(String androidKey, String iOSKey, String logActionName) {
        this.androidKey = androidKey;
        this.iosKey = iOSKey;
        this.logActionName = logActionName;
    }

    @Override
    public String getVersion() {
        return ENTERPRISE.getName();
    }

    @Override
    public CaptchaParams toInfo(PlatformType platformType) {
        if (platformType == PlatformType.ANDROID) {
            return new EnterpriseCaptchaParams(androidKey, null, logActionName);
        } else if (platformType == PlatformType.IOS) {
            return new EnterpriseCaptchaParams( null, iosKey, logActionName);
        }
        return null;
    }
}
