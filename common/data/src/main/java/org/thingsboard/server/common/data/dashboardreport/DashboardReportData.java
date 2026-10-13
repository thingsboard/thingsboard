// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.dashboardreport;

import lombok.Data;

@Data
public class DashboardReportData {

    private byte[] data;
    private String name;
    private String contentType;

}
