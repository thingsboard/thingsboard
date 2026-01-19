/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
