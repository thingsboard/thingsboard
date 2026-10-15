// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.components;

import org.thingsboard.server.common.data.report.configuration.style.Insets;

public interface LayoutReportComponent extends ReportComponent {

    Insets getMargins();

    Insets getPaddings();

    String getBackground();

    Integer getBorderWidth();

    Integer getBorderRadius();

    String getBorderColor();

}
