// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.context;

import lombok.Data;

@Data
public class HeaderFooterRenderLayout {
    private String htmlContent;
    private String firstPageHtmlContent;
    private boolean enabled;
    private boolean firstPageEnabled;
    private int heightPx;
    private int firstPageHeightPx;
}
