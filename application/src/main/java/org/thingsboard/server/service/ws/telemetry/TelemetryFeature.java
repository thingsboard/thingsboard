// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.telemetry;

/**
 * Created by ashvayka on 08.05.17.
 */
public enum TelemetryFeature {

    ATTRIBUTES, TIMESERIES;

    public static TelemetryFeature forName(String name) {
        return TelemetryFeature.valueOf(name.toUpperCase());
    }

}
