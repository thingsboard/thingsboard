// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.mobile;

import org.thingsboard.server.common.data.mobile.app.MobileAppVersionInfo;
import org.thingsboard.server.common.data.mobile.app.StoreInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientLoginInfo;
import org.thingsboard.server.common.data.selfregistration.SignUpSelfRegistrationParams;

import java.util.List;

public record LoginMobileInfo(List<OAuth2ClientLoginInfo> oAuth2ClientLoginInfos,
                              SignUpSelfRegistrationParams selfRegistrationParams,
                              StoreInfo storeInfo,
                              MobileAppVersionInfo versionInfo) {
}
