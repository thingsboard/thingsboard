// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import lombok.Data;

@Data
public class FeaturesInfo {
    boolean isEmailEnabled;
    boolean isSmsEnabled;
    boolean isNotificationEnabled;
    boolean isOauthEnabled;
    boolean isTwoFaEnabled;
}
