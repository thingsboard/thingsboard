// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.mobile;

import org.thingsboard.server.common.data.mobile.app.MobileAppVersionInfo;
import org.thingsboard.server.common.data.mobile.app.StoreInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientLoginInfo;

import java.util.List;

public record LoginMobileInfo(List<OAuth2ClientLoginInfo> oAuth2ClientLoginInfos,
                              StoreInfo storeInfo,
                              MobileAppVersionInfo versionInfo) {
}
