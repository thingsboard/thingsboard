// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tuya.util;

public enum ServiceRPC {
    GET_STATUS("getStatus", "/v1.0/iot-03/devices/%s/status", false),
    GET_CATEGORY("getCategory", "/v1.0/iot-03/categories/%s/status", false),
    GET_LOGS("getLogs", "/v1.0/iot-03/devices/%s/logs", false),
    GET_REPORT_LOGS("getReportLogs", "/v1.0/iot-03/devices/%s/report-logs", false),
    GET_SPECIFICATION("getSpecification", "/v1.0/iot-03/devices/%s/specification", false),
    GET_FUNCTIONS("getFunctions", "/v1.0/iot-03/devices/%s/functions", false);

    public final String method;
    public final String path;
    public final boolean requiresParameter;

    ServiceRPC(String method, String path, Boolean requiresParameter) {
        this.method = method;
        this.path = path;
        this.requiresParameter = requiresParameter;
    }
}
