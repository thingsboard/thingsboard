// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.data;

public class Latencies {

    public static final String WS_CONNECT = "wsConnect";
    public static final String WS_SUBSCRIBE = "wsSubscribe";
    public static final String LOG_IN = "logIn";
    public static final String EDQS_QUERY = "edqsQuery";

    public static String request(String key) {
        return String.format("%sRequest", key);
    }

    public static String wsUpdate(String key) {
        return String.format("%sWsUpdate", key);
    }

}
