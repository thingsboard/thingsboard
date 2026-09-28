// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.components;

import org.thingsboard.server.common.data.report.configuration.DataSource;

import java.util.List;

public interface DataReportComponent extends ReportComponent {

    List<DataSource> getDataSources();

}
