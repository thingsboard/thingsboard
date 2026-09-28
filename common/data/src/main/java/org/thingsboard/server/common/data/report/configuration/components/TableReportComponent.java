// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.components;

import org.thingsboard.server.common.data.report.configuration.TableSortOrder;
import org.thingsboard.server.common.data.report.configuration.style.Heading;

public interface TableReportComponent extends DataReportComponent {

    boolean isShowTableHeading();

    Heading getTableHeading();

    TableSortOrder getTableSortOrder();

}
