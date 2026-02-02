/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.mobile.bundle;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema
public class MobileAppBundleInfo extends MobileAppBundle {

    @Schema(description = "Android package name")
    private String androidPkgName;
    @Schema(description = "IOS package name")
    private String iosPkgName;
    @Schema(description = "List of available oauth2 clients")
    private List<OAuth2ClientInfo> oauth2ClientInfos;
    @Schema(description = "Indicates if qr code is available for bundle")
    private boolean qrCodeEnabled;

    public MobileAppBundleInfo(MobileAppBundle mobileApp, String androidPkgName, String iosPkgName, boolean qrCodeEnabled) {
        super(mobileApp);
        this.androidPkgName = androidPkgName;
        this.iosPkgName = iosPkgName;
        this.qrCodeEnabled = qrCodeEnabled;
    }

    public MobileAppBundleInfo(MobileAppBundle mobileApp, String androidPkgName, String iosPkgName, boolean qrCodeEnabled, List<OAuth2ClientInfo> oauth2ClientInfos) {
        super(mobileApp);
        this.androidPkgName = androidPkgName;
        this.iosPkgName = iosPkgName;
        this.qrCodeEnabled = qrCodeEnabled;
        this.oauth2ClientInfos = oauth2ClientInfos;
    }

    public MobileAppBundleInfo() {
        super();
    }

    public MobileAppBundleInfo(MobileAppBundleId mobileAppBundleId) {
        super(mobileAppBundleId);
    }

}
