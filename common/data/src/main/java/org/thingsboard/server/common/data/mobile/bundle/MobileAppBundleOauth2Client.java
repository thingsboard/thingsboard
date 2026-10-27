// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.mobile.bundle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MobileAppBundleOauth2Client {

    private MobileAppBundleId mobileAppBundleId;
    private OAuth2ClientId oAuth2ClientId;

}
