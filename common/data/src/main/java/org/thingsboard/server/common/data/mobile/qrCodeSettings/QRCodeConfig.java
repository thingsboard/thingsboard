// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.mobile.qrCodeSettings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.BadgePosition;
import org.thingsboard.server.common.data.validation.NoXss;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class QRCodeConfig {

    private boolean showOnHomePage;
    private boolean badgeEnabled;
    private boolean qrCodeLabelEnabled;
    private BadgePosition badgePosition;
    @NoXss
    private String qrCodeLabel;

}
