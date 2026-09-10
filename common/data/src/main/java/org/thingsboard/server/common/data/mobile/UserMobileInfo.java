// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.HomeDashboardInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.mobile.app.MobileAppVersionInfo;
import org.thingsboard.server.common.data.mobile.app.StoreInfo;


public record UserMobileInfo(User user,
                             StoreInfo storeInfo,
                             MobileAppVersionInfo versionInfo,
                             HomeDashboardInfo homeDashboardInfo,
                             JsonNode pages) {
}
