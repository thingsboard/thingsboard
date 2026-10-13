// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.oauth2.PlatformType;

import static org.thingsboard.server.common.data.selfregistration.CaptchaVersion.V_2;

@Schema
@NoArgsConstructor
public class V2CaptchaParams extends AbstractCaptchaParams {

    public V2CaptchaParams(String siteKey) {
        this.siteKey = siteKey;
    }

    @Override
    public String getVersion() {
        return V_2.getName();
    }

    @Override
    public CaptchaParams toInfo(PlatformType platformType) {
        return new V2CaptchaParams(siteKey);
    }
}
