// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
