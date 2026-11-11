// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.oauth2.PlatformType;

import static org.thingsboard.server.common.data.selfregistration.CaptchaVersion.V_3;

@Schema
@NoArgsConstructor
public class V3CaptchaParams extends AbstractCaptchaParams {

    public V3CaptchaParams(String siteKey, String logActionName) {
        this.siteKey = siteKey;
        this.logActionName = logActionName;
    }

    @Override
    public String getVersion() {
        return V_3.getName();
    }

    @Override
    public CaptchaParams toInfo(PlatformType platformType) {
        return new V3CaptchaParams(siteKey, logActionName);
    }
}
