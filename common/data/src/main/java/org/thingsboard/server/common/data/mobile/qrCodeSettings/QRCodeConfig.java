// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.mobile.qrCodeSettings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
